package de.robv.android.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class XposedHelpers {
    private XposedHelpers() {
    }

    public static Class<?> findClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        return Class.forName(name, false, classLoader);
    }

    /** Same lookup without the checked exception, for call sites whose caller already catches. */
    public static Class<?> findClassQuietly(String name, ClassLoader classLoader) {
        try {
            return findClass(name, classLoader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) throws Throwable {
        Class<?> clazz = findClass(className, classLoader);
        int count = parameterTypesAndCallback.length;
        XC_MethodHook callback = count == 0 ? null : (XC_MethodHook) parameterTypesAndCallback[count - 1];
        Class<?>[] types = new Class[count - 1];
        for (int i = 0; i < types.length; i++) {
            types[i] = (Class) parameterTypesAndCallback[i];
        }
        Method method = findMethod(clazz, methodName, types);
        if (method == null) {
            throw new NoSuchMethodException(className + "#" + methodName);
        }
        XposedBridge.hook(method, callback);
    }

    public static void hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback) {
        XposedBridge.hookAllMethods(clazz, methodName, callback);
    }

    public static void hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
        XposedBridge.hookAllConstructors(clazz, callback);
    }

    public static Object getObjectField(Object obj, String name) {
        try {
            Field f = findField(obj.getClass(), name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void setObjectField(Object obj, String name, Object value) {
        try {
            Field f = findField(obj.getClass(), name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int getIntField(Object obj, String name) {
        Object value = getObjectField(obj, name);
        return ((Number) value).intValue();
    }

    public static boolean getBooleanField(Object obj, String name) {
        Object value = getObjectField(obj, name);
        return value instanceof Boolean ? (Boolean) value : ((Number) value).intValue() != 0;
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try {
            java.lang.reflect.Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (java.lang.reflect.Constructor<?> candidate : constructors) {
                if (candidate.getParameterCount() != args.length) continue;
                Class<?>[] parameters = candidate.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (args[i] != null && !parameters[i].isAssignableFrom(args[i].getClass())) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) continue;
                candidate.setAccessible(true);
                return candidate.newInstance(args);
            }
            throw new NoSuchMethodException(clazz.getName() + " <init>(" + args.length + ")");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object callMethod(Object obj, String name, Object... args) {
        try {
            Class<?>[] types = typesOf(args);
            Method m = findMethod(obj.getClass(), name, types);
            if (m == null) {
                m = findMethodByCount(obj.getClass(), name, args.length);
            }
            if (m == null) {
                throw new NoSuchMethodException(obj.getClass().getName() + "#" + name);
            }
            m.setAccessible(true);
            return m.invoke(obj, args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String name, Object... args) {
        try {
            Class<?>[] types = typesOf(args);
            Method m = findMethod(clazz, name, types);
            if (m == null) {
                m = findMethodByCount(clazz, name, args.length);
            }
            if (m == null) {
                throw new NoSuchMethodException(clazz.getName() + "#" + name);
            }
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Class<?>[] typesOf(Object[] args) {
        if (args == null) {
            return new Class[0];
        }
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> superclass = clazz;
        while (true) {
            Class<?> c = superclass;
            if (c != null) {
                try {
                    return c.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    superclass = c.getSuperclass();
                }
            } else {
                throw new NoSuchFieldException(name);
            }
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>[] types) {
        Class<?> superclass = clazz;
        while (true) {
            Class<?> c = superclass;
            if (c != null) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == types.length) {
                        Class<?>[] p = m.getParameterTypes();
                        boolean ok = true;
                        for (int i = 0; i < p.length; i++) {
                            if (types[i] != null && !p[i].isAssignableFrom(types[i])) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            return m;
                        }
                    }
                }
                superclass = c.getSuperclass();
            } else {
                return null;
            }
        }
    }

    private static Method findMethodByCount(Class<?> clazz, String name, int count) {
        Class<?> superclass = clazz;
        while (true) {
            Class<?> c = superclass;
            if (c != null) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == count) {
                        return m;
                    }
                }
                superclass = c.getSuperclass();
            } else {
                return null;
            }
        }
    }
}
