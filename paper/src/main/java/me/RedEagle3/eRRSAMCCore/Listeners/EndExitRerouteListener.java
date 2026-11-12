package me.RedEagle3.eRRSAMCCore.Listeners;

import me.RedEagle3.eRRSAMCCore.modules.EndEntityRerouteModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;

/**
 * Moves non-player entities exiting the End through its return portal away from
 * the vanilla world-spawn point. Nether portals and portals into the End are untouched.
 * No WorldGuard hook is needed: the behavior depends on portal direction/type.
 */
public final class EndExitRerouteListener implements Listener {
    private final EndEntityRerouteModule module;
    private long lastWarningAt;

    public EndExitRerouteListener(EndEntityRerouteModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEndExit(EntityPortalEvent event) {
        if (!module.getConfig().getBoolean("end-exit-reroute.enabled", true)
                || event.getEntity() instanceof Player
                || event.getPortalType() != PortalType.ENDER
                || event.getFrom().getWorld() == null
                || event.getFrom().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        // Only End -> Overworld. Do not hijack routes into the End, or custom
        // cross-world portal destinations provided by another plugin.
        Location originalTarget = event.getTo();
        if (originalTarget == null || originalTarget.getWorld() == null
                || originalTarget.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        String path = "end-exit-reroute.safe-destination.";
        String worldName = module.getConfig().getString(path + "world", "world");
        World world = Bukkit.getWorld(worldName == null ? "world" : worldName);
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) {
            // Do not silently allow the entity to land on the spawn flags.
            event.setCancelled(true);
            warnLimited("Invalid End-exit destination world '" + worldName + "'. Portal transfer cancelled.");
            return;
        }

        Location configured = new Location(
                world,
                module.getConfig().getDouble(path + "x", 772),
                module.getConfig().getDouble(path + "y", 64.0),
                module.getConfig().getDouble(path + "z", 1533),
                (float) module.getConfig().getDouble(path + "yaw", 0),
                (float) module.getConfig().getDouble(path + "pitch", 0));
        int radius = Math.max(0, Math.min(16, module.getConfig().getInt("end-exit-reroute.search-radius", 8)));
        Location safe = findSafeLocation(event.getEntity(), configured, radius);
        if (safe == null) {
            event.setCancelled(true);
            warnLimited("No safe End-exit spot within " + radius + " blocks of "
                    + configured.getBlockX() + ", " + configured.getBlockY() + ", "
                    + configured.getBlockZ() + ". Portal transfer cancelled.");
            return;
        }

        // This modifies the *portal's own destination*; it does not trigger
        // another teleport, nor does it change the world spawn or player spawn.
        event.setTo(safe);
        if (module.getConfig().getBoolean("end-exit-reroute.debug", false)) {
            module.getLogger().info("End exit: " + event.getEntity().getType() + " -> "
                    + safe.getWorld().getName() + " " + safe.getBlockX() + " "
                    + safe.getBlockY() + " " + safe.getBlockZ());
        }
    }

    private Location findSafeLocation(Entity entity, Location origin, int radius) {
        Location centered = centered(origin);
        if (isSafe(entity, centered)) {
            return centered;
        }
        // Try nearer blocks first. Prefer the configured height, then the
        // few levels above/below it. Avoid searching large areas on the hot path.
        for (int distance = 0; distance <= radius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) {
                        continue;
                    }
                    for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8, 9, -9, 10,
                        -10}) {
                        Location candidate = centered.clone().add(dx, dy, dz);
                        if (isSafe(entity, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Location centered(Location location) {
        return new Location(location.getWorld(), location.getBlockX() + 0.5,
                location.getY(), location.getBlockZ() + 0.5,
                location.getYaw(), location.getPitch());
    }

    /** Ensure a solid, non-hazardous floor and free space for the entity's size. */
    private boolean isSafe(Entity entity, Location feet) {
        World world = feet.getWorld();
        if (world == null) {
            return false;
        }
        double height = Math.max(0.25, entity.getBoundingBox().getHeight());
        double halfX = Math.max(0.05, entity.getBoundingBox().getWidthX() / 2.0);
        double halfZ = Math.max(0.05, entity.getBoundingBox().getWidthZ() / 2.0);
        int minX = (int) Math.floor(feet.getX() - halfX + 0.0001);
        int maxX = (int) Math.floor(feet.getX() + halfX - 0.0001);
        int minZ = (int) Math.floor(feet.getZ() - halfZ + 0.0001);
        int maxZ = (int) Math.floor(feet.getZ() + halfZ - 0.0001);
        int minY = (int) Math.floor(feet.getY());
        int maxY = (int) Math.ceil(feet.getY() + height) - 1;
        if (minY <= world.getMinHeight() || maxY >= world.getMaxHeight()) {
            return false;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block floor = world.getBlockAt(x, minY - 1, z);
                if (!floor.getType().isSolid() || isHazard(floor.getType())) {
                    return false;
                }
                for (int y = minY; y <= maxY; y++) {
                    if (!world.getBlockAt(x, y, z).getType().isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isHazard(Material material) {
        return material == Material.MAGMA_BLOCK || material == Material.CACTUS
                || material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE;
    }

    private void warnLimited(String message) {
        long now = System.currentTimeMillis();
        if (now - lastWarningAt > 60_000) {
            module.getLogger().warning(message);
            lastWarningAt = now;
        }
    }
}
