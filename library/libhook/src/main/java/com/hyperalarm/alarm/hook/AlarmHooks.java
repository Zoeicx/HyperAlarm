package com.hyperalarm.alarm.hook;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.hyperalarm.common.alarm.AlarmContract;
import com.hyperalarm.common.alarm.BridgeContract;
import com.hyperalarm.common.log.XposedLog;
import com.hyperalarm.alarm.core.SettingsKeys;
import com.hyperalarm.alarm.core.TaskManager;

import de.robv.android.xposed.AndroidAppHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.util.Iterator;
import java.util.List;

/**
 * Everything the module installs inside the clock process. All hooks are installed only in the
 * {@code com.android.deskclock} process; nothing here may run anywhere else.
 *
 * <p>Each hook is installed in its own try block: a renamed or missing class on a newer Mi Clock
 * version must not stop the remaining hooks (the alarm runner in particular) from working.</p>
 */
public final class AlarmHooks {
    private static final String TAG = "AlarmHooks";

    private static final String CLOCK_PKG = "com.android.deskclock";
    private static final String CLOCK_ALARM = "com.android.deskclock.Alarm";
    private static final String ALARM_HELPER = "com.android.deskclock.util.AlarmHelper";
    private static final String ALARM_FRAGMENT = "com.android.deskclock.alarm.AlarmClockFragment";
    private static final String CLOCK_UTIL = "com.android.deskclock.util.Util";
    private static final String SHIFT_DATA_HELPER =
            "com.android.deskclock.alarm.shiftalarm.ShiftAlarmDataHelper";
    private static final String MODULE_PKG = "com.hyperalarm";
    /** Menu item id for the module entry; high enough to stay clear of the clock's own ids. */
    private static final int MENU_ID = 16805;
    /**
     * Marks that this process already has the hooks installed.
     *
     * <p>A plain static field only survives inside one module classloader, and a hot reload hands
     * the clock a fresh one: the new generation would install every hook a second time and each
     * alarm would run twice. The flag therefore lives in a system property, which is process wide.
     * </p>
     */
    private static final String PROP_INSTALLED = "hyperalarm.hooks.installed";
    private static final String PROP_OBSERVING = "hyperalarm.alarms.observed";
    private static volatile boolean installed;
    /** Module icon for the overflow entry, built once per process. */
    private static volatile Bitmap sMenuIcon;
    /** Watches the alarm tables for the app; kept here so it is registered exactly once. */
    private static ContentObserver alarmChangeObserver;

    private AlarmHooks() {
    }

    public static void install(ClassLoader cl) {
        try {
            installHooks(cl);
        } catch (Throwable t) {
            // ModernInit calls this from the framework's package-loaded callback: a throw here takes
            // the clock process down before it can run an alarm.
            XposedLog.e(TAG, "hook installation failed", t);
        }
    }

