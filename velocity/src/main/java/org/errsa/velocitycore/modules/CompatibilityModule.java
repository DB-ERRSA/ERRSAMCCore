package org.errsa.velocitycore.modules;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.PluginContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.errsa.velocitycore.CoreConfig;
import org.slf4j.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Periodically reports installed proxy plugin versions to the configured compatibility endpoint. */
public final class CompatibilityModule {
    private final ProxyServer proxy;
    private final Logger log;
    private final CoreConfig configs;
    private final ScheduledExecutorService workers;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public CompatibilityModule(ProxyServer proxy, Logger log, CoreConfig configs, ScheduledExecutorService workers) {
        this.proxy = proxy;
        this.log = log;
        this.configs = configs;
        this.workers = workers;
    }

    public void start() throws Exception {
        Properties settings = configs.loadModule("compatibility-velocity", "plugins/errsa-compat-velocity.properties",
                "/compatibility.properties");
        long interval = Math.max(60, Long.parseLong(settings.getProperty("report-interval-seconds", "300")));
        workers.scheduleWithFixedDelay(() -> report(settings), 5, interval, TimeUnit.SECONDS);
    }

    private void report(Properties settings) {
        String endpoint = settings.getProperty("endpoint", "").trim();
        String secret = settings.getProperty("secret", "").trim();
        if (!endpoint.startsWith("https://") || secret.length() < 24 || secret.startsWith("REPLACE_")) return;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("server", "velocity");
            data.put("minecraft", "");
            Map<String, String> versions = new LinkedHashMap<>();
            for (String id : new String[]{"geyser", "floodgate", "viaversion"}) {
                proxy.getPluginManager().getPlugin(id).ifPresent(plugin -> {
                    String key = switch (id) {
                        case "geyser" -> "Geyser";
                        case "floodgate" -> "Floodgate";
                        default -> "ViaVersion";
                    };
                    versions.put(key, plugin.getDescription().getVersion().orElse("unknown"));
                });
            }
            data.put("plugins", versions);
            ArrayList<String> inventory = new ArrayList<>();
            for (PluginContainer plugin : proxy.getPluginManager().getPlugins()) {
                inventory.add(plugin.getDescription().getName().orElse(plugin.getDescription().getId()));
            }
            data.put("inventory", inventory);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + secret)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(data))).build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Compatibility endpoint returned HTTP {}", response.statusCode());
            }
        } catch (Exception exception) {
            log.warn("Compatibility report failed: {}", exception.toString());
        }
    }
}
