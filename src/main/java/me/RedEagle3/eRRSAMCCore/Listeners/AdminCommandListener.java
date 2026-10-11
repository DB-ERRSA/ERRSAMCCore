package me.RedEagle3.eRRSAMCCore.Listeners;

import net.ess3.api.events.JailStatusChangeEvent;
import net.ess3.api.events.MuteStatusChangeEvent;
import net.essentialsx.api.v2.events.UserKickEvent;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AdminCommandListener implements Listener {

    private final JavaPlugin plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AdminCommandListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBanCommand(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().substring(1).trim().split("\\s+");
        if (args.length < 2) return;

        String command = args[0].toLowerCase(Locale.ROOT);
        if (command.contains(":")) command = command.substring(command.indexOf(':') + 1);

        boolean isBan = command.equals("ban") || command.equals("tempban");
        boolean isUnban = command.equals("unban") || command.equals("pardon");
        if (!isBan && !isUnban) return;

        String target = args[1];
        BanEntry previousEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(target);
        boolean wasBanned = previousEntry != null;
        String staff = event.getPlayer().getName();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BanEntry currentEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(target);

            if (isBan && currentEntry != null) {
                String action = currentEntry.getExpiration() == null ? "banned" : "temp-banned";
                String length = currentEntry.getExpiration() == null ? "Permanent" : formatDurationUntil(currentEntry.getExpiration().getTime());

                sendLog(staff, action, target, length, currentEntry.getReason());
            } else if (isUnban && wasBanned && currentEntry == null) {
                sendLog(staff, "unbanned", target, "N/A", null);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMuteStatusChange(MuteStatusChangeEvent event) {
        String action = event.getValue() ? "muted" : "unmuted";
        String length = event.getValue() ? event.getTimestamp().map(this::formatDurationUntil).orElse("Permanent / Unknown") : "N/A";
        String reason = event.getReason();

        sendLog(event.getController().getName(), action, event.getAffected().getName(), length, reason);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJailStatusChange(JailStatusChangeEvent event) {
        String action = event.getValue() ? "jailed" : "released from jail";

        sendLog(event.getController().getName(), action, event.getAffected().getName(), "N/A", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUserKick(UserKickEvent event) {
        sendLog(event.getKicker().getName(), "kicked", event.getKicked().getName(), "N/A", event.getReason());
    }

    private void sendLog(String staff, String action, String target, String length, String reason) {
        String webhookUrl = plugin.getConfig().getString("discord-webhook", "").trim();
        if (webhookUrl.isEmpty()) return;

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
            request = HttpRequest.newBuilder().uri(URI.create(webhookUrl)).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid Discord webhook URL in config.yml.");
            return;
        }

        CompletableFuture<HttpResponse<String>> requestFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

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
        if (remaining <= 0) return "Expired";

        Duration duration = Duration.ofMillis(remaining);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m");
        if (result.isEmpty()) result.append("<1m");

        return result.toString().trim();
    }

    private String jsonEscape(String value) {
        if (value == null || value.isBlank()) return "Not provided";

        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}