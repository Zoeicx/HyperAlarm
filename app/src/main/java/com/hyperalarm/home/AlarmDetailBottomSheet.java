package com.hyperalarm.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperalarm.R;
import com.hyperalarm.home.ShiftEditBottomSheet.SavedListener;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.alarm.BridgeContract;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fan.appcompat.app.AlertDialog;
import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;
import com.hyperalarm.common.utils.ThreadUtils;

public class AlarmDetailBottomSheet extends BottomSheetModal {
    private final Activity activity;
    private final View content;
    private int id;
    private int hour;
    private int minute;
    private int days;
    private String shiftInfo = "";
    private int shiftGroupRowId;
    private int shiftAlarmRowId;
    private long shiftStartTime;
    private int shiftDuration;
    private int[] shiftHours;
    private int[] shiftMinutes;
    private boolean[] shiftEnabled;
    private String alertUri = "";
    private boolean ringEnabled;
    private boolean newAlarm;
    private boolean saved;
    private String label = "";
    private AlarmTimeWheelView wheel;
    private TextView scriptView;
    private TextView repeatView;
    private TextView runView;
    private TextView ringtoneView;
    private fan.slidingwidget.widget.SlidingButton ringSwitch;

    public void setNewAlarm(boolean newAlarm) {
        this.newAlarm = newAlarm;
    }

    @Override
    public void dismiss() {
        if (newAlarm && !saved && id > 0) {
            AlarmHookWriter.delete(activity, id, false);
        }
        super.dismiss();
    }

    public AlarmDetailBottomSheet(Activity activity) {
        super(activity);
        this.activity = activity;
        content = LayoutInflater.from(activity).inflate(R.layout.view_alarm_detail_sheet, null);
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
        setCanceledOnTouchOutside(true);
        bind();
    }

    private void bind() {
        wheel = content.findViewById(R.id.detail_time_wheel);
        wheel.setOnTimeChangedListener((h, m) -> {
            hour = h;
            minute = m;
            updateCountdown();
        });
        repeatView = content.findViewById(R.id.detail_repeat);
        scriptView = content.findViewById(R.id.detail_script);
        runView = content.findViewById(R.id.detail_run);
        ringtoneView = content.findViewById(R.id.detail_ringtone_value);
        ringSwitch = content.findViewById(R.id.detail_ring_enabled);
        ringSwitch.setChecked(ringEnabled);
        ringSwitch.setOnCheckedChangeListener((button, checked) -> {
            ringEnabled = checked;
            updateRingVisibility();
        });
        runView.setOnClickListener(v -> runNow());

        content.findViewById(R.id.detail_close).setOnClickListener(v -> dismiss());
        content.findViewById(R.id.detail_save).setOnClickListener(v -> save());
        content.findViewById(R.id.detail_repeat_row).setOnClickListener(v -> chooseRepeat());
        content.findViewById(R.id.detail_ringtone_row).setOnClickListener(v -> {
            ClockRingtonePickerActivity.setTarget(this);
            Intent intent = new Intent(activity, ClockRingtonePickerActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("shift", shiftAlarmRowId > 0);
            // The picker used to open on the system default, so the current choice was invisible and
            // "just open it and press OK" silently replaced the ringtone.
            intent.putExtra("current", alertUri);
            activity.startActivity(intent);
        });
        content.findViewById(R.id.detail_note_row).setOnClickListener(v -> chooseScript());
    }

    public void showAlarm(int id, int hour, int minute, String label, int days, String shiftInfo,
                          int shiftGroupRowId, int shiftAlarmRowId, long shiftStartTime,
                          int shiftDuration, int[] shiftHours, int[] shiftMinutes, boolean[] shiftEnabled,
                          String alertUri) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.days = days;
        this.shiftInfo = shiftInfo == null ? "" : shiftInfo;
        this.shiftGroupRowId = shiftGroupRowId;
        this.shiftAlarmRowId = shiftAlarmRowId;
        this.shiftStartTime = shiftStartTime;
        this.shiftDuration = shiftDuration;
        this.shiftHours = shiftHours;
        this.shiftMinutes = shiftMinutes;
        this.shiftEnabled = shiftEnabled;
        this.alertUri = alertUri == null ? "" : alertUri;
        String rawLabel = label == null ? "" : label;
        this.ringEnabled = rawLabel.startsWith("@");
        this.label = this.ringEnabled ? rawLabel.substring(1) : rawLabel;
        if (ringSwitch != null) ringSwitch.setChecked(this.ringEnabled);
        wheel.setTime(hour, minute);
        repeatView.setText(days == 512 && !this.shiftInfo.isEmpty()
                ? this.shiftInfo : AlarmRepeatText.get(activity, days));
        showScript();
        updateRingtoneView();
        updateRingVisibility();
        updateCountdown();
        show();
    }

