package com.hyperalarm.alarm.core;

import android.app.KeyguardManager;
import android.content.Context;
import com.hyperalarm.common.utils.RootShell;

/**
 * Gets the screen unlocked before a script runs, because /data stays encrypted while the device is
 * locked and the script file would be unreadable.
 *
 * <p>The optional {@code password} and {@code unlock_phone} files under
 * {@link TaskManager#MODULE_DIR} make this unattended: {@code unlock_phone} is the script that
 * actually types the secret, the {@code password} file holds it. Without a password we only wake
 * the screen and try a swipe, and report that data is still encrypted instead of pretending the
 * script can run.</p>
 */
public final class UnlockHelper {
    private static final int MAX_ATTEMPTS = 3;
    private static final long SETTLE_MS = 1200L;

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
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isKeyguardLocked()) return new Result(true, "");

        String password = RootShell.readFile(TaskManager.PASSWORD_FILE).trim();
        boolean hasPassword = !password.isEmpty()
                && RootShell.fileExists(TaskManager.UNLOCK_SCRIPT);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            RootShell.run(
                    "input keyevent KEYCODE_WAKEUP",
                    "sleep 1",
                    "input swipe 500 1500 500 500",
                    "sleep 1"
            );
            if (hasPassword) {
                RootShell.run("sh " + RootShell.quote(TaskManager.UNLOCK_SCRIPT)
                        + " " + RootShell.quote(password));
            }
            sleep(SETTLE_MS);
            if (!km.isDeviceLocked()) {
                if (restoreClockUi) {
                    RootShell.run(
                            "am start -n com.android.deskclock/com.android.deskclock.DeskClockTabActivity",
                            "sleep 1",
                            "input keyevent POWER"
                    );
                }
                return new Result(true, "");
            }
        }

        return new Result(false, hasPassword
                ? "多次尝试解锁屏幕失败，data 分区可能仍处于加密状态"
                : "未找到密码文件，data 分区被加密保护，无法读取脚本");
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
