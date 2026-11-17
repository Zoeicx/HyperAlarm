package de.robv.android.xposed;

import android.app.Application;

public final class AndroidAppHelper {
    private AndroidAppHelper() {
    }

    public static Application currentApplication() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Object app = cls.getMethod("currentApplication", new Class[0]).invoke(null, new Object[0]);
            if (app instanceof Application) {
                return (Application) app;
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }
}
