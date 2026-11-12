package com.errsa.webbridge.eventbridge;

import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

/** Owns the optional EventBridge channel, periodic requests, and received state. */
public final class EventBridgeService implements PluginMessageListener {
    private static final String CHANNEL = "eventbridge:main";
    private static final Path SNAPSHOT_PATH = Path.of("plugins/dynmap/web/errsa-eventbridge.json");
    private final JavaPlugin plugin;
    private final Logger logger;
    private final EventBridgeCodec codec = new EventBridgeCodec();
    private final EventBridgeSnapshotWriter snapshotWriter;
    private BukkitTask task;
    private Object calendar;
    private Object newsletters;
    private boolean running;

    public EventBridgeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.snapshotWriter = new EventBridgeSnapshotWriter(SNAPSHOT_PATH, logger);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            requestNow();
            publishSnapshot();
        }, 60L, 600L);
        publishSnapshot();
        logger.info("EventBridge snapshot enabled -> " + SNAPSHOT_PATH);
    }

    public void stop() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        calendar = null;
        newsletters = null;
        codec.reset();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!running || !CHANNEL.equals(channel)) {
            return;
        }
        try {
            Object decoded = codec.decode(message);
            if (decoded == null) {
                return;
            }
            Object receivedType = EventBridgeFields.read(decoded, "type");
            String type = receivedType == null ? null : String.valueOf(receivedType);
            if ("EVENT_CALENDAR_SNAPSHOT".equals(type)) {
                calendar = codec.decodePayload(decoded,
                        "com.eventbridge.common.net.payload.EventCalendarSnapshot");
                publishSnapshot();
            } else if ("NEWSLETTER_LIST_SNAPSHOT".equals(type)) {
                newsletters = codec.decodePayload(decoded,
                        "com.eventbridge.common.net.payload.NewsletterListSnapshot");
                publishSnapshot();
            }
        } catch (ReflectiveOperationException | IllegalStateException error) {
            logger.warning("Cannot process EventBridge message: " + error.getMessage());
        }
    }

    private void requestNow() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }
        Player carrier = players.iterator().next();
        try {
            carrier.sendPluginMessage(plugin, CHANNEL, codec.encodeCalendarRequest());
            carrier.sendPluginMessage(plugin, CHANNEL, codec.encodeNewsletterRequest());
        } catch (ReflectiveOperationException | IllegalStateException error) {
            logger.warning("Cannot request EventBridge snapshot: " + error.getMessage());
        }
    }

    private void publishSnapshot() {
        if (running) {
            snapshotWriter.writeSnapshot(calendar, newsletters);
        }
    }
}
