package org.errsa.velocitycore.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.errsa.velocitycore.CoreConfig;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

/** Startup-only inventory publication to the existing ERRSA wiki snapshot path. */
public final class WikiSyncModule {
    private final ProxyServer proxy;
    private final Logger log;
    private final CoreConfig configs;
    private final ScheduledExecutorService workers;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WikiSyncModule(ProxyServer proxy, Logger log, CoreConfig configs, ScheduledExecutorService workers) {
        this.proxy = proxy;
        this.log = log;
        this.configs = configs;
        this.workers = workers;
    }

    public void start() throws Exception {
        Properties config = configs.loadModule("wiki-sync", "plugins/errsa-wiki-sync/config.properties",
                "/wiki-sync.properties");
        workers.execute(() -> publish(config));
    }

    private void publish(Properties config) {
        String owner = config.getProperty("github.owner", "").trim();
        String repo = config.getProperty("github.repo", "").trim();
        String branch = config.getProperty("github.branch", "main").trim();
        String token = config.getProperty("github.token", "").trim();
        if (owner.isBlank() || repo.isBlank() || token.isBlank()) {
            log.warn("WikiSync credentials not configured; publication skipped.");
            return;
        }
        try {
            ArrayList<Map<String, String>> inventory = new ArrayList<>();
            proxy.getPluginManager().getPlugins().stream()
                    .sorted(Comparator.comparing(p -> p.getDescription().getId()))
                    .forEach(plugin -> inventory.add(toRecord(plugin)));
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("schema", 1);
            document.put("server", "velocity");
            document.put("serverVersion", proxy.getVersion().getVersion());
            document.put("capturedAt", Instant.now().toString());
            document.put("plugins", inventory);
            String content = Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(document));
            String base = "https://api.github.com/repos/" + encode(owner) + "/" + encode(repo)
                    + "/contents/docs/assets/wiki-sync/velocity.json";
            HttpRequest get = HttpRequest.newBuilder(URI.create(base + "?ref=" + encode(branch)))
                    .header("Authorization", "Bearer " + token).header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(15)).build();
            HttpResponse<String> found = client.send(get, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Update Velocity plugin inventory");
            body.put("branch", branch);
            body.put("content", content);
            if (found.statusCode() == 200) {
                String sha = mapper.readTree(found.body()).path("sha").asText("");
                if (!sha.isBlank()) body.put("sha", sha);
            } else if (found.statusCode() != 404) {
                log.warn("WikiSync failed reading GitHub snapshot: HTTP {}", found.statusCode());
                return;
            }
            HttpRequest put = HttpRequest.newBuilder(URI.create(base))
                    .header("Authorization", "Bearer " + token).header("Accept", "application/vnd.github+json")
                    .header("Content-Type", "application/json").timeout(Duration.ofSeconds(20))
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> sent = client.send(put, HttpResponse.BodyHandlers.ofString());
            if (sent.statusCode() == 200 || sent.statusCode() == 201) {
                log.info("WikiSync published proxy inventory to GitHub.");
            } else {
                log.warn("WikiSync GitHub update failed: HTTP {}", sent.statusCode());
            }
        } catch (Exception exception) {
            log.warn("WikiSync publication failed: {}", exception.toString());
        }
    }

    private Map<String, String> toRecord(PluginContainer plugin) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", plugin.getDescription().getName().orElse(plugin.getDescription().getId()));
        item.put("id", plugin.getDescription().getId());
        item.put("version", plugin.getDescription().getVersion().orElse("unknown"));
        item.put("description", plugin.getDescription().getDescription().orElse(""));
        return item;
    }

    private static String encode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
