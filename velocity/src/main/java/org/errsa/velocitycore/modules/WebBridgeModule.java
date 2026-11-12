package org.errsa.velocitycore.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.errsa.velocitycore.CoreConfig;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Preserves the original WebBridge JSON schema, source-file paths, and posting interval. */
public final class WebBridgeModule {
    private final ProxyServer proxy;
    private final Logger log;
    private final CoreConfig configs;
    private final ScheduledExecutorService workers;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean sending = new AtomicBoolean();

    public WebBridgeModule(ProxyServer proxy, Logger log, CoreConfig configs, ScheduledExecutorService workers) {
        this.proxy = proxy;
        this.log = log;
        this.configs = configs;
        this.workers = workers;
    }

    public void start() throws Exception {
        Properties settings = configs.loadModule("web-bridge", "plugins/ERRSAWebBridgeVelocity/config.properties",
                "/web-bridge.properties");
        long interval = Math.max(10, Long.parseLong(settings.getProperty("interval-seconds", "15")));
        workers.scheduleWithFixedDelay(() -> push(settings), 3, interval, TimeUnit.SECONDS);
        log.info("WebBridge scheduled every {} seconds", interval);
    }

    private void push(Properties settings) {
        if (!sending.compareAndSet(false, true)) return;
        try {
            String endpoint = settings.getProperty("endpoint", "").trim();
            String secret = settings.getProperty("secret", "").trim();
            if (!endpoint.startsWith("https://") || secret.length() < 12 || secret.startsWith("CHANGE_ME")) return;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("schema", 1);
            payload.put("generatedAt", Instant.now().toString());
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("online", proxy.getPlayerCount());
            if (Boolean.parseBoolean(settings.getProperty("include-online-players", "true"))) {
                ArrayList<Map<String, String>> players = new ArrayList<>();
                for (Player player : proxy.getAllPlayers()) {
                    players.add(Map.of("uuid", player.getUniqueId().toString(), "username", player.getUsername()));
                }
                state.put("players", players);
            }
            payload.put("proxy", state);
            Map<String, Object> sources = new LinkedHashMap<>();
            sources.put("playtime", loadSource(settings.getProperty("playtime-file",
                    "plugins/RankProgressionProxy/playtime-data.yml")));
            sources.put("eventbridge", loadSource(settings.getProperty("eventbridge-file",
                    "plugins/eventbridge/eventbridge-state.json")));
            payload.put("sources", sources);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "ERRSAWebBridgeVelocity/1.0.1")
                    .header("X-ERRSA-Bridge-Key", secret)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("WebBridge endpoint returned HTTP {}", response.statusCode());
            }
        } catch (Exception exception) {
            log.warn("WebBridge snapshot failed: {}", exception.toString());
        } finally {
            sending.set(false);
        }
    }

    private Map<String, Object> loadSource(String filename) {
        // Exact consumer semantics should be compared with the existing website before production rollout.
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", filename);
        try {
            Path path = Path.of(filename);
            if (Files.isRegularFile(path)) {
                byte[] bytes = Files.readAllBytes(path);
                result.put("available", true);
                result.put("encoding", "base64");
                result.put("size", bytes.length);
                result.put("modifiedAt", Files.getLastModifiedTime(path).toMillis());
                result.put("content", java.util.Base64.getEncoder().encodeToString(bytes));
            } else {
                result.put("available", false);
            }
        } catch (Exception exception) {
            result.put("available", false);
        }
        return result;
    }
}
