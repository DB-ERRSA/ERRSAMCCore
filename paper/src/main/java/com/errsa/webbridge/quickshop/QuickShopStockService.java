package com.errsa.webbridge.quickshop;

import static com.errsa.webbridge.quickshop.QuickShopReflection.first;
import static com.errsa.webbridge.quickshop.QuickShopReflection.invokeNoArg;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

/** Coordinates QuickShop scans, short-lived refresh requests, and persisted stock. */
public final class QuickShopStockService {
    private static final Path SNAPSHOT_PATH = Path.of("plugins/dynmap/web/errsa-shop-stock.json");
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Path cachePath;
    private final QuickShopStockReader reader = new QuickShopStockReader();
    private final Map<String, CachedStock> lastKnown = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshQueued = new AtomicBoolean();
    private QuickShopEventHooks eventHooks;
    private BukkitTask refreshTask;
    private boolean warned;
    private boolean running;

    public QuickShopStockService(JavaPlugin plugin, Path dataFolder) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cachePath = dataFolder.resolve("shop-stock-cache.properties");
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        QuickShopCacheStore.loadCache(cachePath, lastKnown, logger);
        publish(false, List.of(), "waiting for first QuickShop scan", 0, 0, lastKnown.size());
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 80L, 600L);
        eventHooks = new QuickShopEventHooks(plugin, this::onTransaction, () -> queueRefresh(8L));
        eventHooks.register();
        logger.info("QuickShop stock publishing enabled -> " + SNAPSHOT_PATH);
    }

    public void stop() {
        running = false;
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (eventHooks != null) {
            eventHooks.unregister();
            eventHooks = null;
        }
        QuickShopCacheStore.saveCache(cachePath, lastKnown, logger);
        refreshQueued.set(false);
        lastKnown.clear();
        warned = false;
    }

    private void queueRefresh(long delayTicks) {
        if (!running || !refreshQueued.compareAndSet(false, true)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            refreshQueued.set(false);
            if (running) {
                refresh();
            }
        }, delayTicks);
    }

    private void onTransaction(Object event) {
        try {
            if (Boolean.TRUE.equals(invokeNoArg(event, "isCancelled"))) {
                return;
            }
            Object shop = first(event, "getShop", "shop");
            if (shop == null) {
                Object transaction = first(event, "getTransaction", "getResult");
                if (transaction != null) {
                    shop = first(transaction, "getShop", "shop");
                }
            }
            if (shop != null) {
                captureShop(shop, true, "transaction");
            }
        } catch (Exception error) {
            logger.fine("QuickShop transaction payload unavailable: " + error.getMessage());
        }
        queueRefresh(3L);
        queueRefresh(12L);
    }

    private void captureShop(Object shop, boolean allowZero, String cause) {
        try {
            ShopLocation location = reader.location(shop);
            if (location == null || reader.isUnlimited(shop)) {
                return;
            }
            Long stock = reader.physicalStock(shop, location);
            String source = "container-" + cause;
            if (stock == null || (!allowZero && stock <= 0L)) {
                Long reported = reader.reportedStock(shop);
                if (reported != null && (reported > 0L || allowZero)) {
                    stock = reported;
                    source = "quickshop-" + cause;
                }
            }
            if (stock != null && (stock > 0L || allowZero)) {
                remember(location, stock, source, Instant.now());
            }
        } catch (Exception error) {
            logger.fine("Could not sample transaction stock: " + error.getMessage());
        }
    }

    private void remember(ShopLocation location, long stock, String source, Instant observedAt) {
        lastKnown.put(location.key(), new CachedStock(stock, observedAt, source));
        QuickShopCacheStore.saveCache(cachePath, lastKnown, logger);
    }

    private void refresh() {
        if (!running) {
            return;
        }
        try {
            Collection<?> shops = reader.allShops();
            List<QuickShopStockRow> rows = new ArrayList<>();
            int resolved = 0;
            int unresolved = 0;
            int containerStock = 0;
            int cached = 0;
            for (Object shop : shops) {
                try {
                    ShopLocation location = reader.location(shop);
                    if (location == null) {
                        continue;
                    }
                    boolean unlimited = reader.isUnlimited(shop);
                    Long stock = null;
                    String source = null;
                    boolean fromCache = false;
                    Instant observedAt = null;
                    if (!unlimited) {
                        stock = reader.physicalStock(shop, location);
                        if (stock != null && stock > 0L) {
                            source = "container";
                            containerStock++;
                            observedAt = Instant.now();
                            remember(location, stock, source, observedAt);
                        } else {
                            Long reported = reader.reportedStock(shop);
                            if (reported != null && reported > 0L) {
                                stock = reported;
                                source = "quickshop";
                                observedAt = Instant.now();
                                remember(location, reported, source, observedAt);
                            } else {
                                CachedStock sample = lastKnown.get(location.key());
                                if (sample != null) {
                                    stock = sample.stock();
                                    source = "paper-cache";
                                    observedAt = sample.observedAt();
                                    fromCache = true;
                                    cached++;
                                } else {
                                    stock = null;
                                }
                            }
                        }
                    }
                    if (!unlimited && (stock == null || fromCache)) {
                        if (!fromCache) {
                            unresolved++;
                        }
                    } else {
                        resolved++;
                    }
                    rows.add(new QuickShopStockRow(location.world(), location.x(),
                            location.y(), location.z(), stock, unlimited,
                            source, fromCache, observedAt));
                } catch (Exception error) {
                    unresolved++;
                }
            }
            String diagnostic = unresolved > 0
                    ? "live stock resolved for " + resolved + " shop(s); " + cached
                            + " from persistent cache; " + unresolved + " unresolved/unloaded"
                    : null;
            publish(true, rows, diagnostic, shops.size(), resolved, cached);
            warned = false;
        } catch (Exception error) {
            if (!warned) {
                logger.warning("QuickShop scan failed: " + error.getMessage());
                warned = true;
            }
            publish(false, List.of(), error.getMessage(), 0, 0, lastKnown.size());
        }
    }

    private void publish(boolean available, List<QuickShopStockRow> rows,
                         String error, int count, int resolved, int cached) {
        QuickShopSnapshotWriter.write(
                SNAPSHOT_PATH, logger, available, rows, error, count, resolved, cached);
    }
}
