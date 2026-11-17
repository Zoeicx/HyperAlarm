package com.hyperalarm.common.utils.shell;

import android.content.ComponentName;

import com.hyperalarm.common.utils.RootShell;

/** Enables or disables a component through the shared root shell. */
public class ShellPackageManager {
    private static final String PM = "pm";

    public static boolean enable(String packageName) {
        return enableOrDisable(packageName, true);
    }

    public static boolean enable(ComponentName componentName) {
        return enableOrDisable(componentName.flattenToString(), true);
    }

    public static boolean disable(String packageName) {
        return enableOrDisable(packageName, false);
    }

    public static boolean disable(ComponentName componentName) {
        return enableOrDisable(componentName.flattenToString(), false);
    }

    public static boolean enableOrDisable(String packageName, boolean isEnable) {
        String status = isEnable ? "enable" : "disable";
        String command = PM + " " + status + " " + RootShell.quote(packageName);
        com.topjohnwu.superuser.Shell.Result result = RootShell.run(command);
        return result != null && result.getCode() == 0;
    }

    public static boolean enableOrDisable(ComponentName componentName, boolean isEnable) {
        return enableOrDisable(componentName.flattenToString(), isEnable);
    }
}
