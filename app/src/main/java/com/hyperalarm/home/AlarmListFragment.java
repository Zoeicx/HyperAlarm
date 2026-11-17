package com.hyperalarm.home;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.hyperalarm.R;
import com.hyperalarm.common.alarm.AlarmContract;
import com.hyperalarm.common.alarm.AlarmItem;
import com.hyperalarm.common.log.AndroidLog;
import com.hyperalarm.common.alarm.BridgeContract;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.base.BasePreferenceFragment;
import com.hyperalarm.common.utils.AppSettingsStore;
import com.hyperalarm.common.utils.PrefsBridge;
import fan.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.hyperalarm.common.utils.ThreadUtils;

public class AlarmListFragment extends BasePreferenceFragment {
    /** {@code daysofweek} values the clock uses for its named presets (see AGENTS §11.2). */
    private static final int WORKDAY_PRESET = 128;
    private static final int HOLIDAY_PRESET = 256;
    private static final int SHIFT_DAYS = 512;

    private final List<AlarmItem> rows = new ArrayList<>();
    private PreferenceScreen screen;
    private boolean showAll;
    private boolean selectionMode;
    private final Set<Integer> selected = new HashSet<>();
    private static volatile List<AlarmItem> sCachedRows;
    private static volatile long sCachedAt;
    /** Generation of the load in flight, -1 when idle. A single flag could not be released by a
     *  load whose result was discarded, which left the list stuck on "loading" forever. */
    private static volatile int sLoadingGeneration = -1;
    /** Incremented for every load; only the newest result may be applied. */
    private static final java.util.concurrent.atomic.AtomicInteger sLoadGeneration =
            new java.util.concurrent.atomic.AtomicInteger();
    /** Set when a change arrives while a load is running: run once more when it finishes. */
    private static volatile boolean sReloadPending;
    /** True when the last load failed instead of returning an empty list. */
    private static volatile boolean sLoadFailed;
    private static volatile AlarmListFragment sActive;
    private static volatile Context sAppContext;
    private OnBackPressedCallback backCallback;
    private NextAlarmPreference nextHeader;
    /**
     * Live updates reach us through the clock process.
     *
     * <p>Watching the provider directly is not possible: it requires
     * {@code miui.permission.ACCESS_ALARM_PROVIDER}, which only the clock holds (registering an
     * observer throws SecurityException). The hook process watches the tables and broadcasts
     * {@code ALARMS_CHANGED} instead, so the list follows a change - the clock firing an alarm,
     * another app touching the provider - without the app polling.</p>
     */
    private BroadcastReceiver alarmChangeReceiver;
    private final Handler observerHandler = new Handler(Looper.getMainLooper());
    /** Collapses a burst of change broadcasts into one reload. */
    private static final long OBSERVER_DEBOUNCE_MS = 400L;
    /** A broadcast this soon after our own write is that write echoing back. */
    private static final long OWN_WRITE_WINDOW_MS = 2000L;
    private final Runnable observerReload = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            invalidate();
            loadAlarms(false);
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            (prefs, key) -> {
                if (AppSettingsStore.PREF_SHOW_ALL_ALARMS.equals(key)) {
                    showAll = AppSettingsStore.isShowAllAlarmsEnabled(requireContext());
                    rebuildPreferences();
                }
            };

    @Override
    public int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected int getListViewPaddingBottom() {
        return Math.round(120 * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        sActive = this;
        if (sAppContext == null) sAppContext = requireContext().getApplicationContext();
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (selectionMode) exitSelection();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
        showAll = AppSettingsStore.isShowAllAlarmsEnabled(requireContext());
        screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);
        if (sCachedRows != null) {
            rows.addAll(sCachedRows);
        }
        rebuildPreferences();
        loadAlarms(false);
    }

    /** Reloads the list that is currently on screen, if any. */
    public static void reloadActive() {
        AlarmListFragment active = sActive;
        if (active != null && active.isAdded()) active.loadAlarms(true);
    }

