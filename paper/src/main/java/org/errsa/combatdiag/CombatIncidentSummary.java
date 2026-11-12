package org.errsa.combatdiag;

import java.util.Map;

/** Compact staff-facing view of evidence that remains review-only. */
final class CombatIncidentSummary {

    private CombatIncidentSummary() {
    }

    static String compact(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "No detailed measurement (manual or external trigger).";
        }
        return "Cause=" + value(details, "damage_cause")
                + " | Damage=" + value(details, "final_damage")
                + " | Packet=" + value(details, "measurement_quality")
                + " | Evidence=" + value(details, "packet_evidence_available");
    }

    static String movement(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "No damage-event movement snapshot available.";
        }
        return "Attacker Elytra=" + value(details, "attacker_elytra")
                + " velocity=" + value(details, "attacker_velocity")
                + " ping=" + value(details, "attacker_ping_ms") + "ms"
                + " | Victim Elytra=" + value(details, "victim_elytra")
                + " velocity=" + value(details, "victim_velocity")
                + " ping=" + value(details, "victim_ping_ms") + "ms";
    }

    private static Object value(Map<String, Object> data, String key) {
        return data.getOrDefault(key, "n/a");
    }
}
