package com.errsa.webbridge.eventbridge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/** Checks legacy EventBridge output fields and deduplication. */
public final class EventBridgeSmoke {
    public static final class Event {
        public String id = "event-42";
        public String name = "PvP Arena";
        public long startEpochMillis = 1000L;
        public boolean signupEnabled = true;
    }

    public static final class Calendar {
        public List<Event> running = List.of(new Event());
        public List<Event> upcoming = List.of(new Event());
    }

    public static final class Newsletter {
        public String id = "news-1";
        public String title = "Update";
        public boolean read = false;
    }

    public static final class Newsletters {
        public List<Newsletter> newsletters = List.of(new Newsletter());
    }

    public static void main(String[] arguments) throws Exception {
        Path file = Files.createTempFile("errsa-eventbridge-", ".json");
        EventBridgeSnapshotWriter writer = new EventBridgeSnapshotWriter(
                file, Logger.getLogger("EventBridgeSmoke"));
        writer.writeSnapshot(new Calendar(), new Newsletters());
        String json = Files.readString(file);
        for (String expected : List.of("\"available\":true", "\"name\":\"PvP Arena\"",
                "\"signupEnabled\":true", "\"title\":\"Update\"")) {
            if (!json.contains(expected)) {
                throw new AssertionError("Missing EventBridge JSON field: " + expected);
            }
        }
        if (json.split("\\\"event-42\\\"", -1).length != 2) {
            throw new AssertionError("EventBridge event ID deduplication changed");
        }
        System.out.println("PASS EventBridge snapshot and event deduplication");
    }
}