    /**
     * Marks the cache stale without dropping it.
     *
     * <p>Clearing it meant that a failed retry showed an empty list: the fallback for a failed load
     * is "keep what the user was looking at", and there was nothing left to keep.</p>
     */
    /**
     * Re-reads the list filters and repaints.
     *
     * <p>The setting is written through the settings provider, not through the preferences the list
     * listens to, and the three pages stay alive ({@code setOffscreenPageLimit(3)}), so coming back
     * to the alarm page does not reliably run onResume. Without this the switch looked like it did
     * nothing until the next reload or a process restart.</p>
     */
    public static void refreshFiltersActive() {
        AlarmListFragment active = sActive;
        if (active == null || !active.isAdded() || active.screen == null) return;
        try {
            active.showAll = AppSettingsStore.isShowAllAlarmsEnabled(active.requireContext());
            active.rebuildPreferences();
        } catch (Throwable t) {
            AndroidLog.w("AlarmListFragment", "filter refresh failed: " + t);
        }
    }

    public static void invalidate() {
        sCachedAt = 0L;
    }

    /**
     * A write is on its way to the clock, so any load in flight read the state before it.
     *
     * <p>Bumping the generation makes that load discard its result. Without it the stale rows could
     * land after the write, and the self-echo suppression (see {@code lastWriteAt}) then dropped the
     * broadcast that would have corrected them.</p>
     */
    static void onWriteStarted() {
        sLoadGeneration.incrementAndGet();
        sCachedAt = 0L;
    }

    /** The clock accepted a write: show what it reports now, not what the optimistic path guessed. */
    static void onWriteCommitted() {
        sCachedAt = 0L;
        reloadActive();
    }

    public static void optimisticSave(int id, int hour, int minute, String label, int days, boolean create) {
        try {
            if (sCachedRows == null) sCachedRows = new ArrayList<>();
            AlarmItem row = null;
            for (AlarmItem item : sCachedRows) {
                if (item.id == id) {
                    row = item;
                    break;
                }
            }
            sortRows(sCachedRows);
            if (row == null && create) {
                row = new AlarmItem();
                row.id = id;
                sCachedRows.add(row);
            }
            if (row != null) {
                row.hour = hour;
                row.minute = minute;
                row.label = label == null ? "" : label;
                row.days = days;
            }
            sCachedAt = System.currentTimeMillis();
            final AlarmListFragment active = sActive;
            if (active != null && active.screen != null) {
                active.rows.clear();
                active.rows.addAll(sCachedRows);
                active.rebuildPreferences();
            }
        } catch (Throwable ignored) {
        }
    }

    private void toggleSelection(int alarmId) {
        if (!selectionMode) {
            selectionMode = true;
            selected.clear();
        }
        if (!selected.add(alarmId)) selected.remove(alarmId);
        refreshSelection();
    }