    private void updateCountdown() {
        // Same rule as the list: an alarm that only fires on some days must not count down to
        // "today at 08:00" when today is not one of them. Long.MAX_VALUE means "cannot be decided"
        // (法定节假日 needs the clock's lunar table), and then the line simply stays empty instead of
        // showing a wrong time.
        Calendar now = Calendar.getInstance();
        long totalMinutes = AlarmListFragment.minutesUntil(now, hour, minute, days);
        if (totalMinutes == Long.MAX_VALUE || totalMinutes <= 0L) {
            ((TextView) content.findViewById(R.id.detail_countdown)).setText("");
            return;
        }
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        String text;
        if (hours > 0 && minutes > 0) {
            text = activity.getString(R.string.countdown_hours_minutes, hours, minutes);
        } else if (hours > 0) {
            text = activity.getString(R.string.countdown_hours, hours);
        } else {
            text = activity.getString(R.string.countdown_minutes, minutes);
        }
        ((TextView) content.findViewById(R.id.detail_countdown)).setText(text);
    }

    private void updateRingVisibility() {
        View[] rows = {content.findViewById(R.id.detail_ringtone_row)};
        int offset = Math.round(8 * activity.getResources().getDisplayMetrics().density);
        for (View row : rows) {
            if (row == null) continue;
            row.animate().cancel();
            if (ringEnabled) {
                if (row.getVisibility() != View.VISIBLE) {
                    row.setVisibility(View.VISIBLE);
                    row.setAlpha(0f);
                    row.setTranslationY(-offset);
                }
                row.animate().alpha(1f).translationY(0f).setDuration(180).start();
            } else if (row.getVisibility() == View.VISIBLE) {
                row.animate().alpha(0f).translationY(-offset).setDuration(150)
                        .withEndAction(() -> {
                            row.setVisibility(View.GONE);
                            row.setAlpha(1f);
                            row.setTranslationY(0f);
                        }).start();
            }
        }
    }

    private void showScript() {
        if (label == null || label.trim().isEmpty()) {
            scriptView.setText(R.string.script_choose);
            scriptView.setTextColor(0xFF7CC7FF);
        } else {
            scriptView.setText(label);
            scriptView.setTextColor(0xFFF2F2F2);
        }
        if (runView != null) {
            boolean hasScript = label != null && !label.trim().isEmpty();
            runView.setAlpha(hasScript ? 1f : 0.45f);
        }
    }

