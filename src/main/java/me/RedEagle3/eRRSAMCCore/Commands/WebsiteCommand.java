package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class WebsiteCommand implements CommandExecutor {

    private final ERRSAMCCore plugin;

    public WebsiteCommand(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        boolean isPlayerOnBedrock = player.getName().startsWith(".");

        String link = plugin.getConfig().getString("website-link", "https://campusgroups.erau.edu/errsa/craft/");

        if (!isPlayerOnBedrock) {

            TextComponent linkText = new TextComponent("Click here to visit!");
            linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to visit our website!").color(net.md_5.bungee.api.ChatColor.GOLD).create()));

            player.spigot().sendMessage(new ComponentBuilder("[Website] ").color(net.md_5.bungee.api.ChatColor.GOLD).append(linkText).create());

        } else {
            player.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "[Website] " + net.md_5.bungee.api.ChatColor.AQUA + "For Bedrock players, please type "
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA + net.md_5.bungee.api.ChatColor.UNDERLINE + link + net.md_5.bungee.api.ChatColor.AQUA + " into any internet browser.");
        }

        return true;
    }
}
