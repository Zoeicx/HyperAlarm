package com.hyperalarm.common.alarm;

/**
 * Names the app and the clock process must agree on.
 *
 * <p>The bridge hands a JSON array of {@link AlarmItem} from the clock process to the app, and the
 * rooot fallback reads the same provider tables. Both ends used to spell the column and field names
 * out by hand, so a rename only showed up as alarms silently disappearing in the list. Keep every
 * name here.</p>
 */
public final class AlarmContract {
    public static final String ALARM_URI = "content://com.android.deskclock/alarm";
    public static final String SHIFT_URI = "content://com.android.deskclock/shift_alarms";

    /** Alarm table columns the module reads. */
    public static final String COL_ID = "_id";
    public static final String COL_HOUR = "hour";
    public static final String COL_MINUTES = "minutes";
    public static final String COL_DAYS_OF_WEEK = "daysofweek";
    public static final String COL_MESSAGE = "message";
    public static final String COL_ENABLED = "enabled";
    public static final String COL_TYPE = "type";
    public static final String COL_ALERT = "alert";

    /**
     * Clock rows that are bookkeeping rather than a user visible alarm. The value is fixed by the
     * Mi Clock provider, not chosen by us.
     */
    public static final int TYPE_HIDDEN = 2;
    /** {@code shift_alarms.type}: the rotating group row. */
    public static final int TYPE_SHIFT_GROUP = 1;
    /** {@code shift_alarms.type}: one shift inside a group. */
    public static final int TYPE_SHIFT_PART = 0;
    /** {@code daysofweek} value the app uses for "this row belongs to a shift schedule". */
    public static final int SHIFT_DAYS_BIT = 512;
    public static final long DAY_MS = 86400000L;

    /** Columns read from the alarm table, in the order the bridge fills them. */
    public static final String[] ALARM_PROJECTION = {
            COL_ID, COL_HOUR, COL_MINUTES, COL_DAYS_OF_WEEK, COL_MESSAGE, COL_ENABLED, COL_TYPE,
            COL_ALERT
    };

    /** Shift table column names; that table has no stable projection of its own. */
    public static final String COL_SHIFT_GROUP_ID = "groupid";
    public static final String COL_SHIFT_DURATION = "duration";
    public static final String COL_SHIFT_ALARM_COUNT = "alarmCount";
    public static final String COL_SHIFT_START_TIME = "starttime";
    public static final String COL_SHIFT_ENABLE = "enable";
    public static final String COL_SHIFT_ALARM_ID = "alarmid";
    public static final String COL_SHIFT_INDEX = "shiftindex";

    private AlarmContract() {
    }
}
