package com.errsa.webbridge.quickshop;

import java.time.Instant;

/** Last verified stock sample persisted across Core restarts. */
record CachedStock(long stock, Instant observedAt, String source) {
   }
