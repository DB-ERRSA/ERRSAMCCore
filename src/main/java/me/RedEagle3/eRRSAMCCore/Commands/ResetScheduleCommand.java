package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetScheduleCommand implements CommandExecutor {

    private final ERRSAMCCore plugin;

    public ResetScheduleCommand(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        String date = plugin.getConfig().getString("reset-schedule.reset_date", "TBD");
        String cycle = plugin.getConfig().getString("reset-schedule.reset_cycle", "few");

        player.sendMessage(ChatColor.GOLD + "The next server reset is planned for " + ChatColor.AQUA + date + ChatColor.GOLD + ".");
        player.sendMessage(ChatColor.GRAY + "This typically takes place every " + ChatColor.DARK_AQUA + cycle + ChatColor.GRAY + " years to keep the server fresh and updated.");

        return true;
    }
}
