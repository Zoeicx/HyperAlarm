package com.hyperalarm.common.alarm;

/**
 * The wire between the app and the clock process: one permission, one package, eleven actions and
 * the extras they carry.
 *
 * <p>Every name here used to be a literal in both the app and the hook, so a typo produced no error
 * at all - the clock simply ignored the broadcast and the feature looked broken. Change a name here
 * and both sides follow.</p>
 */
public final class BridgeContract {
    /** Signature permission the clock requires from anyone sending these broadcasts. */
    public static final String PERMISSION = "hyperalarm.permission.BRIDGE";
    public static final String CLOCK_PACKAGE = "com.android.deskclock";
    /** The module app, i.e. the only sender the bridge accepts besides the permission itself. */
    public static final String MODULE_PACKAGE = "com.hyperalarm";
    /** Exported clock service that starts the process without opening the UI. */
    public static final String CLOCK_SERVICE = "com.xiaomi.smarthome.ClockService";

    public static final String ACTION_CREATE_SHIFT = "com.hyperalarm.action.CREATE_SHIFT";
    public static final String ACTION_ADD_ALARM = "com.hyperalarm.action.ADD_ALARM";
    public static final String ACTION_DELETE_SHIFT = "com.hyperalarm.action.DELETE_SHIFT";
    public static final String ACTION_RUN_SCRIPT = "com.hyperalarm.action.RUN_SCRIPT";
    public static final String ACTION_QUERY_ALARMS = "com.hyperalarm.action.QUERY_ALARMS";
    public static final String ACTION_UPDATE_ALARM = "com.hyperalarm.action.UPDATE_ALARM";
    public static final String ACTION_DELETE_ALARM = "com.hyperalarm.action.DELETE_ALARM";
    public static final String ACTION_SET_ENABLED = "com.hyperalarm.action.SET_ENABLED";
    public static final String ACTION_SET_RINGTONE = "com.hyperalarm.action.SET_RINGTONE";
    public static final String ACTION_OPEN_RINGTONE = "com.hyperalarm.action.OPEN_RINGTONE";
    public static final String ACTION_RINGTONE_PICKED = "com.hyperalarm.action.RINGTONE_PICKED";
    /**
     * Clock to module, one way: an alarm row changed.
     *
     * <p>The module cannot watch the clock's provider itself - it is protected by
     * {@code miui.permission.ACCESS_ALARM_PROVIDER} - so the clock process watches it and tells the
     * app, which then reloads through the normal (authenticated) query path. A forged broadcast can
     * therefore only cause a reload.</p>
     */
    public static final String ACTION_ALARMS_CHANGED = "com.hyperalarm.action.ALARMS_CHANGED";

    public static final String EXTRA_ID = "id";
    /** The alarm row id a script run or a shift edit refers to. */
    public static final String EXTRA_ALARM_ID = "alarmId";
    public static final String EXTRA_HOUR = "hour";
    public static final String EXTRA_MINUTE = "minute";
    public static final String EXTRA_MINUTES = "minutes";
    public static final String EXTRA_DAYS = "days";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_ENABLED = "enabled";
    public static final String EXTRA_RINGTONE = "uri";
    /** True when the row belongs to a shift group rather than a plain alarm. */
    public static final String EXTRA_SHIFT = "shift";
    /** Script path for {@link #ACTION_RUN_SCRIPT}. */
    public static final String EXTRA_PATH = "path";
    /** Shift group editing: row id, rotation length, start time and the per-shift arrays. */
    public static final String EXTRA_GROUP_ROW_ID = "groupRowId";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_START_TIME = "startTime";
    public static final String EXTRA_HOURS = "hours";
    public static final String EXTRA_VIBRATE = "vibrate";

    private BridgeContract() {
    }
}
