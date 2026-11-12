package com.errsa.inventory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

final class AsyncIncidentWriter implements AutoCloseable {
    private final BlockingQueue<String> queue;
    private final AtomicLong dropped = new AtomicLong();
    private final Thread thread;
    private final Path file;
    private final Logger logger;
    private volatile boolean running = true;
    private final boolean async;

    AsyncIncidentWriter(Path file, int maxQueue, boolean async, Logger logger) {
        this.logger = logger;
        this.file = file;
        this.queue = new ArrayBlockingQueue<>(maxQueue);
        this.async = async;
        if (async) {
            thread = new Thread(this::loop, "InventoryAudit-Writer");
            thread.setDaemon(true);
            thread.start();
        } else thread = null;
    }

    void offer(String line) {
        if (!async) {
            append(line);
            return;
        }
        if (!queue.offer(line)) {
            dropped.incrementAndGet();
        }
    }

    int queueSize() {
        return queue.size();
    }
    long dropped() {
        return dropped.get();
    }

    private void loop() {
        while (running || !queue.isEmpty()) {
            try {
                String first = queue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                List<String> batch = new ArrayList<>(256);
                batch.add(first);
                queue.drainTo(batch, 255);
                appendBatch(batch);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void append(String line) {
        appendBatch(List.of(line));
    }

    private void appendBatch(List<String> lines) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                for (String line : lines) {
                    out.write(line);
                    out.newLine();
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not write audit incident log " + file, ex);
        }
    }

    @Override public void close() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (!queue.isEmpty()) {
            List<String> remaining = new ArrayList<>();
            queue.drainTo(remaining);
            if (!remaining.isEmpty()) {
                appendBatch(remaining);
            }
        }
    }
}