    private static void installHooks(ClassLoader cl) {
        if (installed || System.getProperty(PROP_INSTALLED) != null) {
            installed = true;
            return;
        }
        synchronized (AlarmHooks.class) {
            if (installed || System.getProperty(PROP_INSTALLED) != null) {
                installed = true;
                return;
            }
            installed = true;

            // Channels and the bridge receiver must exist before the first alarm fires.
            try {
                XposedHelpers.findAndHookMethod("com.android.deskclock.DeskClockApp", cl,
                        "onCreate", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!(param.thisObject instanceof Application)) return;
                                Application app = (Application) param.thisObject;
                                // Notifications, the bridge receiver and the observer are independent:
                                // one of them throwing must not take the clock process down with it.
                                try {
                                    TaskManager.initChannels(app);
                                } catch (Throwable t) {
                                    XposedLog.e(TAG, "notification channels failed", t);
                                }
                                try {
                                    ShiftBridge.registerReceiver(app);
                                } catch (Throwable t) {
                                    XposedLog.e(TAG, "bridge receiver failed", t);
                                }
                                try {
                                    observeAlarms(app);
                                } catch (Throwable t) {
                                    XposedLog.e(TAG, "alarm observer failed", t);
                                }
                            }
                        });
            } catch (Throwable t) {
                XposedLog.e(TAG, "DeskClockApp hook failed", t);
            }

            // The alarm itself: this is where a script label turns into a root run.
            try {
                Class<?> alarmService = XposedHelpers.findClass(
                        "com.android.deskclock.alarm.alert.AlarmService", cl);
                int hooked = XposedBridge.hookAllMethods(alarmService, "handleAlarm", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // Suppressing the UI is setResult(null); on an overload that returns a value
                        // the clock would then use null as an object.
                        if (param.method instanceof java.lang.reflect.Method) {
                            Class<?> returnType = ((java.lang.reflect.Method) param.method).getReturnType();
                            if (returnType != void.class) {
                                XposedLog.w(TAG, "skipping non-void handleAlarm overload: " + returnType);
                                return;
                            }
                        }
                        try {
                            onHandleAlarm(param);
                        } catch (Throwable t) {
                            // Whatever goes wrong here, the alarm has to keep working: an exception
                            // out of this callback travels into the clock's own handleAlarm.
                            XposedLog.e(TAG, "handling the alarm failed", t);
                        }
                    }
                });
                if (hooked == 0) {
                    // The class exists but no overload carries the name any more: a clock update
                    // renamed it. Say so loudly - "hooks installed" would otherwise hide it.
                    XposedLog.e(TAG, "AlarmService.handleAlarm not found, alarm hook is a no-op");
                }
            } catch (Throwable t) {
                XposedLog.e(TAG, "AlarmService hook failed", t);
            }

            // Entry point in the clock's overflow menu.
            try {
                Class<?> tab = XposedHelpers.findClass(
                        "com.android.deskclock.TabNavigatorContentFragment", cl);
                int menuHooked = XposedBridge.hookAllMethods(tab, "onCreateOptionsMenu", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            addLogMenu(param);
                        } catch (Throwable t) {
                            XposedLog.e(TAG, "menu entry failed", t);
                        }
                    }
                });
                if (menuHooked == 0) XposedLog.w(TAG, "onCreateOptionsMenu not found, no menu entry");
            } catch (Throwable t) {
                XposedLog.e(TAG, "menu hook failed", t);
            }

            // Optional list filter: hide script alarms from the clock's own list.
            try {
                Class<?> adapter = XposedHelpers.findClass(
                        "com.android.deskclock.alarm.AlarmAdapter", cl);
                int listHooked = XposedBridge.hookAllMethods(adapter, "initData", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        filterScriptAlarms(param);
                    }
                });
                if (listHooked == 0) XposedLog.w(TAG, "initData not found, list filter is a no-op");
            } catch (Throwable t) {
                XposedLog.e(TAG, "alarm list hook failed", t);
            }

            // The home page's "next alarm" line is computed from the full alarm set, so hiding
            // script tasks from the list alone left it counting down to an alarm nobody can see.
            // This rewrites the shown text only; scheduling keeps seeing every alarm.
            try {
                Class<?> alarmClass = XposedHelpers.findClass(CLOCK_ALARM, cl);
                XposedHelpers.findAndHookMethod(ALARM_FRAGMENT, cl, "updateNextAlertTime",
                        alarmClass, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                showNextRealAlarm(param);
                            }
                        });
            } catch (Throwable t) {
                XposedLog.e(TAG, "next alert text hook failed", t);
            }

            System.setProperty(PROP_INSTALLED, "1");
            // Warm the script index so the "is this label runnable" check has an answer before the
            // first alarm fires; done on a worker thread, never here.
            TaskManager.refreshScriptIndex();
            XposedLog.i(TAG, "hooks installed in " + CLOCK_PKG);
        }
    }

    /**
     * Runs the script an alarm label points at.
     *
     * <p>{@code script.sh} keeps the alarm silent (the run is the point of the alarm), while
     * {@code @script.sh} lets the normal alarm flow continue and runs the script in parallel. The
     * label is only a file name: {@link TaskManager#start} refuses anything that is not a plain
     * script name in the module's script directory.</p>
     */
    private static void onHandleAlarm(XC_MethodHook.MethodHookParam param) {
        Object alarm = findAlarm(param.args);
        if (alarm == null) return;

        String label;
        int id;
        try {
            label = (String) XposedHelpers.getObjectField(alarm, "label");
            id = XposedHelpers.getIntField(alarm, "id");
        } catch (Throwable t) {
            return;
        }
        if (label == null) return;
        label = label.trim();
        if (!label.endsWith(".sh")) return;

        boolean ring = label.startsWith("@");
        String script = ring ? label.substring(1).trim() : label;
        if (script.isEmpty()) return;
        if (!(param.thisObject instanceof Service)) return;
        Service service = (Service) param.thisObject;

        // A label that no longer matches a file would otherwise swallow the alarm: the UI is
        // suppressed, the script is rejected, and all the user gets is a notification that goes away.
        // The directory listing is read in the background (the clock process cannot open it), so this
        // is a memory lookup; while the index is still empty the old behaviour is kept.
        if (!ring && !TaskManager.isScriptKnown(script)) {
            XposedLog.w(TAG, "script missing for alarm " + id + ": " + script + ", keeping it audible");
            TaskManager.start(service, id, script, false);   // reports the rejection, without muting
            return;
        }

        // Suppressing the UI also suppresses the clock's own rescheduling, so the next alert has to
        // be arranged first. If that fails (a clock update renamed a helper, a shift row we cannot
        // handle), the alarm is left audible: ringing one more time is much better than suppressing
        // an alarm that will never be scheduled again.
        boolean suppressed = !ring && reschedule(service, alarm, id);
        if (!ring && !suppressed) {
            XposedLog.w(TAG, "rescheduling failed for alarm " + id + ", keeping it audible");
        }
        if (suppressed) param.setResult(null);
        TaskManager.start(service, id, script, suppressed);
        TaskManager.refreshScriptIndex();
    }

    /** Finds the alarm argument without depending on its exact position in the signature. */
    private static Object findAlarm(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg == null) continue;
            Class<?> c = arg.getClass();
            while (c != null) {
                if ("com.android.deskclock.Alarm".equals(c.getName())) return arg;
                c = c.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Arranges the next alert for an alarm whose UI is about to be suppressed.
     *
     * @return true when the clock accepted the rescheduling, so the caller may suppress the UI;
     * false when nothing could be arranged and the alarm should ring normally
     */
    private static boolean reschedule(Service service, Object alarm, int id) {
        try {
            Object days = XposedHelpers.getObjectField(alarm, "daysOfWeek");
            boolean repeat = days != null
                    && Boolean.TRUE.equals(XposedHelpers.callMethod(days, "isRepeatSet"));
            // Shift rows carry bit 512 and type 2; their rotation is scheduled by the clock's own
            // setNextAlert, and treating them as one-shot would disable and delete the row.
            if (!repeat && isShiftAlarm(alarm, days)) repeat = true;

            Class<?> helper = XposedHelpers.findClass(
                    "com.android.deskclock.util.AlarmHelper", service.getClassLoader());
            if (repeat) {
                XposedHelpers.callStaticMethod(helper, "setNextAlert", service);
                return true;
            }
            XposedHelpers.callStaticMethod(helper, "enableAlarm", service, id, false);
            // Everything below is cleanup, and it runs after the row is already disabled. Letting a
            // cleanup failure turn into "rescheduling failed" made the caller keep the alarm audible
            // on a row that is dead - it would ring this once and never again.
            try {
                XposedHelpers.callStaticMethod(helper, "tryDeleteOneshotAlarm", service, alarm);
            } catch (Throwable t) {
                XposedLog.w(TAG, "one-shot cleanup failed: " + t);
            }
            // The clock only marks a dismissed one-shot alarm disabled and cleans the row up
            // on its own next refresh; with the UI suppressed it never gets that far, so the
            // row would stay in the list as a dead entry. Delete it here instead.
            deleteOneshotRow(service, id);
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, "reschedule failed", t);
            return false;
        }
    }

    /** True for a shift alarm row, whose next run is part of a rotation, not a single occurrence. */
    private static boolean isShiftAlarm(Object alarm, Object days) {
        try {
            // Shift rows live in the alarm table itself (type 2, the value AlarmContract calls
            // TYPE_HIDDEN because the clock treats them as bookkeeping) and carry daysofweek 512.
            int type = XposedHelpers.getIntField(alarm, "type");
            if (type == com.hyperalarm.common.alarm.AlarmContract.TYPE_HIDDEN) return true;
        } catch (Throwable ignored) {
        }
        try {
            Object coded = XposedHelpers.callMethod(days, "getCoded");
            if (coded instanceof Integer) {
                return (((Integer) coded) & com.hyperalarm.common.alarm.AlarmContract.SHIFT_DAYS_BIT) != 0;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Removes a fired one-shot alarm row, unless the clock already did it.
     *
     * <p>Runs in the clock process, so the provider accepts the call without root. A row that is
     * already gone (the clock beat us to it) is not an error.</p>
     */
    private static void deleteOneshotRow(Context context, int alarmId) {
        try {
            int deleted = context.getContentResolver().delete(
                    android.net.Uri.parse(com.hyperalarm.common.alarm.AlarmContract.ALARM_URI
                            + "/" + alarmId), null, null);
            if (deleted > 0) XposedLog.i(TAG, "one-shot alarm removed: " + alarmId);
        } catch (Throwable t) {
            XposedLog.e(TAG, "one-shot alarm delete failed: " + alarmId, t);
        }
    }

    /**
     * Drops script alarms from the clock's list when the user asked for it.
     *
     * <p>The switch lives in the system global settings key {@code hyperalarm_hide_script_alarms}
     * because the clock process cannot read the module's own preferences cheaply. Only the list
     * rows are filtered; scheduling is untouched, so a hidden alarm still fires.</p>
     */
    private static void filterScriptAlarms(XC_MethodHook.MethodHookParam param) {
        if (param.args == null || param.args.length == 0) return;
        Object raw = param.args[0];
        if (!(raw instanceof List)) return;

        Context context = null;
        try {
            Object value = XposedHelpers.getObjectField(param.thisObject, "mContext");
            if (value instanceof Context) context = (Context) value;
        } catch (Throwable ignored) {
        }
        if (context == null) context = AndroidAppHelper.currentApplication();
        if (context == null) return;

        try {
            FilterState filter = new FilterState(context);
            if (!filter.hideSilent) return;
            List<?> list = (List) raw;
            Iterator<?> iterator = list.iterator();
            while (iterator.hasNext()) {
                Object bean = iterator.next();
                if (bean == null) continue;
                try {
                    Object alarm = XposedHelpers.getObjectField(bean, "alarm");
                    if (alarm == null) continue;
                    Object value = XposedHelpers.getObjectField(alarm, "label");
                    if (!(value instanceof String)) continue;
                    if (hidesTask(filter, (String) value)) iterator.remove();
                } catch (Throwable t) {
                    XposedLog.w(TAG, "alarm row filter skipped: " + t);
                }
            }
        } catch (Throwable t) {
            // A silent failure here looks exactly like "the switch does nothing"; say so once.
            XposedLog.w(TAG, "alarm list filter failed: " + t);
        }
    }

    /**
     * Relays alarm table changes to the module app.
     *
     * <p>The app cannot register its own observer: the clock's provider requires
     * {@code miui.permission.ACCESS_ALARM_PROVIDER}, which only the clock holds. Running the watch
     * here means the module's list can follow a change (the clock firing an alarm, another app
     * touching the provider) without polling.</p>
     */
    private static void observeAlarms(Context app) {
        if (alarmChangeObserver != null || System.getProperty(PROP_OBSERVING) != null) return;
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable relay = () -> {
            try {
                // The scripts directory may have changed together with the alarms (a new script
                // imported, an old one deleted); re-read it before telling the app.
                TaskManager.refreshScriptIndex();
                Intent intent = new Intent(BridgeContract.ACTION_ALARMS_CHANGED);
                intent.setPackage(MODULE_PKG);
                app.sendBroadcast(intent);
            } catch (Throwable t) {
                XposedLog.e(TAG, "alarm change relay failed", t);
            }
        };
        ContentObserver observer = new ContentObserver(handler) {
            private boolean pending;

            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (pending) return;
                pending = true;
                // A burst (multi-select delete, shift group save) becomes one broadcast.
                handler.postDelayed(() -> {
                    pending = false;
                    relay.run();
                }, 300L);
            }
        };
        try {
            app.getContentResolver().registerContentObserver(
                    Uri.parse(AlarmContract.ALARM_URI), true, observer);
            app.getContentResolver().registerContentObserver(
                    Uri.parse(AlarmContract.SHIFT_URI), true, observer);
            alarmChangeObserver = observer;
            System.setProperty(PROP_OBSERVING, "1");
        } catch (Throwable t) {
            XposedLog.e(TAG, "alarm watch failed", t);
        }
    }

    /** A script task that also lets the alarm ring: {@code @x.sh}. */
    private static boolean isRingingTask(String label) {
        return label != null && label.trim().startsWith("@");
    }

    /** Settings needed by {@link #hidesTask}, read once instead of per row. */
    private static final class FilterState {
        final boolean hideSilent;
        final boolean hideRinging;

        FilterState(Context context) {
            hideSilent = readGlobal(context, SettingsKeys.CLOCK_FILTER) == 1;
            hideRinging = readGlobal(context, SettingsKeys.CLOCK_FILTER_RINGING) == 1;
        }

        private static int readGlobal(Context context, String key) {
            try {
                return Settings.Global.getInt(context.getContentResolver(), key, 0);
            } catch (Throwable t) {
                return 0;
            }
        }
    }

    private static boolean hidesTask(Context context, String label) {
        return hidesTask(new FilterState(context), label);
    }

    /**
     * Whether this label is hidden from the clock list right now.
     *
     * <p>Silent tasks go as soon as the list filter is on; ringing ones ({@code @x.sh}) only when the
     * second switch is on too. The alarm list, the next-alarm line and the countdown lookup all go
     * through this, so they cannot disagree.</p>
     */
    private static boolean hidesTask(FilterState filter, String label) {
        if (!isScriptLabel(label)) return false;
        if (!filter.hideSilent) return false;
        if (!isRingingTask(label)) return true;
        return filter.hideRinging;
    }

    /** An alarm label that names a script: {@code x.sh} or {@code @x.sh}. */
    private static boolean isScriptLabel(String raw) {
        if (raw == null) return false;
        String label = raw.trim();
        if (label.startsWith("@")) label = label.substring(1).trim();
        return label.endsWith(".sh");
    }

    /**
     * Replaces the home page's "next alarm" line when it points at a hidden script task.
     *
     * <p>Only the text is rewritten. {@code AlarmHelper.setNextAlert} and everything else that
     * schedules keeps working on the full alarm set, so a script alarm still fires at its time;
     * it is merely invisible while the clock list filter is on.</p>
     */
    private static void showNextRealAlarm(XC_MethodHook.MethodHookParam param) {
        if (param.args == null || param.args.length == 0) return;
        Object shown = param.args[0];
        if (shown == null) return;

        String label;
        TextView view;
        try {
            label = (String) XposedHelpers.getObjectField(shown, "label");
            view = (TextView) XposedHelpers.getObjectField(param.thisObject, "mNextAlertText");
        } catch (Throwable t) {
            return;
        }
        if (view == null) return;

        Context context = view.getContext();
        if (context == null) return;
        try {
            // The clock only calls this for an alarm it is going to show, so the line belongs on
            // screen. Restoring visibility first is what makes the hidden case below recoverable:
            // hiding it once used to be permanent, because the "this alarm is not a script task"
            // path returned before anything could make it visible again.
            FilterState filter = new FilterState(context);
            if (!filter.hideSilent) {
                view.setVisibility(View.VISIBLE);
                return;
            }
            if (!hidesTask(filter, label)) {
                view.setVisibility(View.VISIBLE);
                return;
            }
            Long next = nextRealAlarmTime(context);
            if (next == null) return;
            if (next <= 0L) {
                // Nothing visible left to count down to: an empty line, not a hidden one, so the
                // next refresh can still fill it in.
                view.setText("");
                return;
            }
            int array = context.getResources()
                    .getIdentifier("alarm_in_furture_new_array", "array", CLOCK_PKG);
            if (array == 0) return;
            Object text = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass(CLOCK_UTIL, context.getClassLoader()),
                    "formatToast", context, next, array);
            if (!(text instanceof String)) return;
            view.setText((String) text);
            view.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            // A display problem must never travel into the clock's own refresh.
            XposedLog.e(TAG, "next alert text failed", t);
        }
    }

    /**
     * Start of the earliest plain alarm that is not a script task.
     *
     * @return milliseconds, 0 when there is none, or {@code null} when this cannot be decided: a
     * shift alarm is enabled and its next time comes from clock code this module does not mirror,
     * in which case the original text is left alone rather than showing a wrong time.
     */
    private static Long nextRealAlarmTime(Context context) {
        ClassLoader cl = context.getClassLoader();
        // One settings read for the whole scan: hidesTask() used to hit SettingsProvider per row.
        FilterState filter = new FilterState(context);
        long best = 0L;
        Cursor cursor = null;
        try {
            Class<?> helper = XposedHelpers.findClass(ALARM_HELPER, cl);
            Class<?> alarmClass = XposedHelpers.findClass(CLOCK_ALARM, cl);
            cursor = (Cursor) XposedHelpers.callStaticMethod(helper, "getAlarmsCursor",
                    context.getContentResolver());
            if (cursor == null) return null;
            while (cursor.moveToNext()) {
                Object alarm = XposedHelpers.newInstance(alarmClass, cursor);
                if (!XposedHelpers.getBooleanField(alarm, "enabled")) continue;
                // Bookkeeping rows (shift parts) are not shown by the list either; counting down to
                // them would point at something the user cannot see.
                if (XposedHelpers.getIntField(alarm, "type") != 0) continue;
                if (hidesTask(filter, (String) XposedHelpers.getObjectField(alarm, "label"))) continue;
                Object time = XposedHelpers.callStaticMethod(helper, "calculateAlarmTime",
                        context, alarm);
                if (!(time instanceof Long)) continue;
                long value = (Long) time;
                if (value > 0L && (best == 0L || value < best)) best = value;
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "next alert lookup failed", t);
            return null;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
        }

        long shift = nextShiftAlarmTime(cl, filter);
        if (shift < 0L) return null;
        if (shift > 0L && (best == 0L || shift < best)) best = shift;
        return best;
    }

    /**
     * Next start of an enabled shift alarm group, or 0 when there is none.
     *
     * <p>Shift groups keep their own schedule (start date, rotation, per-class times), so the time
     * is taken from the clock's own grouping code instead of being recomputed here:
     * {@code ShiftAlarmDataHelper.getShowAlarms} returns one prepared {@code Alarm} per group with
     * {@code time} already filled in.</p>
     *
     * @return -1 when the clock did not give a usable answer, so the caller keeps its own text
     */
    private static long nextShiftAlarmTime(ClassLoader cl, FilterState filter) {
        try {
            Object show = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass(SHIFT_DATA_HELPER, cl), "getShowAlarms");
            if (!(show instanceof List)) return -1L;
            long best = 0L;
            for (Object item : (List<?>) show) {
                if (item == null) continue;
                if (!XposedHelpers.getBooleanField(item, "enabled")) continue;
                // Same predicate as the list and the plain-alarm scan: a ringing task the user chose
                // to keep visible must not be skipped by the countdown either.
                Object label = XposedHelpers.getObjectField(item, "label");
                if (label instanceof String && hidesTask(filter, (String) label)) continue;
                Object time = XposedHelpers.getObjectField(item, "time");
                if (!(time instanceof Long)) continue;
                long value = (Long) time;
                if (value > 0L && (best == 0L || value < best)) best = value;
            }
            return best;
        } catch (Throwable t) {
            XposedLog.e(TAG, "shift alarm lookup failed", t);
            return -1L;
        }
    }

    /** Adds the module entry (with the module's own icon) to the clock's overflow menu. */
    private static void addLogMenu(XC_MethodHook.MethodHookParam param) {
        Menu menu = null;
        for (Object arg : param.args) {
            if (arg instanceof Menu) {
                menu = (Menu) arg;
                break;
            }
        }
        if (menu == null) return;

        Context context = contextOf(param.thisObject);
        if (context == null) context = AndroidAppHelper.currentApplication();
        if (context == null) return;
        final Context menuContext = context;

        MenuItem existing = menu.findItem(MENU_ID);
        if (existing == null) {
            int groupId = menu.size() > 0 ? menu.getItem(0).getGroupId() : 0;
            existing = menu.add(groupId, MENU_ID, 0, "HyperAlarm");
            existing.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            existing.setOnMenuItemClickListener(mi -> {
                startModule(menuContext);
                return true;
            });
        }
        MenuItem item = existing;
        // The icon is refreshed on every menu build instead of only when the item is created: the
        // clock rebuilds the menu for the overflow popup, and an item that already exists came back
        // without one.
        setMenuIcon(item, context);
        showIconsInOverflow(menu);
    }

    /**
     * Puts the module icon on the menu item.
     *
     * <p>The launcher icon is adaptive, so it cannot be used as a menu drawable directly; it is
     * drawn into a bitmap at the menu's density. Both the bitmap and a previous failure are cached,
     * because {@code getApplicationIcon} of another package can throw right after an update and a
     * menu that asks again on every build would otherwise keep a blank entry.</p>
     */
    private static void setMenuIcon(MenuItem item, Context context) {
        Bitmap icon = sMenuIcon;
        if (icon == null) {
            try {
                icon = drawMenuIcon(context.getResources().getDisplayMetrics().density);
                sMenuIcon = icon;
            } catch (Throwable t) {
                XposedLog.e(TAG, "app menu icon failed: " + t, t);
            }
        }
        try {
            if (icon != null) {
                item.setIcon(new BitmapDrawable(context.getResources(), icon));
            } else {
                item.setIcon(android.R.drawable.ic_menu_recent_history);
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "app menu icon apply failed: " + t, t);
        }
    }

    /**
     * Draws the module icon for the overflow entry.
     *
     * <p>The clock process cannot reach this module through its own PackageManager: asking for
     * {@code com.hyperalarm}'s icon throws {@code NameNotFoundException} there, which is why the
     * entry used to appear with an icon only when the lookup happened to be cached. The icon is
     * simple geometry, so it is drawn here from the same coordinates as
     * {@code drawable/ic_launcher_foreground.xml} and needs no resources at all.</p>
     */
    private static Bitmap drawMenuIcon(float density) {
        int size = Math.max(1, Math.round(24.0f * density));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Same geometry as drawable/ic_launcher_foreground.xml (ring open at the lower left, hands,
        // chevron and dash on a 108 grid with the glyph grouped at 40%), just scaled up so the mark
        // fills the menu slot: the launcher draws this into a 108dp canvas where the 40% group is
        // right, but a 24dp menu slot would leave it tiny.
        float group = 0.7f * size / 74.0f;          // glyph fills 70% of the slot
        canvas.translate(size / 2.0f, size / 2.0f);
        canvas.scale(group, group);
        canvas.translate(-54.0f, -54.0f);           // the vector's group pivot

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(0xFFFFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);

        // M18,54 A36,36 0 1,1 54,90 - 270 degrees clockwise from the left, open at the lower left
        stroke.setStrokeWidth(7.5f);
        Path ring = new Path();
        ring.arcTo(new RectF(18, 18, 90, 90), 180, 270, true);
        canvas.drawPath(ring, stroke);

        // M54,32 L54,54 L70,65 - hands
        Path hands = new Path();
        hands.moveTo(54, 32);
        hands.lineTo(54, 54);
        hands.lineTo(70, 65);
        canvas.drawPath(hands, stroke);

        // M16,64 L32,76 L16,88 and M34,88 L44,88 - the chevron and the dash that mean "run"
        stroke.setStrokeWidth(8.5f);
        Path chevron = new Path();
        chevron.moveTo(16, 64);
        chevron.lineTo(32, 76);
        chevron.lineTo(16, 88);
        canvas.drawPath(chevron, stroke);
        canvas.drawLine(34, 88, 44, 88, stroke);
        return bitmap;
    }

    /**
     * Asks the menu to render icons inside the overflow list.
     *
     * <p>{@code setOptionalIconsVisible} is off by default, so an entry that only lives in the
     * overflow - which this one does - is drawn as text alone. MIUI and AppCompat both keep the flag
     * on their own menu implementation, so both are tried.</p>
     */
    private static void showIconsInOverflow(Menu menu) {
        for (Object target : new Object[]{menu}) {
            try {
                XposedHelpers.callMethod(target, "setOptionalIconsVisible", true);
                return;
            } catch (Throwable ignored) {
            }
            try {
                // Some MIUI builds wrap the builder; reach through the usual accessor.
                Object builder = XposedHelpers.callMethod(target, "getMenuBuilder");
                XposedHelpers.callMethod(builder, "setOptionalIconsVisible", true);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static Context contextOf(Object fragment) {
        if (fragment != null) {
            Object activity = XposedHelpers.callMethod(fragment, "getActivity");
            if (activity instanceof Context) return (Context) activity;
            try {
                Object context = XposedHelpers.callMethod(fragment, "getContext");
                if (context instanceof Context) return (Context) context;
            } catch (Throwable ignored) {
            }
        }
        return AndroidAppHelper.currentApplication();
    }

    private static void startModule(Context context) {
        if (context == null) return;
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(MODULE_PKG);
            if (intent == null) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(MODULE_PKG, MODULE_PKG + ".ui.SplashActivity");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            XposedLog.e(TAG, "open module failed", t);
        }
    }

}

