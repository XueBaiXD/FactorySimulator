package com.bai.xuebai.factorysimulator.platform;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Runtime-only compatibility helpers. This class intentionally uses only
 * Java 8 and stable Bukkit API types so one jar can run on old and new JVMs.
 */
public final class RuntimePlatform {
    private RuntimePlatform() {
    }

    public static String serverVersion() {
        try {
            return Bukkit.getName() + " " + Bukkit.getVersion();
        } catch (Throwable ignored) {
            return "Unknown server";
        }
    }

    public static String bukkitPackageVersion() {
        try {
            Package bukkitPackage = Bukkit.class.getPackage();
            String implementationVersion = bukkitPackage == null ? null : bukkitPackage.getImplementationVersion();
            return implementationVersion == null ? "unknown" : implementationVersion;
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public static boolean classAvailable(String className) {
        try {
            Class.forName(className, false, RuntimePlatform.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Object invokeIfPresent(Object target, String methodName, Object... arguments) {
        if (target == null) return null;
        Method method = findMethod(target.getClass(), methodName, arguments);
        if (method == null) return null;
        try {
            if (!method.isAccessible()) method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Method findMethod(Class<?> type, String name, Object[] arguments) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                if (arguments[index] != null && !wrap(parameterTypes[index]).isAssignableFrom(arguments[index].getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) return method;
        }
        return null;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }
}