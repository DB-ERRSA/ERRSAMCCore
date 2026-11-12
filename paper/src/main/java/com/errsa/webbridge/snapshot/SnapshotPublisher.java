package com.errsa.webbridge.snapshot;

import com.errsa.webbridge.player.PlayerSnapshotService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/** Builds and atomically publishes the Dynmap-facing WebBridge snapshot. */
public final class SnapshotPublisher {
    private final FileConfiguration configuration;
    private final Logger logger;
    private final DatabaseSnapshotService databases = new DatabaseSnapshotService();
    private final AtomicReference<Map<String, Object>> players =
            new AtomicReference<>(PlayerSnapshotService.empty());
    private final AtomicReference<String> status =
            new AtomicReference<>("Not synced yet");

    public SnapshotPublisher(FileConfiguration configuration, Logger logger) {
        this.configuration = configuration;
        this.logger = logger;
    }

    public void updatePlayers(Map<String, Object> snapshot) {
        players.set(snapshot);
    }

    public String status() {
        return status.get();
    }

    public String outputPath() {
        return configuration.getString(
                "output-file", "plugins/dynmap/web/errsa-paper.json");
    }

    public synchronized void sync() {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("ok", true);
            snapshot.put("generatedAt", Instant.now().toString());
            snapshot.put("players", players.get());
            snapshot.put("guilds", databases.guildSnapshot(
                    new java.io.File(configuration.getString(
                            "guilds-db", "plugins/Guilds/guilds.db"))));
            snapshot.put("quests", databases.questSnapshot(
                    new java.io.File(configuration.getString(
                            "quests-db", "plugins/PlayerQuests/quests.db"))));
            writeAtomic(Path.of(outputPath()), SnapshotJson.stringify(snapshot));
            status.set("OK - " + Instant.now() + " -> " + outputPath());
        } catch (Exception error) {
            status.set("ERROR - " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
            logger.warning("Snapshot failed: " + error);
        }
    }

    private static void writeAtomic(Path destination, String json) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.writeString(temporary, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        try {
            Files.move(temporary, destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
