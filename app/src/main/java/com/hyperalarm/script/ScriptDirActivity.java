package com.hyperalarm.script;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import com.hyperalarm.R;

public class ScriptDirActivity extends Activity {
    private static final String DIR = "/data/adb/alarm_shell/scripts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(
                    getString(R.string.script_dir_label), DIR));
            Toast.makeText(this, getString(R.string.script_dir_copied, DIR), Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
