package com.errsa.webbridge.quickshop;

import com.errsa.webbridge.snapshot.SnapshotJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Publishes the original QuickShop JSON contract via atomic file replacement. */
final class QuickShopSnapshotWriter {
    private QuickShopSnapshotWriter() {
    }

    static void write(
            Path destination,
            Logger logger,
            boolean available,
            List<QuickShopStockRow> rows,
            String error,
            int shopCount,
            int resolved,
            int cached) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("available", available);
        result.put("generatedAt", Instant.now().toString());
        result.put("shopCount", shopCount);
        result.put("stockResolved", resolved);
        result.put("stockCached", cached);
        result.put("error", error);
        List<Map<String, Object>> shops = new java.util.ArrayList<>(rows.size());
        for (QuickShopStockRow row : rows) {
            Map<String, Object> shop = new LinkedHashMap<>();
            shop.put("world", row.world());
            shop.put("x", row.x());
            shop.put("y", row.y());
            shop.put("z", row.z());
            shop.put("stock", row.stock());
            shop.put("unlimited", row.unlimited());
            shop.put("cached", row.cached());
            shop.put("source", row.source());
            shop.put("observedAt", row.observedAt() == null
                    ? null : row.observedAt().toString());
            shops.add(shop);
        }
        result.put("shops", shops);
        try {
            Path directory = destination.toAbsolutePath().getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
            Files.writeString(temporary, SnapshotJson.stringify(result), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException errorWriting) {
            logger.warning("Cannot write QuickShop snapshot: " + errorWriting.getMessage());
        }
    }
}
