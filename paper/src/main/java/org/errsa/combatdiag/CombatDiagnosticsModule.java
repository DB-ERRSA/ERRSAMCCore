package org.errsa.combatdiag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.modules.CoreModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class CombatDiagnosticsModule extends CoreModule implements Listener {
    public CombatDiagnosticsModule(ERRSAMCCore plugin) {
        super(plugin, "combat-diagnostics");
    }

    final Map<UUID, Long> watched = new HashMap<>(); // main thread only
    final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    final AtomicLong dropped = new AtomicLong();
    private volatile boolean running;
    private Thread writerThread;
    private long sessionSequence;
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor(r -> {Thread t = new
        Thread(r, "ERRSA-cdiag-webhook");t.setDaemon(true);return t;});
    private final HttpClient webhookClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private volatile long lastWebhookMillis;
    final AtomicLong webhookFailures = new AtomicLong();
    volatile String lastIoError = "none";
    private long suppressedWebhookCount;
    // Only accessed on the main server thread. Bounded ring buffers; never store IPs or chat.
    private final Map<UUID, ArrayDeque<Map<String, Object>>> history = new HashMap<>();
    final Map<UUID, ActiveIncident> incidents = new HashMap<>();
    private final Map<String, Long> lastTriggered = new HashMap<>();
    private final Map<UUID, Long> lastInventoryClick = new HashMap<>();
    private final Map<UUID, String> lastInventoryItem = new HashMap<>();
    private final Set<Logger> attachedLoggers = new HashSet<>();
    private Handler alertHandler;
    private long incidentSequence;
    private final Map<String, Long> recentHits = new HashMap<>();
    final ArrayDeque<Map<String, Object>> recentIncidentIndex = new ArrayDeque<>();
    private final Map<UUID, Long> lastFireworkTrigger = new HashMap<>();
    private final Map<UUID, Long> lastArmSwing = new HashMap<>();
    private final Map<UUID, ArrayDeque<PacketObservation>> packetHistory = new HashMap<>();
    private final ArrayDeque<String> debugWarnings = new ArrayDeque<>();
    PacketBridge packetBridge;
    long packetsObserved;
    long damageWithPacketContext;
    long directMeleeWithoutPacketContext;
    final AtomicLong loggerAlertsRecognized = new AtomicLong();
    final AtomicLong loggerAlertsMatched = new AtomicLong();
    void packetAttack(Player attacker, Player target, long receivedAt) {
        // Called on the server thread after ProtocolLib has queued the raw packet identifiers.
        packetsObserved++;
        long processedAt = System.currentTimeMillis();
        PacketObservation observation = new PacketObservation(
                receivedAt,
                processedAt,
                target == null ? null : target.getUniqueId().toString(),
                target == null ? null : target.getName(),
                playerData(attacker),
                target == null ? null : playerData(target));
        ArrayDeque<PacketObservation> packets = packetHistory.computeIfAbsent(
                attacker.getUniqueId(), ignored -> new ArrayDeque<>());
        packets.addLast(observation);
        while (packets.size() > 32) {
            packets.removeFirst();
        }
        if (isCapturing(attacker) || (target != null && isCapturing(target))) {
            record("attack_packet_observed", attacker, target, map(
                    "received_ms", receivedAt,
                    "main_thread_observed_ms", processedAt,
                    "measurement_quality", "packet-not-yet-correlated",
                    "packet_position_note", "Captured on packet processing tick, not at receipt",
                    "attacker", observation.attackerAtProcessing(),
                    "target", observation.targetAtProcessing()));
        }
    }

    void packetBridgeWarning(String message) {
        getLogger().warning(message);
        if (debugWarnings.size() >= 8) {
            debugWarnings.removeFirst();
        }
        debugWarnings.addLast(message);
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();
        running = true;
        writerThread = new Thread(this::writerLoop, "ERRSA-CombatDiagnostics-writer");
        writerThread.setDaemon(true);
        writerThread.start();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        installAlertListener();
        if (getConfig().getBoolean("packet-observation.enabled", true)) {
            packetBridge = new PacketBridge(this);
            packetBridge.start();
        }
        int tickStep = Math.max(1, getConfig().getInt("sample-every-ticks", 2));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            watched.entrySet().removeIf(e -> e.getValue() <= now);
            if (getConfig().getBoolean("automatic.enabled", true)) {
                sampleAll(now);
            }
            for (UUID uuid : watched.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    record("sample", p, null, map("player", playerData(p)));
                }
            }
        }, tickStep, tickStep);
        getLogger().info("Combat diagnostics v0.5.1 enabled: evidence-only, no enforcement. Packet " +
            "observation is optional and does not provide lag compensation.");
    }

    @Override public void onDisable() {
        uninstallAlertListener();
        if (packetBridge != null) {
            packetBridge.stop();
        }
        watched.clear();
        history.clear();
        incidents.clear();
        packetHistory.clear();
        running = false;
        notificationExecutor.shutdown();
        if (writerThread != null) {
            try {
                writerThread.join(7000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (writerThread.isAlive()) getLogger().warning("Writer still draining at shutdown: " + logQueue.size()
                + " queued records");
        }
        try { notificationExecutor.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        return new CombatDiagnosticsCommand(this).execute(sender, args);
    }


    private boolean isWatched(Player p) {
        return watched.containsKey(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)
                || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        boolean directMelee = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
        boolean sweep = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;
        boolean watchedOrActive = isCapturing(attacker) || isCapturing(target);
        if (!watchedOrActive && !getConfig().getBoolean("automatic.enabled", true)) {
            return;
        }
        if (!attacker.getWorld().equals(target.getWorld())) {
            return;
        }

        // Freeze damage-event evidence now. Never replace it with positions from a delayed task.
        long damageAt = System.currentTimeMillis();
        Location eye = attacker.getEyeLocation();
        BoundingBox hitbox = target.getBoundingBox();
        double nearest = eye.distance(new Location(eye.getWorld(),
                clamp(eye.getX(), hitbox.getMinX(), hitbox.getMaxX()),
                clamp(eye.getY(), hitbox.getMinY(), hitbox.getMaxY()),
                clamp(eye.getZ(), hitbox.getMinZ(), hitbox.getMaxZ())));
        double centerDistance = attacker.getLocation().distance(target.getLocation());
        String cause = event.getCause().name();
        String hitKey = attacker.getUniqueId() + ":" + target.getUniqueId() + ":" + cause;
        Long previous = recentHits.put(hitKey, damageAt);
        if (previous != null && damageAt - previous < Math.max(1,
                getConfig().getLong("automatic.hit-dedup-ms", 40))) {
            return;
        }

        Map<String, Object> attackerAtDamage = playerData(attacker);
        Map<String, Object> targetAtDamage = playerData(target);
        boolean cancelled = event.isCancelled();
        double rawDamage = round(event.getDamage());
        double finalDamage = round(event.getFinalDamage());
        boolean captureEvidence = watchedOrActive;
        long swingAge = lastArmSwing.containsKey(attacker.getUniqueId())
                ? damageAt - lastArmSwing.get(attacker.getUniqueId()) : -1;

        // ProtocolLib packet processing can reach the main thread after the Bukkit damage event.
        // Defer correlation only; every damage measurement above remains the event-time snapshot.
        int delay = Math.max(1, getConfig().getInt("packet-observation.correlation-delay-ticks", 2));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            PacketCorrelation.Result correlation = PacketCorrelation.evaluate(
                    packetHistory.getOrDefault(attacker.getUniqueId(), new ArrayDeque<>()),
                    target.getUniqueId().toString(), damageAt,
                    Math.max(50, getConfig().getLong("packet-observation.match-window-ms", 400)),
                    Math.max(0, getConfig().getLong("packet-observation.reorder-tolerance-ms", 50)));
            boolean matched = correlation.matched();
            if (directMelee && !cancelled && finalDamage > 0) {
                if (matched) {
                    damageWithPacketContext++;
                } else {
                    directMeleeWithoutPacketContext++;
                }
            }

            Map<String, Object> details = map(
                    "damage_cause", cause,
                    "direct_melee", directMelee,
                    "sweep", sweep,
                    "cancelled", cancelled,
                    "raw_damage", rawDamage,
                    "final_damage", finalDamage,
                    "server_eye_to_current_hitbox", round(nearest),
                    "server_feet_distance", round(centerDistance),
                    "damage_event_ms", damageAt,
                    "attacker_at_damage", attackerAtDamage,
                    "victim_at_damage", targetAtDamage,
                    "attacker_elytra", attackerAtDamage.get("gliding"),
                    "victim_elytra", targetAtDamage.get("gliding"),
                    "attacker_velocity", map("x", attackerAtDamage.get("vx"),
                            "y", attackerAtDamage.get("vy"), "z", attackerAtDamage.get("vz")),
                    "victim_velocity", map("x", targetAtDamage.get("vx"),
                            "y", targetAtDamage.get("vy"), "z", targetAtDamage.get("vz")),
                    "attacker_ping_ms", attackerAtDamage.get("ping_ms"),
                    "victim_ping_ms", targetAtDamage.get("ping_ms"),
                    "ms_since_arm_swing", swingAge,
                    "packet_integration_active", packetBridge != null && packetBridge.isActive(),
                    "packet_evidence_available", !"uncorrelated".equals(correlation.quality()),
                    "distance_review_only", true,
                    "not_proof", true);
            details.putAll(PacketCorrelation.packetFields(correlation));
            details.put("attack_packet_match", matched);
            details.put("review_distance_exceeded", nearest >
                    getConfig().getDouble("review-distance-blocks", 3.2));
            details.put("measurement_kind", "server_damage_event_not_lag_compensated");

            boolean candidate = directMelee && !cancelled && finalDamage > 0;
            boolean strict = getConfig().getBoolean(
                    "packet-observation.require-match-for-distance-trigger", false);
            if (getConfig().getBoolean("automatic.enabled", true)
                    && candidate && (!strict || matched)
                    && nearest > getConfig().getDouble("automatic.trigger-hit-distance", 3.6)
                    && attacker.isOnline()) {
                Map<String, Object> summary = new LinkedHashMap<>(details);
                summary.put("incident_cause", "melee_distance_review");
                summary.put("measurement_quality", correlation.quality());
                summary.put("classification", "REVIEW_ONLY_NOT_PROOF");
                trigger(attacker, "melee_distance_review", summary, target.isOnline() ? target : null);
            }
            if (captureEvidence && attacker.isOnline()) {
                record("pvp_damage", attacker, target.isOnline() ? target : null, details);
            }
        }, delay);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent e) {
        Player p = e.getPlayer();
        lastArmSwing.put(p.getUniqueId(), System.currentTimeMillis());
        if (isCapturing(p)) {
            record("arm_animation", p, null, map("player", playerData(p)));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onFlightToggle(PlayerToggleFlightEvent e) {
        if (isCapturing(e.getPlayer())) record("flight_toggle", e.getPlayer(), null,
                map("requested", e.isFlying(), "cancelled", e.isCancelled(), "player", playerData(e.getPlayer())));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() instanceof Player p && isCapturing(p))
            record("glide_toggle", p, null, map("gliding_requested", e.isGliding(), "cancelled", e.isCancelled(),
                "player", playerData(p)));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onFirework(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Firework f)) {
            return;
        }
        if (f.getShooter() instanceof Player p && isCapturing(p))
            record("firework_launch", p, null, map("cancelled", e.isCancelled(), "player", playerData(p)));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isWatched(p) && !getConfig().getBoolean("automatic.enabled", true) && !isIncidentActive(p)) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) {
            return;
        }
        Long clickTime = lastInventoryClick.get(p.getUniqueId());
        long delta = clickTime == null ? -1 : System.currentTimeMillis() - clickTime;
        if (getConfig().getBoolean("automatic.enabled", true) && delta >= 0 && delta <=
                getConfig().getLong("automatic.rapid-firework-click-ms", 160)
                && "FIREWORK_ROCKET".equals(lastInventoryItem.get(p.getUniqueId()))
                && !e.isCancelled() && e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
                && System.currentTimeMillis() - lastFireworkTrigger.getOrDefault(p.getUniqueId(), 0L) > 250) {
            lastFireworkTrigger.put(p.getUniqueId(), System.currentTimeMillis());
            trigger(p, "rapid_inventory_firework", map("elapsed_ms", delta,
                    "last_clicked_item", lastInventoryItem.get(p.getUniqueId())));
        }
        if (!isCapturing(p)) {
            return;
        }
        record("firework_interact", p, null, map("ms_since_inventory_click", delta,"action",
            e.getAction().toString(), "hand", e.getHand() == null ? null : e.getHand().toString(), "player",
            playerData(p)));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        if (getConfig().getBoolean("automatic.enabled", true)) {
            lastInventoryClick.put(p.getUniqueId(), System.currentTimeMillis());
            lastInventoryItem.put(p.getUniqueId(), itemType(current).equals("FIREWORK_ROCKET") ||
                itemType(cursor).equals("FIREWORK_ROCKET") ? "FIREWORK_ROCKET" : "OTHER");
        }
        if (!isCapturing(p)) {
            return;
        }
        record("inventory_click", p, null, map("slot", e.getSlot(), "raw_slot", e.getRawSlot(),
                "click", e.getClick().toString(), "action", e.getAction().toString(),
                "cursor_type", itemType(cursor), "slot_item_type", itemType(current),
                "cancelled", e.isCancelled(), "player", playerData(p)));
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && isCapturing(p))
            record("inventory_after_click", p, null, map("player", playerData(p))); });
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onHotbar(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (isCapturing(p)) record("hotbar_change", p, null, map("from", e.getPreviousSlot(), "to", e.getNewSlot(),
            "cancelled", e.isCancelled(), "player", playerData(p)));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (isCapturing(p)) record("hand_swap", p, null, map("cancelled", e.isCancelled(), "main_item",
            itemType(e.getMainHandItem()), "off_item", itemType(e.getOffHandItem()), "player", playerData(p)));
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (watched.remove(p.getUniqueId()) != null) {
            record("watch_disconnect", p, null, map("player", playerData(p)));
        }
        ActiveIncident incident = incidents.remove(p.getUniqueId());
        if (incident != null) record("incident_end", p, null, map("incident_id", incident.id, "reason", "disconnect",
            "review_outcome", "UNDETERMINED"));
        history.remove(p.getUniqueId());
        lastInventoryClick.remove(p.getUniqueId());
        lastInventoryItem.remove(p.getUniqueId());
        lastFireworkTrigger.remove(p.getUniqueId());
        packetHistory.remove(p.getUniqueId());
        lastArmSwing.remove(p.getUniqueId());
    }

    private boolean isIncidentActive(Player p) {
        return incidents.containsKey(p.getUniqueId());
    }
    private boolean isCapturing(Player p) {
        return isWatched(p) || isIncidentActive(p);
    }

    private void sampleAll(long now) {
        int maxPlayers = Math.max(1, getConfig().getInt("automatic.max-buffered-players", 100));
        int maxSamples = Math.max(4, getConfig().getInt("automatic.pre-seconds", 12) * 20 /
                Math.max(1, getConfig().getInt("sample-every-ticks", 2)));
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (++count > maxPlayers && !isCapturing(p)) {
                continue;
            }
            Map<String, Object> snap = map("at_ms", now, "player", playerData(p));
            ArrayDeque<Map<String, Object>> deque = history.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
            deque.addLast(snap);
            while (deque.size() > maxSamples) {
                deque.removeFirst();
            }
            if (isIncidentActive(p)) record("incident_sample", p, null, map("incident_id",
                incidents.get(p.getUniqueId()).id, "player", playerData(p)));
            // Capture the counterpart's post-incident movements even when not separately flagged.
            for (ActiveIncident active : incidents.values()) {
                if (p.getUniqueId().equals(active.otherPlayer) && !isIncidentActive(p))
                    record("incident_counterpart_sample", p, null, map("incident_id", active.id, "player",
                        playerData(p)));
            }
        }
        Iterator<Map.Entry<UUID, ActiveIncident>> it = incidents.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveIncident> e = it.next();
            if (e.getValue().endMillis > now) {
                continue;
            }
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) record("incident_end", p, null, map("incident_id", e.getValue().id,
                    "reason", "post_window_elapsed", "additional_triggers", e.getValue().extraTriggers,
                    "review_outcome", "UNDETERMINED", "requires_human_review", true));
            it.remove();
        }
        lastTriggered.entrySet().removeIf(e -> now - e.getValue() > 120_000);
        recentHits.entrySet().removeIf(e -> now - e.getValue() > 10_000);
        lastArmSwing.entrySet().removeIf(e -> now - e.getValue() > 30_000);
        packetHistory.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(packet -> now - packet.receivedAt() > 30_000);
            return entry.getValue().isEmpty();
        });
        lastInventoryClick.entrySet().removeIf(e -> now - e.getValue() > 30_000);
        lastFireworkTrigger.entrySet().removeIf(e -> now - e.getValue() > 30_000);
    }

    void trigger(Player p, String reason, Map<String, Object> details) {
        trigger(p, reason, details, null);
    }
    void trigger(Player p, String reason, Map<String, Object> details, Player counterpart) {
        if ((!getConfig().getBoolean("automatic.enabled", true) && !reason.startsWith("staff_")) || !p.isOnline())
            return;
        long now = System.currentTimeMillis();
        String key = p.getUniqueId() + ":" + reason;
        int cooldown = Math.max(1, getConfig().getInt("automatic.cooldown-seconds", 20)) * 1000;
        Long last = lastTriggered.get(key);
        if (last != null && now - last < cooldown) {
            ActiveIncident existing = incidents.get(p.getUniqueId());
            if (existing != null) { existing.extraTriggers++; existing.endMillis = Math.max(existing.endMillis,
                    now + Math.max(1, getConfig().getInt("automatic.post-seconds", 8)) * 1000L);
                record("incident_suppressed_repeat", p, counterpart, map("incident_id", existing.id, "reason",
                    reason, "count", existing.extraTriggers));
            }
            return;
        }
        lastTriggered.put(key, now);
        String id = "inc-" + now + "-" + (++incidentSequence);
        ActiveIncident existing = incidents.get(p.getUniqueId());
        if (existing != null) {
            existing.extraTriggers++;
            record("incident_additional_trigger", p, counterpart, map("incident_id", existing.id, "trigger",
                reason, "details", details));
            existing.endMillis = now + Math.max(1, getConfig().getInt("automatic.post-seconds", 8)) * 1000L;
            return;
        }
        ActiveIncident incident = new ActiveIncident(id, counterpart == null ? null : counterpart.getUniqueId(), now,
                now + Math.max(1, getConfig().getInt("automatic.post-seconds", 8)) * 1000L);
        List<Map<String, Object>> pre = new ArrayList<>(history.getOrDefault(p.getUniqueId(), new ArrayDeque<>()));
        incidents.put(p.getUniqueId(), incident);
        recentIncidentIndex.addLast(map(
                "id", id, "player", p.getName(), "reason", reason,
                "utc", Instant.ofEpochMilli(now).toString(),
                "details", new LinkedHashMap<>(details),
                "summary", CombatIncidentSummary.compact(details),
                "movement", CombatIncidentSummary.movement(details)));
        while (recentIncidentIndex.size() > 40) {
            recentIncidentIndex.removeFirst();
        }
        record("incident_start", p, counterpart, map("incident_id", id, "trigger", reason,
                "details", details, "pre_samples", pre, "at_trigger", playerData(p),
                "counterpart_at_trigger", counterpart == null ? null : playerData(counterpart),
                "counterpart_pre_samples", counterpart == null ? List.of() :
                        new ArrayList<>(history.getOrDefault(counterpart.getUniqueId(), new ArrayDeque<>())),
                "evidence_limitations", List.of("Current server snapshots are not lag-compensated",
                    "Damage event is not the original attack packet", "Plugin listeners can modify damage",
                    "Packet capture is timestamped but not server rollback",
                    "Client-side behavior is not directly observable", "Distance thresholds are review triggers only"),
                "notice", "Evidence only. Trigger scores and server snapshots are not proof of cheating."));
        sendWebhook(id, p.getName(), reason, details, counterpart == null ? null : counterpart.getName());
        if (getConfig().getBoolean("automatic.notify-staff", true)) {
            for (Player staff : Bukkit.getOnlinePlayers()) if (staff.hasPermission("errsa.combatdiag.staff"))
                staff.sendMessage("[CombatDiag] REVIEW " + id + " | " + p.getName() + " | " + reason +
                    " | /cdiag incidents");
        }
    }

    // Optional webhook, off-thread, privacy-minimized, and rate limited. No full JSONL evidence is sent.
    private void sendWebhook(String id, String player, String reason, Map<String, Object> details, String other) {
        if (!getConfig().getBoolean("discord.enabled", false)) {
            return;
        }
        String url = getConfig().getString("discord.webhook-url", "").trim();
        if (!url.startsWith("https://discord.com/api/webhooks/") &&
            !url.startsWith("https://discordapp.com/api/webhooks/")) return;
        long now = System.currentTimeMillis();
        long cooldown = Math.max(5, getConfig().getLong("discord.min-seconds-between-alerts", 30)) * 1000L;
        if (now - lastWebhookMillis < cooldown) {
            suppressedWebhookCount++;
            return;
        }
        lastWebhookMillis = now;
        long suppressed = suppressedWebhookCount; suppressedWebhookCount = 0;
        String content = "[ERRSA Diagnostics] INCIDENT " + id + " | Player: " + player + " | Trigger: " + reason
                + (other == null ? "" : " | Other: " + other)
                + " | Review-only (NOT proof); examine local evidence."
                + (suppressed > 0 ? " | " + suppressed + " alerts rate-limited" : "");
        String payload = EvidenceJson.encode(map("content", content, "allowed_mentions", map("parse", List.of())));
        try {
            notificationExecutor.execute(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                            .header("Content-Type",
                                "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
                    int code = webhookClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                    if (code < 200 || code >= 300) { webhookFailures.incrementAndGet();
                        getLogger().warning("Discord webhook HTTP " + code); }
                } catch (Exception ex) { webhookFailures.incrementAndGet();
                    getLogger().warning("Webhook delivery failed: " + ex.getClass().getSimpleName()); }
            });
        } catch (RejectedExecutionException ignored) { /* shutting down */ }
    }

    // Best-effort capture of human-readable plugin alerts. There is no stable Themis event API here.
    void installAlertListener() {
        if (!getConfig().getBoolean("automatic.capture-console-alerts", true)) {
            return;
        }
        alertHandler = new Handler() {
            @Override public void publish(LogRecord r) {
                if (!running || r == null || r.getMessage() == null) {
                    return;
                }
                String msg = r.getMessage();
                ExternalAlertParser.Alert alert = ExternalAlertParser.parse(msg);
                if (alert == null) {
                    return;
                }
                String name = alert.playerName();
                String source = alert.source();
                String check = alert.check();
                loggerAlertsRecognized.incrementAndGet();
                final String playerName = name, alertSource = source, alertCheck = check;
                // Bukkit lookups and evidence snapshots always run on the main thread.
                if (!plugin.isEnabled()) {
                    return;
                }
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayerExact(playerName);
                        if (p != null) { loggerAlertsMatched.incrementAndGet(); trigger(p, "external_" + alertSource
                            + "_" + alertCheck, map("source", alertSource, "check", alertCheck,
                                "message", msg.length() > 350 ? msg.substring(0,350) : msg)); }
                    });
                } catch (IllegalStateException ignored) { /* Server is shutting down */ }
            }
            @Override
            public void flush() {
                // No buffered output to flush.
            }
            @Override
            public void close() {
                // No resources to release.
            }
        };
        // Logger handlers are best-effort: plugins can emit directly to console without java.util.logging.
        attachLogger(Bukkit.getLogger());
        attachLogger(Logger.getLogger(""));
        attachLogger(Logger.getLogger("Themis"));
        attachLogger(Logger.getLogger("ExploitFixer"));
    }
    private void attachLogger(Logger logger) {
        if (attachedLoggers.add(logger)) {
            logger.addHandler(alertHandler);
        }
    }
    void uninstallAlertListener() {
        if (alertHandler == null) {
            return;
        }
        for (Logger logger : attachedLoggers) {
            logger.removeHandler(alertHandler);
        }
        attachedLoggers.clear();
        alertHandler = null;
    }

    private Map<String, Object> playerData(Player p) {
        Location l = p.getLocation(); Vector v = p.getVelocity();
        return map("uuid", p.getUniqueId().toString(), "name", p.getName(), "world", p.getWorld().getName(),
                "x", round(l.getX()), "y", round(l.getY()), "z", round(l.getZ()),
                "yaw", round(l.getYaw()), "pitch", round(l.getPitch()),
                "vx", round(v.getX()), "vy", round(v.getY()), "vz", round(v.getZ()),
                "ping_ms", p.getPing(), "gliding", p.isGliding(), "flying", p.isFlying(), "on_ground", p.isOnGround(),
                "main_hand", itemType(p.getInventory().getItemInMainHand()),
                "off_hand", itemType(p.getInventory().getItemInOffHand()),
                "chest", itemType(p.getInventory().getChestplate()));
    }

    void record(String type, Player primary, Player secondary, Map<String, Object> data) {
        int capacity = Math.max(100, getConfig().getInt("max-queued-records", 10000));
        if (logQueue.size() >= capacity) {
            dropped.incrementAndGet();
            return;
        }
        Map<String, Object> item = map("schema_version", 6, "at_utc", Instant.now().toString(), "epoch_ms",
            System.currentTimeMillis(),
                "seq", ++sessionSequence, "type", type, "primary_uuid", primary.getUniqueId().toString(),
                "secondary_uuid", secondary == null ? null : secondary.getUniqueId().toString(), "data", data);
        logQueue.offer(EvidenceJson.encode(item));
    }

    private void writerLoop() {
        new CombatEvidenceWriter(
                getDataFolder().toPath().resolve("logs"),
                logQueue,
                () -> running,
                () -> getConfig().getInt("retention-days", 7),
                getLogger(),
                error -> lastIoError = error
        ).run();
    }

    private static double clamp(double n, double lo, double hi) {
        return Math.max(lo, Math.min(hi, n));
    }
    private static double round(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }
    private static String itemType(ItemStack i) {
        return i == null ? "NONE" : i.getType().name();
    }
    static Map<String, Object> map(Object... vals) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i=0; i<vals.length; i+=2) {
            result.put((String)vals[i], vals[i+1]);
        }
        return result;
    }

}
