package com.hyperalarm.alarm.hook;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;

import com.hyperalarm.common.utils.ThreadUtils;
import com.hyperalarm.common.log.XposedLog;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.alarm.core.ScriptPath;
import com.hyperalarm.alarm.core.TaskManager;
import com.hyperalarm.common.alarm.BridgeContract;
import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;

/**
 * Creates a real Mi Clock shift group through the clock's own
 * ShiftAlarmDbHelper. The provider only supports querying shift_alarms, so the
 * write has to happen inside the clock process.
 *
 * <p>The receiver is the module's only inbound channel: alarm writes, shift writes, ringtone
 * selection and manual script runs all arrive here as ordered broadcasts guarded by
 * {@code hyperalarm.permission.BRIDGE}. Every branch answers through {@code setResultCode} so the
 * caller can tell a successful write from a silently dropped one.</p>
 */
public final class ShiftBridge {
    private static final String TAG = "ShiftBridge";

    public static final String BRIDGE_PERMISSION = BridgeContract.PERMISSION;
    public static final String SCRIPTS_DIR = TaskManager.SCRIPTS_DIR;
    // The names live in BridgeContract so the app and this receiver cannot drift apart.
    public static final String ACTION_CREATE_SHIFT = BridgeContract.ACTION_CREATE_SHIFT;
    public static final String ACTION_ADD_ALARM = BridgeContract.ACTION_ADD_ALARM;
    public static final String ACTION_DELETE_SHIFT = BridgeContract.ACTION_DELETE_SHIFT;
    public static final String ACTION_RUN_SCRIPT = BridgeContract.ACTION_RUN_SCRIPT;
    public static final String ACTION_QUERY_ALARMS = BridgeContract.ACTION_QUERY_ALARMS;
    public static final String ACTION_UPDATE_ALARM = BridgeContract.ACTION_UPDATE_ALARM;
    public static final String ACTION_DELETE_ALARM = BridgeContract.ACTION_DELETE_ALARM;
    public static final String ACTION_SET_ENABLED = BridgeContract.ACTION_SET_ENABLED;
    public static final String ACTION_SET_RINGTONE = BridgeContract.ACTION_SET_RINGTONE;
    public static final String ACTION_OPEN_RINGTONE = BridgeContract.ACTION_OPEN_RINGTONE;
    public static final String ACTION_RINGTONE_PICKED = BridgeContract.ACTION_RINGTONE_PICKED;

    private static final String ALARM_URI = "content://com.android.deskclock/alarm";
    private static final String SHIFT_URI = "content://com.android.deskclock/shift_alarms";

    /** Set while the clock's own ringtone picker is open so its result can be routed back. */
    private static volatile int pendingRingtoneId = -1;
    private static volatile boolean pendingRingtoneShift;
    /** When the pending request stops being attributed to a picker result. */
    private static volatile long pendingRingtoneExpiresAt;
    /** Ten minutes is far longer than a picker session and short enough not to leak into the next. */
    private static final long RINGTONE_PICK_TIMEOUT_MS = 10 * 60 * 1000L;

    /** Process wide marker: a reloaded module generation must not register a second receiver. */
    private static final String PROP_REGISTERED = "hyperalarm.bridge.registered";
    private static boolean registered;

    private ShiftBridge() {
    }

