package org.errsa.combatdiag;

import java.util.Collection;
import java.util.Map;

/** Conservative matching; never treats correlation as proof of client behavior. */
final class PacketCorrelation {

    private PacketCorrelation() {
    }

    record Result(String quality, PacketObservation packet, int candidates, long deltaMillis) {
        boolean matched() {
            return "packet-correlated".equals(quality);
        }
    }

    static Result evaluate(Collection<PacketObservation> observations, String victimId,
                           long damageAt, long windowMs, long reorderToleranceMs) {
        PacketObservation closest = null;
        int matching = 0;
        boolean unresolved = false;
        long nearestDelta = Long.MAX_VALUE;
        for (PacketObservation candidate : observations) {
            long delta = damageAt - candidate.receivedAt();
            if (delta < -reorderToleranceMs || delta > windowMs) {
                continue;
            }
            if (candidate.targetUuid() == null) {
                unresolved = true;
                continue;
            }
            if (!victimId.equals(candidate.targetUuid())) {
                continue;
            }
            matching++;
            if (Math.abs(delta) < Math.abs(nearestDelta)) {
                closest = candidate;
                nearestDelta = delta;
            }
        }
        if (matching == 0) {
            return new Result(unresolved ? "timing-ambiguous" : "uncorrelated", null, 0, -1);
        }
        // A packet timestamp after the damage event or multiple packets is not a clean match.
        if (matching > 1 || nearestDelta < 0) {
            return new Result("timing-ambiguous", closest, matching, nearestDelta);
        }
        return new Result("packet-correlated", closest, 1, nearestDelta);
    }

    static Map<String, Object> packetFields(Result result) {
        PacketObservation packet = result.packet();
        return CombatDiagnosticsModule.map(
                "measurement_quality", result.quality(),
                "packet_match_count", result.candidates(),
                "packet_delta_ms", result.deltaMillis(),
                "packet_received_ms", packet == null ? null : packet.receivedAt(),
                "packet_processing_ms", packet == null ? null : packet.processedAt(),
                "packet_attacker_position", packet == null ? null : packet.attackerAtProcessing(),
                "packet_victim_position", packet == null ? null : packet.targetAtProcessing(),
                "packet_position_note", "Server position on packet processing tick, not at network receipt");
    }
}