    private void runNow() {
        if (label == null || label.trim().isEmpty()) {
            Toast.makeText(activity, R.string.script_choose_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = label.trim();
        if (name.startsWith("@")) name = name.substring(1).trim();
        final String script = name;
        Toast.makeText(activity, R.string.script_running, Toast.LENGTH_SHORT).show();
        final Context app = activity.getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            // The answer means "the clock accepted the request", not "the script finished": the run
            // continues on its own and reports through the notification, so only a failure to hand
            // the request over is worth another toast (the "running" one is already shown).
            boolean accepted = ClockBridge.write(app, BridgeContract.ACTION_RUN_SCRIPT, intent -> intent
                    .putExtra(BridgeContract.EXTRA_PATH, script)
                    .putExtra(BridgeContract.EXTRA_ALARM_ID, id));
            if (!accepted) {
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        R.string.script_run_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateRingtoneView() {
        String text = RingtoneNameMapper.display(activity, alertUri);
        ringtoneView.setText(text);
    }

    void updateRingtone(Uri uri) {
        alertUri = uri == null ? "" : uri.toString();
        updateRingtoneView();
    }

    private void chooseRingtone() {
        try {
            RingtoneManager manager = new RingtoneManager(activity);
            android.database.Cursor cursor = manager.getCursor();
            if (cursor == null) return;
            List<String> names = new ArrayList<>();
            List<Uri> uris = new ArrayList<>();
            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            while (cursor.moveToNext()) {
                String title = titleCol >= 0 ? cursor.getString(titleCol)
                        : activity.getString(R.string.ringtone);
                Uri uri = manager.getRingtoneUri(cursor.getPosition());
                if (uri != null) {
                    names.add(title == null ? uri.toString() : title);
                    uris.add(uri);
                }
            }
            cursor.close();
            if (names.isEmpty()) {
                Toast.makeText(activity, R.string.ringtone_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.ringtone)
                    .setItems(names.toArray(new String[0]), (d, which) -> {
                        ringtoneView.setText(names.get(which));
                        AlarmHookWriter.setRingtone(activity, id, uris.get(which).toString(),
                                shiftAlarmRowId > 0);
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(activity, R.string.ringtone_load_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseScript() {
        // No root pre-check here: it would block on the authorisation prompt. The listing itself
        // decides, and an empty answer means root is missing rather than "no scripts".
        ThreadUtils.runOnBackground(() -> {
            List<String> names = new ArrayList<>();
            com.topjohnwu.superuser.Shell.Result result = RootShell.run(
                    "ls -1 /data/adb/alarm_shell/scripts");
            final boolean readable = result != null && result.getCode() == 0;
            if (result != null) {
                for (String line : result.getOut()) {
                    if (line.trim().endsWith(".sh")) names.add(line.trim());
                }
            }
            activity.runOnUiThread(() -> {
                if (names.isEmpty()) {
                    // Distinguish "no scripts yet" from "cannot look": the first needs an import, the
                    // second a root grant.
                    Toast.makeText(activity,
                            readable ? R.string.script_dir_empty : R.string.root_required,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.script_choose)
                        .setItems(names.toArray(new String[0]), (d, which) -> {
                            label = names.get(which);
                            showScript();
                        })
                        .setNeutralButton(activity.getString(R.string.script_open_dir), (d, which) ->
                                activity.startActivity(new Intent(activity, ScriptImportActivity.class)))
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            });
        });
    }

    private void chooseRepeat() {
        if (shiftAlarmRowId > 0) {
            // A shift row's schedule is the rotation, and the bridge deliberately ignores `days` for
            // shift updates - so offering the repeat choices would claim an edit that never happens.
            // The rotation itself is edited where it is edited: the shift sheet.
            new ShiftEditBottomSheet(activity)
                    .onSaved(() -> {
                        days = 512;
                        repeatView.setText(AlarmRepeatText.get(activity, 512));
                        saved = true;
                        dismiss();
                    })
                    .showExisting(id, label, shiftGroupRowId, shiftStartTime, shiftDuration,
                            shiftHours, shiftMinutes, shiftEnabled);
            return;
        }
        String[] items = {
                activity.getString(R.string.repeat_once),
                activity.getString(R.string.repeat_every_day),
                activity.getString(R.string.repeat_workdays),
                activity.getString(R.string.repeat_holidays),
                activity.getString(R.string.repeat_mon_fri),
                activity.getString(R.string.repeat_custom),
                activity.getString(R.string.repeat_shift)
        };
        int[] values = {0, 127, 128, 256, 31, -1, 512};
        View list = LayoutInflater.from(activity).inflate(R.layout.dialog_repeat_options, null);
        LinearLayout standard = list.findViewById(R.id.repeat_standard_group);
        LinearLayout special = list.findViewById(R.id.repeat_special_group);
        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < items.length; i++) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_repeat_option,
                    i < 5 ? standard : special, false);
            TextView title = row.findViewById(R.id.option_title);
            View arrow = row.findViewById(R.id.option_arrow);
            title.setText(items[i]);
            if (days == values[i]) title.setTextColor(0xFF7CC7FF);
            arrow.setVisibility(i >= 5 ? View.VISIBLE : View.GONE);
            final int index = i;
            row.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                if (index == 5) {
                    chooseCustomRepeat();
                } else if (index == 6) {
                    // Saving a shift replaces this alarm with a shift group; the sheet has to follow,
                    // otherwise its next save would write the old repeat/days back over it.
                    SavedListener afterShift = () -> {
                        days = 512;
                        repeatView.setText(AlarmRepeatText.get(activity, 512));
                        saved = true;
                        dismiss();
                    };
                    if (days == 512 && shiftGroupRowId > 0) {
                        new ShiftEditBottomSheet(activity).onSaved(afterShift).showExisting(id, label,
                                shiftGroupRowId, shiftStartTime, shiftDuration,
                                shiftHours, shiftMinutes, shiftEnabled);
                    } else {
                        new ShiftEditBottomSheet(activity).onSaved(afterShift)
                                .showNew(id, hour, minute, label);
                    }
                    return;
                } else {
                    days = values[index];
                    repeatView.setText(AlarmRepeatText.get(activity, days));
                }
            });
            (i < 5 ? standard : special).addView(row);
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.repeat_title)
                .setView(list)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        holder[0] = dialog;
        dialog.show();
    }

    private void chooseCustomRepeat() {
        String[] names = AlarmRepeatText.weekdayOptions(activity);
        boolean[] checked = AlarmRepeatText.booleanDays(days);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.repeat_custom_title)
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_ok, (d, which) -> {
                    days = AlarmRepeatText.fromBooleans(checked);
                    repeatView.setText(AlarmRepeatText.get(activity, days));
                })
                .show();
    }

    private void save() {
        if (id <= 0) {
            dismiss();
            return;
        }
        final int saveHour = hour;
        final int saveMinute = minute;
        final int saveDays = days;
        final String saveLabel = (ringEnabled ? "@" : "") + (label == null ? "" : label);
        // The write goes through the clock process, so the sheet closes immediately and the result
        // is reported once the clock answers: a success toast for a rejected write is worse than a
        // short wait.
        saved = true;
        dismiss();
        AlarmListFragment.optimisticSave(id, saveHour, saveMinute, saveLabel, saveDays, newAlarm);
        final Context app = activity.getApplicationContext();
        final boolean shift = shiftAlarmRowId > 0;
        ThreadUtils.runOnBackground(() -> {
            boolean ok = AlarmHookWriter.updateNow(app, id, saveHour, saveMinute, saveDays,
                    saveLabel, shift);
            activity.runOnUiThread(() -> {
                Toast.makeText(activity,
                        ok ? R.string.alarm_saved : R.string.alarm_save_failed,
                        Toast.LENGTH_SHORT).show();
                if (!ok) {
                    // The optimistic row is wrong now; ask the list for the truth.
                    AlarmListFragment.invalidate();
                    AlarmListFragment.reloadActive();
                }
            });
        });
    }

}
