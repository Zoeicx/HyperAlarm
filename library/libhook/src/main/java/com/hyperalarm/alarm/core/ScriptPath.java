package com.hyperalarm.alarm.core;
import com.hyperalarm.common.utils.RootShell;

/**
 * Resolves an alarm label / broadcast argument to a script file directly inside
 * {@link TaskManager#SCRIPTS_DIR}.
 *
 * <p>A label is a user editable field of a Mi Clock alarm and it also arrives through the
 * {@code RUN_SCRIPT} bridge, so anything able to write an alarm can pick the string. Running
 * "whatever the label says" with root would turn every alarm write into a root execution
 * primitive, therefore both execution paths ({@link TaskManager} for alarm firing and
 * {@link com.hyperalarm.alarm.hook.ShiftBridge} for a manual run) resolve the value here.</p>
 *
 * <p>Only a plain file name is accepted: no directories, no absolute paths. A script has to live
 * directly in the scripts directory, and {@link RootShell#realPath(String)} makes sure a symlink
 * pointing somewhere else (for example {@code /sdcard/Download}) is refused as well.</p>
 */
public final class ScriptPath {
    private static final int MAX_NAME_LENGTH = 128;

    private ScriptPath() {
    }

    /**
     * @param raw label or bridge argument; a plain script file name
     * @return the absolute path of the script, or {@code null} when the value is not a plain name
     * or resolves outside {@link TaskManager#SCRIPTS_DIR}
     */
    public static String resolve(String raw) {
        String name = plainName(raw);
        if (name == null) return null;
        String path = TaskManager.SCRIPTS_DIR + "/" + name;
        String real = RootShell.realPath(path);
        // A script that does not exist yet keeps its path so the caller can still report
        // "script missing" instead of "rejected"; an existing one has to stay in place.
        if (real == null) return path;
        String realRoot = RootShell.realPath(TaskManager.SCRIPTS_DIR);
        if (realRoot == null) realRoot = TaskManager.SCRIPTS_DIR;
        return real.startsWith(realRoot + "/") ? path : null;
    }

    /** The trimmed value when it is a plain script file name, otherwise {@code null}. */
    public static String plainName(String raw) {
        // Checked before trim(): String.trim() drops every char <= ' ', which would silently
        // swallow a NUL and let a name that the shell sees differently pass the checks below.
        if (raw == null || raw.indexOf('\0') >= 0) return null;
        String value = raw.trim();
        if (value.isEmpty() || value.length() > MAX_NAME_LENGTH) return null;
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0) return null;
        if (".".equals(value) || "..".equals(value)) return null;
        return value;
    }

    /** True when the value is a plain script file name. */
    public static boolean isPlainName(String raw) {
        return plainName(raw) != null;
    }
}
