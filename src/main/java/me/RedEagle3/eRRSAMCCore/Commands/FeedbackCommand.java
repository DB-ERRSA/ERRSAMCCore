package me.RedEagle3.eRRSAMCCore.Commands;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedbackCommand implements CommandExecutor {

    private final ERRSAMCCore plugin;

    public FeedbackCommand(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        boolean isPlayerOnBedrock = player.getName().startsWith(".");

        String link = plugin.getConfig().getString("feedback-link", "https://campusgroups.erau.edu/errsa/craft/");

        if (!isPlayerOnBedrock) {
            TextComponent linkText = new TextComponent("Click here for our feedback form!");
            linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("We would love to hear your ideas!").color(net.md_5.bungee.api.ChatColor.GOLD).create()));

            player.spigot().sendMessage(new ComponentBuilder("[Feedback] ").color(net.md_5.bungee.api.ChatColor.GOLD).append(linkText).create());

        } else {
            player.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "[Feedback] " + net.md_5.bungee.api.ChatColor.AQUA + "For Bedrock players, please enter "
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA + "" + net.md_5.bungee.api.ChatColor.UNDERLINE + link + net.md_5.bungee.api.ChatColor.AQUA + " into your browser, " + "or check the Minecraft Welcome channel in our Discord for the direct link.");
        }
        return true;
    }
}