    public static void registerReceiver(Application app) {
        if (registered || System.getProperty(PROP_REGISTERED) != null) return;
        synchronized (ShiftBridge.class) {
            if (registered || System.getProperty(PROP_REGISTERED) != null) return;
            registered = true;
            hookRingtonePicker(app);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    if (!fromModuleApp(context)) return;
                    String action = intent.getAction();

                    // Opening the picker has to happen on the main thread; everything else below
                    // reads or writes the alarm provider or reflects into the shift API, and doing
                    // that inside onReceive blocks the clock's main thread - which also delays the
                    // ordered broadcast the app is waiting on.
                    if (ACTION_OPEN_RINGTONE.equals(action)) {
                        pendingRingtoneId = intent.getIntExtra(BridgeContract.EXTRA_ID, -1);
                        pendingRingtoneShift = intent.getBooleanExtra(BridgeContract.EXTRA_SHIFT, false);
                        // Expires on its own: if the user cancels the picker, the id must not stay
                        // around to be attributed to the next picker the clock opens by itself.
                        pendingRingtoneExpiresAt = System.currentTimeMillis() + RINGTONE_PICK_TIMEOUT_MS;
                        openRingtonePicker(context);
                        setResultCode(1);
                        return;
                    }

                    final PendingResult pending = goAsync();
                    final Context app = context.getApplicationContext() != null
                            ? context.getApplicationContext() : context;
                    ThreadUtils.runOnBackground(() -> {
                        try {
                            dispatch(pending, app, action, intent);
                        } catch (Throwable t) {
                            XposedLog.e(TAG, "bridge action failed: " + action, t);
                        } finally {
                            pending.finish();
                        }
                    });
                }

                /** The part that does the actual work; runs on a worker thread. */
                private void dispatch(PendingResult pending, Context context, String action,
                                      Intent intent) {
                    // The result has to be set on the PendingResult from goAsync(); calling the
                    // receiver's own setResultCode() from this thread throws
                    // "Call while result is not pending" and the app sees the action as failed.
                    if (ACTION_UPDATE_ALARM.equals(action)) {
                        pending.setResultCode(updateAlarm(context, intent) ? 1 : 0);
                        return;
                    }
                    if (ACTION_DELETE_ALARM.equals(action)) {
                        pending.setResultCode(deleteAlarm(context, intent) ? 1 : 0);
                        return;
                    }
                    if (ACTION_SET_RINGTONE.equals(action)) {
                        pending.setResultCode(setRingtone(context, intent) ? 1 : 0);
                        return;
                    }
                    if (ACTION_SET_ENABLED.equals(action)) {
                        pending.setResultCode(setEnabled(context, intent) ? 1 : 0);
                        return;
                    }
                    if (ACTION_QUERY_ALARMS.equals(action)) {
                        pending.setResultCode(1);
                        pending.setResultData(HookAlarmQuery.query(context));
                        return;
                    }
                    if (ACTION_RUN_SCRIPT.equals(action)) {
                        boolean ok = runScript(context, intent.getStringExtra(BridgeContract.EXTRA_PATH),
                                intent.getIntExtra(BridgeContract.EXTRA_ALARM_ID, -1));
                        pending.setResultCode(ok ? 1 : 0);
                        return;
                    }
                    if (ACTION_DELETE_SHIFT.equals(action)) {
                        pending.setResultCode(deleteShift(context, intent.getIntExtra(BridgeContract.EXTRA_ALARM_ID, 0)) ? 1 : 0);
                    } else if (ACTION_ADD_ALARM.equals(action)) {
                        int id = addAlarm(context);
                        pending.setResultCode(id > 0 ? 1 : 0);
                        pending.setResultData(String.valueOf(id));
                    } else if (ACTION_CREATE_SHIFT.equals(action)) {
                        pending.setResultCode(create(context, intent) ? 1 : 0);
                    }
                    // Anything else is not ours: no fallback branch, no default action.
                }

                /**
                 * Only the module app may drive the bridge.
                 *
                 * <p>The signature permission is not enough on its own: a custom permission belongs
                 * to whichever app declares it first, so an app installed before this module can
                 * squat {@code hyperalarm.permission.BRIDGE} as a normal permission and then send
                 * RUN_SCRIPT, which ends in a root shell. The sender uid is the part the permission
                 * cannot tell us. A negative uid means the platform would not say (system-posted
                 * broadcasts); those keep relying on the permission, as before.</p>
                 */
                private boolean fromModuleApp(Context context) {
                    // getSentFromUid() is API 34 and minSdk is 35, so this is a direct call: a
                    // security decision is the last place to want a reflective lookup that could
                    // quietly resolve to nothing.
                    int sender = getSentFromUid();
                    if (sender < 0) return true;
                    int expected = -1;
                    try {
                        expected = context.getPackageManager()
                                .getPackageUid(BridgeContract.MODULE_PACKAGE, 0);
                    } catch (Throwable ignored) {
                    }
                    if (expected > 0 && sender == expected) return true;
                    XposedLog.w(TAG, "bridge broadcast rejected, sender uid " + sender
                            + " is not the module app");
                    return false;
                }
            };

