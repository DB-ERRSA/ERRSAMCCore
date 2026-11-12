package me.RedEagle3.eRRSAMCCore.lifecycle;

import me.RedEagle3.eRRSAMCCore.Commands.*;
import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.GUI.MenuGUI;
import me.RedEagle3.eRRSAMCCore.Listeners.AdminCommandListener;
import net.milkbowl.vault.economy.Economy;

/** Registers only the commands and listeners owned by the Core itself. */
public final class CoreCommandRegistrar {
    private final ERRSAMCCore plugin;
    private final Economy economy;

    public CoreCommandRegistrar(ERRSAMCCore plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void register() {
        MenuGUI menu = new MenuGUI(plugin);
        plugin.getCommand("sell").setExecutor(new SellCommand(plugin, economy));
        plugin.getCommand("discord").setExecutor(new DiscordCommand(plugin));
        plugin.getCommand("feedback").setExecutor(new FeedbackCommand(plugin));
        plugin.getCommand("report").setExecutor(new ReportCommand(plugin));
        plugin.getCommand("tutorial").setExecutor(new TutorialCommand());
        plugin.getCommand("resetschedule").setExecutor(
                new ResetScheduleCommand(plugin));
        plugin.getCommand("website").setExecutor(new WebsiteCommand(plugin));
        plugin.getCommand("map").setExecutor(new MapCommand(plugin));
        plugin.getCommand("menu").setExecutor(new MenuCommand(menu));

        plugin.getServer().getPluginManager().registerEvents(menu, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new AdminCommandListener(plugin), plugin);
    }
}
