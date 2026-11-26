package com.hyperalarm.alarm.core;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.DisplayMetrics;

import com.hyperalarm.common.log.XposedLog;
import com.hyperalarm.common.utils.RootShell;
import com.topjohnwu.superuser.Shell;

/**
 * Makes the credential-encrypted data available before a script runs.
 *
 * <p>The gate is {@link UserManager#isUserUnlocked()}, not {@code KeyguardManager.isKeyguardLocked()}:
 * the keyguard is also reported as locked when the screen is merely off, while the user (and every
 * app's data) has been decrypted since the first unlock after boot. Waking and unlocking in that
 * state is not only unnecessary - it reveals whatever app was behind the keyguard, which users saw
 * as "HyperAlarm opens by itself after the alarm".</p>
 *
 * <p>When the user is still locked, {@code locksettings verify} is tried first: it unlocks CE
 * storage and the user through LockSettingsService while leaving the keyguard up, so the screen is
 * never woken. The optional {@code password} and {@code unlock_phone} files under
 * {@link TaskManager#MODULE_DIR} are the fallback for ROMs without that command: {@code unlock_phone}
 * is the script that types the secret, the {@code password} file holds it. A wrong password is never
 * retried: a few more attempts can trip the platform's lockout, which would be far worse than one
 * failed task.</p>
 */
public final class UnlockHelper {
    private static final String TAG = "UnlockHelper";
    private static final String CLOCK_ACTIVITY =
            "com.android.deskclock/com.android.deskclock.DeskClockTabActivity";

    private static final long WAKE_SETTLE_MS = 500L;
    private static final long KEYGUARD_INPUT_SETTLE_MS = 300L;
    private static final long UNLOCK_TIMEOUT_MS = 6_000L;
    private static final long POLL_MS = 200L;
    private static final long CLOCK_SETTLE_MS = 700L;
    private static final long CLOCK_LAUNCH_TIMEOUT_MS = 10_000L;
    private static final long CREDENTIAL_TIMEOUT_MS = 10_000L;

    /**
     * Credential that {@code locksettings} already rejected in this clock process.
     *
     * <p>Every rejected attempt is counted by the platform. Several alarms can fire inside the
     * locked window after a reboot, so without this guard each one would try the same wrong
     * password again. Only a changed password (different hash) or a clock-process restart clears
     * it; the settings page already has that restart button.</p>
     */
    private static volatile boolean sCredentialRejected;
    private static volatile int sRejectedPasswordHash;

    private UnlockHelper() {
    }

    /**
     * @param restoreClockUi true for a silent task: with the alarm silenced nothing would explain
     *                       why the device woke up, so the clock is brought back to the front and the
     *                       screen is put back to sleep. A ringing task ({@code @x.sh}) must pass
     *                       false - pulling the clock list over the ringing alarm and pressing power
     *                       is how the alarm used to look like it stopped by itself.
     */
    public static Result ensureUnlocked(Context context, boolean restoreClockUi) {
        // The common case: the device was unlocked once after boot, so /data is readable even when
        // the screen is off and the keyguard is showing. Touch nothing.
        if (isUserUnlocked(context)) return new Result(true, "");

        KeyguardManager km =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null) {
            return new Result(false, "无法读取锁屏状态，data 分区可能仍处于加密状态");
        }

        String password = readPassword();
        boolean hasScript = RootShell.fileExists(TaskManager.UNLOCK_SCRIPT);
        boolean secure = isDeviceSecure(km);

        if (secure && password.isEmpty()) {
            XposedLog.w(TAG, "device is locked and secure, but no password is configured");
            return new Result(false, "设备仍处于锁定状态，未设置锁屏密码，无法解锁 data 分区");
        }

        if (secure) {
            // Prefer the platform's own credential check: LockSettingsService unlocks CE storage and
            // the user without dismissing the keyguard, so the screen stays exactly as it was.
            DirectUnlock direct = tryDirectUnlock(context, password);
            if (direct == DirectUnlock.UNLOCKED) {
                XposedLog.i(TAG, "unlocked the user through locksettings without touching the UI");
                // The user is unlocked now, so the clock can be started safely even if it was still
                // behind the keyguard. This is what makes the next unlock show the clock instead of
                // the app the user was editing before locking the device. Do not wait for the
                // launch: the screen may be off and the activity then never reports "displayed".
                if (restoreClockUi) bringClockToFront(false);
                return new Result(true, "");
            }
            if (direct == DirectUnlock.REJECTED) {
                XposedLog.w(TAG, "locksettings did not accept the credential; not retrying");
                return new Result(false,
                        "未能通过锁屏凭据解锁；为避免触发密码错误次数限制，未重复尝试");
            }
            if (!hasScript) {
                XposedLog.w(TAG, "locksettings is unavailable and " + TaskManager.UNLOCK_SCRIPT
                        + " is missing");
                return new Result(false, "设备仍处于锁定状态，未找到解锁脚本 "
                        + TaskManager.UNLOCK_SCRIPT);
            }
            XposedLog.w(TAG, "locksettings is unavailable; falling back to the keyguard UI");
        }

