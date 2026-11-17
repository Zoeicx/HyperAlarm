/*
 * This file is part of HyperAlarm.

 * HyperAlarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperAlarm Contributions
 */
package com.hyperalarm.common.utils;

import android.os.Handler;
import com.hyperalarm.common.log.ModuleLog;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The module's single place for background work and main-thread hops.
 *
 * <p>It lives in {@code common} because the app, the hook and the fork-base screens all need it. A
 * fixed pool keeps a burst of UI actions from spawning one thread each; components with a real
 * serialisation requirement (the log database writer) keep their own executor on purpose.</p>
 */
public class ThreadUtils {
    private static volatile ExecutorService sExecutorService;
    private static volatile Thread sMainThread;
    private static volatile Handler sMainThreadHandler;

    public static boolean isMainThread() {
        if (sMainThread == null) {
            sMainThread = Looper.getMainLooper().getThread();
        }
        return Thread.currentThread() == sMainThread;
    }

    public static Handler getUiThreadHandler() {
        if (sMainThreadHandler == null) {
            sMainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return sMainThreadHandler;
    }

    public static void ensureMainThread() {
        if (!isMainThread()) {
            throw new RuntimeException("Must be called on the UI thread");
        }
    }

    /**
     * Runs the task off the main thread, reporting a failure instead of storing it in a future
     * nobody reads.
     */
    public static CompletableFuture<Void> postOnBackgroundThread(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, getBackgroundExecutor())
                .exceptionally(error -> {
                    ModuleLog.e("ThreadUtils", "background task failed", unwrap(error));
                    return null;
                });
    }

    // 支持有返回值的异步调用
    public static <T> CompletableFuture<T> postOnBackgroundThread(Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, getBackgroundExecutor());
    }

    /** Fire-and-forget background work on the shared pool. */
    public static void runOnBackground(Runnable runnable) {
        if (runnable == null) return;
        postOnBackgroundThread(runnable);
    }

    private static Throwable unwrap(Throwable error) {
        return error.getCause() != null ? error.getCause() : error;
    }

    public static void postOnMainThread(Runnable runnable) {
        getUiThreadHandler().post(runnable);
    }

    /**
     * The shared pool.
     *
     * <p>Bounded on purpose: several call sites block in a root shell that has no timeout (the log
     * sync copies files through su), and an unbounded queue would let a stuck command swallow an
     * unlimited number of tasks. A rejected task still gets reported rather than vanishing.</p>
     */
    public static ExecutorService getBackgroundExecutor() {
        if (sExecutorService == null) {
            synchronized (ThreadUtils.class) {
                if (sExecutorService == null) {
                    int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
                    ThreadPoolExecutor pool = new ThreadPoolExecutor(
                            threads, threads,
                            30L, TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(256),
                            runnable -> {
                                Thread thread = new Thread(runnable, "HyperAlarm-bg");
                                thread.setDaemon(true);
                                thread.setUncaughtExceptionHandler((t, e) ->
                                        ModuleLog.e("ThreadUtils", "uncaught in " + t.getName(), e));
                                return thread;
                            },
                            new ThreadPoolExecutor.CallerRunsPolicy());
                    pool.allowCoreThreadTimeOut(true);
                    sExecutorService = pool;
                }
            }
        }
        return sExecutorService;
    }

    public static void postOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        getUiThreadHandler().postDelayed(runnable, delayMillis);
    }
}
