package com.errsa.webbridge.eventbridge;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Reflection adapter for optional EventBridge transport and payload classes. */
final class EventBridgeCodec {
    private ClassLoader loader;

    byte[] encodeCalendarRequest() throws ReflectiveOperationException {
        ClassLoader eventLoader = loader();
        Class<?> requestClass = Class.forName(
                "com.eventbridge.common.net.payload.EventCalendarRequest", true, eventLoader);
        Object request = requestClass.getConstructor(boolean.class).newInstance(false);
        return encode("EVENT_CALENDAR_REQUEST", request);
    }

    byte[] encodeNewsletterRequest() throws ReflectiveOperationException {
        return encode("NEWSLETTER_LIST_REQUEST", new Object());
    }

    Object decode(byte[] message) throws ReflectiveOperationException {
        Class<?> codec = Class.forName("com.eventbridge.common.net.Codec", true, loader());
        return findStatic(codec, "decode", 1).invoke(null, message);
    }

    Object decodePayload(Object message, String className) throws ReflectiveOperationException {
        Class<?> codec = Class.forName("com.eventbridge.common.net.Codec", true, loader());
        Class<?> payload = Class.forName(className, true, loader());
        return findStatic(codec, "decodePayload", 2).invoke(null, message, payload);
    }

    void reset() {
        loader = null;
    }

    private byte[] encode(String type, Object payload) throws ReflectiveOperationException {
        Class<?> codec = Class.forName("com.eventbridge.common.net.Codec", true, loader());
        Object encoded = findStatic(codec, "encode", 3).invoke(null, 1, type, payload);
        return (byte[]) encoded;
    }

    private ClassLoader loader() {
        if (loader != null) {
            return loader;
        }
        Plugin eventBridge = Bukkit.getPluginManager().getPlugin("EventBridge");
        if (eventBridge == null) {
            throw new IllegalStateException("EventBridge plugin not found");
        }
        loader = eventBridge.getClass().getClassLoader();
        return loader;
    }

    private static Method findStatic(Class<?> type, String methodName, int arguments)
            throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.getName().equals(methodName)
                    && method.getParameterCount() == arguments) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + methodName);
    }
}
