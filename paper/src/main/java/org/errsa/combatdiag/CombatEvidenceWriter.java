package org.errsa.combatdiag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

/** Dedicated background JSONL writer and retention policy. */
final class CombatEvidenceWriter {
    private final Path directory;
    private final BlockingQueue<String> queue;
    private final BooleanSupplier running;
    private final IntSupplier retentionDays;
    private final Logger logger;
    private final Consumer<String> errorReporter;

    CombatEvidenceWriter(Path directory,
                         BlockingQueue<String> queue,
                         BooleanSupplier running,
                         IntSupplier retentionDays,
                         Logger logger,
                         Consumer<String> errorReporter) {
        this.directory = directory;
        this.queue = queue;
        this.running = running;
        this.retentionDays = retentionDays;
        this.logger = logger;
        this.errorReporter = errorReporter;
    }

    void run() {
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            logger.severe("Cannot create log directory: " + ex);
            return;
        }

        LocalDate current = null;
        BufferedWriter output = null;
        try {
            while (running.getAsBoolean() || !queue.isEmpty()) {
                String line;
                try {
                    line = queue.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    if (!running.getAsBoolean() && queue.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (line == null) {
                    continue;
                }
                LocalDate today = LocalDate.now(ZoneOffset.UTC);
                if (!today.equals(current)) {
                    if (output != null) {
                        output.close();
                    }
                    current = today;
                    output = Files.newBufferedWriter(
                            directory.resolve("combat-" + today + ".jsonl"),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                    cleanup();
                }
                output.write(line);
                output.newLine();
                output.flush();
            }
        } catch (IOException ex) {
            errorReporter.accept(ex.toString());
            logger.severe("Evidence logging failure: " + ex);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                    // The original write failure has already been reported.
                }
            }
        }
    }

    private void cleanup() {
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC)
                .minusDays(Math.max(1, retentionDays.getAsInt()));
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(
                directory, "combat-*.jsonl")) {
            for (Path path : paths) {
                String name = path.getFileName().toString();
                try {
                    LocalDate date = LocalDate.parse(name.substring(7, 17));
                    if (date.isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                    }
                } catch (RuntimeException ignored) {
                    // Never delete unexpected filenames.
                }
            }
        } catch (IOException ex) {
            logger.warning("Retention cleanup failed: " + ex);
        }
    }
}
