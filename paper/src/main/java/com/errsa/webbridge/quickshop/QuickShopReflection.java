package com.errsa.webbridge.quickshop;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Handles compatibility across multiple QuickShop API releases. */
final class QuickShopReflection {
    private QuickShopReflection() {
    }

    static Object invokeNoArg(Object target, String name) throws Exception {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                return method.invoke(target);
            }
        }
        return null;
    }

    static Object first(Object target, String... names) throws Exception {
        for (String name : names) {
            Object value = invokeNoArg(target, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    static Object invoke(Object target, String name, Object... arguments) throws Exception {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name)
                    || method.getParameterCount() != arguments.length) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameters.length; index++) {
                if (!isCompatible(parameters[index], arguments[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method.invoke(target, arguments);
            }
        }
        return null;
    }

    private static boolean isCompatible(Class<?> parameter, Object argument) {
        if (argument == null) {
            return !parameter.isPrimitive();
        }
        if (parameter.isInstance(argument)) {
            return true;
        }
        if (!parameter.isPrimitive()) {
            return false;
        }
        return parameter == int.class && argument instanceof Integer
                || parameter == long.class && argument instanceof Long
                || parameter == boolean.class && argument instanceof Boolean
                || parameter == double.class && argument instanceof Double
                || parameter == float.class && argument instanceof Float
                || parameter == short.class && argument instanceof Short
                || parameter == byte.class && argument instanceof Byte
                || parameter == char.class && argument instanceof Character;
    }

    static String worldName(Object location) throws Exception {
        Object world = invokeNoArg(location, "getWorld");
        Object name = world == null ? null : invokeNoArg(world, "getName");
        return name == null ? null : String.valueOf(name);
    }

    static Integer intCall(Object target, String name) {
        try {
            Object result = invokeNoArg(target, name);
            return result instanceof Number number ? number.intValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    static boolean boolCall(Object target, String name, boolean fallback) {
        try {
            Object result = invokeNoArg(target, name);
            return result instanceof Boolean value ? value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static Boolean boolCallNullable(Object target, String name, Object... arguments) {
        try {
            Object result = invoke(target, name, arguments);
            return result instanceof Boolean value ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    static Long numberFrom(Object result) throws Exception {
        if (result instanceof Number number) {
            return number.longValue();
        }
        if (result instanceof OptionalLong optional) {
            return optional.isPresent() ? optional.getAsLong() : null;
        }
        if (result instanceof OptionalInt optional) {
            return optional.isPresent() ? (long) optional.getAsInt() : null;
        }
        if (result instanceof Optional<?> optional) {
            return optional.isPresent() ? numberFrom(optional.get()) : null;
        }
        if (result instanceof CompletionStage<?> future) {
            return numberFrom(future.toCompletableFuture().get(1200L, TimeUnit.MILLISECONDS));
        }
        if (result instanceof Future<?> future) {
            return numberFrom(future.get(1200L, TimeUnit.MILLISECONDS));
        }
        return null;
    }
}
