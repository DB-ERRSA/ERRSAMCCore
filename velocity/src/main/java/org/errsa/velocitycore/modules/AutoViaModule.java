package org.errsa.velocitycore.modules;


import com.moandjiezana.toml.Toml;




import com.velocitypowered.api.proxy.ProxyServer;
import common.UpdateState;
import common.StagedInstaller;
import common.UpdateVias;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/** Velocity-only plugin: stage once per boot, apply previous stages during initialization. */

public final class AutoViaModule {
    private final Object schedulerOwner;
    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;
    private final AtomicBoolean checking = new AtomicBoolean();

    public AutoViaModule(ProxyServer proxy, Path dataDirectory, Logger logger, Object schedulerOwner) {
        this.schedulerOwner = schedulerOwner;
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void start() {
        try {
            Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("config.toml");
            if (Files.notExists(configPath)) {
                try (InputStream resource = getClass().getResourceAsStream("/config.toml")) {
                    if (resource == null) throw new IOException("Bundled config.toml missing");
                    Files.copy(resource, configPath);
                }
            }
            Toml config = new Toml().read(configPath.toFile());
            // Apply only a JAR staged during a previous boot. At this point
            // Velocity has not begun accepting connections; replacement becomes
            // active at the NEXT proxy restart, not in the current JVM.
            if (Boolean.TRUE.equals(config.getBoolean("auto-install", true))) {
                applyPreviouslyStaged();
            }
            if (!Boolean.TRUE.equals(config.getBoolean("check-on-startup", true))) return;
            long delay = Math.max(1L, config.getLong("delay-seconds", 5L));
            proxy.getScheduler().buildTask(schedulerOwner, () -> checkUpdates(config))
                    .delay(Duration.ofSeconds(delay)).schedule();
            logger.info("ERRSA AutoViaUpdater enabled (startup-only staging and next-startup replacement).");
        } catch (Exception exception) {
            logger.error("Could not initialize ERRSA AutoViaUpdater", exception);
        }
    }

    private void applyPreviouslyStaged() {
        try {
            UpdateState state = new UpdateState(dataDirectory);
            Path staged = dataDirectory.resolve("staged-updates/ViaVersion.jar");
            if (state.get("staged.version").isBlank() || !Files.isRegularFile(staged)) return;
            String installed = installedVersion();
            if (installed.equals("unknown")) {
                logger.warn("ViaVersion not loaded; automatic installation skipped for safety.");
                return;
            }
            if (installed.equals(state.get("staged.version"))) {
                Files.deleteIfExists(staged);
                state.set("staged.version", "");
                state.set("staged.build", "");
                state.set("applied.pending", "");
                state.save();
                return;
            }
            // An applied replacement is waiting to load on a future restart.
            if (!state.get("applied.pending").isBlank()) return;
            Path plugins = dataDirectory.getParent().getParent();
            Path replaced = StagedInstaller.install(plugins, staged, dataDirectory.resolve("backups"));
            state.set("applied.pending", state.get("staged.version"));
            state.save();
            logger.info("Prepared ViaVersion {} at {} during proxy startup. It will LOAD on the NEXT restart; current runtime remains {}.",
                    state.get("staged.version"), replaced, installed);
        } catch (Exception exception) {
            logger.error("Automatic ViaVersion install safely aborted; existing JAR preserved where possible", exception);
        }
    }

    private String installedVersion() {
        return proxy.getPluginManager().getPlugin("viaversion")
                .flatMap(container -> container.getDescription().getVersion()).orElse("unknown");
    }

    private void checkUpdates(Toml config) {
        if (!checking.compareAndSet(false, true)) return;
        try {
            Path staged = dataDirectory.resolve("staged-updates");
            Path stagedJar = staged.resolve("ViaVersion.jar");
            UpdateState state = new UpdateState(dataDirectory);
            String installed = installedVersion();
            state.set("installed.version", installed);
            // An old staged JAR is no longer needed if the matching version is now installed.
            if (installed.equals(state.get("staged.version"))) {
                state.set("staged.version", "");
                state.set("staged.build", "");
                Files.deleteIfExists(stagedJar);
                state.set("applied.pending", "");
            }
            state.save();
            try {
                UpdateVias.Release available = UpdateVias.latest(config.getBoolean("snapshot", false));
                state.set("available.version", available.version());
                state.set("available.build", Integer.toString(available.build()));
                state.save();
                logger.info("ViaVersion: installed={}, available={} (Jenkins build {}), staged={}",
                        installed, available.version(), available.build(), state.get("staged.version").isBlank() ? "none" : state.get("staged.version"));
                if (!state.get("applied.pending").isBlank()) {
                    logger.info("A prepared ViaVersion replacement is awaiting another restart; no additional downloads.");
                } else if (installed.equals(available.version())) {
                    logger.info("ViaVersion is already installed; no download needed.");
                } else if (state.pendingMatches(available, stagedJar)) {
                    logger.info("ViaVersion {} is already staged; no download needed.", available.version());
                } else {
                    UpdateVias.stage(available, staged);
                    state.set("staged.version", available.version());
                    state.set("staged.build", Integer.toString(available.build()));
                    state.save();
                    logger.info("ViaVersion {} staged at {}. It will be prepared automatically during the next proxy startup.", available.version(), stagedJar);
                }
            } catch (Exception exception) {
                logger.warn("ViaVersion update check failed; installed version remains {}", installed, exception);
            }
        } catch (Exception exception) {
            logger.warn("Update check failed", exception);
        } finally {
            checking.set(false);
        }
    }
}
