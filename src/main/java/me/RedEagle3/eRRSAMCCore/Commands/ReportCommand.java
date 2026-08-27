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

public class ReportCommand implements CommandExecutor {

    private final ERRSAMCCore plugin;

    public ReportCommand(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        boolean isPlayerOnBedrock = player.getName().startsWith(".");

        String link = plugin.getConfig().getString("report-link", "https://campusgroups.erau.edu/errsa/craft/");

        if (!isPlayerOnBedrock) {
            TextComponent linkText = new TextComponent("Click here for the report form.");
            linkText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            linkText.setUnderlined(true);
            linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            linkText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to be taken to the report form.").color(net.md_5.bungee.api.ChatColor.DARK_RED).create()));

            player.spigot().sendMessage(new ComponentBuilder("[Report] ").color(net.md_5.bungee.api.ChatColor.DARK_RED).append(linkText).create());

        } else {
            player.sendMessage(net.md_5.bungee.api.ChatColor.DARK_RED + "[Report] " + net.md_5.bungee.api.ChatColor.AQUA + "For Bedrock players, please enter "
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA + "" + net.md_5.bungee.api.ChatColor.UNDERLINE + link + net.md_5.bungee.api.ChatColor.AQUA + " into your browser, " + "or check the Minecraft Welcome channel in our Discord for the direct link.");
        }
        return true;
    }
}
