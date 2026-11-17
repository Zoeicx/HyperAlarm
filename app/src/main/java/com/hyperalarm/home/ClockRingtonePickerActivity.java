package com.hyperalarm.home;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.hyperalarm.R;

/** Opens the system/theme ringtone picker chooser and returns the selected URI. */
public class ClockRingtonePickerActivity extends Activity {
    private static final int REQ = 100;
    private static java.lang.ref.WeakReference<AlarmDetailBottomSheet> sTarget;
    private int alarmId;
    private boolean shift;
    /** The alarm's current ringtone, so the picker opens on it instead of on the system default. */
    private String current;

    static void setTarget(AlarmDetailBottomSheet sheet) {
        sTarget = sheet == null ? null : new java.lang.ref.WeakReference<>(sheet);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alarmId = getIntent().getIntExtra("id", -1);
        shift = getIntent().getBooleanExtra("shift", false);
        current = getIntent().getStringExtra("current");
        Uri existing = current == null || current.isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                : Uri.parse(current);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra("is_set_mode", false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        try {
            startActivityForResult(intent, REQ);
        } catch (Throwable t) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null && alarmId > 0) {
                // Wait for the clock's answer before claiming it worked: this used to toast "updated"
                // over a write that the clock may have rejected.
                final boolean isShift = shift;
                final String value = uri.toString();
                final AlarmDetailBottomSheet target = sTarget == null ? null : sTarget.get();
                com.hyperalarm.common.utils.ThreadUtils.runOnBackground(() -> {
                    boolean ok = AlarmHookWriter.setRingtoneNow(getApplicationContext(),
                            alarmId, value, isShift);
                    runOnUiThread(() -> {
                        if (ok) {
                            if (target != null) target.updateRingtone(uri);
                            AlarmListFragment.invalidate();
                            AlarmListFragment.reloadActive();
                            Toast.makeText(this, R.string.ringtone_updated, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, R.string.ringtone_update_failed, Toast.LENGTH_LONG).show();
                        }
                        sTarget = null;
                        finish();
                    });
                });
                return;
            }
        }
        sTarget = null;
        finish();
    }
}