            IntentFilter filter = new IntentFilter(ACTION_CREATE_SHIFT);
            filter.addAction(ACTION_ADD_ALARM);
            filter.addAction(ACTION_DELETE_SHIFT);
            filter.addAction(ACTION_RUN_SCRIPT);
            filter.addAction(ACTION_QUERY_ALARMS);
            filter.addAction(ACTION_UPDATE_ALARM);
            filter.addAction(ACTION_DELETE_ALARM);
            filter.addAction(ACTION_SET_ENABLED);
            filter.addAction(ACTION_SET_RINGTONE);
            filter.addAction(ACTION_OPEN_RINGTONE);
            try {
                // minSdk is 35, so the flag-bearing overload is always the one that runs; the
                // permission keeps the receiver signature-protected.
                app.registerReceiver(receiver, filter, BRIDGE_PERMISSION, null,
                        Context.RECEIVER_EXPORTED);
                System.setProperty(PROP_REGISTERED, "1");
            } catch (Throwable t) {
                registered = false;
                XposedLog.e(TAG, "shift receiver register failed", t);
            }
        }
    }

    /** Writes time, label and repeat days; a shift alarm additionally updates its group in place. */
    private static boolean updateAlarm(Context context, Intent intent) {
        try {
            int id = intent.getIntExtra(BridgeContract.EXTRA_ID, -1);
            if (id <= 0) return false;
            int hour = intent.getIntExtra(BridgeContract.EXTRA_HOUR, 0);
            int minute = intent.getIntExtra(BridgeContract.EXTRA_MINUTE, 0);
            int days = intent.getIntExtra(BridgeContract.EXTRA_DAYS, 0);
            boolean shift = intent.getBooleanExtra(BridgeContract.EXTRA_SHIFT, false);
            String label = intent.getStringExtra(BridgeContract.EXTRA_LABEL);

            ContentValues values = new ContentValues();
            values.put("hour", hour);
            values.put("minutes", minute);
            values.put("message", label == null ? "" : label);
            if (!shift) {
                // A shift group owns the schedule of its rows, so repeat days only apply to
                // plain alarms.
                values.put("daysofweek", days);
            }
            context.getContentResolver().update(Uri.parse(ALARM_URI + "/" + id), values, null, null);
            if (shift) {
                updateShift(context, id, hour, minute, label);
            } else {
                // A new time or repeat pattern only takes effect once the next alert is recomputed.
                reschedule(context);
            }
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, "update alarm failed", t);
            return false;
        }
    }

    private static void updateShift(Context context, int alarmId, int hour, int minute, String label) {
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> dataHelper = XposedHelpers.findClass(
                    "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper", cl);
            Object group = XposedHelpers.callStaticMethod(dataHelper,
                    "getShiftGroupFromAlarmId", alarmId);
            if (group == null) return;

            List<?> shifts = (List) XposedHelpers.getObjectField(group, "shiftAlarms");
            if (shifts != null) {
                for (Object shift : shifts) {
                    if (XposedHelpers.getIntField(shift, "alarmId") == alarmId) {
                        setField(shift, "hour", hour);
                        setField(shift, "minutes", minute);
                    }
                }
            }
            setField(group, "label", label == null ? "" : label);

            Class<?> dbClass = XposedHelpers.findClass(
                    "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDbHelper", cl);
            Object db = XposedHelpers.callStaticMethod(dbClass, "getInstance");
            XposedHelpers.callMethod(db, "updateShiftAlarmGroup", group);
            XposedHelpers.callStaticMethod(dataHelper, "resetCache");
            try {
                // Rescheduling is best effort: a group whose next alert cannot be computed is
                // still saved, and the clock recomputes it on its own next pass.
                Class<?> alarmHelper = XposedHelpers.findClass(
                        "com.android.deskclock.util.AlarmHelper", cl);
                XposedHelpers.callStaticMethod(alarmHelper, "setNextAlert", context);
            } catch (Throwable ignored) {
            }
            context.getContentResolver().notifyChange(Uri.parse(SHIFT_URI), null);
        } catch (Throwable t) {
            XposedLog.e(TAG, "update shift failed", t);
        }
    }

    private static boolean deleteAlarm(Context context, Intent intent) {
        try {
            int id = intent.getIntExtra(BridgeContract.EXTRA_ID, -1);
            if (id <= 0) return false;
            if (intent.getBooleanExtra(BridgeContract.EXTRA_SHIFT, false)) return deleteShift(context, id);
            boolean removed = context.getContentResolver()
                    .delete(Uri.parse(ALARM_URI + "/" + id), null, null) > 0;
            // Deleting the row does not remove the PendingIntent that rings it.
            reschedule(context);
            return removed;
        } catch (Throwable t) {
            XposedLog.e(TAG, "delete alarm failed", t);
            return false;
        }
    }

    /** Opens the clock's own ringtone picker so the result uses the real ringtone list. */
    private static void openRingtonePicker(Context context) {
        try {
            Intent intent = new Intent("miui.intent.action.ALARM_RINGTONE_PICKER");
            intent.setClassName("com.android.deskclock",
                    "com.android.deskclock.settings.AlarmRingtonePickerActivity");
            intent.putExtra("is_set_mode", false);
            intent.putExtra("is_from_alarm", true);
            intent.putExtra("android.intent.extra.ringtone.EXISTING_URI",
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", true);
            intent.putExtra("android.intent.extra.ringtone.TYPE", RingtoneManager.TYPE_ALARM);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            XposedLog.e(TAG, "open ringtone picker failed", t);
        }
    }

    /** Routes the picker's result back to the app; the picker itself belongs to the clock. */
    private static void hookRingtonePicker(Application app) {
        try {
            Class<?> cls = XposedHelpers.findClass(
                    "com.android.deskclock.settings.AlarmRingtonePickerActivity",
                    app.getClassLoader());
            XposedBridge.hookAllMethods(cls, "finish", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        int id = pendingRingtoneId;
                        boolean expired = System.currentTimeMillis() > pendingRingtoneExpiresAt;
                        if (id <= 0 || expired || !(param.thisObject instanceof Activity)) {
                            // No request of ours is waiting (or it timed out): this finish() belongs
                            // to a picker the clock opened on its own, so nothing is reported.
                            pendingRingtoneId = -1;
                            return;
                        }
                        Object alert = XposedHelpers.getObjectField(param.thisObject, "mAlert");
                        Intent result = new Intent(ACTION_RINGTONE_PICKED);
                        result.setPackage("com.hyperalarm");
                        result.putExtra(BridgeContract.EXTRA_ID, id);
                        result.putExtra(BridgeContract.EXTRA_SHIFT, pendingRingtoneShift);
                        result.putExtra(BridgeContract.EXTRA_RINGTONE, alert == null ? "" : alert.toString());
                        ((Activity) param.thisObject).sendBroadcast(result);
                        pendingRingtoneId = -1;
                        pendingRingtoneExpiresAt = 0L;
                    } catch (Throwable t) {
                        XposedLog.w(TAG, "ringtone result not delivered: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "ringtone picker hook failed", t);
        }
    }

    private static boolean setRingtone(Context context, Intent intent) {
        try {
            int id = intent.getIntExtra(BridgeContract.EXTRA_ID, -1);
            String value = intent.getStringExtra(BridgeContract.EXTRA_RINGTONE);
            if (id <= 0 || value == null) return false;

            ContentValues values = new ContentValues();
            values.put("alert", value);
            context.getContentResolver().update(Uri.parse(ALARM_URI + "/" + id), values, null, null);

            if (intent.getBooleanExtra(BridgeContract.EXTRA_SHIFT, false)) {
                ClassLoader cl = context.getClassLoader();
                Class<?> dataHelper = XposedHelpers.findClass(
                        "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper", cl);
                Object group = XposedHelpers.callStaticMethod(dataHelper,
                        "getShiftGroupFromAlarmId", id);
                if (group != null) {
                    setField(group, "alert", Uri.parse(value));
                    Class<?> dbClass = XposedHelpers.findClass(
                            "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDbHelper", cl);
                    Object db = XposedHelpers.callStaticMethod(dbClass, "getInstance");
                    XposedHelpers.callMethod(db, "updateShiftAlarmGroup", group);
                    XposedHelpers.callStaticMethod(dataHelper, "resetCache");
                    try {
                        Class<?> alarmHelper = XposedHelpers.findClass(
                                "com.android.deskclock.util.AlarmHelper", cl);
                        XposedHelpers.callStaticMethod(alarmHelper, "setNextAlert", context);
                    } catch (Throwable ignored) {
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, "set ringtone failed", t);
            return false;
        }
    }

    /** The row enable flag; shift groups additionally need their own enable switch flipped. */

    /**
     * Re-arms the clock's next alert after a plain alarm changed.
     *
     * <p>{@code setEnabled} / {@code deleteAlarm} / {@code updateAlarm} only touch the provider; the
     * PendingIntent that actually rings lives in AlarmManager and is only recomputed by
     * {@code AlarmHelper.setNextAlert}. Without it, disabling the only alarm left it ringing.</p>
     */
    private static void reschedule(Context context) {
        try {
            Class<?> alarmHelper = XposedHelpers.findClass(
                    "com.android.deskclock.util.AlarmHelper", context.getClassLoader());
            XposedHelpers.callStaticMethod(alarmHelper, "setNextAlert", context);
        } catch (Throwable t) {
            XposedLog.e(TAG, "setNextAlert after a plain alarm write failed", t);
        }
    }

    private static boolean setEnabled(Context context, Intent intent) {
        try {
            int id = intent.getIntExtra(BridgeContract.EXTRA_ID, -1);
            boolean enabled = intent.getBooleanExtra(BridgeContract.EXTRA_ENABLED, true);
            if (id <= 0) return false;

            ContentValues values = new ContentValues();
            values.put("enabled", enabled ? 1 : 0);
            context.getContentResolver().update(Uri.parse(ALARM_URI + "/" + id), values, null, null);

            if (intent.getBooleanExtra(BridgeContract.EXTRA_SHIFT, false)) {
                ClassLoader cl = context.getClassLoader();
                Class<?> dataHelper = XposedHelpers.findClass(
                        "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper", cl);
                XposedHelpers.callStaticMethod(dataHelper, "enableShiftAlarm", id, enabled, 0L);
            } else {
                reschedule(context);
            }
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, "set enabled failed", t);
            return false;
        }
    }

    /**
     * Runs a script right now (the "run immediately" action in the app) and writes the same log a
     * fired alarm would.
     *
     * <p>The path is resolved through {@link ScriptPath}, so a manual run can only reach the module
     * script directory - the same rule the alarm path follows.</p>
     */
    /**
     * Runs a script requested through the bridge ("run now" in the app).
     *
     * <p>The receiver runs on the clock's main thread, so this only hands the request to
     * {@link TaskManager}: the label is validated, executed and logged on its worker thread, and the
     * result surfaces through the same notification a fired alarm produces. Deferring also keeps the
     * broadcast answer meaningful - it reports "accepted", not "finished after N seconds".</p>
     */
    private static boolean runScript(Context context, String path, int alarmId) {
        String label = path == null ? "" : path.trim();
        if (label.isEmpty()) return false;
        TaskManager.start(context, alarmId, label, true);
        return true;
    }

    private static boolean deleteShift(Context context, int alarmId) {
        if (alarmId <= 0) return false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> dataHelper = XposedHelpers.findClass(
                    "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper", cl);
            XposedHelpers.callStaticMethod(dataHelper, "deleteShiftAlarmByAlarmId", alarmId);
            XposedHelpers.callStaticMethod(dataHelper, "resetCache");
            Class<?> alarmHelper = XposedHelpers.findClass(
                    "com.android.deskclock.util.AlarmHelper", cl);
            XposedHelpers.callStaticMethod(alarmHelper, "setNextAlert", context);
            context.getContentResolver().notifyChange(Uri.parse(SHIFT_URI), null);
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, "delete shift failed", t);
            return false;
        }
    }

    /** Inserts a bare 08:00 alarm and returns its row id, or 0 when the clock refused it. */
    private static int addAlarm(Context context) {
        try {
            ContentValues values = new ContentValues();
            values.put("hour", 8);
            values.put("minutes", 0);
            values.put("enabled", 1);
            values.put("daysofweek", 0);
            values.put("message", "");
            Uri uri = context.getContentResolver().insert(Uri.parse(ALARM_URI), values);
            if (uri == null) return 0;
            return Integer.parseInt(uri.getLastPathSegment());
        } catch (Throwable t) {
            XposedLog.e(TAG, "add alarm failed", t);
            return 0;
        }
    }

    /**
     * Creates or updates a shift group.
     *
     * <p>The group object, its rows and the database write all live behind the clock's reflection
     * surface ({@code ShiftAlarmGroup}, {@code ShiftAlarmDbHelper}, {@code ShiftAlarmDataHelper});
     * writing the {@code shift_alarms} table from outside would bypass the cache the clock keeps in
     * front of it.</p>
     */
    private static boolean create(Context context, Intent intent) {
        try {
            ClassLoader cl = context.getClassLoader();
            int duration = Math.max(1, Math.min(31, intent.getIntExtra(BridgeContract.EXTRA_DURATION, 4)));
            int hour = intent.getIntExtra(BridgeContract.EXTRA_HOUR, 8);
            int minute = intent.getIntExtra(BridgeContract.EXTRA_MINUTE, 0);
            int[] hours = intent.getIntArrayExtra(BridgeContract.EXTRA_HOURS);
            int[] minutes = intent.getIntArrayExtra(BridgeContract.EXTRA_MINUTES);
            boolean[] enabled = intent.getBooleanArrayExtra(BridgeContract.EXTRA_ENABLED);
            int groupRowId = intent.getIntExtra(BridgeContract.EXTRA_GROUP_ROW_ID, 0);
            int alarmId = intent.getIntExtra(BridgeContract.EXTRA_ALARM_ID, 0);
            String label = intent.getStringExtra(BridgeContract.EXTRA_LABEL);
            boolean vibrate = intent.getBooleanExtra(BridgeContract.EXTRA_VIBRATE, true);
            long startTime = intent.getLongExtra(BridgeContract.EXTRA_START_TIME, 0L);
            if (startTime <= 0) {
                // Start of today: the clock derives the current shift from start time + duration.
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                startTime = c.getTimeInMillis();
            }

            Class<?> groupClass = XposedHelpers.findClass(
                    "com.android.deskclock.alarm.shiftalarm.ShiftAlarmGroup", cl);
            Object group = null;
            boolean existing = false;
            if (groupRowId > 0 && alarmId > 0) {
                Class<?> dataHelper = XposedHelpers.findClass(
                        "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper", cl);
                group = XposedHelpers.callStaticMethod(dataHelper, "getShiftGroupFromAlarmId", alarmId);
                existing = group != null;
            }
            if (group == null) {
                group = XposedHelpers.callStaticMethod(groupClass, "getDefault");
            }
            if (group == null) return false;

            XposedHelpers.callMethod(group, "resetDuration", duration);
            setField(group, "alarmCount", duration);
            setField(group, "startTime", startTime);
            setField(group, "enable", true);
            setField(group, "vibrate", vibrate);
            setField(group, "label", label == null ? "" : label);

            List<?> shifts = (List) XposedHelpers.getObjectField(group, "shiftAlarms");
            if (shifts == null || shifts.isEmpty()) return false;
            for (int i = 0; i < shifts.size(); i++) {
                Object shift = shifts.get(i);
                int h = (hours == null || i >= hours.length) ? hour : hours[i];
                int m = (minutes == null || i >= minutes.length) ? minute : minutes[i];
                boolean e = enabled == null || i >= enabled.length || enabled[i];
                setField(shift, "hour", h);
                setField(shift, "minutes", m);
                setField(shift, "enable", e);
            }

            Class<?> dbClass = XposedHelpers.findClass(
                    "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDbHelper", cl);
            Object db = XposedHelpers.callStaticMethod(dbClass, "getInstance");
            XposedHelpers.callMethod(db,
                    existing ? "updateShiftAlarmGroup" : "insertShiftAlarmGroup", group);
            try {
                Class<?> dataClass = XposedHelpers.findClass(
                        "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper", cl);
                XposedHelpers.callStaticMethod(dataClass, "resetCache");
            } catch (Throwable ignored) {
            }
            try {
                Class<?> alarmHelper = XposedHelpers.findClass(
                        "com.android.deskclock.util.AlarmHelper", cl);
                XposedHelpers.callStaticMethod(alarmHelper, "setNextAlert", context);
            } catch (Throwable t) {
                XposedLog.e(TAG, "shift setNextAlert failed", t);
            }
            try {
                context.getContentResolver().notifyChange(Uri.parse(SHIFT_URI), null);
            } catch (Throwable ignored) {
            }
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, "create shift failed", t);
            return false;
        }
    }

    /** Field write that walks the class hierarchy; the clock's model classes inherit their fields. */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = null;
        Class<?> type = target.getClass();
        while (type != null && field == null) {
            try {
                field = type.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        if (field == null) throw new NoSuchFieldException(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
