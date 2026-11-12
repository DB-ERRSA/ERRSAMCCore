package org.errsa.combatdiag;

import java.util.UUID;

/** An active capture window for a combat incident. */
final class ActiveIncident {

    final String id;
    final UUID otherPlayer;
    final long started;
    long endMillis;
    int extraTriggers;

    ActiveIncident(String id, UUID otherPlayer, long started,
                   long endMillis) {
        this.id = id;
        this.otherPlayer = otherPlayer;
        this.started = started;
        this.endMillis = endMillis;
    }
}
