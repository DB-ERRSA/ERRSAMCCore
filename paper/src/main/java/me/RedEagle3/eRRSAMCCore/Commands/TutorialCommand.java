package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        // Make the player run /warp tutorial
        player.performCommand("warp tutorial");

        return true;
    }
}
