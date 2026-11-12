package me.RedEagle3.eRRSAMCCore.modules;

import java.io.File;
import java.io.IOException;

import me.RedEagle3.eRRSAMCCore.Commands.EndExitCommand;
import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.Listeners.EndExitRerouteListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;

/** Redirects non-player End return-portal transfers only. */
public final class EndEntityRerouteModule extends CoreModule {

    public EndEntityRerouteModule(ERRSAMCCore plugin) {
        super(plugin, "end-entity-reroute");
    }

    @Override
    public void onEnable() {
        migrateLegacySettings();
        if (getConfig().getBoolean("end-exit-reroute.enabled", true)) {
            plugin.getServer().getPluginManager().registerEvents(
                    new EndExitRerouteListener(this), plugin);
        }
        PluginCommand command = plugin.getCommand("endexit");
        if (command != null) {
            command.setExecutor(new EndExitCommand(this));
        }
    }

    private void migrateLegacySettings() {
        String legacy = "end-exit-reroute";
        if (!plugin.getConfig().isConfigurationSection(legacy)) {
            return;
        }
        if (new File(getDataFolder(), ".legacy-migrated").exists()) {
            return;
        }
        // Preserve user-edited destination settings from the original Core config.
        for (String key : plugin.getConfig()
                .getConfigurationSection(legacy).getKeys(true)) {
            String path = legacy + "." + key;
            if (plugin.getConfig().isConfigurationSection(path)) {
                continue;
            }
            getConfig().set(path, plugin.getConfig().get(path));
        }
        saveConfig();
        try {
            new File(getDataFolder(), ".legacy-migrated").createNewFile();
        } catch (IOException ex) {
            getLogger().warning("Unable to mark legacy reroute migration: "
                    + ex.getMessage());
        }
    }

    public void saveConfig() {
        try {
            getConfig().save(new File(getDataFolder(), "config.yml"));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot save reroute config", ex);
        }
    }
}
