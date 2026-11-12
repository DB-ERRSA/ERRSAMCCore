package com.errsa.inventory;

import org.bukkit.command.CommandSender;

/** Staff-facing inventory audit status and configuration reload. */
final class InventoryAuditCommand {
    private final InventoryAuditModule module;

    InventoryAuditCommand(InventoryAuditModule module) {
        this.module = module;
    }

    boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            sender.sendMessage("§6InventoryAudit§7: states=§f" + module.stateCount()
                    + " §7queue=§f" + module.queueSize()
                    + " §7dropped=§f" + module.droppedCount()
                    + " §7incidents[L/M/H/C]=§f" + module.incidentCount(Severity.LOW) + "/" +
                        module.incidentCount(Severity.MEDIUM)
                    + "/" + module.incidentCount(Severity.HIGH) + "/" + module.incidentCount(Severity.CRITICAL));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            module.reloadSettings();
            sender.sendMessage("§aInventoryAudit configuration reloaded.");
            return true;
        }
        return false;
    }
}
