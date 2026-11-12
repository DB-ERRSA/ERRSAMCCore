package com.errsa.webbridge.quickshop;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns only QuickShop-related listeners, so shutting down WebBridge is isolated. */
final class QuickShopEventHooks {
    private static final String[] TRANSACTION_TYPES = {
            "com.ghostchu.quickshop.api.event.transaction.ShopTransactionEvent",
            "com.ghostchu.quickshop.api.event.transaction.ShopPostTransactionEvent",
            "com.ghostchu.quickshop.api.event.ShopTransactionEvent"
    };

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Listener listener = new Listener() { };
    private final java.util.function.Consumer<Object> transactionHandler;
    private final Runnable activityHandler;

    QuickShopEventHooks(JavaPlugin plugin,
                        java.util.function.Consumer<Object> transactionHandler,
                        Runnable activityHandler) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.transactionHandler = transactionHandler;
        this.activityHandler = activityHandler;
    }

    void register() {
        boolean foundTransactionEvent = false;
        for (String className : TRANSACTION_TYPES) {
            try {
                Class<?> type = Class.forName(className);
                registerEvent(type, true);
                foundTransactionEvent = true;
                logger.info("Listening for QuickShop event: " + className);
            } catch (ClassNotFoundException unavailable) {
                // QuickShop versions expose different transaction event names.
            } catch (Exception error) {
                logger.warning("Could not register QuickShop event " + className
                        + ": " + error.getMessage());
            }
        }
        if (!foundTransactionEvent) {
            logger.warning("No QuickShop transaction event found; using activity refresh fallback.");
        }
        try {
            registerEvent(Class.forName("org.bukkit.event.player.PlayerInteractEvent"), false);
            registerEvent(Class.forName("org.bukkit.event.inventory.InventoryCloseEvent"), false);
        } catch (ClassNotFoundException unavailable) {
            logger.warning("Cannot register player activity event: " + unavailable.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void registerEvent(Class<?> eventClass, boolean transaction) {
        if (!Event.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("Not a Bukkit event: " + eventClass.getName());
        }
        EventExecutor executor = (ignoredListener, event) -> {
            if (transaction) {
                transactionHandler.accept(event);
            } else {
                activityHandler.run();
            }
        };
        Bukkit.getPluginManager().registerEvent(
                (Class<? extends Event>) eventClass, listener,
                EventPriority.MONITOR, executor, plugin, true);
    }

    void unregister() {
        HandlerList.unregisterAll(listener);
    }
}
