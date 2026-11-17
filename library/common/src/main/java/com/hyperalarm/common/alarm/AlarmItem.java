package com.hyperalarm.common.alarm;

/**
 * One alarm row as it travels from the clock process to the app.
 *
 * <p>Gson serialises the public fields, so <b>the field names are the wire format</b>: rename a
 * field here and the app silently drops the entry, which looks like "the alarm disappeared" rather
 * than an error. Both sides read this class, so a rename only needs one place - keep it that way.</p>
 *
 * <p>Everything a list row or the detail sheet needs is on the object; the shift members are only
 * filled for rows that belong to a rotating group ({@link #days} ==
 * {@link AlarmContract#SHIFT_DAYS_BIT}).</p>
 */
public class AlarmItem {
    public int id;
    public int hour;
    public int minute;
    /** {@code daysofweek} bit mask, Monday is bit 0. */
    public int days;
    public String label = "";
    public boolean enabled = true;
    public String alert = "";

    /** Row id of the shift group in {@code shift_alarms}, 0 for a plain alarm. */
    public int shiftGroupRowId;
    /** Row id of the alarm row that represents the running shift. */
    public int shiftAlarmRowId;
    public long shiftGroupId;
    public long shiftStartTime;
    /** How many days the rotation lasts. */
    public int shiftDuration;
    /** How many shifts the group holds, used for the summary label. */
    public int shiftAlarmCount;
    public int[] shiftHours;
    public int[] shiftMinutes;
    public boolean[] shiftEnabled;

    /** True when this row describes a shift group rather than a plain alarm. */
    public boolean isShift() {
        return days == AlarmContract.SHIFT_DAYS_BIT;
    }

    /** True when the label names a script task, i.e. this row runs something on fire. */
    public boolean isScriptTask() {
        if (label == null) return false;
        String name = label.trim();
        return name.endsWith(".sh");
    }
}
