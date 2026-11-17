package com.hyperalarm.home;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.alarm.AlarmContract;
import com.hyperalarm.common.alarm.AlarmItem;
import com.hyperalarm.common.alarm.BridgeContract;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The one way the app talks to the clock process.
 *
 * <p>Every call goes through an ordered broadcast guarded by
 * {@link BridgeContract#PERMISSION}, after warming the clock process up: the bridge receiver is
 * registered in {@code DeskClockApp.onCreate}, so a cold process needs a moment before it can answer
 * anything. Reads fall back to a root {@code content query} when the bridge stays silent (clock not
 * running, module disabled, LSPosed scope wrong) - the app should still be able to show the list.</p>
 *
 * <p>Timeouts and delays live here rather than at each call site, so the list, the detail sheet and
 * the writers all behave the same when the clock is slow.</p>
 */
final class ClockBridge {
    /** Give DeskClockApp.onCreate a chance to register the receiver. */
    private static final long WARMUP_MS = 250;
    /** One retry before a read falls back to root. */
    private static final long RETRY_MS = 300;
    /** How long a read waits for the clock to answer. */
    private static final long READ_TIMEOUT_MS = 1800;
    /** Writes only need the acknowledgement, not the payload. */
    private static final long WRITE_TIMEOUT_MS = 1200;

    private static final Gson GSON = new Gson();
    private static final Type ITEM_LIST = new TypeToken<List<AlarmItem>>() { }.getType();
    private static final Pattern QUERY_ROW = Pattern.compile("([A-Za-z0-9_]+)=([^,]*)");
    private static final long WARMUP_MIN_INTERVAL_MS = 5000L;
    private static volatile long sLastWarmUp;

    private ClockBridge() {
    }

    /** A broadcast answer: the result code the clock set and whatever it put in the result data. */
    static final class Reply {
        final int code;
        final String data;

        Reply(int code, String data) {
            this.code = code;
            this.data = data;
        }

        boolean ok() {
            return code == 1;
        }
    }

    /** Fills the outgoing intent; kept as a small interface so call sites stay readable. */
    interface Filler {
        void fill(Intent intent);
    }

    /**
     * Starts the clock process without opening the UI. Called often on purpose - it is cheap and
     * rate limited, and a cold process is the main reason a call would otherwise fail.
     */
    static void warmUp(Context context) {
        if (context == null) return;
        long now = System.currentTimeMillis();
        if (now - sLastWarmUp < WARMUP_MIN_INTERVAL_MS) return;
        sLastWarmUp = now;
        try {
            Intent service = new Intent();
            service.setComponent(new ComponentName(
                    BridgeContract.CLOCK_PACKAGE, BridgeContract.CLOCK_SERVICE));
            context.startService(service);
            Thread.sleep(WARMUP_MS);
        } catch (Throwable ignored) {
        }
    }

    /** Sends one bridge action and waits for the ordered result, or gives up after the timeout. */
    static Reply send(Context context, String action, Filler filler, long timeoutMs) {
        if (context == null) return new Reply(0, null);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Reply> reply = new AtomicReference<>();
        Intent intent = new Intent(action);
        intent.setPackage(BridgeContract.CLOCK_PACKAGE);
        if (filler != null) filler.fill(intent);
        context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent result) {
                reply.set(new Reply(getResultCode(), getResultData()));
                latch.countDown();
            }
        }, new Handler(Looper.getMainLooper()), 0, null, null);
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        Reply answered = reply.get();
        return answered == null ? new Reply(0, null) : answered;
    }

    /** Fire-and-ack helper for the write actions; true when the clock reported success. */
    /** When this process last asked the clock to change an alarm; 0 before the first write. */
    private static volatile long sLastWriteAt;

    /** A change seen by the alarm observer is ours when it arrives right after a write. */
    static long lastWriteAt() {
        return sLastWriteAt;
    }

    static boolean write(Context context, String action, Filler filler) {
        warmUp(context);
        sLastWriteAt = System.currentTimeMillis();
        // Any load already in flight read the clock before this write; the alarm observer will not
        // correct it, because its broadcast arrives inside the self-echo window above.
        AlarmListFragment.onWriteStarted();
        boolean ok = send(context, action, filler, WRITE_TIMEOUT_MS).ok();
        if (ok) AlarmListFragment.onWriteCommitted();
        return ok;
    }

    /** Like {@link #write} but keeps the answer, for actions that report a row id or a payload. */
    static Reply call(Context context, String action, Filler filler) {
        warmUp(context);
        sLastWriteAt = System.currentTimeMillis();
        AlarmListFragment.onWriteStarted();
        Reply reply = send(context, action, filler, WRITE_TIMEOUT_MS);
        if (reply.ok()) AlarmListFragment.onWriteCommitted();
        return reply;
    }

    /**
     * The alarm list: bridge first (it sees the clock's own cache), root query second.
     *
     * @return the rows, possibly empty, never {@code null}
     */
    static List<AlarmItem> alarms(Context context) {
        List<AlarmItem> fromBridge = queryByBridge(context);
        if (fromBridge != null) return fromBridge;
        return queryByRoot();
    }

    private static List<AlarmItem> queryByBridge(Context context) {
        warmUp(context);
        Reply reply = send(context, BridgeContract.ACTION_QUERY_ALARMS, null, READ_TIMEOUT_MS);
        if (!reply.ok() || reply.data == null || reply.data.isEmpty()) {
            // A freshly started process may have registered the receiver just after the first try.
            try {
                Thread.sleep(RETRY_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            }
            reply = send(context, BridgeContract.ACTION_QUERY_ALARMS, null, READ_TIMEOUT_MS);
        }
        if (!reply.ok() || reply.data == null || reply.data.isEmpty()) return null;
        return parse(reply.data);
    }

    private static List<AlarmItem> parse(String json) {
        List<AlarmItem> result = new ArrayList<>();
        try {
            List<AlarmItem> parsed = GSON.fromJson(json, ITEM_LIST);
            if (parsed != null) {
                for (AlarmItem item : parsed) {
                    if (item != null && item.id > 0) result.add(item);
                }
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    /**
     * Reads both provider tables through root, for when the bridge cannot answer. The provider is
     * owned by the clock, which is why the app cannot query it directly.
     */
    /**
     * Last-resort read through root.
     *
     * @return the rows, or null when the query itself failed - an empty list means "no alarms",
     * which the caller must not confuse with "the clock could not be read"
     */
    private static List<AlarmItem> queryByRoot() {
        List<AlarmItem> result = new ArrayList<>();
        try {
            com.topjohnwu.superuser.Shell.Result r = RootShell.run(
                    "content query --user 0 --uri " + AlarmContract.ALARM_URI
                            + " --projection " + String.join(":", AlarmContract.ALARM_PROJECTION)
                            + " 2>&1");
            if (r == null || r.getCode() != 0) return null;
            if (r != null) {
                for (String line : r.getOut()) {
                    if (!line.startsWith("Row:")) continue;
                    AlarmItem item = new AlarmItem();
                    int type = 0;
                    Matcher m = QUERY_ROW.matcher(line.substring(line.indexOf(':') + 1));
                    while (m.find()) {
                        String key = m.group(1);
                        String value = m.group(2).trim();
                        if (AlarmContract.COL_ID.equals(key)) item.id = parseInt(value);
                        else if (AlarmContract.COL_HOUR.equals(key)) item.hour = parseInt(value);
                        else if (AlarmContract.COL_MINUTES.equals(key)) item.minute = parseInt(value);
                        else if (AlarmContract.COL_MESSAGE.equals(key)) item.label = value;
                        else if (AlarmContract.COL_DAYS_OF_WEEK.equals(key)) item.days = parseInt(value);
                        else if (AlarmContract.COL_ENABLED.equals(key)) item.enabled = "1".equals(value);
                        else if (AlarmContract.COL_TYPE.equals(key)) type = parseInt(value);
                        else if (AlarmContract.COL_ALERT.equals(key)) item.alert = value;
                    }
                    if (item.id > 0 && type != AlarmContract.TYPE_HIDDEN) result.add(item);
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        List<AlarmItem> shift = queryShiftGroupsByRoot();
        if (shift == null) return null;   // the shift table could not be read either
        result.addAll(shift);
        return result;
    }

    private static List<AlarmItem> queryShiftGroupsByRoot() {
        List<AlarmItem> result = new ArrayList<>();
        Map<Long, Group> groups = new LinkedHashMap<>();
        try {
            com.topjohnwu.superuser.Shell.Result r = RootShell.run(
                    "content query --user 0 --uri " + AlarmContract.SHIFT_URI + " 2>&1");
            if (r == null || r.getCode() != 0) return null;
            for (String line : r.getOut()) {
                if (!line.startsWith("Row:")) continue;
                int type = 0, rowId = 0, alarmId = 0, hour = 0, minute = 0, index = 0;
                int duration = 0, alarmCount = 0;
                long groupId = 0, startTime = 0;
                boolean enabled = true;
                String message = "";
                Matcher m = QUERY_ROW.matcher(line.substring(line.indexOf(':') + 1));
                while (m.find()) {
                    String key = m.group(1);
                    String value = m.group(2).trim();
                    if (AlarmContract.COL_TYPE.equals(key)) type = parseInt(value);
                    else if (AlarmContract.COL_ID.equals(key)) rowId = parseInt(value);
                    else if (AlarmContract.COL_SHIFT_GROUP_ID.equals(key)) groupId = parseLong(value);
                    else if (AlarmContract.COL_SHIFT_ALARM_ID.equals(key)) alarmId = parseInt(value);
                    else if (AlarmContract.COL_HOUR.equals(key)) hour = parseInt(value);
                    else if (AlarmContract.COL_MINUTES.equals(key)) minute = parseInt(value);
                    else if (AlarmContract.COL_SHIFT_INDEX.equals(key)) index = parseInt(value);
                    else if (AlarmContract.COL_SHIFT_DURATION.equals(key)) duration = parseInt(value);
                    else if (AlarmContract.COL_SHIFT_ALARM_COUNT.equals(key)) alarmCount = parseInt(value);
                    else if (AlarmContract.COL_SHIFT_START_TIME.equals(key)) startTime = parseLong(value);
                    else if (AlarmContract.COL_MESSAGE.equals(key)) message = value;
                    else if (AlarmContract.COL_SHIFT_ENABLE.equals(key)) enabled = "1".equals(value);
                }
                if (type == AlarmContract.TYPE_SHIFT_GROUP) {
                    Group group = new Group();
                    group.rowId = rowId;
                    group.groupId = groupId;
                    group.duration = duration;
                    group.alarmCount = alarmCount;
                    group.startTime = startTime;
                    group.message = message;
                    group.enabled = enabled;
                    groups.put(groupId, group);
                } else if (type == AlarmContract.TYPE_SHIFT_PART && groupId != 0) {
                    Part part = new Part();
                    part.rowId = rowId;
                    part.alarmId = alarmId;
                    part.hour = hour;
                    part.minute = minute;
                    part.index = index;
                    part.enabled = enabled;
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
            return null;
        }

        long now = System.currentTimeMillis();
        for (Group group : groups.values()) {
            if (group.parts.isEmpty()) continue;
            int duration = Math.max(1, group.duration > 0 ? group.duration : group.parts.size());
            int current = now < group.startTime
                    ? 1
                    : (int) (((now - group.startTime) / AlarmContract.DAY_MS) % duration) + 1;
            Part selected = pickPart(group.parts, current);
            if (selected.alarmId <= 0) continue;

            int size = Math.max(duration, group.parts.size());
            AlarmItem item = new AlarmItem();
            item.id = selected.alarmId;
            item.hour = selected.hour;
            item.minute = selected.minute;
            item.days = AlarmContract.SHIFT_DAYS_BIT;
            item.enabled = group.enabled;
            item.label = group.message == null ? "" : group.message;
            item.shiftGroupRowId = group.rowId;
            item.shiftAlarmRowId = selected.rowId;
            item.shiftGroupId = group.groupId;
            item.shiftStartTime = group.startTime;
            item.shiftDuration = duration;
            item.shiftAlarmCount = Math.max(group.alarmCount, group.parts.size());
            item.shiftHours = new int[size];
            item.shiftMinutes = new int[size];
            item.shiftEnabled = new boolean[size];
            for (int i = 0; i < size; i++) {
                Part part = i < group.parts.size() ? group.parts.get(i) : selected;
                item.shiftHours[i] = part.hour;
                item.shiftMinutes[i] = part.minute;
                item.shiftEnabled[i] = part.enabled;
            }
            result.add(item);
        }
        return result;
    }

    /** The running shift, else the first enabled one, else the first part. */
    private static Part pickPart(List<Part> parts, int current) {
        for (Part part : parts) {
            if (part.index == current && part.enabled) return part;
        }
        for (Part part : parts) {
            if (part.enabled) return part;
        }
        return parts.get(0);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** One shift group read from the provider. */
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

    /** One shift inside a group. */
    private static final class Part {
        int rowId;
        int alarmId;
        int hour;
        int minute;
        int index;
        boolean enabled;
    }
}
