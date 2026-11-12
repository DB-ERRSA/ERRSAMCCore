package org.errsa.combatdiag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interprets recognized anti-cheat console messages without server API calls. */
final class ExternalAlertParser {
    private static final Pattern THEMIS_ALERT = Pattern.compile(
            "(?:\\[Themis\\]\\s*)?([A-Za-z0-9_]{3,16}) was flagged for (.+?) hacks!",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLOIT_ALERT = Pattern.compile(
            "(?:ExploitFixer\\s*[»>]\\s*)([A-Za-z0-9_]{3,16})\\s*\\([^)]*\\)\\s*failed\\s+([A-Za-z0-9_]+)",
            Pattern.CASE_INSENSITIVE);

    private ExternalAlertParser() {
    }

    static Alert parse(String message) {
        Matcher themis = THEMIS_ALERT.matcher(message);
        if (themis.find()) {
            return new Alert(themis.group(1), "Themis", themis.group(2));
        }
        Matcher exploit = EXPLOIT_ALERT.matcher(message);
        if (exploit.find()) {
            return new Alert(exploit.group(1), "ExploitFixer", exploit.group(2));
        }
        return null;
    }

    record Alert(String playerName, String source, String check) {
    }
}
