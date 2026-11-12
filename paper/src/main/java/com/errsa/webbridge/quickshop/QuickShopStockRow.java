package com.errsa.webbridge.quickshop;

import java.time.Instant;

/** Serialized shop-stock snapshot entry. */
record QuickShopStockRow(
         String world, int x, int y, int z, Long stock, boolean unlimited,
         String source, boolean cached, Instant observedAt) {
   }
