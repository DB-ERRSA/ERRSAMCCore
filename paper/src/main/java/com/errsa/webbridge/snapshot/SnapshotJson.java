package com.errsa.webbridge.snapshot;

import java.util.Map;

/** Deterministic UTF-8-safe JSON serialization for WebBridge snapshot values. */
public final class SnapshotJson {
    private SnapshotJson() {
    }

    public static String stringify(Object value) {
        StringBuilder json = new StringBuilder();
        appendValue(json, value);
        return json.toString();
    }

    private static void appendValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String text) {
            appendQuoted(json, text);
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(json, map);
        } else if (value instanceof Iterable<?> values) {
            appendArray(json, values);
        } else {
            appendQuoted(json, String.valueOf(value));
        }
    }

    private static void appendMap(StringBuilder json, Map<?, ?> values) {
        json.append('{');
        boolean firstEntry = true;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!firstEntry) {
                json.append(',');
            }
            firstEntry = false;
            appendQuoted(json, String.valueOf(entry.getKey()));
            json.append(':');
            appendValue(json, entry.getValue());
        }
        json.append('}');
    }

    private static void appendArray(StringBuilder json, Iterable<?> values) {
        json.append('[');
        boolean firstValue = true;
        for (Object value : values) {
            if (!firstValue) {
                json.append(',');
            }
            firstValue = false;
            appendValue(json, value);
        }
        json.append(']');
    }

    private static void appendQuoted(StringBuilder json, String text) {
        json.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '\b' -> json.append("\\b");
                case '\t' -> json.append("\\t");
                case '\n' -> json.append("\\n");
                case '\f' -> json.append("\\f");
                case '\r' -> json.append("\\r");
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                default -> {
                    if (character < ' ') {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }
}
