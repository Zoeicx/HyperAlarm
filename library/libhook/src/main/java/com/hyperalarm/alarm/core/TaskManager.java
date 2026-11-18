package com.hyperalarm.alarm.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.os.Looper;
import android.widget.Toast;

import com.hyperalarm.common.log.XposedLog;
import com.topjohnwu.superuser.Shell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashSet;
import java.util.Set;

import com.hyperalarm.common.alarm.BridgeContract;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.utils.ThreadUtils;

/**
 * Runs one root shell task per (alarm id + script path). The main libsu shell already
 * serializes commands, so different alarms naturally queue behind each other.
 *
 * <p>The script itself is started through {@code nsenter -t 1 -m}: this process lives in the
 * clock's mount namespace, which cannot see the data directories the scripts are written for, so
 * it has to hop into init's namespace first.</p>
 */
public final class TaskManager {
    private static final String TAG = "TaskManager";
    public static final String MODULE_DIR = "/data/adb/alarm_shell";
    public static final String SCRIPTS_DIR = MODULE_DIR + "/scripts";
    public static final String PASSWORD_FILE = MODULE_DIR + "/password";
    public static final String UNLOCK_SCRIPT = MODULE_DIR + "/unlock_phone";

    private static final String CHANNEL_STATE = "alarm_shell_state";
    private static final String CHANNEL_RESULT = "alarm_shell_result";
    private static final int STATE_ID = Integer.MAX_VALUE - 1;
    private static final int RESULT_BASE = 100000;
    private static final int FOREGROUND_ID = 9001;
    /** Per-stream ceiling for captured stdout/stderr; a chatty script must not OOM the clock. */
    private static final int OUTPUT_LIMIT = 256 * 1024;
    /** A run that outlives this is reported as failed instead of blocking the queue. */
    private static final long SCRIPT_TIMEOUT_MS = 30L * 60L * 1000L;
    /** True while a task of this module holds the clock service's foreground state. */
    private static volatile boolean sForegroundOwned;
    /** Channel of the clock's own ringing alarm; if a notification uses it, the clock owns the foreground. */
    private static final String ALARM_CHANNEL = "channel_id_deskclock_alarm";
    private static final AtomicBoolean SETUP_DONE = new AtomicBoolean(false);
    /**
     * Names the last directory listing found, or null before the first listing.
     *
     * <p>The clock process cannot read the script directory itself ({@code /data/adb} is root only),
     * so the check that keeps an alarm audible when its script is missing has to happen against a
     * list read through root in the background. Until that first listing arrives the answer is
     * "assume it exists", which is the behaviour from before the check existed.</p>
     */
    private static final AtomicReference<Set<String>> SCRIPT_INDEX = new AtomicReference<>();
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);
    private static final Map<String, Task> RUNNING = new ConcurrentHashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Runnable stateUpdater;

    /** The default {@code unlock_phone}: wake the screen, swipe, type the password, confirm. */
    /** The app's external files dir; mirrored logs are owned by the app so only it can read them. */
    private static final String APP_FILES_DIR =
            "/sdcard/Android/data/" + BridgeContract.MODULE_PACKAGE + "/files";
    private static final String LOG_MIRROR_DIR = APP_FILES_DIR + "/script_logs/";

    private static final String UNLOCK_SCRIPT_B64 =
            "IyBFeGVjdXRlZCBvbmx5IHdoZW4gdGhlIGFsYXJtIGZpcmVzIHdoaWxlIHRoZSBkZXZpY2UgaXMgbG9ja2VkIGFuZCAvZGF0YSBpcyBzdGlsbCBlbmNyeXB0ZWQuCmlucHV0IGtleWV2ZW50IEtFWUNPREVfV0FLRVVQCnNsZWVwIDEKaW5wdXQgc3dpcGUgNTAwIDE1MDAgNTAwIDUwMApzbGVlcCAxCmlucHV0IHRleHQgJDEKaW5wdXQga2V5ZXZlbnQgS0VZQ09ERV9FTlRFUgpzbGVlcCAxCg==";

    private TaskManager() {
    }

    public static void initChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel state = new NotificationChannel(
                CHANNEL_STATE, "脚本任务状态", NotificationManager.IMPORTANCE_LOW);
        state.setShowBadge(false);
        NotificationChannel result = new NotificationChannel(
                CHANNEL_RESULT, "脚本任务结果", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(state);
        nm.createNotificationChannel(result);
    }

    /**
     * Starts one root task for the script an alarm label points at. The label itself is never
     * trusted: it goes through {@link ScriptPath} so a label like {@code /sdcard/x.sh} or
     * {@code ../x.sh} is rejected instead of being run as root.
     *
     * @param silent true for a plain {@code script.sh} label: the run owns a foreground
     *               notification and the alarm UI stays suppressed
     */
    public static void start(Context context, int alarmId, String rawScript, boolean silent) {
        String script = rawScript == null ? "" : rawScript.trim();
        if (script.isEmpty()) return;

        // Nothing here may block: this runs on the clock's main thread (alarm firing and the
        // bridge's run request both land there). The label is only reserved, the script is
        // resolved and executed on the worker thread.
        String key = alarmId + "-" + script;
        Task task = new Task(key, script, script, alarmId, System.currentTimeMillis(), silent);
        if (RUNNING.putIfAbsent(key, task) != null) {
            toast(context, "当前脚本任务正在执行，已忽略重复触发");
            return;
        }

        initChannels(context);
        try {
            if (silent) startForeground(context, task);
            startStateUpdater(context);
            new Thread(() -> runTask(context, task), "HyperAlarmTask-" + task.name).start();
        } catch (Throwable t) {
            // The reservation above would otherwise outlive a failed start and turn every later
            // trigger of this alarm into a "duplicate" until the clock process restarts.
            RUNNING.remove(key);
            XposedLog.e(TAG, "task start failed: " + t);
            finish(context, task, false, "脚本任务启动失败: " + t, -1, 0);
        }
    }

    private static void runTask(Context context, Task task) {
        try {
            // Resolving the label touches the file system through root (readlink -f), so it happens
            // here rather than in start(), which runs on the clock's main thread.
            String path = ScriptPath.resolve(task.raw);
            if (path == null) {
                RUNNING.remove(task.key);
                reject(context, task.alarmId, task.raw);
                return;
            }
            task.path = path;

            // First run in a fresh clock process: RootShell.isRoot() would answer "not yet" and the
            // task would be reported as "no root" while root works. This is the task thread, so the
            // definitive probe may block here, and it also brings libsu's shell up for ensureSetup.
            if (!RootShell.probeRootNow()) {
                finish(context, task, false, "权限不足，请给小米时钟授予 root 权限", -1, 0);
                return;
            }
            if (!ensureSetup(context)) {
                finish(context, task, false, "模块目录初始化失败，请检查 root 权限", -1, 0);
                return;
            }
            if (!RootShell.fileExists(task.path)) {
                finish(context, task, false, "脚本不存在: " + task.path, -1, 0);
                return;
            }

            // Silent tasks put the clock back on screen; ringing ones must not be covered up.
            UnlockHelper.Result unlock = UnlockHelper.ensureUnlocked(context, task.ownsForeground);
            if (!unlock.ok) {
                finish(context, task, false, unlock.message, -1, 0);
                return;
            }

            File logDir = new File(context.getCacheDir(), "log/" + task.name);
            if (!logDir.exists()) logDir.mkdirs();
            File captureOut = new File(logDir, ".out." + System.nanoTime());
            File captureErr = new File(logDir, ".err." + System.nanoTime());
            // Bounded: a script that never exits must not hold the task thread (and the RUNNING
            // entry) forever. 30 minutes is far longer than any alarm script should need.
            Shell.Result result = RootShell.runWithTimeout(SCRIPT_TIMEOUT_MS,
                    "/system/bin/nsenter -t 1 -m -- sh " + RootShell.quote(task.path)
                            + " > " + RootShell.quote(captureOut.getAbsolutePath())
                            + " 2> " + RootShell.quote(captureErr.getAbsolutePath()));
            int code = result == null ? -1 : result.getCode();
            String out = readText(captureOut);
            String err = readText(captureErr);
            if (out.isEmpty() && !err.isEmpty()) {
                out = err;
                err = "";
            }
            captureOut.delete();
            captureErr.delete();
            File log = writeLog(context, task, out, err, code);
            long elapsed = System.currentTimeMillis() - task.startedAt;
            finish(context, task, code == 0, code == 0
                    ? "脚本执行结束，耗时 " + elapsed / 1000.0 + " 秒"
                    : "脚本执行失败，退出码 " + code, code, elapsed, log);
        } catch (Throwable t) {
            finish(context, task, false, "脚本执行异常: " + t, -1, 0);
        }
    }

    /**
     * Re-reads the script directory in the background and remembers the names it found.
     *
     * <p>Never blocks the caller: this is also called from hooks that run on the clock's main
     * thread. A refresh already in flight is not started twice.</p>
     */
    public static void refreshScriptIndex() {
        if (!REFRESHING.compareAndSet(false, true)) return;
        ThreadUtils.runOnBackground(() -> {
            try {
                Shell.Result result = RootShell.run("ls -1 " + RootShell.quote(SCRIPTS_DIR) + " 2>/dev/null");
                if (result == null || !result.isSuccess()) {
                    // "could not read" is not "empty": storing an empty set here would make every
                    // script look missing and flip tasks from silent to audible + notification.
                    XposedLog.w(TAG, "script index refresh failed, keeping the old index");
                    return;
                }
                Set<String> names = new HashSet<>();
                for (String line : result.getOut()) {
                    if (line == null) continue;
                    String name = line.trim();
                    if (!name.isEmpty()) names.add(name);
                }
                SCRIPT_INDEX.set(names);
            } catch (Throwable t) {
                XposedLog.w(TAG, "script index refresh failed: " + t);
            } finally {
                REFRESHING.set(false);
            }
        });
    }

    /**
     * Whether the script directory is known to contain this name.
     *
     * <p>Optimistic while the index has not been read yet: an alarm whose script cannot be checked
     * keeps the old behaviour instead of turning audible forever on a cold start.</p>
     */
    public static boolean isScriptKnown(String name) {
        Set<String> index = SCRIPT_INDEX.get();
        return index == null || index.contains(name);
    }

    /**
     * Creates the module directory once and drops the default unlock script in place.
     *
     * @return false when a setup step failed, so the caller reports it instead of running a script
     *         that cannot write a log or read the password file
     */
    private static boolean ensureSetup(Context context) {
        if (SETUP_DONE.get()) return true;
        synchronized (TaskManager.class) {
            if (SETUP_DONE.get()) return true;
            Shell.Result dir = RootShell.run("mkdir -p " + RootShell.quote(SCRIPTS_DIR));
            if (dir == null || !dir.isSuccess()) {
                XposedLog.e(TAG, "cannot create " + SCRIPTS_DIR);
                return false;
            }
            // chmod instead of relying on the umask: this file holds the lock screen password.
            Shell.Result password = RootShell.run("touch " + RootShell.quote(PASSWORD_FILE)
                    + " && chmod 600 " + RootShell.quote(PASSWORD_FILE));
            if (password == null || !password.isSuccess()) {
                XposedLog.e(TAG, "cannot prepare " + PASSWORD_FILE);
                return false;
            }
            if (!RootShell.fileExists(UNLOCK_SCRIPT)) {
                RootShell.writeFileFromBase64(UNLOCK_SCRIPT, UNLOCK_SCRIPT_B64);
            }
            SETUP_DONE.set(true);
            return true;
        }
    }

    /**
     * Reports a label that is not a script name inside {@link #SCRIPTS_DIR} instead of running it.
     *
     * <p>The rejection lands in the same two places a real run does: a log file in the script log
     * directory (mirrored to the app readable copy so the log importer picks it up) and the module
     * log. A silently ignored alarm would be indistinguishable from a broken script later.</p>
     */
    private static void reject(Context context, int alarmId, String script) {
        String reason = "拒绝执行：标签必须是 " + SCRIPTS_DIR + " 下的脚本名，不能包含路径或子目录: " + script;
        // This path never reaches finish(), so the state tick and its notification have to be
        // released here or the "running" line stays on screen forever.
        stopStateUpdaterIfIdle(context);
        File log = writeRejectLog(context, alarmId, script, reason);
        XposedLog.e(TAG, reason + (log == null ? "" : " (log: " + log.getAbsolutePath() + ")"));
        initChannels(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Notification.Builder b = builder(context, CHANNEL_RESULT)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("脚本任务已拒绝")
                .setContentText(reason)
                .setStyle(new Notification.BigTextStyle().bigText(reason))
                .setAutoCancel(true);
        if (log != null) {
            b.setContentIntent(PendingIntent.getActivity(context, stableId(alarmId, script),
                    logIntent(log), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        nm.notify(stableId(alarmId, script), b.build());
    }

    /** Writes the rejection with the same file layout a real run uses, then mirrors it for the app. */
    private static File writeRejectLog(Context context, int alarmId, String script, String reason) {
        String name = rejectedLogName(script);
        try {
            String ts = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
            File dir = new File(context.getCacheDir(), "log/" + name);
            if (!dir.exists() && !dir.mkdirs()) return null;
            File file = new File(dir, ts + ".log");
            StringBuilder sb = new StringBuilder();
            sb.append("script: ").append(script).append('\n');
            sb.append("alarmId: ").append(alarmId).append('\n');
            sb.append("start: ").append(System.currentTimeMillis()).append('\n');
            sb.append("exitCode: -1\n");
            sb.append("--- stdout ---\n");
            sb.append("--- stderr ---\n").append(reason).append('\n');
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            mirrorLog(file, name);
            return file;
        } catch (Throwable t) {
            return null;
        }
    }

    /** A directory name for a rejected label: it may contain anything, including path separators. */
    private static String rejectedLogName(String script) {
        String name = ScriptPath.plainName(script);
        if (name == null && script != null) {
            name = script.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        }
        // The fallback keeps dots, so a label of ".." survived it and turned into a path to the
        // parent directory - both the cache log write and chown -R on the mirror would have landed
        // one level too high.
        if (name == null || name.isEmpty() || name.equals(".") || name.equals("..")) return "rejected";
        return name.length() > 64 ? name.substring(0, 64) : name;
    }

    /**
     * Copies a finished log to the app readable location. The HyperAlarm app cannot read the clock
     * process private cache, so this copy is what its script log importer reads.
     */
    public static void mirrorLog(File file, String name) {
        String mirrorDir = LOG_MIRROR_DIR + name;
        // Script output routinely carries tickets, tokens and internal host names, so the copy is
        // restricted to the app that has to read it: ownership comes from the app's own external
        // dir, mode 600 for the file and 700 for the directory. If the ownership lookup fails the
        // chain stops and the copy keeps the defaults, which the app can still read.
        RootShell.run("mkdir -p " + RootShell.quote(mirrorDir)
                + " && cp " + RootShell.quote(file.getAbsolutePath()) + " " + RootShell.quote(mirrorDir + "/")
                + " && chown -R $(stat -c %u:%g " + RootShell.quote(APP_FILES_DIR) + ") "
                + RootShell.quote(mirrorDir)
                + " && chmod 700 " + RootShell.quote(mirrorDir)
                + " && chmod 600 " + RootShell.quote(mirrorDir + "/" + file.getName()));
        // The chain stops at the first failure, which is the documented behaviour above; say so,
        // otherwise the app quietly shows nothing for a run that did happen.
        if (!RootShell.fileExists(mirrorDir + "/" + file.getName())) {
            XposedLog.w(TAG, "log mirror failed for " + name);
        }
    }

    /**
     * Reads captured output, up to {@link #OUTPUT_LIMIT} bytes.
     *
     * <p>A script that prints in a loop used to be read entirely into memory, and an OOM in this
     * process is not something a catch block recovers from. The truncation is written into the log
     * so the missing tail is not mistaken for the script ending normally.</p>
     */
    private static String readText(File file) {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            boolean truncated = false;
            while ((n = in.read(buffer)) > 0) {
                int room = OUTPUT_LIMIT - out.size();
                if (room <= 0) {
                    truncated = true;
                    break;
                }
                out.write(buffer, 0, Math.min(n, room));
                if (n > room) {
                    truncated = true;
                    break;
                }
            }
            String text = out.toString(StandardCharsets.UTF_8.name());
            return truncated ? text + "\n[... output truncated at " + OUTPUT_LIMIT + " bytes ...]" : text;
        } catch (Throwable t) {
            return "";
        }
    }

    private static File writeLog(Context context, Task task, String out, String err, int code) {
        try {
            // One name per run: the timestamp alone repeats when two runs of the same script finish
            // within a second, and the importers would then see a single record (or overwrite one).
            String ts = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date())
                    + "-" + task.alarmId + "-" + (System.nanoTime() % 100000L);
            File dir = new File(context.getCacheDir(), "log/" + task.name);
            if (!dir.exists() && !dir.mkdirs()) return null;
            File file = new File(dir, ts + ".log");
            StringBuilder sb = new StringBuilder();
            sb.append("script: ").append(task.path).append('\n');
            sb.append("alarmId: ").append(task.alarmId).append('\n');
            sb.append("start: ").append(task.startedAt).append('\n');
            sb.append("exitCode: ").append(code).append('\n');
            sb.append("--- stdout ---\n").append(out).append('\n');
            sb.append("--- stderr ---\n").append(err).append('\n');
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            mirrorLog(file, task.name);
            return file;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void finish(Context context, Task task, boolean ok, String text, int code) {
        finish(context, task, ok, text, code, 0, null);
    }

    private static void finish(Context context, Task task, boolean ok, String text, int code,
                               long elapsed) {
        finish(context, task, ok, text, code, elapsed, null);
    }

    private static void finish(Context context, Task task, boolean ok, String text, int code,
                               long elapsed, File log) {
        RUNNING.remove(task.key);
        stopStateUpdaterIfIdle(context);
        // sForegroundOwned, not task.ownsForeground: when two tasks overlap (a silent one and a
        // ringing one), the ringing task finishes last, ownsForeground is false for it, and the
        // foreground state the silent task took would never be released.
        if (sForegroundOwned && RUNNING.isEmpty() && context instanceof Service) {
            // DETACH is documented to keep the notification on screen, and the old guard around it
            // asked alarmNotificationActive(), which matches the clock's *persistent* "next alarm"
            // line - always true on this ROM, so the foreground was never released and every silent
            // run left "脚本任务执行中" pinned to the shade until the clock process died.
            //
            // REMOVE is the honest call, but only when this module is the one holding the foreground:
            // sForegroundOwned is set by our own startForeground, so a task that never got one (a
            // manual run, a refusal from the platform) cannot pull the flag from under the clock.
            Service service = (Service) context;
            if (sForegroundOwned) {
                try {
                    service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
                } catch (Throwable ignored) {
                }
                sForegroundOwned = false;
            }
            cancelNotification(context, FOREGROUND_ID);
        }
        // stopStateUpdaterIfIdle() above already dropped the "running" notification, but only when
        // this was the last task: cancelling it here unconditionally used to blank it while another
        // script was still running, and the tick brought it back a second later.
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent pi = PendingIntent.getActivity(context, task.notifyId,
                logIntent(log), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = builder(context, CHANNEL_RESULT)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(ok ? task.name + " 执行结束" : task.name + " 执行失败")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true);
        if (nm != null) nm.notify(task.notifyId, b.build());
    }

    /**
     * Notification tap target: HyperAlarm's script log page.
     *
     * <p>The clock's own log viewer cannot show these runs - they are written to this module's
     * database by the app - so the tap has to cross into the module. The clock process may only do
     * that through an exported component, hence the trampoline, which opens the real (non exported)
     * page after checking the script name.</p>
     */
    private static Intent logIntent(File log) {
        Intent intent = new Intent();
        intent.setClassName(BridgeContract.MODULE_PACKAGE, "com.hyperalarm.log.ScriptLogLauncher");
        if (log != null && log.getParentFile() != null) {
            intent.putExtra("script", log.getParentFile().getName());
            // The file name is the run's timestamp, which is how the app finds the record.
            intent.putExtra("file", log.getName());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static void startForeground(Context context, Task task) {
        if (!(context instanceof Service)) return;      // a manual run from the app has no service
        Service service = (Service) context;
        try {
            Notification n = builder(context, CHANNEL_STATE)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle("脚本任务执行中")
                    .setContentText(task.name)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build();
            service.startForeground(FOREGROUND_ID, n);
            sForegroundOwned = true;
        } catch (Throwable t) {
            // A foreground notification the platform refuses means this task runs without the
            // protection it thinks it has; say so instead of pretending it worked.
            XposedLog.w(TAG, "startForeground refused, task runs unprotected", t);
        }
    }

    /** True while the clock itself has an alarm notification up, i.e. it owns the foreground now. */
    private static boolean alarmNotificationActive(Context context) {
        try {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return false;
            for (StatusBarNotification active : nm.getActiveNotifications()) {
                Notification n = active.getNotification();
                if (n != null && ALARM_CHANNEL.equals(n.getChannelId())) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Stops the once-a-second state tick and drops its notification once nothing is running.
     *
     * <p>Two cancels on purpose: a tick that is already inside {@code notify()} when the worker
     * finishes posts the notification *after* this method's cancel, so a second cancel is queued
     * behind it on the main thread. The tick stops itself the same way if it is the one that notices
     * the empty set, and a rejected label never reaches {@code finish()} at all - that path relies
     * on it too.</p>
     */
    private static void stopStateUpdaterIfIdle(Context context) {
        if (!RUNNING.isEmpty()) return;
        if (stateUpdater != null) {
            MAIN.removeCallbacks(stateUpdater);
            stateUpdater = null;
        }
        cancelStateNotification(context);
        final Context app = context.getApplicationContext() != null
                ? context.getApplicationContext()
                : context;
        MAIN.post(() -> {
            if (RUNNING.isEmpty()) cancelStateNotification(app);
        });
    }

    /** Drops one of this module's notifications; safe from either thread. */
    private static void cancelNotification(Context context, int id) {
        try {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(id);
        } catch (Throwable ignored) {
        }
    }

    private static void cancelStateNotification(Context context) {
        cancelNotification(context, STATE_ID);
    }

    private static void startStateUpdater(Context context) {
        if (stateUpdater != null) return;
        stateUpdater = new Runnable() {
            @Override
            public void run() {
                Task first = null;
                for (Task candidate : RUNNING.values()) {
                    first = candidate;
                    break;
                }
                if (first == null) {
                    // A worker may finish between the check above and the iteration; without this the
                    // clock main thread took a NoSuchElementException from a normal task completion.
                    //
                    // The notification has to go as well, and this is the only place that can be sure
                    // it does: a tick that was already inside notify() when the worker finished posts
                    // it after finish()'s cancel, and a rejected label never reaches finish() at all.
                    // Either way the state tick then stops here and nobody would ever cancel it.
                    cancelStateNotification(context);
                    stateUpdater = null;
                    return;
                }
                long elapsed = (System.currentTimeMillis() - first.startedAt) / 1000;
                String text = "任务数: " + RUNNING.size() + " / 已执行: " + elapsed + " 秒\n"
                        + first.name;
                Notification.Builder b = builder(context, CHANNEL_STATE)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setContentTitle("脚本任务执行中")
                        .setContentText(text)
                        .setStyle(new Notification.BigTextStyle().bigText(text))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setProgress(0, 0, true);
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(STATE_ID, b.build());
                MAIN.postDelayed(this, 1000);
            }
        };
        MAIN.post(stateUpdater);
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder builder(Context context, String channel) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(context, channel);
        }
        return new Notification.Builder(context).setPriority(Notification.PRIORITY_DEFAULT);
    }

    private static void toast(Context context, String text) {
        try {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    public static List<String> runningNames() {
        List<String> names = new ArrayList<>();
        for (Task t : RUNNING.values()) names.add(t.name);
        return names;
    }

    /**
     * Notification id for one alarm + script pair.
     *
     * <p>Truncating the alarm id with {@code & 0xffff} made 0 and 65536 collide, so one alarm's result
     * could overwrite another's, and a stale notification could open the wrong log. Manual runs have
     * no alarm id (-1) and used to share a single id as well.</p>
     */
    private static int stableId(int alarmId, String script) {
        int hash = 17;
        hash = hash * 31 + alarmId;
        hash = hash * 31 + (script == null ? 0 : script.hashCode());
        return RESULT_BASE + (hash & 0x7fffffff) % 200000;
    }

    private static final class Task {
        final String key;
        /** Label as written on the alarm or sent by the bridge; resolved on the worker thread. */
        final String raw;
        /** Absolute script path, filled in once {@link ScriptPath#resolve} accepted the label. */
        volatile String path;
        final String name;
        final int alarmId;
        final long startedAt;
        final int notifyId;
        final boolean ownsForeground;

        Task(String key, String raw, String name, int alarmId, long startedAt,
             boolean ownsForeground) {
            this.key = key;
            this.raw = raw;
            this.name = name;
            this.alarmId = alarmId;
            this.startedAt = startedAt;
            this.notifyId = stableId(alarmId, name);
            this.ownsForeground = ownsForeground;
        }
    }
}
