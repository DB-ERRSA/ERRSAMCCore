package com.errsa.inventory;

import java.util.Locale;

enum Severity {
    DEBUG, LOW, MEDIUM, HIGH, CRITICAL;

    boolean atLeast(Severity other) {
        return ordinal() >= other.ordinal();
    }

    static Severity parse(String text, Severity fallback) {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
