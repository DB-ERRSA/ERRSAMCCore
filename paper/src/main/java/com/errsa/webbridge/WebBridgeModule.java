package com.errsa.webbridge;

import com.errsa.webbridge.player.PlayerSnapshotService;
import com.errsa.webbridge.snapshot.SnapshotPublisher;
import com.errsa.webbridge.quickshop.QuickShopStockService;
import com.errsa.webbridge.eventbridge.EventBridgeService;
import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.modules.CoreModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

/** Core-managed WebBridge lifecycle; no second JavaPlugin instance. */
public final class WebBridgeModule extends CoreModule {
    private final PlayerSnapshotService playerSnapshots = new PlayerSnapshotService();
    private SnapshotPublisher publisher;
    private QuickShopStockService quickShop;
    private EventBridgeService eventBridge;
    private BukkitTask playerTask;
    private BukkitTask syncTask;

    public WebBridgeModule(ERRSAMCCore plugin) {
        super(plugin, "web-bridge");
    }

    @Override
    public void onEnable() {
        try {
            startServices();
        } catch (RuntimeException | LinkageError failure) {
            onDisable();
            throw failure;
        }
    }

    private void startServices() {
        publisher = new SnapshotPublisher(getConfig(), getLogger());
        long interval = Math.max(5L,
                getConfig().getLong("sync-interval-seconds", 15L));
        playerTask = Bukkit.getScheduler().runTaskTimer(
                plugin, () -> publisher.updatePlayers(playerSnapshots.capture()),
                1L, 20L);
        syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, publisher::sync, 20L, interval * 20L);
        eventBridge = new EventBridgeService(plugin);
        eventBridge.start();
        quickShop = new QuickShopStockService(plugin, getDataFolder().toPath());
        quickShop.start();
        getLogger().info("WebBridge snapshots scheduled every "
                + interval + " seconds.");
    }

    @Override
    public void onDisable() {
        if (playerTask != null) {
            playerTask.cancel();
            playerTask = null;
        }
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        if (eventBridge != null) {
            eventBridge.stop();
            eventBridge = null;
        }
        if (quickShop != null) {
            quickShop.stop();
            quickShop = null;
        }
        publisher = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] arguments) {
        if (publisher == null) {
            sender.sendMessage("WebBridge is not initialized.");
            return true;
        }
        if (arguments.length > 0 && arguments[0].equalsIgnoreCase("sync")) {
            // Database queries must not run on the Bukkit main thread.
            Bukkit.getScheduler().runTaskAsynchronously(plugin, publisher::sync);
            sender.sendMessage("WebBridge sync queued.");
        }
        sender.sendMessage("ERRSA WebBridge " + publisher.status());
        sender.sendMessage("Public file: " + publisher.outputPath());
        return true;
    }
}
