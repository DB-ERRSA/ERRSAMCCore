package org.errsa.combatdiag;

import java.util.Map;

/** ATTACK packet receipt time and server positions captured on its processing tick. */
record PacketObservation(
        long receivedAt,
        long processedAt,
        String targetUuid,
        String targetName,
        Map<String, Object> attackerAtProcessing,
        Map<String, Object> targetAtProcessing) {
}
