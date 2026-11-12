package me.RedEagle3.eRRSAMCCore.modules;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/** Optional WorldGuard integration; no hard dependency at compile time. */
final class PortalRegionResolver {

    private PortalRegionResolver() {
    }

    static Set<String> regionsAt(Location location) {
        Set<String> ids = new HashSet<>();
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            return ids;
        }
        try {
            Class<?> adapter = Class.forName(
                    "com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> weWorld = Class.forName("com.sk89q.worldedit.world.World");
            Object adapted = adapter.getMethod("adapt", org.bukkit.World.class)
                    .invoke(null, location.getWorld());
            Object wg = Class.forName("com.sk89q.worldguard.WorldGuard")
                    .getMethod("getInstance").invoke(null);
            Object platform = wg.getClass().getMethod("getPlatform")
                    .invoke(wg);
            Object container = platform.getClass().getMethod("getRegionContainer")
                    .invoke(platform);
            Object manager = container.getClass().getMethod("get", weWorld)
                    .invoke(container, adapted);
            if (manager == null) {
                return ids;
            }
            Class<?> vector = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object position = vector.getMethod("at", int.class, int.class,
                    int.class).invoke(null, location.getBlockX(),
                    location.getBlockY(), location.getBlockZ());
            Method applicableMethod = manager.getClass().getMethod(
                    "getApplicableRegions", vector);
            Object applicable = applicableMethod.invoke(manager, position);
            for (Object region : (Iterable<?>) applicable) {
                ids.add((String) region.getClass().getMethod("getId")
                        .invoke(region));
            }
        } catch (ReflectiveOperationException | ClassCastException ex) {
            // WorldGuard absent or incompatible: cuboid portals still work.
        }
        return ids;
    }
}
