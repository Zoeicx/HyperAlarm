package com.hyperalarm.log;

public class ScriptLogActivity extends LogViewerActivity {
    @Override
    protected boolean isScriptMode() {
        return true;
    }

    /** Set when the clock's notification asks for one specific run instead of the list. */
    @Override
    protected long getTargetLogTimestamp() {
        return getIntent() == null ? 0L
                : getIntent().getLongExtra(ScriptLogLauncher.EXTRA_TARGET_TS, 0L);
    }
}
