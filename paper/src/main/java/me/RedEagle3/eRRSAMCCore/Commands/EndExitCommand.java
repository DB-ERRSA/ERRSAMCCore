package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.modules.EndEntityRerouteModule;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /endexit set (also /shulkerbay set for old command compatibility). */
public final class EndExitCommand implements CommandExecutor {
    private final EndEntityRerouteModule module;

    public EndExitCommand(EndEntityRerouteModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Stand at the destination in-game to set it.");
                return true;
            }
            if (!sender.hasPermission("ERRSAMCCore.endexit")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
                sender.sendMessage(ChatColor.RED + "The destination must be in an Overworld.");
                return true;
            }
            Location loc = player.getLocation();
            String path = "end-exit-reroute.safe-destination.";
            module.getConfig().set(path + "world", player.getWorld().getName());
            module.getConfig().set(path + "x", loc.getX());
            module.getConfig().set(path + "y", loc.getY());
            module.getConfig().set(path + "z", loc.getZ());
            module.getConfig().set(path + "yaw", loc.getYaw());
            module.getConfig().set(path + "pitch", loc.getPitch());
            module.saveConfig();
            sender.sendMessage(ChatColor.GREEN + "End-exit destination saved: "
                    + player.getWorld().getName() + " " + loc.getBlockX() + " "
                    + loc.getBlockY() + " " + loc.getBlockZ()
                    + ChatColor.GRAY + " (applies to non-player End exit portal transfers).");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " set");
        return true;
    }
}
