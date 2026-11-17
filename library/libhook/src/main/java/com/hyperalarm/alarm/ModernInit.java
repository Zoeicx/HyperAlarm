package com.hyperalarm.alarm;

import com.hyperalarm.common.log.XposedLog;
import com.hyperalarm.alarm.hook.AlarmHooks;

import de.robv.android.xposed.XposedBridge;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Entry point of the module, referenced by {@code META-INF/xposed/java_init.list}.
 *
 * <p>Only one package is ever hooked: Mi Clock. Everything else (the app UI, the shell scripts,
 * the log viewer) lives in the module apk, so this class stays small on purpose - a mistake here
 * would take the clock process down with it.</p>
 */
public final class ModernInit extends XposedModule {
    private static final String TAG = "ModernInit";

    private static final String CLOCK_PKG = "com.android.deskclock";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        XposedBridgeAccess.bind(this);
        // From here on the clock-side code logs with a level instead of a bare string.
        XposedLog.init(this);
        log("module loaded: " + param.getProcessName());
    }

    /** Name of the process this code runs in, or null when the platform will not say. */
    private static String currentProcessName() {
        try {
            return android.app.Application.getProcessName();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!CLOCK_PKG.equals(param.getPackageName())) return;
        // Only the main process: a second clock process would install the same hooks and register
        // the same bridge receiver again, and every broadcast would then be handled twice.
        // PackageLoadedParam does not name the process, so ask the runtime; when it cannot answer,
        // hooking continues rather than silently doing nothing.
        String process = currentProcessName();
        if (process != null && !process.isEmpty() && !CLOCK_PKG.equals(process)) {
            log("skipping secondary process: " + process);
            return;
        }
        XposedBridgeAccess.bind(this);
        log("hooking " + param.getPackageName());
        AlarmHooks.install(param.getDefaultClassLoader());
    }

    private void log(String message) {
        XposedBridgeAccess.log(message);
    }

    /** Thin seam over {@link XposedBridge} so the legacy shim stays replaceable in one place. */
    private static final class XposedBridgeAccess {
        private XposedBridgeAccess() {
        }

        static void bind(XposedInterface api) {
            XposedBridge.bind(api);
        }

        static void log(String message) {
            XposedLog.i(TAG, String.valueOf(message));
        }
    }
}
