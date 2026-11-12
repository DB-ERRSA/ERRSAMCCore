package com.errsa.inventory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.modules.CoreModule;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

public final class InventoryAuditModule extends CoreModule implements Listener {
    public InventoryAuditModule(ERRSAMCCore plugin) { super(plugin, "inventory-audit");
        for (Severity severity : Severity.values()) {
            counters.put(severity, new AtomicLong());
        }
    }

    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();
    private final Map<String, Long> recentIncidents = new ConcurrentHashMap<>();
    private final Map<String, Long> consoleCooldowns = new ConcurrentHashMap<>();
    private final EnumMap<Severity, AtomicLong> counters = new EnumMap<>(Severity.class);

    private long tick;
    private Settings settings;
    private BukkitTask tickTask;
    private BukkitTask cleanupTask;
    private AsyncIncidentWriter writer;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick++, 1L, 1L);
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, settings.cleanupPeriodTicks(),
            settings.cleanupPeriodTicks());
        getLogger().info("InventoryAudit 2.0 enabled in incident mode. Console threshold=" +
            settings.consoleMinSeverity()
                + ", routine event logging=" + settings.writeRoutineEvents() + ".");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (writer != null) {
            writer.close();
        }
        states.clear();
        recentIncidents.clear();
        consoleCooldowns.clear();
    }

    void reloadSettings() {
        reloadConfig();
        settings = Settings.from(getConfig());
        if (writer != null) {
            writer.close();
        }
        writer = new AsyncIncidentWriter(
                getDataFolder().toPath().resolve(settings.logFile()),
                settings.maxFileQueue(),
                settings.asyncFileWriter(),
                getLogger());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerState s = states.computeIfAbsent(p.getUniqueId(), id -> new PlayerState());
        s.joinGraceUntil = tick + settings.joinGraceTicks();
        acceptCurrent(p, s, "JOIN");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        PlayerState s = states.get(e.getPlayer().getUniqueId());
        if (s != null) {
            acceptCurrent(e.getPlayer(), s, "QUIT");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        PlayerState s = states.computeIfAbsent(p.getUniqueId(), id -> new PlayerState());
        boolean worldChange = e.getFrom().getWorld() != null && e.getTo() != null && e.getTo().getWorld() != null
                && !e.getFrom().getWorld().getUID().equals(e.getTo().getWorld().getUID());
        if (worldChange) {
            s.worldGraceUntil = Math.max(s.worldGraceUntil, tick + settings.worldChangeGraceTicks());
            s.pendingContextChange = true;
        }
        scheduleObservation(p.getUniqueId(), "TELEPORT", 1L);
        scheduleObservation(p.getUniqueId(), "TELEPORT", 5L);
        scheduleObservation(p.getUniqueId(), "TELEPORT", 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        PlayerState s = states.computeIfAbsent(p.getUniqueId(), id -> new PlayerState());
        s.worldGraceUntil = Math.max(s.worldGraceUntil, tick + settings.worldChangeGraceTicks());
        s.pendingContextChange = true;
        // Do not compare old-world and new-world hashes as if they belong to one inventory.
        acceptCurrent(p, s, "WORLD_CHANGE");
        scheduleObservation(p.getUniqueId(), "WORLD_CHANGE", 1L);
        scheduleObservation(p.getUniqueId(), "WORLD_CHANGE", 5L);
        scheduleObservation(p.getUniqueId(), "WORLD_CHANGE", 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamemode(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();
        PlayerState s = states.computeIfAbsent(p.getUniqueId(), id -> new PlayerState());
        s.gamemodeGraceUntil = Math.max(s.gamemodeGraceUntil, tick + settings.gamemodeChangeGraceTicks());
        s.pendingContextChange = true;
        scheduleObservation(p.getUniqueId(), "GAMEMODE_CHANGE", 1L);
        scheduleObservation(p.getUniqueId(), "GAMEMODE_CHANGE", 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        PlayerState s = states.computeIfAbsent(p.getUniqueId(), id -> new PlayerState());
        s.respawnGraceUntil = Math.max(s.respawnGraceUntil, tick + settings.respawnGraceTicks());
        s.pendingContextChange = true;
        scheduleObservation(p.getUniqueId(), "RESPAWN", 1L);
        scheduleObservation(p.getUniqueId(), "RESPAWN", 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) {
            return;
        }
        if (!settings.shulkerContentOverlap()) {
            return;
        }
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShulkerBox)) {
            return;
        }

        ShulkerMirrorCheck.Result result = ShulkerMirrorCheck.check(
                top, p.getInventory(), settings);
        if (result != null) {
            incident(p,
                    "SHULKER_CONTENTS_MIRRORED_IN_PLAYER_INVENTORY",
                    Severity.HIGH,
                    result.description());
        }
    }

    private void scheduleObservation(UUID playerId, String source, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                return;
            }
            observe(p, source + ":FOLLOWUP_" + delay + "T");
        }, delay);
    }

    private void observe(Player p, String source) {
        PlayerState s = states.computeIfAbsent(p.getUniqueId(), id -> new PlayerState());
        String context = contextKey(p);
        int hash = AuditInventorySnapshot.hash(p.getInventory());
        long now = tick;

        // Per-world inventory plugins intentionally replace inventory during these windows.
        if (now <= s.joinGraceUntil || now <= s.worldGraceUntil || now <= s.gamemodeGraceUntil ||
            now <= s.respawnGraceUntil) {
            s.contextHashes.put(context, new ContextHash(hash, now));
            s.lastContext = context;
            s.lastHash = hash;
            s.pendingContextChange = false;
            routine(p, "EXPECTED_CONTEXT_SYNC", source + " context=" + context + " hash=" + hash);
            return;
        }

        ContextHash expected = s.contextHashes.get(context);
        if (expected != null && expected.hash() != hash && settings.oldHashInSameContext()) {
            // Only suspicious when the same world+gamemode context unexpectedly rolls to a historical inventory.
            String priorContext = s.hashToContext.get(hash);
            if (priorContext != null && priorContext.equals(context) && !Objects.equals(s.lastHash, hash)) {
                incident(p, "OLDER_INVENTORY_HASH_REAPPEARED_SAME_CONTEXT", Severity.HIGH,
                        "context=" + context + " previousHash=" + expected.hash() + " currentHash=" + hash +
                            " observedAt=" + source);
            }
        }

        // A hash known from another world is explicitly NOT an anomaly. That's normal per-world inventory behavior.
        s.contextHashes.put(context, new ContextHash(hash, now));
        s.hashToContext.put(hash, context);
        s.lastContext = context;
        s.lastHash = hash;
        s.pendingContextChange = false;
    }

    private void acceptCurrent(Player p, PlayerState s, String source) {
        int hash = AuditInventorySnapshot.hash(p.getInventory());
        String context = contextKey(p);
        s.contextHashes.put(context, new ContextHash(hash, tick));
        s.hashToContext.put(hash, context);
        s.lastContext = context;
        s.lastHash = hash;
        routine(p, "BASELINE", source + " context=" + context + " hash=" + hash);
    }

    private String contextKey(Player p) {
        return p.getWorld().getUID() + "/" + p.getGameMode().name();
    }

    private void incident(Player p, String code, Severity severity, String details) {
        String context = p.getWorld().getUID() + "/" + p.getGameMode();
        String dedupeKey = p.getUniqueId() + "|" + code + "|" + context + "|" +
            details.replaceAll("observedAt=[^ ]+", "observedAt=*");
        long last = recentIncidents.getOrDefault(dedupeKey, Long.MIN_VALUE / 2);
        if (tick - last < settings.duplicateWindowTicks()) {
            return;
        }
        recentIncidents.put(dedupeKey, tick);

        counters.get(severity).incrementAndGet();
        String line = "severity=" + severity + " code=" + code + " player=" + p.getName() + " uuid=" + p.getUniqueId()
                + " world=" + p.getWorld().getName() + " gamemode=" + p.getGameMode() + " " + details;

        if (severity.atLeast(settings.fileMinSeverity())) {
            writer.offer(Instant.now() + " " + line);
        }
        if (severity.atLeast(settings.consoleMinSeverity())) {
            String throttleKey = p.getUniqueId() + "|" + code;
            long nowMs = System.currentTimeMillis();
            long lastMs = consoleCooldowns.getOrDefault(throttleKey, 0L);
            if (nowMs - lastMs >= settings.perPlayerCodeCooldownSeconds() * 1000L) {
                consoleCooldowns.put(throttleKey, nowMs);
                getLogger().warning(line);
            }
        }
    }

    private void routine(Player p, String event, String details) {
        if (!settings.writeRoutineEvents()) {
            return;
        }
        writer.offer(Instant.now() + " severity=DEBUG event=" + event + " player=" + p.getName() + " " + details);
    }

    int stateCount() {
        return states.size();
    }

    int queueSize() {
        return writer.queueSize();
    }

    long droppedCount() {
        return writer.dropped();
    }

    long incidentCount(Severity severity) {
        return counters.get(severity).get();
    }

    private void cleanup() {
        long cutoff = tick - settings.rememberedContextMinutes() * 60L * 20L;
        states.entrySet().removeIf(entry -> {
            PlayerState s = entry.getValue();
            s.contextHashes.entrySet().removeIf(e -> e.getValue().tick() < cutoff);
            s.hashToContext.entrySet().removeIf(e -> !s.contextHashes.containsKey(e.getValue()));
            Player p = Bukkit.getPlayer(entry.getKey());
            return (p == null || !p.isOnline()) && s.contextHashes.isEmpty();
        });
        recentIncidents.entrySet().removeIf(e -> tick - e.getValue() > settings.duplicateWindowTicks() * 4L);
        long cooldownCutoff = System.currentTimeMillis() - settings.perPlayerCodeCooldownSeconds() * 4000L;
        consoleCooldowns.entrySet().removeIf(e -> e.getValue() < cooldownCutoff);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        return new InventoryAuditCommand(this).execute(sender, args);
    }


}
