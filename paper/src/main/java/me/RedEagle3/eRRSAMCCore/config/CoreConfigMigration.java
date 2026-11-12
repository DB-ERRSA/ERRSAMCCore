package me.RedEagle3.eRRSAMCCore.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** Safely transfers pre-modular Core settings to the grouped configuration. */
public final class CoreConfigMigration {
    private static final Map<String, String> LEGACY_KEYS = legacyKeys();

    private CoreConfigMigration() {
    }

    public static void migrate(ERRSAMCCore plugin) {
        FileConfiguration config = plugin.getConfig();
        File source = new File(plugin.getDataFolder(), "config.yml");
        if (!source.isFile()) {
            return;
        }
        FileConfiguration onDisk = YamlConfiguration.loadConfiguration(source);
        boolean migrationNeeded = LEGACY_KEYS.keySet().stream()
                .anyMatch(key -> onDisk.contains(key, true));
        if (!migrationNeeded) {
            return;
        }

        File backup = new File(plugin.getDataFolder(),
                "config.pre-modular-backup.yml");
        try {
            if (source.isFile() && !backup.exists()) {
                Files.copy(source.toPath(), backup.toPath(),
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Cannot back up old config; "
                    + "configuration migration stopped: " + ex.getMessage());
            return;
        }

        // On first migration legacy values win over bundled defaults.
        LEGACY_KEYS.forEach((oldKey, newKey) -> {
            if (onDisk.contains(oldKey, true)) {
                if (!onDisk.contains(newKey, true)) {
                    config.set(newKey, onDisk.get(oldKey));
                }
                config.set(oldKey, null);
            }
        });
        plugin.saveConfig();
        plugin.getLogger().info("Migrated legacy Core settings. Backup: "
                + backup.getName());
    }

    private static Map<String, String> legacyKeys() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("discord-link", "links.discord");
        paths.put("feedback-link", "links.feedback");
        paths.put("report-link", "links.report");
        paths.put("website-link", "links.website");
        paths.put("map-link", "links.map");
        paths.put("sell-price", "economy.diamond-sell-price");
        paths.put("sell.diamond-price", "economy.diamond-sell-price");
        paths.put("reset-schedule.reset_date", "reset.date");
        paths.put("reset-schedule.reset_cycle", "reset.cycle");
        paths.put("discord-webhook", "moderation.discord-webhook");
        return paths;
    }
}
