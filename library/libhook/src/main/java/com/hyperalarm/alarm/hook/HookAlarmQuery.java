package com.hyperalarm.alarm.hook;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.gson.Gson;
import com.hyperalarm.common.alarm.AlarmContract;
import com.hyperalarm.common.alarm.AlarmItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the clock's alarms from inside the clock process and hands them to the app.
 *
 * <p>The app cannot read this provider itself (it belongs to the clock and is guarded by the
 * clock's own permissions), so a query arrives as an ordered broadcast and is answered from here.
 * The payload is a JSON array of {@link AlarmItem}: the field names are the contract, see
 * {@link AlarmContract} for the column names behind them.</p>
 */
final class HookAlarmQuery {
    private static final Gson GSON = new Gson();

    private HookAlarmQuery() {
    }

    static String query(Context context) {
        List<AlarmItem> result = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        try (Cursor c = resolver.query(Uri.parse(AlarmContract.ALARM_URI), AlarmContract.ALARM_PROJECTION,
                null, null, null)) {
            if (c != null) {
                int idCol = c.getColumnIndexOrThrow(AlarmContract.COL_ID);
                int hourCol = c.getColumnIndexOrThrow(AlarmContract.COL_HOUR);
                int minuteCol = c.getColumnIndexOrThrow(AlarmContract.COL_MINUTES);
                int daysCol = c.getColumnIndexOrThrow(AlarmContract.COL_DAYS_OF_WEEK);
                int msgCol = c.getColumnIndexOrThrow(AlarmContract.COL_MESSAGE);
                int enabledCol = c.getColumnIndexOrThrow(AlarmContract.COL_ENABLED);
                int typeCol = c.getColumnIndexOrThrow(AlarmContract.COL_TYPE);
                int alertCol = c.getColumnIndexOrThrow(AlarmContract.COL_ALERT);
                while (c.moveToNext()) {
                    if (c.getInt(typeCol) == AlarmContract.TYPE_HIDDEN) continue;
                    AlarmItem item = new AlarmItem();
                    item.id = c.getInt(idCol);
                    item.hour = c.getInt(hourCol);
                    item.minute = c.getInt(minuteCol);
                    item.days = c.getInt(daysCol);
                    item.label = c.isNull(msgCol) ? "" : c.getString(msgCol);
                    item.enabled = c.getInt(enabledCol) == 1;
                    item.alert = c.isNull(alertCol) ? "" : c.getString(alertCol);
                    result.add(item);
                }
            }
        } catch (Throwable ignored) {
            // A partial list is still useful: the app keeps whatever was collected so far.
        }
        result.addAll(shiftGroups(resolver));
        return GSON.toJson(result);
    }

    /**
     * One entry per shift group, describing the shift that is running right now.
     *
     * <p>{@code shift_alarms} stores the group ({@code type=1}) and its parts ({@code type=0})
     * separately. Which part is "current" is derived the same way the clock does it: the day index
     * since {@code starttime} modulo the group duration.</p>
     */
    private static List<AlarmItem> shiftGroups(ContentResolver resolver) {
        List<AlarmItem> result = new ArrayList<>();
        Map<Long, Group> groups = new LinkedHashMap<>();
        try (Cursor c = resolver.query(Uri.parse(AlarmContract.SHIFT_URI), null, null, null, null)) {
            if (c == null) return result;
            while (c.moveToNext()) {
                int type = intValue(c, AlarmContract.COL_TYPE);
                long groupId = longValue(c, AlarmContract.COL_SHIFT_GROUP_ID);
                if (type == AlarmContract.TYPE_SHIFT_GROUP) {
                    Group group = new Group();
                    group.rowId = intValue(c, AlarmContract.COL_ID);
                    group.groupId = groupId;
                    group.duration = intValue(c, AlarmContract.COL_SHIFT_DURATION);
                    group.alarmCount = intValue(c, AlarmContract.COL_SHIFT_ALARM_COUNT);
                    group.startTime = longValue(c, AlarmContract.COL_SHIFT_START_TIME);
                    group.enabled = intValue(c, AlarmContract.COL_SHIFT_ENABLE) == 1;
                    group.message = stringValue(c, AlarmContract.COL_MESSAGE);
                    groups.put(groupId, group);
                } else if (type == AlarmContract.TYPE_SHIFT_PART && groupId != 0) {
                    Part part = new Part();
                    part.rowId = intValue(c, AlarmContract.COL_ID);
                    part.alarmId = intValue(c, AlarmContract.COL_SHIFT_ALARM_ID);
                    part.hour = intValue(c, AlarmContract.COL_HOUR);
                    part.minute = intValue(c, AlarmContract.COL_MINUTES);
                    part.index = intValue(c, AlarmContract.COL_SHIFT_INDEX);
                    part.enabled = intValue(c, AlarmContract.COL_SHIFT_ENABLE) == 1;
                    Group group = groups.get(groupId);
                    if (group == null) {
                        group = new Group();
                        group.groupId = groupId;
                        groups.put(groupId, group);
                    }
                    group.parts.add(part);
                }
            }
        } catch (Throwable ignored) {
            return result;
        }

        long now = System.currentTimeMillis();
        for (Group group : groups.values()) {
            if (group.parts.isEmpty()) continue;
            int duration = Math.max(1, group.duration > 0 ? group.duration : group.parts.size());
            int current = now < group.startTime
                    ? 1
                    : ((int) (((now - group.startTime) / AlarmContract.DAY_MS) % ((long) duration))) + 1;
            Part selected = pickPart(group.parts, current);
            // A group without an alarm row has nothing to show or edit yet.
            if (selected.alarmId <= 0) continue;

            AlarmItem item = new AlarmItem();
            item.id = selected.alarmId;
            item.hour = selected.hour;
            item.minute = selected.minute;
            item.days = AlarmContract.SHIFT_DAYS_BIT;
            item.label = group.message == null ? "" : group.message;
            item.enabled = group.enabled;
            item.shiftGroupRowId = group.rowId;
            item.shiftAlarmRowId = selected.rowId;
            item.shiftGroupId = group.groupId;
            item.shiftStartTime = group.startTime;
            item.shiftDuration = duration;
            item.shiftAlarmCount = Math.max(group.alarmCount, group.parts.size());
            item.shiftHours = new int[group.parts.size()];
            item.shiftMinutes = new int[group.parts.size()];
            item.shiftEnabled = new boolean[group.parts.size()];
            for (int i = 0; i < group.parts.size(); i++) {
                Part part = group.parts.get(i);
                item.shiftHours[i] = part.hour;
                item.shiftMinutes[i] = part.minute;
                item.shiftEnabled[i] = part.enabled;
            }
            result.add(item);
        }
        return result;
    }

    /** The running part, else the first enabled one, else the first part. */
    private static Part pickPart(List<Part> parts, int current) {
        for (Part part : parts) {
            if (part.index == current && part.enabled) return part;
        }
        for (Part part : parts) {
            if (part.enabled) return part;
        }
        return parts.get(0);
    }

    private static int intValue(Cursor c, String name) {
        int index = c.getColumnIndex(name);
        return index < 0 || c.isNull(index) ? 0 : c.getInt(index);
    }

    private static long longValue(Cursor c, String name) {
        int index = c.getColumnIndex(name);
        return index < 0 || c.isNull(index) ? 0L : c.getLong(index);
    }

    private static String stringValue(Cursor c, String name) {
        int index = c.getColumnIndex(name);
        return index < 0 || c.isNull(index) ? "" : c.getString(index);
    }

    /** One shift group as read from {@code shift_alarms}. */
    private static final class Group {
        int rowId;
        long groupId;
        int duration;
        int alarmCount;
        long startTime;
        boolean enabled;
        String message = "";
        final List<Part> parts = new ArrayList<>();
    }

    /** One shift inside a group, i.e. one rotating alarm row. */
    private static final class Part {
        int rowId;
        int alarmId;
        int hour;
        int minute;
        int index;
        boolean enabled;
    }
}
