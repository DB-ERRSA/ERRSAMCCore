package me.RedEagle3.eRRSAMCCore;

import me.RedEagle3.eRRSAMCCore.Commands.*;
import me.RedEagle3.eRRSAMCCore.GUI.MenuGUI;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;


public class ERRSAMCCore extends JavaPlugin {

    private Economy economy;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        MenuGUI menuGUI = new MenuGUI(this);

        getCommand("sell").setExecutor(new SellCommand(this, economy));
        getCommand("discord").setExecutor(new DiscordCommand(this));
        getCommand("feedback").setExecutor(new FeedbackCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("tutorial").setExecutor(new TutorialCommand());
        getCommand("resetschedule").setExecutor(new ResetScheduleCommand(this));
        getCommand("website").setExecutor(new WebsiteCommand(this));
        getCommand("map").setExecutor(new MapCommand(this));
        getCommand("menu").setExecutor(new MenuCommand(menuGUI));

        getServer().getPluginManager().registerEvents(menuGUI, this);

        getLogger().info("ERRSA MC Core has been enabled!");
    }

    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {return false;}

        economy = rsp.getProvider();
        return economy != null;
    }
}