    /** Applies the selection state to every row of the current screen. */
    private void refreshSelection() {
        if (screen == null) return;
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference p = screen.getPreference(i);
            if (p instanceof AlarmPreference) {
                AlarmPreference ap = (AlarmPreference) p;
                ap.setSelected(selected.contains(ap.getKey() == null ? -1 : parseKey(ap.getKey())));
                ap.refresh();
            }
        }
        // Only while selecting: this also runs after every rebuild, and touching the title bar then
        // would put the delete button on a normal, non-selecting list.
        if (!selectionMode) return;
        HyperAlarmHomeFragment parent = parent();
        if (parent != null) {
            parent.showSelectionActionBar(selected.size(), this::confirmDelete, this::exitSelection);
        }
    }

    private int parseKey(String key) {
        try {
            return Integer.parseInt(key.substring(key.indexOf('-') + 1));
        } catch (Throwable t) {
            return -1;
        }
    }

    private HyperAlarmHomeFragment parent() {
        return getParentFragment() instanceof HyperAlarmHomeFragment
                ? (HyperAlarmHomeFragment) getParentFragment() : null;
    }

    private void exitSelection() {
        selectionMode = false;
        selected.clear();
        if (backCallback != null) backCallback.setEnabled(false);
        if (screen != null) {
            for (int i = 0; i < screen.getPreferenceCount(); i++) {
                Preference p = screen.getPreference(i);
                if (p instanceof AlarmPreference) {
                    AlarmPreference ap = (AlarmPreference) p;
                    ap.setSelected(false);
                    ap.refresh();
                }
            }
        }
        HyperAlarmHomeFragment parent = parent();
        if (parent != null) parent.restoreActionBar();
    }

    private void confirmDelete() {
        if (selected.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.alarm_delete_title)
                .setMessage(requireContext().getString(R.string.alarm_delete_message, selected.size()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    final java.util.List<Integer> ids = new java.util.ArrayList<>(selected);
                    final java.util.List<AlarmItem> deleted = new java.util.ArrayList<>();
                    for (int id : ids) {
                        AlarmItem row = findRow(id);
                        if (row != null) deleted.add(row);
                    }
                    // Remove from the in-memory list immediately; DB delete is async.
                    for (AlarmItem row : deleted) {
                        rows.remove(row);
                        if (sCachedRows != null) sCachedRows.remove(row);
                    }
                    rebuildPreferences();
                    exitSelection();
                    // Captured before leaving the main thread: getActivity() is not valid from a
                    // worker, and the fragment may be gone by the time the deletes finish.
                    final android.content.Context appContext = requireContext().getApplicationContext();
                    final AlarmListFragment self = this;
                    ThreadUtils.runOnBackground(() -> {
                        boolean allOk = true;
                        for (AlarmItem row : deleted) {
                            // Wait for each acknowledgement: reloading before the writes land would
                            // bring the rows back from the clock's still-unchanged table.
                            allOk &= AlarmHookWriter.deleteNow(appContext, row.id,
                                    row.shiftGroupRowId > 0);
                        }
                        final boolean ok = allOk;
                        ThreadUtils.postOnMainThread(() -> {
                            invalidate();
                            if (self.isAdded()) {
                                loadAlarms(true);
                                if (!ok) {
                                    Toast.makeText(self.requireContext(), R.string.alarm_delete_failed,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    });
                })
                .show();
    }

    private AlarmItem findRow(int id) {
        for (AlarmItem row : rows) if (row.id == id) return row;
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        showAll = AppSettingsStore.isShowAllAlarmsEnabled(requireContext());
        PrefsBridge.getSharedPreferences().registerOnSharedPreferenceChangeListener(prefListener);
        startObserving();
        if (sCachedRows != null && rows.isEmpty()) {
            rows.addAll(sCachedRows);
            rebuildPreferences();
        }
        // The receiver is unregistered while paused, so changes made in the meantime were never
        // delivered: a fresh cache here does not mean the list is current.
        loadAlarms(true);
    }

    private void startObserving() {
        if (alarmChangeReceiver != null) return;
        alarmChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Our own write already refreshed the list; this is its echo.
                if (System.currentTimeMillis() - ClockBridge.lastWriteAt() < OWN_WRITE_WINDOW_MS) {
                    return;
                }
                observerHandler.removeCallbacks(observerReload);
                observerHandler.postDelayed(observerReload, OBSERVER_DEBOUNCE_MS);
            }
        };
        try {
            // EXPORTED on purpose: the sender is the clock process, a different app. A forged
            // broadcast can only make the list reload, and the reload itself is authenticated.
            androidx.core.content.ContextCompat.registerReceiver(requireContext(), alarmChangeReceiver,
                    new IntentFilter(BridgeContract.ACTION_ALARMS_CHANGED),
                    androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
        } catch (Throwable t) {
            alarmChangeReceiver = null;
        }
    }

    private void stopObserving() {
        observerHandler.removeCallbacks(observerReload);
        if (alarmChangeReceiver == null) return;
        try {
            requireContext().unregisterReceiver(alarmChangeReceiver);
        } catch (Throwable ignored) {
        }
        alarmChangeReceiver = null;
    }

    @Override
    public void onPause() {
        PrefsBridge.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefListener);
        stopObserving();
        // Multi-select belongs to this page being in front: leaving it keeps a floating delete
        // button over the settings page, acting on rows the user can no longer see.
        if (selectionMode) exitSelection();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (sActive == this) sActive = null;
        if (nextHeader != null) nextHeader.stopTicking();
        super.onDestroy();
    }

    private void rebuildPreferences() {
        if (screen == null) return;
        screen.removeAll();
        nextHeader = new NextAlarmPreference(requireContext());
        screen.addPreference(nextHeader);
        List<AlarmItem> visible = new ArrayList<>();
        for (AlarmItem row : rows) {
            if (showAll || isScriptAlarm(row)) visible.add(row);
        }
        sortRows(visible);
        nextHeader.setText(nextAlarmText(visible));
        if (sLoadFailed && !visible.isEmpty()) {
            // Rows are on screen and they are the previous snapshot: say so, and let a tap retry.
            Preference stale = new Preference(requireContext());
            stale.setTitle(R.string.alarm_load_failed);
            stale.setSummary(R.string.alarm_load_failed_hint);
            stale.setPersistent(false);
            stale.setOnPreferenceClickListener(preference -> {
                loadAlarms(true);
                return true;
            });
            screen.addPreference(stale);
        }
        // The card counts down in relative terms, so it has to repaint itself every minute; the
        // supplier recomputes from this snapshot, which also re-picks the alarm once one passes.
        final List<AlarmItem> snapshot = new ArrayList<>(visible);
        nextHeader.setTextSupplier(() -> nextAlarmText(snapshot));
        if (visible.isEmpty()) {
            Preference empty = new Preference(requireContext());
            empty.setTitle(requireContext().getString(
                    sLoadingGeneration >= 0 ? R.string.loading
                            : (sLoadFailed ? R.string.alarm_load_failed : R.string.no_alarms)));
            if (sLoadFailed && sLoadingGeneration < 0) {
                empty.setSummary(R.string.alarm_load_failed_hint);
                empty.setEnabled(true);
                empty.setOnPreferenceClickListener(preference -> {
                    loadAlarms(true);
                    return true;
                });
            } else {
                empty.setEnabled(false);
            }
            screen.addPreference(empty);
            return;
        }
        for (AlarmItem row : visible) {
            AlarmPreference item = new AlarmPreference(requireContext(), row.id, row.hour,
                    row.minute, row.days, row.label, row.enabled, ShiftSummary.of(requireContext(), row),
                    row.shiftGroupRowId, new AlarmPreference.SelectionHost() {
                        @Override
                        public boolean isSelectionMode() {
                            return selectionMode;
                        }

                        @Override
                        public boolean isSelected(int alarmId) {
                            return selected.contains(alarmId);
                        }

                        @Override
                        public void onClick(int alarmId) {
                            toggleSelection(alarmId);
                        }

                        @Override
                        public void onLongClick(int alarmId) {
                            if (!selectionMode) {
                                selectionMode = true;
                                selected.clear();
                                if (backCallback != null) backCallback.setEnabled(true);
                            }
                            selected.add(alarmId);
                            refreshSelection();
                        }
                    }, () ->
                    new AlarmDetailBottomSheet(requireActivity())
                            .showAlarm(row.id, row.hour, row.minute, row.label, row.days,
                                    ShiftSummary.of(requireActivity(), row),
                                    row.shiftGroupRowId, row.shiftAlarmRowId, row.shiftStartTime,
                                    row.shiftDuration, row.shiftHours, row.shiftMinutes,
                                    row.shiftEnabled, row.alert));
            screen.addPreference(item);
        }
        // A rebuild drops every row object, so the check marks and the action bar have to be
        // re-applied; otherwise the selection is still held in memory but invisible.
        refreshSelection();
    }

    public static void preload(Context context) {
        if (context != null) sAppContext = context.getApplicationContext();
        startLoad(false);
    }

    private static void sortRows(List<AlarmItem> list) {
        Collections.sort(list, new Comparator<AlarmItem>() {
            @Override
            public int compare(AlarmItem a, AlarmItem b) {
                return Integer.compare(a.hour * 60 + a.minute, b.hour * 60 + b.minute);
            }
        });
    }

    private String nextAlarmText(List<AlarmItem> visible) {
        Calendar now = Calendar.getInstance();
        long best = Long.MAX_VALUE;
        AlarmItem next = null;
        for (AlarmItem row : visible) {
            if (!row.enabled) continue;
            long minutes = minutesUntil(now, row.hour, row.minute, row.days);
            if (minutes >= 0 && minutes < best) {
                best = minutes;
                next = row;
            }
        }
        Context context = getContext() != null ? getContext() : sAppContext;
        if (context == null) return "";
        if (next == null) return context.getString(R.string.next_alarm_none);
        long h = best / 60;
        long m = best % 60;
        String when;
        if (h > 0 && m > 0) when = context.getString(R.string.next_alarm_in_hours_minutes, h, m);
        else if (h > 0) when = context.getString(R.string.next_alarm_in_hours, h);
        else when = context.getString(R.string.next_alarm_in_minutes, Math.max(1, m));
        String script = next.label == null || next.label.trim().isEmpty()
                ? "" : next.label.trim();
        if (script.startsWith("@")) script = script.substring(1);
        String task = script.isEmpty() ? context.getString(R.string.task_fallback) : script;
        return when + ((char) 10) + context.getString(R.string.next_alarm_runs, task);
    }

    static long minutesUntil(Calendar now, int hour, int minute, int days) {
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (days == 0 || days == 127) {
            if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1);
            return (target.getTimeInMillis() - now.getTimeInMillis()) / 60000L;
        }
        if (days == SHIFT_DAYS) {
            // A shift group is described by its currently running part, so today-or-tomorrow at that
            // time is the right answer.
            if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1);
            return (target.getTimeInMillis() - now.getTimeInMillis()) / 60000L;
        }
        if (days == WORKDAY_PRESET) {
            // 法定工作日: Mon-Fri. The holiday table lives in the clock's lunar calendar, which this
            // module does not mirror, so holidays are not subtracted - but weekends are, which is what
            // the old "treat it like every day" got wrong.
            for (int i = 0; i < 8; i++) {
                Calendar candidate = (Calendar) target.clone();
                candidate.add(Calendar.DAY_OF_YEAR, i);
                if (!candidate.after(now)) continue;
                int dow = candidate.get(Calendar.DAY_OF_WEEK);
                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                    return (candidate.getTimeInMillis() - now.getTimeInMillis()) / 60000L;
                }
            }
            return Long.MAX_VALUE;
        }
        if (days == HOLIDAY_PRESET) {
            // 法定节假日 depends on the same calendar; better no countdown than a wrong one.
            return Long.MAX_VALUE;
        }
        // Find the next matching weekday. Bit 0 is Monday.
        for (int i = 0; i < 8; i++) {
            Calendar candidate = (Calendar) target.clone();
            candidate.add(Calendar.DAY_OF_YEAR, i);
            int calendarDay = candidate.get(Calendar.DAY_OF_WEEK); // Sunday=1
            int bit = 1 << ((calendarDay + 5) % 7);
            if ((days & bit) != 0 && candidate.after(now)) {
                return (candidate.getTimeInMillis() - now.getTimeInMillis()) / 60000L;
            }
        }
        return Long.MAX_VALUE;
    }

    private void loadAlarms(boolean force) {
        startLoad(force);
    }

    private static void startLoad(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && sCachedRows != null && now - sCachedAt < 15_000L) {
            return;
        }
        if (sLoadingGeneration >= 0) {
            // A change (or a resume) arrived while a load was running. The running load may have
            // read the clock before that change landed, so ask for one more pass instead of
            // dropping the request.
            sReloadPending = true;
            return;
        }
        final int generation = sLoadGeneration.incrementAndGet();
        sLoadingGeneration = generation;
        ThreadUtils.runOnBackground(() -> {
            List<AlarmItem> loaded = null;
            boolean failed = false;
            try {
                loaded = ClockBridge.alarms(sAppContext);
            } catch (Throwable t) {
                failed = true;
            }
            if (loaded == null) {
                // The bridge and the root fallback both failed: that is an error the user can retry,
                // not an empty alarm list.
                failed = true;
                loaded = sCachedRows == null ? new ArrayList<>() : new ArrayList<>(sCachedRows);
            }
            final boolean stale = generation != sLoadGeneration.get();
            // Release the slot only if this load still owns it: a newer load may already be running.
            if (sLoadingGeneration == generation) sLoadingGeneration = -1;
            if (stale) {
                if (sReloadPending) {
                    sReloadPending = false;
                    startLoad(true);
                }
                return;
            }
            final List<AlarmItem> finalLoaded = loaded;
            sLoadFailed = failed;
            if (sReloadPending) {
                sReloadPending = false;
                startLoad(true);
            }
            sortRows(finalLoaded);
            sCachedRows = new ArrayList<>(finalLoaded);
            sCachedAt = System.currentTimeMillis();
            final AlarmListFragment active = sActive;
            if (active != null && active.getActivity() != null) {
                active.getActivity().runOnUiThread(() -> {
                    if (active.isAdded() && active.screen != null) {
                        active.rows.clear();
                        active.rows.addAll(finalLoaded);
                        active.rebuildPreferences();
                    }
                });
            }
        });
    }

    private static boolean isScriptAlarm(AlarmItem row) {
        return row != null && row.isScriptTask();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Throwable t) { return 0; }
    }



}
