package me.RedEagle3.eRRSAMCCore.modules;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/** Tracks player region transitions, teleport countdowns and cooldowns. */
final class PortalPlayerListener implements Listener {

    private final CustomPortalModule module;
    private final ERRSAMCCore plugin;
    private final Map<UUID, String> lastPortal = new HashMap<>();
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Long> lockedUntil = new HashMap<>();
    final Map<UUID, Location> firstCorners = new HashMap<>();
    final Map<UUID, Location> secondCorners = new HashMap<>();
    private BukkitTask scanTask;
    private long currentTick;

    PortalPlayerListener(CustomPortalModule module) {
        this.module = module;
        this.plugin = module.getPlugin();
    }

    void start() {
        int interval = Math.max(1, module.getConfig().getInt(
                "settings.check-interval-ticks", 5));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            currentTick += interval;
            for (Player player : Bukkit.getOnlinePlayers()) {
                scan(player, player.getLocation());
            }
        }, interval, interval);
    }

    void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        org.bukkit.event.HandlerList.unregisterAll(this);
        pending.values().forEach(BukkitTask::cancel);
        pending.clear();
        lastPortal.clear();
        lockedUntil.clear();
        firstCorners.clear();
        secondCorners.clear();
    }

    private void scan(Player player, Location position) {
        UUID id = player.getUniqueId();
        if (lockedUntil.getOrDefault(id, 0L) > currentTick) {
            return;
        }
        String active = matchPortal(position);
        String previous = lastPortal.put(id, active == null ? "" : active);
        if (active == null) {
            if (previous != null && !previous.isEmpty()) {
                cancelPending(player, true);
            }
            return;
        }
        if (active.equals(previous)) {
            return;
        }
        cancelPending(player, false);
        ConfigurationSection portal = module.getConfig().getConfigurationSection(
                "portals." + active);
        if (portal == null) {
            return;
        }
        String message = portal.getString("enter-message", "");
        if (!message.isBlank()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&', message));
        }
        playSound(player, "settings.entry-sound");
        int delay = Math.max(0, portal.getInt("delay-ticks", module.getConfig()
                .getInt("settings.default-delay-ticks", 20)));
        if (delay == 0) {
            finishTeleport(player, active, true);
        } else {
            pending.put(id, Bukkit.getScheduler().runTaskLater(plugin,
                    () -> finishTeleport(player, active, false), delay));
        }
    }

    private void finishTeleport(Player player, String portalId, boolean instant) {
        UUID id = player.getUniqueId();
        pending.remove(id);
        if (!player.isOnline() || (!instant && !portalId.equals(
                matchPortal(player.getLocation())))) {
            return;
        }
        ConfigurationSection portal = module.getConfig().getConfigurationSection(
                "portals." + portalId);
        if (portal == null) {
            return;
        }
        Location destination = readLocation(portal.getConfigurationSection(
                "destination"));
        if (destination == null) {
            module.getLogger().warning("Invalid destination for portal: " + portalId);
            return;
        }
        lockedUntil.put(id, currentTick + Math.max(1, module.getConfig().getInt(
                "settings.cooldown-ticks", 40)));
        if (!player.teleport(destination)) {
            module.getLogger().warning("Teleport failed for portal: " + portalId);
            lockedUntil.remove(id);
            return;
        }
        // Treat the arrival region as already entered to prevent ping-pong.
        String arrival = matchPortal(player.getLocation());
        lastPortal.put(id, arrival == null ? "" : arrival);
        double forward = portal.getDouble("forward-velocity", 0);
        if (forward != 0) {
            Vector direction = destination.getDirection().setY(0).normalize();
            player.setVelocity(direction.multiply(forward));
        }
        long effectDelay = Math.max(1, module.getConfig().getLong(
                "settings.arrival-effects-delay-ticks", 2));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !player.getWorld().equals(destination.getWorld())) {
                return;
            }
            playSound(player, "settings.teleport-sound");
            playArrivalParticles(player);
        }, effectDelay);
    }


    private void playArrivalParticles(Player player) {
        if (!module.getConfig().getBoolean("settings.particles-enabled", true)) {
            return;
        }
        String name = module.getConfig().getString("settings.particle", "HAPPY_VILLAGER");
        Particle particle;
        try {
            particle = Particle.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            // Keep compatibility with Bukkit versions that use the older alias.
            try {
                particle = Particle.valueOf(name.equalsIgnoreCase("HAPPY_VILLAGER")
                        ? "VILLAGER_HAPPY" : "HAPPY_VILLAGER");
            } catch (IllegalArgumentException ignored) {
                module.getLogger().warning("Unknown custom portal particle: " + name);
                return;
            }
        }
        int count = Math.max(0, module.getConfig().getInt("settings.particle-count", 30));
        if (count > 0) {
            // Player-local particles display reliably after the teleport packet arrives.
            player.spawnParticle(particle, player.getLocation().add(0, 1.0, 0),
                    count, 1.5, 1.0, 1.5, 0.0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getWorld().equals(to.getWorld())
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        // Detect narrow WorldGuard portals without waiting for the periodic scan.
        scan(event.getPlayer(), to);
    }

    private void cancelPending(Player player, boolean message) {
        BukkitTask task = pending.remove(player.getUniqueId());
        if (task == null) {
            return;
        }
        task.cancel();
        if (message && module.getConfig().getBoolean("settings.cancel-on-exit", true)) {
            player.sendMessage(ChatColor.RED + "Teleportation Cancelled.");
            playSound(player, "settings.cancel-sound");
        }
    }

    private String matchPortal(Location location) {
        ConfigurationSection portals = module.getConfig().getConfigurationSection("portals");
        if (portals == null || location.getWorld() == null) {
            return null;
        }
        Set<String> regions = null;
        String bestMatch = null;
        int bestPriority = Integer.MIN_VALUE;
        for (String id : portals.getKeys(false)) {
            ConfigurationSection portal = portals.getConfigurationSection(id);
            if (portal == null || !portal.getBoolean("enabled", true)
                    || !location.getWorld().getName().equals(portal.getString("world", "world"))) {
                continue;
            }
            boolean matched;
            int specificity;
            if ("worldguard".equalsIgnoreCase(portal.getString("trigger"))) {
                if (regions == null) {
                    regions = PortalRegionResolver.regionsAt(location);
                }
                String regionId = portal.getString("region", id);
                matched = regions.contains(regionId);
                specificity = regionId.length();
            } else {
                matched = insideCuboid(location, portal);
                specificity = 0;
            }
            int priority = portal.getInt("priority", 0) * 1000 + specificity;
            if (matched && priority > bestPriority) {
                bestPriority = priority;
                bestMatch = id;
            }
        }
        return bestMatch;
    }

    private boolean insideCuboid(Location location, ConfigurationSection portal) {
        ConfigurationSection a = portal.getConfigurationSection("corner-1");
        ConfigurationSection b = portal.getConfigurationSection("corner-2");
        if (a == null || b == null) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= Math.min(a.getDouble("x"), b.getDouble("x"))
                && x <= Math.max(a.getDouble("x"), b.getDouble("x")) + 1
                && y >= Math.min(a.getDouble("y"), b.getDouble("y"))
                && y <= Math.max(a.getDouble("y"), b.getDouble("y")) + 1
                && z >= Math.min(a.getDouble("z"), b.getDouble("z"))
                && z <= Math.max(a.getDouble("z"), b.getDouble("z")) + 1;
    }

    Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world", "world"));
        if (world == null) {
            return null;
        }
        return new Location(world, section.getDouble("x"),
                section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    void writeLocation(String path, Location location) {
        module.getConfig().set(path + ".world", location.getWorld().getName());
        module.getConfig().set(path + ".x", location.getX());
        module.getConfig().set(path + ".y", location.getY());
        module.getConfig().set(path + ".z", location.getZ());
        module.getConfig().set(path + ".yaw", location.getYaw());
        module.getConfig().set(path + ".pitch", location.getPitch());
    }

    void save() {
        try {
            File file = new File(module.getDataFolder(), "config.yml");
            module.getConfig().save(file);
        } catch (IOException ex) {
            module.getLogger().warning("Unable to save custom portals: " + ex);
        }
    }

    private void playSound(Player player, String path) {
        String name = module.getConfig().getString(
                path, "ENTITY_ENDERMAN_TELEPORT");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name), 0.75f, 1f);
        } catch (IllegalArgumentException ex) {
            module.getLogger().warning("Unknown portal sound: " + name);
        }
    }

    @EventHandler
    public void onWarp(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().toLowerCase().matches("/warp(?:\\s+.*)?")) {
            UUID id = event.getPlayer().getUniqueId();
            cancelPending(event.getPlayer(), false);
            lastPortal.remove(id);
            lockedUntil.put(id, currentTick + module.getConfig().getInt(
                    "settings.warp-bypass-ticks", 40));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        cancelPending(event.getPlayer(), false);
        lastPortal.remove(id);
        lockedUntil.remove(id);
    }

}
