package com.errsa.inventory;

import java.util.HashMap;
import java.util.Map;

final class PlayerState {
    final Map<String, ContextHash> contextHashes = new HashMap<>();
    final Map<Integer, String> hashToContext = new HashMap<>();
    Integer lastHash;
    String lastContext;
    long joinGraceUntil;
    long worldGraceUntil;
    long gamemodeGraceUntil;
    long respawnGraceUntil;
    boolean pendingContextChange;
}
