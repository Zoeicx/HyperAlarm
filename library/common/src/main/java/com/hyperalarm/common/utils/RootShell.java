package com.hyperalarm.common.utils;

import com.hyperalarm.common.log.ModuleLog;
import com.topjohnwu.superuser.Shell;

import java.util.List;

/**
 * Thin wrapper around libsu. All commands run through the main root shell.
 *
 * <p>Everything that reaches a shell has to go through {@link #quote(String)}: script paths and
 * arguments are user influenced (an alarm label, a file name picked in a documents provider), and
 * a bare string in a command line is an injection point on its own.</p>
 */
public final class RootShell {
    private RootShell() {
    }

    /** Last known answer of {@link #isRoot()}; -1 until the first probe finishes. */
    private static volatile int sRootState = -1;

    /**
     * Whether the module currently holds root.
     *
     * <p>Never blocks: the first call starts a background probe and reports {@code false} until it
     * answers. {@code Shell.getShell()} can wait for the authorisation prompt, and every caller of
     * this is a UI event handler - a settings switch or a button press must not hang on it.</p>
     */
    public static boolean isRoot() {
        int state = sRootState;
        if (state < 0) {
            probeRoot();
            return false;
        }
        return state == 1;
    }

    /** Whether the last probe answered yes, without starting one. */
    public static boolean wasRootProbed() {
        return sRootState >= 0;
    }

    /** Starts a background probe if none is running. */
    public static void probeRoot() {
        if (sRootState >= 0) return;
        ThreadUtils.runOnBackground(RootShell::probeRootNow);
    }

    /**
     * Answers "do we have root" right now, waiting for the answer.
     *
     * <p>Only for worker threads. {@link #isRoot()} cannot block - it is called from UI handlers -
     * so it reports {@code false} until its background probe finishes. A task thread about to run a
     * root command has no such constraint, and inheriting that "not yet" answer made the first
     * alarm after a clock process start report "no root" while root worked fine.</p>
     */
    public static boolean probeRootNow() {
        try {
            Shell shell = Shell.getShell();
            boolean root = shell != null && shell.isRoot();
            sRootState = root ? 1 : 0;
            return root;
        } catch (Throwable t) {
            ModuleLog.w("RootShell", "root probe failed: " + t.getMessage());
            sRootState = 0;
            return false;
        }
    }

    /**
     * Runs commands through the single root shell and waits for the result.
     *
     * <p>Failures are logged instead of returning a silent {@code null}: a hung {@code su} or a denied
     * authorisation is exactly what a user reports as "nothing happened". libsu itself has no
     * per-command timeout, so callers that must not block forever run on the background pool, which is
     * bounded (see {@link ThreadUtils#getBackgroundExecutor()}).</p>
     */
    public static Shell.Result run(String... commands) {
        try {
            return Shell.cmd(commands).exec();
        } catch (Throwable t) {
            ModuleLog.w("RootShell", "command failed: " + t.getMessage());
            return null;
        }
    }

    /**
     * Like {@link #run}, but gives up after {@code timeoutMs}.
     *
     * <p>libsu has no per-command timeout, and a hung {@code su} or a script that never exits used to
     * block the task thread forever: {@code RUNNING} never emptied, the state tick kept running, and
     * every later trigger of that alarm was dropped as a duplicate. The worker is interrupted and the
     * caller gets {@code null}, which the callers already treat as "the command did not run".</p>
     */
    public static Shell.Result runWithTimeout(long timeoutMs, String... commands) {
        final java.util.concurrent.ExecutorService worker =
                java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r, "RootShell-timeout");
                    thread.setDaemon(true);
                    return thread;
                });
        try {
            java.util.concurrent.Future<Shell.Result> future = worker.submit(() -> run(commands));
            return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException timeout) {
            ModuleLog.w("RootShell", "command timed out after " + timeoutMs + " ms");
            return null;
        } catch (Throwable t) {
            ModuleLog.w("RootShell", "command failed: " + t.getMessage());
            return null;
        } finally {
            worker.shutdownNow();
        }
    }

    /** Trimmed stdout of a command, empty when it failed. */
    public static String runOutput(String command) {
        Shell.Result result = run(command);
        if (result == null || !result.isSuccess()) return "";
        return joined(result.getOut()).trim();
    }

    /** True when the command exited successfully. */
    public static boolean succeeded(String command) {
        Shell.Result result = run(command);
        return result != null && result.isSuccess();
    }

    public static boolean fileExists(String path) {
        Shell.Result r = run("[ -f " + quote(path) + " ] && echo yes");
        return r != null && r.isSuccess() && joined(r.getOut()).contains("yes");
    }

    /**
     * Canonical path as the root shell sees it, so symlinks and mount tricks are already
     * resolved when the caller compares it against an allowed directory.
     */
    public static String realPath(String path) {
        Shell.Result r = run("readlink -f " + quote(path));
        if (r == null || !r.isSuccess()) return null;
        String out = joined(r.getOut()).trim();
        return out.isEmpty() ? null : out;
    }

    public static String readFile(String path) {
        Shell.Result r = run("cat " + quote(path));
        if (r == null || !r.isSuccess()) return "";
        return joined(r.getOut());
    }

    public static boolean writeFileFromBase64(String path, String base64) {
        Shell.Result r = run(
                "echo " + quote(base64) + " | base64 -d > " + quote(path),
                "chmod 700 " + quote(path)
        );
        return r != null && r.isSuccess();
    }

    public static String quote(String value) {
        if (value == null) return "''";
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String joined(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString();
    }
}
