package de.robv.android.xposed;

import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

public final class XposedBridge {
    static XposedInterface framework;

    public static void bind(XposedInterface api) {
        framework = api;
    }

    public static void log(String text) {
        XposedInterface api = framework;
        if (api != null) {
            api.log(4, "HyperAlarm", text == null ? "null" : text);
        } else {
            Log.i("HyperAlarm", String.valueOf(text));
        }
    }

    /**
     * Hooks every declared overload of a method name.
     *
     * @return how many methods were actually hooked. Zero means the class exists but the method was
     *         renamed or removed - a clock update does that - and the caller logs it instead of
     *         believing the hook was installed.
     */
    public static int hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback) {
        if (clazz == null) return 0;
        int hooked = 0;
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                hook(m, callback);
                hooked++;
            }
            current = current.getSuperclass();
        }
        return hooked;
    }


    public static void hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
        if (clazz == null) {
            return;
        }
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            hook(c, callback);
        }
    }

    static void hook(Executable executable, XC_MethodHook callback) {
        try {
            executable.setAccessible(true);
            framework.hook(executable).intercept(chain -> {
                XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
                param.thisObject = chain.getThisObject();
                List<Object> args = chain.getArgs();
                param.args = args == null ? new Object[0] : args.toArray();
                param.method = chain.getExecutable();
                try {
                    callback.beforeHookedMethod(param);
                    if (param.hasThrowable()) {
                        throw param.getThrowable();
                    }
                    Object result = param.isResultSet() ? param.getResult() : chain.proceed();
                    param.setResult(result);
                    callback.afterHookedMethod(param);
                    if (param.hasThrowable()) {
                        throw param.getThrowable();
                    }
                    return param.isResultSet() ? param.getResult() : result;
                } catch (Throwable t) {
                    if (!param.hasThrowable()) {
                        param.setThrowable(t);
                    }
                    throw param.getThrowable();
                }
            });
        } catch (Throwable t) {
            log("hook failed: " + executable + " -> " + t);
        }
    }
}
