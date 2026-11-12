package com.errsa.compat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.modules.CoreModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Reports installed plugin metadata to the configured HTTPS endpoint. */
public final class PaperReporter extends CoreModule {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private BukkitTask reportTask;

    public PaperReporter(ERRSAMCCore plugin) {
        super(plugin, "compatibility-paper");
    }

    @Override
    public void onEnable() {
        long interval = Math.max(60,
                getConfig().getLong("report-interval-seconds", 300)) * 20L;
        reportTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::report, 40L, interval);
    }

    @Override
    public void onDisable() {
        if (reportTask != null) {
            reportTask.cancel();
            reportTask = null;
        }
    }

    private void report() {
        String endpoint = getConfig().getString("endpoint", "");
        String secret = getConfig().getString("secret", "");
        if (!endpoint.startsWith("https://") || secret.length() < 24) {
            return;
        }

        // Read Bukkit metadata on the main server thread, send over HTTP async.
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        String floodgateJson = floodgate == null
                ? "{}"
                : "{\"Floodgate\":\""
                + escape(floodgate.getDescription().getVersion()) + "\"}";

        StringBuilder inventory = new StringBuilder("[");
        Plugin[] installed = Bukkit.getPluginManager().getPlugins();
        for (int i = 0; i < installed.length; i++) {
            if (i > 0) {
                inventory.append(',');
            }
            inventory.append('"').append(escape(installed[i].getName()))
                    .append('"');
        }
        inventory.append(']');

        String payload = "{\"server\":\"paper\",\"minecraft\":\""
                + escape(Bukkit.getMinecraftVersion())
                + "\",\"plugins\":" + floodgateJson
                + ",\"inventory\":" + inventory + "}";
        sendReport(endpoint, secret, payload);
    }

    private void sendReport(String endpoint, String secret, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + secret)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            getLogger().warning("Compatibility report HTTP "
                                    + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        getLogger().log(Level.WARNING,
                                "Compatibility report failed", ex);
                        return null;
                    });
        } catch (RuntimeException ex) {
            getLogger().log(Level.WARNING, "Compatibility report failed", ex);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
