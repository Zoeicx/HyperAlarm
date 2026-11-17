package com.hyperalarm.home;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperalarm.R;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.alarm.BridgeContract;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import fan.appcompat.app.AlertDialog;
import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;
import fan.slidingwidget.widget.SlidingButton;
import com.hyperalarm.common.utils.ThreadUtils;

/** Editor for Mi Clock shift groups, backed by the clock-process ShiftBridge. */
public class ShiftEditBottomSheet extends BottomSheetModal {
    private final Activity activity;
    private final View content;
    private final LinearLayout rows;
    private final TextView durationView;
    private final TextView dateView;

    private int alarmId;
    private String label;
    private int groupRowId;
    private int duration;
    private long startTime;
    private int[] hours;
    private int[] minutes;
    private boolean[] enabled;

    /** Called after the clock accepted the shift schedule. */
    public interface SavedListener {
        void onShiftSaved();
    }

    private SavedListener savedListener;

    /** @return this, so the caller can chain the setup call */
    public ShiftEditBottomSheet onSaved(SavedListener listener) {
        this.savedListener = listener;
        return this;
    }

    public ShiftEditBottomSheet(Activity activity) {
        super(activity);
        this.activity = activity;
        content = LayoutInflater.from(activity).inflate(R.layout.view_shift_edit, null);
        setContentView(content);
        setDragHandleViewEnabled(true);
        setAnimationInterruptible(true);
        try {
            getBottomSheetView().setBackgroundColor(0xFF242424);
        } catch (Throwable ignored) {
        }
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setDraggable(true);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(true);
        behavior.setForceFullHeight(true);
        behavior.setModeConfig(0);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        setCanceledOnTouchOutside(false);

        rows = content.findViewById(R.id.shift_rows);
        durationView = content.findViewById(R.id.shift_duration);
        dateView = content.findViewById(R.id.shift_date);
        content.findViewById(R.id.shift_close).setOnClickListener(v -> dismiss());
        content.findViewById(R.id.shift_save).setOnClickListener(v -> save());
        content.findViewById(R.id.shift_duration_row).setOnClickListener(v -> chooseDuration());
        content.findViewById(R.id.shift_date_row).setOnClickListener(v -> chooseDate());
    }

    public void showNew(int alarmId, int hour, int minute, String label) {
        this.alarmId = alarmId;
        this.label = label == null ? "" : label;
        this.groupRowId = 0;
        this.duration = 4;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        this.startTime = c.getTimeInMillis();
        this.hours = new int[duration];
        this.minutes = new int[duration];
        this.enabled = new boolean[duration];
        for (int i = 0; i < duration; i++) {
            hours[i] = hour;
            minutes[i] = minute;
            enabled[i] = true;
        }
        refresh();
        show();
    }

    public void showExisting(int alarmId, String label, int groupRowId, long startTime,
                             int duration, int[] hours, int[] minutes, boolean[] enabled) {
        this.alarmId = alarmId;
        this.label = label == null ? "" : label;
        this.groupRowId = groupRowId;
        this.startTime = startTime;
        this.duration = Math.max(1, duration);
        // Copied, not referenced: this editor writes straight into these arrays, and the caller
        // hands over the arrays of the cached row. Editing and then cancelling used to leave the
        // unsaved times sitting in the list cache.
        this.hours = hours == null ? new int[this.duration] : hours.clone();
        this.minutes = minutes == null ? new int[this.duration] : minutes.clone();
        this.enabled = enabled == null ? new boolean[this.duration] : enabled.clone();
        normalizeArrays();
        refresh();
        show();
    }

    private void normalizeArrays() {
        duration = Math.max(1, Math.min(31, duration));
        if (hours == null || hours.length < duration) {
            int[] n = new int[duration];
            if (hours != null) System.arraycopy(hours, 0, n, 0, hours.length);
            hours = n;
        }
        if (minutes == null || minutes.length < duration) {
            int[] n = new int[duration];
            if (minutes != null) System.arraycopy(minutes, 0, n, 0, minutes.length);
            minutes = n;
        }
        if (enabled == null || enabled.length < duration) {
            boolean[] n = new boolean[duration];
            if (enabled != null) System.arraycopy(enabled, 0, n, 0, enabled.length);
            enabled = n;
        }
    }

