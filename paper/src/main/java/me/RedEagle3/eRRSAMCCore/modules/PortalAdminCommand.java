package me.RedEagle3.eRRSAMCCore.modules;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/** Staff commands to create and maintain configured portals. */
final class PortalAdminCommand implements CommandExecutor {

    private final CustomPortalModule module;
    private final PortalPlayerListener runtime;

    PortalAdminCommand(CustomPortalModule module, PortalPlayerListener runtime) {
        this.module = module;
        this.runtime = runtime;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!sender.hasPermission("errsa.core.portals.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.YELLOW
                    + "/coreportal pos1|pos2|destination <id>");
            sender.sendMessage(ChatColor.YELLOW
                    + "/coreportal create|remove|enable|disable <id>");
            sender.sendMessage(ChatColor.YELLOW
                    + "/coreportal reload|list");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            module.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Custom portals reloaded.");
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            ConfigurationSection portals = module.getConfig()
                    .getConfigurationSection("portals");
            sender.sendMessage(ChatColor.YELLOW + "Portals: "
                    + (portals == null ? "none" : portals.getKeys(false)));
            return true;
        }
        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Use /coreportal help in game.");
            return true;
        }
        String id = args[1].toLowerCase();
        if (!id.matches("[a-z0-9_-]{1,48}")) {
            sender.sendMessage(ChatColor.RED + "Use a-z, 0-9, _ or - for IDs.");
            return true;
        }
        String action = args[0].toLowerCase();
        UUID uuid = player.getUniqueId();
        switch (action) {
            case "pos1" -> runtime.firstCorners.put(uuid, player.getLocation());
            case "pos2" -> runtime.secondCorners.put(uuid, player.getLocation());
            case "create" -> {
                Location first = runtime.firstCorners.get(uuid);
                Location second = runtime.secondCorners.get(uuid);
                if (first == null || second == null
                        || !first.getWorld().equals(second.getWorld())) {
                    sender.sendMessage(ChatColor.RED
                            + "Select both corners in the same world first.");
                    return true;
                }
                String path = "portals." + id;
                module.getConfig().set(path + ".enabled", false);
                module.getConfig().set(path + ".trigger", "cuboid");
                module.getConfig().set(path + ".world", first.getWorld().getName());
                runtime.writeLocation(path + ".corner-1", first);
                runtime.writeLocation(path + ".corner-2", second);
                sender.sendMessage(ChatColor.YELLOW
                        + "Portal created disabled. Set destination, then enable.");
            }
            case "destination" -> {
                if (!module.getConfig().isConfigurationSection("portals." + id)) {
                    sender.sendMessage(ChatColor.RED + "Create the portal first.");
                    return true;
                }
                runtime.writeLocation("portals." + id + ".destination",
                        player.getLocation());
            }
            case "remove" -> module.getConfig().set("portals." + id, null);
            case "enable", "disable" -> {
                String path = "portals." + id;
                if (!module.getConfig().isConfigurationSection(path)) {
                    sender.sendMessage(ChatColor.RED + "Unknown portal.");
                    return true;
                }
                if (action.equals("enable") && runtime.readLocation(module.getConfig()
                        .getConfigurationSection(path + ".destination")) == null) {
                    sender.sendMessage(ChatColor.RED + "Set a valid destination first.");
                    return true;
                }
                module.getConfig().set(path + ".enabled", action.equals("enable"));
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Use /coreportal help.");
                return true;
            }
        }
        runtime.save();
        sender.sendMessage(ChatColor.GREEN + "Portal " + id + ": " + action);
        return true;
    }
}
