package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.GUI.MenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {

    private final MenuGUI menuGUI;

    public MenuCommand(MenuGUI menuGUI) {
        this.menuGUI = menuGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        menuGUI.open(player);

        return true;
    }
}
