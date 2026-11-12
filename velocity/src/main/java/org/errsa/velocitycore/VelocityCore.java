package org.errsa.velocitycore;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.errsa.velocitycore.modules.AutoViaModule;
import org.errsa.velocitycore.modules.CompatibilityModule;
import org.errsa.velocitycore.modules.WebBridgeModule;
import org.errsa.velocitycore.modules.WikiSyncModule;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

@Plugin(id = "errsa-velocity-core", name = "ERRSA Velocity Core", version = "1.0.0", authors = {"ERRSA MC Dev Team"})
public final class VelocityCore {
    private final ProxyServer proxy;
    private final Logger logger;
    private final CoreConfig configs;
    private final ScheduledExecutorService workers = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "errsa-velocity-core-worker");
        thread.setDaemon(true);
        return thread;
    });

    @Inject
    public VelocityCore(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.configs = new CoreConfig(dataDirectory);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        start("compatibility-velocity", () -> new CompatibilityModule(proxy, logger, configs, workers).start());
        start("web-bridge", () -> new WebBridgeModule(proxy, logger, configs, workers).start());
        start("wiki-sync", () -> new WikiSyncModule(proxy, logger, configs, workers).start());
        start("auto-via-updater", () -> {
            Path path = configs.modulePath("auto-via-updater");
            java.nio.file.Files.createDirectories(path);
            Path modern = path.resolve("config.toml");
            Path old = Path.of("plugins/autoviaupdater/config.toml");
            if (java.nio.file.Files.notExists(modern) && java.nio.file.Files.exists(old)) {
                java.nio.file.Files.copy(old, modern);
            }
            new AutoViaModule(proxy, path, logger, this).start();
        });
        logger.info("ERRSA Velocity Core module initialization complete.");
    }

    private void start(String name, ThrowingRunnable action) {
        try {
            if (!configs.isEnabled(name)) {
                logger.info("Module disabled: {}", name);
                return;
            }
            action.run();
            logger.info("Module started: {}", name);
        } catch (Exception exception) {
            logger.error("Module failed to initialize: " + name, exception);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        workers.shutdownNow();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
