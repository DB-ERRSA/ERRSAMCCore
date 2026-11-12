package com.errsa.webbridge.quickshop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/** Backward-compatible persistence for shop-stock-cache.properties. */
final class QuickShopCacheStore {
    private QuickShopCacheStore() {
    }

    static void loadCache(Path path, Map<String, CachedStock> samples, Logger logger) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String[] fields = properties.getProperty(key, "").split(",", 3);
                if (fields.length < 2) {
                    continue;
                }
                try {
                    long stock = Long.parseLong(fields[0]);
                    Instant observedAt = Instant.ofEpochMilli(Long.parseLong(fields[1]));
                    String source = fields.length == 3 ? fields[2] : "paper-cache";
                    samples.put(key, new CachedStock(stock, observedAt, source));
                } catch (NumberFormatException invalidSample) {
                    logger.warning("Ignoring invalid QuickShop cache entry: " + key);
                }
            }
            logger.info("Loaded " + samples.size() + " cached QuickShop stock values.");
        } catch (IOException error) {
            logger.warning("Cannot load QuickShop cache: " + error.getMessage());
        }
    }

    static synchronized void saveCache(
            Path path, Map<String, CachedStock> samples, Logger logger) {
        try {
            Path directory = path.toAbsolutePath().getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            Properties properties = new Properties();
            for (Map.Entry<String, CachedStock> entry : samples.entrySet()) {
                CachedStock sample = entry.getValue();
                String value = sample.stock() + "," + sample.observedAt().toEpochMilli()
                        + "," + sample.source();
                properties.setProperty(entry.getKey(), value);
            }
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "ERRSAWebBridge last-known QuickShop stock");
            }
        } catch (IOException error) {
            logger.warning("Cannot save QuickShop cache: " + error.getMessage());
        }
    }
}
