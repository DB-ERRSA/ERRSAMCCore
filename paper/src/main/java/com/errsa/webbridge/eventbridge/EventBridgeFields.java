package com.errsa.webbridge.eventbridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Reads fields exposed by varying EventBridge model versions. */
final class EventBridgeFields {
    private EventBridgeFields() {
    }

    static Object read(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            // Try a declared field, then public accessors.
        }
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not all models expose their internal fields.
        }
        try {
            Method accessor = target.getClass().getMethod(name);
            return accessor.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            // Try the conventional JavaBean accessor.
        }
        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static List<?> list(Object value) {
        return value instanceof List<?> values ? values : List.of();
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String enumName(Object value) {
        return value instanceof Enum<?> enumeration
                ? enumeration.name() : string(value);
    }

    static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value);
    }

    static Object number(Object value) {
        return value instanceof Number ? value : 0;
    }

    static Object nullableNumber(Object value) {
        return value instanceof Number ? value : null;
    }
}
