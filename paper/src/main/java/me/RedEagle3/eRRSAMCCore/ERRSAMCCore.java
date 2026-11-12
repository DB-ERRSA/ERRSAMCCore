package me.RedEagle3.eRRSAMCCore;

import me.RedEagle3.eRRSAMCCore.config.CoreConfigMigration;
import me.RedEagle3.eRRSAMCCore.lifecycle.CoreCommandRegistrar;
import me.RedEagle3.eRRSAMCCore.lifecycle.CoreModuleManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** The only Bukkit JavaPlugin entry point for ERRSA-MC-Core. */
public final class ERRSAMCCore extends JavaPlugin {

    private CoreModuleManager moduleManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        CoreConfigMigration.migrate(this);

        Economy economy = resolveEconomy();
        if (economy == null) {
            getLogger().severe("Vault economy unavailable; disabling Core.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new CoreCommandRegistrar(this, economy).register();
        moduleManager = new CoreModuleManager(this);
        moduleManager.enableConfiguredModules();
        getLogger().info("ERRSA-MC-Core enabled. Active modules: "
                + moduleManager.activeCount());
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableModules();
            moduleManager = null;
        }
    }

    private Economy resolveEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> provider = getServer()
                .getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }
}
