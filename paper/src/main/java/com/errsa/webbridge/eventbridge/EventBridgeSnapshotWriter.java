package com.errsa.webbridge.eventbridge;

import com.errsa.webbridge.snapshot.SnapshotJson;

import static com.errsa.webbridge.eventbridge.EventBridgeFields.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Serializes EventBridge state into the existing Dynmap-facing schema. */
final class EventBridgeSnapshotWriter {
    private final Path destination;
    private final Logger logger;

    EventBridgeSnapshotWriter(Path destination, Logger logger) {
        this.destination = destination;
        this.logger = logger;
    }

    synchronized void writeSnapshot(Object calendar, Object newsletters) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ok", true);
        snapshot.put("available", calendar != null || newsletters != null);
        snapshot.put("generatedAt", Instant.now().toString());

        Map<String, Object> uniqueEvents = new LinkedHashMap<>();
        appendEvents(uniqueEvents, list(read(calendar, "running")));
        appendEvents(uniqueEvents, list(read(calendar, "upcoming")));
        List<Map<String, Object>> events = new ArrayList<>();
        for (Object event : uniqueEvents.values()) {
            events.add(eventProperties(event));
        }
        snapshot.put("events", events);

        List<Map<String, Object>> news = new ArrayList<>();
        for (Object newsletter : list(read(newsletters, "newsletters"))) {
            news.add(newsletterProperties(newsletter));
        }
        snapshot.put("newsletters", news);
        try {
            Path directory = destination.toAbsolutePath().getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
            Files.writeString(temporary, SnapshotJson.stringify(snapshot), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            logger.warning("Cannot write EventBridge snapshot: " + error.getMessage());
        }
    }

    private static void appendEvents(Map<String, Object> result, List<?> source) {
        for (Object event : source) {
            if (event == null) {
                continue;
            }
            Object id = read(event, "id");
            String key = id == null ? "event-" + result.size() : String.valueOf(id);
            result.put(key, event);
        }
    }

    private static Map<String, Object> eventProperties(Object event) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", string(read(event, "id")));
        properties.put("name", string(read(event, "name")));
        properties.put("serverName", string(read(event, "serverName")));
        properties.put("gameType", string(read(event, "gameType")));
        properties.put("startEpochMillis", number(read(event, "startEpochMillis")));
        properties.put("endEpochMillis", nullableNumber(read(event, "endEpochMillis")));
        properties.put("status", enumName(read(event, "status")));
        properties.put("description", string(read(event, "description")));
        properties.put("signupEnabled", booleanValue(read(event, "signupEnabled")));
        properties.put("signupCap", nullableNumber(read(event, "signupCap")));
        properties.put("signedUpCount", number(read(event, "signedUpCount")));
        properties.put("runtimePhase", string(read(event, "runtimePhase")));
        properties.put("runtimeAlivePlayers", nullableNumber(read(event, "runtimeAlivePlayers")));
        properties.put("runtimeEliminatedPlayers", nullableNumber(read(event, "runtimeEliminatedPlayers")));
        properties.put("runtimeSpectators", nullableNumber(read(event, "runtimeSpectators")));
        properties.put("runtimeCountdownSeconds", nullableNumber(read(event, "runtimeCountdownSeconds")));
        properties.put("runtimeWinner", string(read(event, "runtimeWinner")));
        return properties;
    }

    private static Map<String, Object> newsletterProperties(Object newsletter) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", string(read(newsletter, "id")));
        properties.put("title", string(read(newsletter, "title")));
        properties.put("author", string(read(newsletter, "author")));
        properties.put("sentEpochMillis", number(read(newsletter, "sentEpochMillis")));
        properties.put("read", booleanValue(read(newsletter, "read")));
        return properties;
    }
}
