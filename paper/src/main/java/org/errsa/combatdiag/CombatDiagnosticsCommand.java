package org.errsa.combatdiag;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Staff controls for watch sessions, evidence review and diagnostics. */
final class CombatDiagnosticsCommand {
    private final CombatDiagnosticsModule module;

    CombatDiagnosticsCommand(CombatDiagnosticsModule module) {
        this.module = module;
    }

    boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("errsa.combatdiag.staff")) { sender.sendMessage("No permission."); return true; }
        if (args.length == 0) {
            sender.sendMessage("/cdiag watch <player> [minutes] | stop <player> | mark <player> " +
                "[reason] | incidents | show <id> | packet | health | reload | status"); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "watch" -> {
                if (args.length < 2) {
                    sender.sendMessage("/cdiag watch <online player> [minutes]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player must be online.");
                    return true;
                }
                int max = Math.max(1, module.getConfig().getInt("max-watch-minutes", 30));
                int minutes = 10;
                if (args.length > 2) { try { minutes = Integer.parseInt(args[2]); } catch (NumberFormatException
                    ex) { sender.sendMessage("Invalid minutes."); return true; } }
                if (minutes < 1 || minutes > max) {
                    sender.sendMessage("Minutes must be 1-" + max);
                    return true;
                }
                module.watched.put(target.getUniqueId(), System.currentTimeMillis() + minutes * 60_000L);
                module.record("watch_start", target, null, module.map("operator", sender.getName(),
                    "duration_minutes", minutes));
                sender.sendMessage("Now recording " + target.getName() + " for " + minutes +
                    " minutes. Files: plugins/ERRSA-MC-Core/combat-diagnostics/logs/");
            }
            case "stop" -> {
                if (args.length < 2) {
                    sender.sendMessage("/cdiag stop <online player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player must be online.");
                    return true;
                }
                if (module.watched.remove(target.getUniqueId()) != null) {
                    module.record("watch_stop", target, null, module.map("operator", sender.getName()));
                    sender.sendMessage("Stopped: " + target.getName());
                } else sender.sendMessage("Not being watched.");
            }
            case "mark" -> {
                if (args.length < 2) {
                    sender.sendMessage("/cdiag mark <online player> [reason]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player must be online.");
                    return true;
                }
                String reason = args.length > 2 ? String.join("_", Arrays.copyOfRange(args, 2, args.length)) :
                    "staff_mark";
                if (reason.length() > 80) {
                    reason = reason.substring(0, 80);
                }
                module.trigger(target, "staff_" + reason, module.map("operator", sender.getName()));
                sender.sendMessage("Incident marker requested for " + target.getName() + ".");
            }
            case "incidents" -> {
                sender.sendMessage("Recent incident IDs (UTC, latest first):");
                int shown = 0;
                for (Iterator<Map<String, Object>> it = module.recentIncidentIndex.descendingIterator(); it.hasNext()
                    && shown < 10; shown++) {
                    Map<String, Object> entry = it.next();
                    sender.sendMessage(" - " + entry.get("id") + " " + entry.get("player") + " " + entry.get("reason"));
                }
                sender.sendMessage("Evidence: plugins/ERRSA-MC-Core/combat-diagnostics/logs/ (not a " +
                    "determination of cheating)");
            }
            case "show" -> {
                if (args.length < 2) {
                    sender.sendMessage("/cdiag show <incident-id>");
                    return true;
                }
                Map<String, Object> found = null;
                for (Map<String, Object> entry : module.recentIncidentIndex) if (args[1].equals(entry.get("id")))
                    { found = entry; break; }
                if (found == null) { sender.sendMessage("Incident not in recent-memory index; check JSONL records.");
                    return true; }
                sender.sendMessage("[CombatDiag] " + found.get("id") + " | " + found.get("player") + " | " +
                    found.get("reason"));
                sender.sendMessage("UTC: " + found.get("utc") + " | Evidence is review-only, not proof of cheating.");
                sender.sendMessage(String.valueOf(found.get("summary")));
                sender.sendMessage(String.valueOf(found.get("movement")));
            }
            case "packet" -> {
                sender.sendMessage("[CombatDiag] Packet observation: " + (module.packetBridge == null ? "disabled"
                    : module.packetBridge.status()));
                sender.sendMessage("Attack packets: " + module.packetsObserved +
                    " | melee with recent matching packet: " + module.damageWithPacketContext
                    + " | without matching packet: " + module.directMeleeWithoutPacketContext);
                sender.sendMessage("This is NOT rollback or lag-compensated hitbox verification.");
            }
            case "health" -> {
                sender.sendMessage("[CombatDiag] Queue " + module.logQueue.size() + " / " +
                    module.getConfig().getInt("max-queued-records", 10000)
                     + " | dropped " + module.dropped.get() + " | webhook errors " + module.webhookFailures.get());
                sender.sendMessage("Automatic=" + module.getConfig().getBoolean("automatic.enabled", true)
                     + ", console integration=" + module.getConfig().getBoolean("automatic.capture-console-alerts",
                         true)
                     + ", webhook=" + module.getConfig().getBoolean("discord.enabled", false) + ", IO=" +
                         module.lastIoError);
                sender.sendMessage("Logger alerts recognized=" + module.loggerAlertsRecognized + ", player matched="
                    + module.loggerAlertsMatched);
                sender.sendMessage("Packet bridge=" + (module.packetBridge == null ? "disabled" :
                    module.packetBridge.status())
                    + ", packets=" + module.packetsObserved + ", correlated_melee=" + module.damageWithPacketContext);
                sender.sendMessage("Do not use incidents as automatic grounds for punishment.");
            }
            case "reload" -> {
                module.reloadConfig();
                module.uninstallAlertListener(); module.installAlertListener();
                if (module.packetBridge != null) {
                    module.packetBridge.stop();
                }
                module.packetBridge = null;
                if (module.getConfig().getBoolean("packet-observation.enabled", true)) {
                    module.packetBridge = new PacketBridge(module); module.packetBridge.start();
                }
                sender.sendMessage("Configuration reloaded; restart required to change sample-every-ticks.");
            }
            case "status" -> {
                sender.sendMessage("Active watches: " + module.watched.size() + ", active incidents: " +
                    module.incidents.size() + ", queue: " + module.logQueue.size() + ", dropped records: " +
                    module.dropped.get());
                for (UUID uuid : module.watched.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    sender.sendMessage(" - " + (p == null ? uuid.toString() : p.getName()) + " (" + Math.max(0,
                        (module.watched.get(uuid)-System.currentTimeMillis())/1000) + "s left)");
                }
            }
            default -> sender.sendMessage("/cdiag watch <player> [minutes] | stop <player> | mark <player> " +
                "[reason] | incidents | show <id> | packet | health | reload | status");
        }
        return true;
    }
}
