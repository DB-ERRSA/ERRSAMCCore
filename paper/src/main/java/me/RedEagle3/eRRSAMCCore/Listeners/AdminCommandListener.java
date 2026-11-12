package me.RedEagle3.eRRSAMCCore.Listeners;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.ess3.api.events.JailStatusChangeEvent;
import net.ess3.api.events.MuteStatusChangeEvent;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Records successful moderation actions without assuming that an issuer is a player.
 * In EssentialsX 2.21.0, console-issued mute/jail events have a null controller.
 * Its UserKickEvent also has reversed issuer/target arguments in Commandkick,
 * so we observe the resulting Bukkit PlayerKickEvent instead.
 *
 * This listener does not execute, cancel or change moderation commands.
 */
public class AdminCommandListener implements Listener {

    private final JavaPlugin plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, PendingIssuer> pendingIssuers = new HashMap<>();

    private record PendingIssuer(String name) {}

    public AdminCommandListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        observeCommand(event.getMessage(), event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        String actor = (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) ?
            "CONSOLE" : safeName(sender.getName(), "CONSOLE");
        observeCommand(event.getCommand(), actor);
    }

    private void observeCommand(String rawCommand, String actor) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return;
        }

        String commandLine = rawCommand.trim();
        if (commandLine.startsWith("/")) {
            commandLine = commandLine.substring(1);
        }
        String[] args = commandLine.split("\\s+");
        if (args.length < 2) {
            return;
        }

        String command = args[0].toLowerCase(Locale.ROOT);
        if (command.contains(":")) {
            command = command.substring(command.indexOf(':') + 1);
        }

        // EssentialsX's event controller is null for console commands and for
        // automatic expirations. Correlating an observed command lets us tell
        // CONSOLE apart from an automatic SYSTEM expiration.
        if (command.equals("mute") || command.equals("emute") || command.equals("unmute") ||
            command.equals("eunmute")) {
            rememberIssuer("mute", args[1], actor);
        } else if (command.equals("jail") || command.equals("ejail")
                || command.equals("togglejail") || command.equals("etogglejail")
                || command.equals("unjail") || command.equals("eunjail")) {
            rememberIssuer("jail", args[1], actor);
        } else if (command.equals("kick") || command.equals("ekick")) {
            rememberIssuer("kick", args[1], actor);
        }

        boolean isBan = command.equals("ban") || command.equals("tempban");
        boolean isUnban = command.equals("unban") || command.equals("pardon");
        if (!isBan && !isUnban) {
            return;
        }

        String target = args[1];
        BanEntry<?> previousEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(target);
        boolean wasBanned = previousEntry != null;

        // Inspect the resulting ban list rather than logging an attempted or failed command.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BanEntry<?> currentEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(target);

            if (isBan && currentEntry != null) {
                String action = currentEntry.getExpiration() == null ? "banned" : "temp-banned";
                String length = currentEntry.getExpiration() == null ? "Permanent" :
                    formatDurationUntil(currentEntry.getExpiration().getTime());
                sendLog(actor, action, target, length, currentEntry.getReason());
            } else if (isUnban && wasBanned && currentEntry == null) {
                sendLog(actor, "unbanned", target, "N/A", null);
            }
        }, 1L);
    }

    private void rememberIssuer(String action, String typedTarget, String actor) {
        String target = typedTarget;
        if ("kick".equals(action)) {
            // EssentialsX may resolve a partial username; use the resolved name if available.
            Player resolved = Bukkit.getPlayerExact(typedTarget);
            if (resolved == null) {
                resolved = Bukkit.getPlayer(typedTarget);
            }
            if (resolved != null) {
                target = resolved.getName();
            }
        }
        String key = actionKey(action, target);
        PendingIssuer entry = new PendingIssuer(safeName(actor, "CONSOLE"));
        pendingIssuers.put(key, entry);

        // Never keep stale attribution for a later automatic expiration or kick.
        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingIssuers.remove(key, entry), 2L);
    }

    private String actionKey(String action, String target) {
        return action + ":" + target.toLowerCase(Locale.ROOT);
    }

    private String findIssuer(String action, String target) {
        PendingIssuer entry = pendingIssuers.get(actionKey(action, target));
        return entry == null ? "SYSTEM" : entry.name();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMuteStatusChange(MuteStatusChangeEvent event) {
        String target = event.getAffected() == null ? "UNKNOWN" : safeName(event.getAffected().getName(), "UNKNOWN");
        String actor = event.getController() == null
                ? findIssuer("mute", target)
                : safeName(event.getController().getName(), "SYSTEM");
        String action = event.getValue() ? "muted" : "unmuted";
        String length = event.getValue()
                ? event.getTimestamp().map(this::formatDurationUntil).orElse("Permanent / Unknown")
                : "N/A";
        sendLog(actor, action, target, length, event.getReason());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJailStatusChange(JailStatusChangeEvent event) {
        String target = event.getAffected() == null ? "UNKNOWN" : safeName(event.getAffected().getName(), "UNKNOWN");
        String actor = event.getController() == null
                ? findIssuer("jail", target)
                : safeName(event.getController().getName(), "SYSTEM");
        String action = event.getValue() ? "jailed" : "released from jail";
        sendLog(actor, action, target, "N/A", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        // Only log kicks associated with an observed kick command, not timeout,
        // proxy disconnect, anti-cheat or other unrelated kick events.
        String target = event.getPlayer().getName();
        PendingIssuer entry = pendingIssuers.remove(actionKey("kick", target));
        if (entry == null) {
            return;
        }
        sendLog(entry.name(), "kicked", target, "N/A", event.getReason());
    }

    private String safeName(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name;
    }

    private void sendLog(String staff, String action, String target, String length, String reason) {
        String configuredUrl = plugin.getConfig().getString("moderation.discord-webhook", "");
        String webhookUrl = configuredUrl == null ? "" : configuredUrl.trim();
        if (webhookUrl.isEmpty()) {
            return;
        }

        String description = staff + " " + action + " " + target;
        String payload = """
                {
                  "embeds": [{
                    "title": "⚠️ Administrative Action Issued",
                    "description": "%s",
                    "color": 15105570,
                    "fields": [
                      {"name": "Length", "value": "%s", "inline": true},
                      {"name": "Reason", "value": "%s", "inline": false}
                    ]
                  }]
                }
                """.formatted(jsonEscape(description), jsonEscape(length), jsonEscape(reason));

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid Discord webhook URL in config.yml.");
            return;
        }

        CompletableFuture<HttpResponse<String>> requestFuture = httpClient.sendAsync(request,
            HttpResponse.BodyHandlers.ofString());

        requestFuture.thenAccept(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                plugin.getLogger().warning("Discord moderation webhook returned HTTP " + response.statusCode());
            }
        }).exceptionally(error -> {
            plugin.getLogger().warning("Failed to send moderation log to Discord: " + error.getMessage());
            return null;
        });
    }

    private String formatDurationUntil(long timestamp) {
        long remaining = timestamp - System.currentTimeMillis();
        if (remaining <= 0) {
            return "Expired";
        }

        Duration duration = Duration.ofMillis(remaining);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("d ");
        }
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m");
        }
        if (result.isEmpty()) {
            result.append("<1m");
        }

        return result.toString().trim();
    }

    private String jsonEscape(String value) {
        if (value == null || value.isBlank()) {
            return "Not provided";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r",
            "\\r").replace("\t", "\\t");
    }
}