    private void refresh() {
        normalizeArrays();
        durationView.setText(activity.getString(R.string.shift_duration_days, duration));
        dateView.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new java.util.Date(startTime)));
        rows.removeAllViews();
        for (int i = 0; i < duration; i++) {
            final int index = i;
            View row = LayoutInflater.from(activity)
                    .inflate(R.layout.item_shift_edit_row, rows, false);
            TextView title = row.findViewById(R.id.shift_row_title);
            TextView summary = row.findViewById(R.id.shift_row_summary);
            SlidingButton toggle = row.findViewById(R.id.shift_row_switch);
            title.setText(activity.getString(R.string.shift_part_title, i + 1));
            summary.setText(enabled[i]
                    ? String.format(Locale.US, "%02d:%02d", hours[i], minutes[i])
                    : activity.getString(R.string.shift_rest));
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(enabled[i]);
            toggle.setOnCheckedChangeListener((button, checked) -> {
                enabled[index] = checked;
                summary.setText(checked
                        ? String.format(Locale.US, "%02d:%02d", hours[index], minutes[index])
                        : activity.getString(R.string.shift_rest));
                row.setAlpha(checked ? 1f : 0.5f);
            });
            row.setAlpha(enabled[i] ? 1f : 0.5f);
            row.setOnClickListener(v -> chooseTime(index));
            rows.addView(row);
        }
    }

    private void chooseDuration() {
        String[] items = new String[14];
        for (int i = 0; i < items.length; i++) {
            items[i] = activity.getString(R.string.shift_duration_days, i + 1);
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.shift_cycle_title)
                .setSingleChoiceItems(items, duration - 1, (d, which) -> {
                    d.dismiss();
                    int old = duration;
                    int next = which + 1;
                    int oldHour = hours.length > 0 ? hours[0] : 8;
                    int oldMinute = minutes.length > 0 ? minutes[0] : 0;
                    int[] nh = new int[next];
                    int[] nm = new int[next];
                    boolean[] ne = new boolean[next];
                    for (int i = 0; i < next; i++) {
                        int src = Math.min(i, old - 1);
                        nh[i] = src >= 0 && src < hours.length ? hours[src] : oldHour;
                        nm[i] = src >= 0 && src < minutes.length ? minutes[src] : oldMinute;
                        ne[i] = src < 0 || src >= enabled.length || enabled[src];
                    }
                    duration = next;
                    hours = nh;
                    minutes = nm;
                    enabled = ne;
                    refresh();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void chooseDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startTime);
        new DatePickerDialog(activity, (view, year, month, day) -> {
            Calendar n = Calendar.getInstance();
            n.set(year, month, day, 0, 0, 0);
            n.set(Calendar.MILLISECOND, 0);
            startTime = n.getTimeInMillis();
            refresh();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void chooseTime(int index) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (activity.getResources().getDisplayMetrics().density * 20);
        box.setPadding(pad, 0, pad, 0);
        AlarmTimeWheelView wheel = new AlarmTimeWheelView(activity);
        wheel.setTime(hours[index], minutes[index]);
        box.addView(wheel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (activity.getResources().getDisplayMetrics().density * 220)));
        SlidingButton enable = new SlidingButton(activity);
        enable.setChecked(enabled[index]);
        LinearLayout enableRow = new LinearLayout(activity);
        enableRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        enableRow.setPadding(0, pad / 2, 0, 0);
        TextView text = new TextView(activity);
        text.setText(R.string.shift_ring);
        text.setTextColor(0xFFF2F2F2);
        text.setTextSize(16);
        enableRow.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        enableRow.addView(enable);
        box.addView(enableRow);
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.shift_part_title, index + 1))
                .setView(box)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_ok, (d, w) -> {
                    hours[index] = wheel.getHour();
                    minutes[index] = wheel.getMinute();
                    enabled[index] = enable.isChecked();
                    refresh();
                })
                .show();
    }

    /** True while a save is in flight; the ack is asynchronous, so the sheet stays open. */
    private boolean saving;

    private void save() {
        // One write per tap: the sheet only closes when the bridge answers, so a second tap used to
        // send a second CREATE_SHIFT and a new group (groupRowId == 0) became two shift schedules.
        if (saving) return;
        normalizeArrays();
        if (enabledCount() < 1) {
            Toast.makeText(activity, R.string.shift_need_one, Toast.LENGTH_SHORT).show();
            return;
        }
        saving = true;
        final Context app = activity.getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            boolean ok = ClockBridge.write(app, BridgeContract.ACTION_CREATE_SHIFT, intent -> intent
                    .putExtra(BridgeContract.EXTRA_DURATION, duration)
                    .putExtra(BridgeContract.EXTRA_START_TIME, startTime)
                    .putExtra(BridgeContract.EXTRA_LABEL, label == null ? "" : label)
                    .putExtra(BridgeContract.EXTRA_GROUP_ROW_ID, groupRowId)
                    .putExtra(BridgeContract.EXTRA_ALARM_ID, alarmId)
                    .putExtra(BridgeContract.EXTRA_HOURS, hours)
                    .putExtra(BridgeContract.EXTRA_MINUTES, minutes)
                    .putExtra(BridgeContract.EXTRA_ENABLED, enabled));
            if (ok && groupRowId == 0 && alarmId > 0) {
                // The placeholder alarm this shift grew out of is not needed any more.
                RootShell.run("content delete --uri " + com.hyperalarm.common.alarm.AlarmContract.ALARM_URI
                        + "/" + alarmId);
            }
            activity.runOnUiThread(() -> {
                saving = false;
                if (ok) {
                    AlarmListFragment.invalidate();
                    AlarmListFragment.reloadActive();
                    if (savedListener != null) savedListener.onShiftSaved();
                    Toast.makeText(activity, R.string.shift_saved, Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(activity, R.string.shift_save_failed, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private int enabledCount() {
        int count = 0;
        for (boolean b : enabled) if (b) count++;
        return count;
    }
}
