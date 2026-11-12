package me.RedEagle3.eRRSAMCCore.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** Shared lifecycle and dedicated configuration directory for every module. */
public abstract class CoreModule implements CommandExecutor {

    protected final ERRSAMCCore plugin;
    private final String folder;
    private FileConfiguration configuration;

    protected CoreModule(ERRSAMCCore plugin, String folder) {
        this.plugin = plugin;
        this.folder = folder;
    }

    public final void saveDefaultConfig() {
        File directory = getDataFolder();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create " + directory);
        }

        File target = new File(directory, "config.yml");
        if (!target.exists()) {
            String resourcePath = "modules/" + folder + "/config.yml";
            try (InputStream input = plugin.getResource(resourcePath)) {
                if (input != null) {
                    Files.copy(input, target.toPath());
                } else {
                    Files.writeString(target.toPath(), "# Module settings\n");
                }
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "Unable to write " + target, ex);
            }
        }
        reloadConfig();
    }

    public final FileConfiguration getConfig() {
        return configuration;
    }

    public final void reloadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public final File getDataFolder() {
        return new File(plugin.getDataFolder(), folder);
    }

    public final Logger getLogger() {
        return plugin.getLogger();
    }

    public final ERRSAMCCore getPlugin() {
        return plugin;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        return false;
    }
}
