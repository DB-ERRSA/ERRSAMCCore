package com.errsa.webbridge.quickshop;

import com.errsa.webbridge.snapshot.SnapshotJson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** Dependency-free regression checks for exported QuickShop formats. */
public final class QuickShopSmoke {
    public static void main(String[] arguments) throws Exception {
        Path directory = Files.createTempDirectory("errsa-quickshop-smoke-");
        Logger logger = Logger.getLogger("QuickShopSmoke");
        Path cache = directory.resolve("shop-stock-cache.properties");
        Map<String, CachedStock> values = new ConcurrentHashMap<>();
        values.put("world|1|2|3", new CachedStock(12L, Instant.ofEpochMilli(123L), "container"));
        QuickShopCacheStore.saveCache(cache, values, logger);
        values.clear();
        QuickShopCacheStore.loadCache(cache, values, logger);
        if (values.size() != 1 || values.get("world|1|2|3").stock() != 12L) {
            throw new AssertionError("Existing cache format did not round-trip");
        }
        Path snapshot = directory.resolve("stock.json");
        QuickShopStockRow sample = new QuickShopStockRow(
                "world", 1, 2, 3, 12L, false, "container", false, Instant.EPOCH);
        QuickShopSnapshotWriter.write(snapshot, logger, true, List.of(sample), null, 1, 1, 0);
        String json = Files.readString(snapshot);
        for (String expected : List.of("\"shopCount\":1", "\"stock\":12",
                "\"world\":\"world\"", "\"observedAt\":\"1970-01-01T00:00:00Z\"")) {
            if (!json.contains(expected)) {
                throw new AssertionError("Missing QuickShop JSON field: " + expected);
            }
        }
        if (!"{\"quoted\":\"x\\\"y\"}".equals(
                SnapshotJson.stringify(Map.of("quoted", "x\"y")))) {
            throw new AssertionError("JSON escaping changed");
        }
        System.out.println("PASS QuickShop cache, JSON fields, and escaping");
    }
}