        XposedLog.i(TAG, "locksettings fallback: waking the screen");
        wakeScreen(context);
        prepareKeyguardInput(context);

        if (secure) {
            Shell.Result script = RootShell.run("sh "
                    + RootShell.quote(TaskManager.UNLOCK_SCRIPT)
                    + " " + RootShell.quote(password));
            if (script == null || !script.isSuccess()) {
                XposedLog.w(TAG, "unlock script returned a failure; still waiting for the keyguard");
            }
        }

        if (!waitForUnlocked(context, UNLOCK_TIMEOUT_MS)) {
            // Deliberately no second attempt with the same password: Android's wrong-credential
            // counter is device-wide and a lockout would stop the user from unlocking at all.
            XposedLog.w(TAG, "user is still locked after the unlock attempt");
            return new Result(false, secure
                    ? "已尝试解锁，但设备仍处于锁定状态；为避免触发密码错误次数限制，未重复尝试"
                    : "无法解除锁屏，data 分区可能仍处于加密状态");
        }

        if (restoreClockUi) {
            // The keyguard is gone now: bring the clock to the front and wait for the launch before
            // turning the screen off. Pressing power after a fixed 1s used to race a slow cold start
            // that was still showing the previous app.
            bringClockToFront(true);
            sleep(CLOCK_SETTLE_MS);
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm == null || pm.isInteractive()) {
                // KEYCODE_SLEEP only sleeps; POWER would wake the screen again if it had already
                // timed out between the checks.
                RootShell.run("input keyevent KEYCODE_SLEEP");
            }
        }
        return new Result(true, "");
    }

    /**
     * Unlocks the user through {@code locksettings verify}, which goes through
     * LockSettingsService: a correct credential unlocks CE storage and the user while leaving the
     * keyguard up. No screen wake, no input injection, and no previous app revealed.
     */
    private static DirectUnlock tryDirectUnlock(Context context, String password) {
        if (sCredentialRejected && sRejectedPasswordHash == password.hashCode()) {
            XposedLog.w(TAG, "this credential was already rejected; not trying it again");
            return DirectUnlock.REJECTED;
        }
        // Some ROMs strip the debug command. Do not treat that as a wrong credential.
        if (!RootShell.succeeded("command -v locksettings >/dev/null 2>&1")) {
            return DirectUnlock.UNAVAILABLE;
        }
        Shell.Result result = RootShell.runWithTimeout(CREDENTIAL_TIMEOUT_MS,
                "locksettings verify --user 0 --old " + RootShell.quote(password));
        if (isUserUnlocked(context)) return DirectUnlock.UNLOCKED;
        if (result == null) {
            // The command may still be running. Retrying the same credential through the UI here
            // could double-count one attempt, so report a failure instead.
            XposedLog.w(TAG, "locksettings verify timed out");
            markCredentialRejected(password);
            return DirectUnlock.REJECTED;
        }
        int code = result.getCode();
        if (code == 126 || code == 127) return DirectUnlock.UNAVAILABLE;
        if (result.isSuccess()) {
            return waitForUnlocked(context, UNLOCK_TIMEOUT_MS)
                    ? DirectUnlock.UNLOCKED
                    : DirectUnlock.UNAVAILABLE;
        }
        // Never log either stream: on a mismatch locksettings echoes the credential it was given.
        // Only stderr can carry command/permission errors; stdout is checked for the rejection
        // markers alone, so a password that happens to contain "not found" cannot be mistaken for
        // a missing command.
        String err = RootShell.joined(result.getErr());
        if (err.contains("Unknown command")
                || err.contains("Error while executing")
                || err.contains("Permission Denial")
                || err.contains("not found")
                || err.contains("No such file")
                || err.contains("does not support lock screen")) {
            return DirectUnlock.UNAVAILABLE;
        }
        String out = RootShell.joined(result.getOut());
        if (out.contains("didn't match")
                || out.contains("Request throttled")
                || out.contains("old credential was not provided")
                || out.contains("Profile uses unified challenge")) {
            markCredentialRejected(password);
            return DirectUnlock.REJECTED;
        }
        markCredentialRejected(password);
        return DirectUnlock.REJECTED;
    }

    private static void markCredentialRejected(String password) {
        sRejectedPasswordHash = password.hashCode();
        sCredentialRejected = true;
    }

    private enum DirectUnlock {
        UNLOCKED,
        /** The platform rejected the credential or could not tell us; do not type it again. */
        REJECTED,
        /** The command is missing or unusable; the keyguard UI is the fallback. */
        UNAVAILABLE
    }

    /** Whether credential-encrypted storage for this user is available right now. */
    private static boolean isUserUnlocked(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (um != null) {
            try {
                return um.isUserUnlocked();
            } catch (Throwable t) {
                XposedLog.w(TAG, "cannot read the user unlocked state: " + t);
            }
        }
        // Very old or heavily modified framework: fall back to the keyguard flags.
        KeyguardManager km =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && !km.isKeyguardLocked() && !km.isDeviceLocked();
    }

    private static boolean isDeviceSecure(KeyguardManager km) {
        try {
            return km.isDeviceSecure();
        } catch (Throwable t) {
            // A secure keyguard is exactly the case that needs a credential.
            return km.isDeviceLocked();
        }
    }

    private static void wakeScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isInteractive()) return;
        if (!RootShell.succeeded("input keyevent KEYCODE_WAKEUP")) {
            XposedLog.w(TAG, "KEYCODE_WAKEUP failed");
        }
        // The keyguard may only report itself after the display is on.
        sleep(WAKE_SETTLE_MS);
    }

    /**
     * Wakes the keyguard's credential entry without ever typing the password.
     *
     * <p>This is the part the custom {@code unlock_phone} script used to rely on: it was always run
     * after a swipe, so scripts that only type the secret keep working. {@code wm dismiss-keyguard}
     * handles swipe-only keyguards on its own and is a harmless no-op on a secure one.</p>
     */
    private static void prepareKeyguardInput(Context context) {
        RootShell.run(
                "wm dismiss-keyguard >/dev/null 2>&1",
                "input keyevent KEYCODE_MENU >/dev/null 2>&1"
        );

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        if (metrics.widthPixels <= 0 || metrics.heightPixels <= 0) return;
        int x = metrics.widthPixels / 2;
        int from = metrics.heightPixels * 3 / 4;
        int to = metrics.heightPixels / 4;
        RootShell.run("input swipe " + x + " " + from + " " + x + " " + to + " 250");
        sleep(KEYGUARD_INPUT_SETTLE_MS);
    }

    private static boolean waitForUnlocked(Context context, long timeoutMs) {
        long deadline = SystemClock.uptimeMillis() + timeoutMs;
        while (true) {
            if (isUserUnlocked(context)) return true;
            long left = deadline - SystemClock.uptimeMillis();
            if (left <= 0) return false;
            sleep(Math.min(POLL_MS, left));
        }
    }

    /**
     * Brings the clock's tab activity to the front.
     *
     * @param waitForLaunch true when the caller is about to turn the screen off and must know the
     *                      launch finished; false when the screen may be off and {@code -W} would
     *                      only sit until its timeout
     * @return true when {@code am} reported a successful launch
     */
    private static boolean bringClockToFront(boolean waitForLaunch) {
        String command = (waitForLaunch ? "am start -W -n " : "am start -n ") + CLOCK_ACTIVITY;
        Shell.Result result = waitForLaunch
                ? RootShell.runWithTimeout(CLOCK_LAUNCH_TIMEOUT_MS, command)
                : RootShell.run(command);
        if (result == null || !result.isSuccess()) {
            XposedLog.w(TAG, "cannot start the clock activity");
            return false;
        }
        String output = RootShell.joined(result.getOut());
        if (output.contains("Error:") || output.contains("Error type")) {
            XposedLog.w(TAG, "clock activity launch failed: " + output.trim());
            return false;
        }
        return true;
    }

    /** The password verbatim; only a text file's trailing line ending is removed. */
    private static String readPassword() {
        String raw = RootShell.readFile(TaskManager.PASSWORD_FILE);
        if (raw == null) return "";
        int end = raw.length();
        while (end > 0 && (raw.charAt(end - 1) == '\n' || raw.charAt(end - 1) == '\r')) {
            end--;
        }
        return raw.substring(0, end);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** {@code ok} together with the reason shown when the device could not be unlocked. */
    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }
}
