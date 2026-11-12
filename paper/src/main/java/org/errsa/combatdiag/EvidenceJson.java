package org.errsa.combatdiag;

import java.util.Map;
import java.util.StringJoiner;

/** Serializes the bounded, privacy-reduced incident event payloads. */
final class EvidenceJson {
    private EvidenceJson() {
    }

    static String encode(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n",
            "\\n").replace("\r", "\\r") + "\"";
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?,?> m) {
            StringJoiner j = new StringJoiner(",", "{", "}");
            for (Map.Entry<?,?> e : m.entrySet()) j.add(encode(String.valueOf(e.getKey())) + ":" +
                encode(e.getValue()));
            return j.toString();
        }
        if (value instanceof Iterable<?> it) {
            StringJoiner j = new StringJoiner(",", "[", "]"); for (Object x : it) j.add(encode(x)); return j.toString();
        }
        return encode(value.toString());
    }
}
