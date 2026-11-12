package me.RedEagle3.eRRSAMCCore.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.errsa.AutoUnlockPvPDrops;
import com.errsa.compat.PaperReporter;
import com.errsa.inventory.InventoryAuditModule;
import com.errsa.webbridge.WebBridgeModule;
import edu.errsa.wikisync.WikiSyncModule;
import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.modules.CoreModule;
import me.RedEagle3.eRRSAMCCore.modules.CustomPortalModule;
import me.RedEagle3.eRRSAMCCore.modules.EndEntityRerouteModule;
import org.bukkit.command.PluginCommand;
import org.errsa.combatdiag.CombatDiagnosticsModule;

/** Centralized module startup/shutdown; one configuration switch per feature. */
public final class CoreModuleManager {
    private final ERRSAMCCore plugin;
    private final List<CoreModule> activeModules = new ArrayList<>();

    public CoreModuleManager(ERRSAMCCore plugin) {
        this.plugin = plugin;
    }

    public void enableConfiguredModules() {
        enable("end-entity-reroute", new EndEntityRerouteModule(plugin), null);
        enable("custom-portals", new CustomPortalModule(plugin), "coreportal");
        enable("inventory-audit", new InventoryAuditModule(plugin),
                "inventoryaudit");
        enable("combat-diagnostics", new CombatDiagnosticsModule(plugin),
                "combatdiag");
        enable("auto-unlock-pvp-drops", new AutoUnlockPvPDrops(plugin), null);
        enable("compatibility-paper", new PaperReporter(plugin), null);
        enableWebBridge();
        enableWikiSync();
    }

    private void enableWebBridge() {
        if (isEnabled("web-bridge") && !conflictsWith("ERRSAWebBridge")) {
            enable("web-bridge", new WebBridgeModule(plugin), "errsawebbridge");
        }
    }

    private void enableWikiSync() {
        if (isEnabled("wiki-sync") && !conflictsWith("ERRSAWikiSync")) {
            enable("wiki-sync", new WikiSyncModule(plugin), null);
        }
    }

    private boolean conflictsWith(String standalonePlugin) {
        boolean conflict = plugin.getServer().getPluginManager()
                .getPlugin(standalonePlugin) != null;
        if (conflict) {
            plugin.getLogger().severe("Remove standalone " + standalonePlugin
                    + " before enabling its integrated Core module.");
        }
        return conflict;
    }

    private boolean isEnabled(String id) {
        return plugin.getConfig().getBoolean("modules." + id + ".enabled", false);
    }

    private void enable(String id, CoreModule module, String commandName) {
        if (!isEnabled(id)) {
            plugin.getLogger().info("Module disabled: " + id);
            return;
        }
        try {
            module.saveDefaultConfig();
            module.onEnable();
            activeModules.add(module);
            if (commandName != null) {
                PluginCommand command = plugin.getCommand(commandName);
                if (command != null) {
                    command.setExecutor(module);
                }
            }
            plugin.getLogger().info("Module enabled: " + id);
        } catch (Exception | LinkageError ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Module startup failed: " + id, ex);
        }
    }

    public int activeCount() {
        return activeModules.size();
    }

    public void disableModules() {
        for (int index = activeModules.size() - 1; index >= 0; index--) {
            try {
                activeModules.get(index).onDisable();
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "Module shutdown failed", ex);
            }
        }
        activeModules.clear();
    }
}
