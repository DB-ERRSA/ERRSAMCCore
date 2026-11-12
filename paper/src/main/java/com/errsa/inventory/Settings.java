package com.errsa.inventory;

import org.bukkit.configuration.file.FileConfiguration;

record Settings(
        Severity consoleMinSeverity, Severity fileMinSeverity, boolean writeRoutineEvents, String logFile,
        long worldChangeGraceTicks, long gamemodeChangeGraceTicks, long joinGraceTicks, long respawnGraceTicks,
        long duplicateWindowTicks, long perPlayerCodeCooldownSeconds, long rememberedContextMinutes,
        boolean oldHashInSameContext, boolean shulkerContentOverlap, int minimumShulkerOverlapItems,
        int minimumSingleFingerprintMirrorItems, boolean respawnDestinationChange, long cleanupPeriodTicks,
            boolean asyncFileWriter, int maxFileQueue) {
    static Settings from(FileConfiguration c) {
        return new Settings(
                Severity.parse(c.getString("logging.console-min-severity", "HIGH"), Severity.HIGH),
                Severity.parse(c.getString("logging.file-min-severity", "MEDIUM"), Severity.MEDIUM),
                c.getBoolean("logging.write-routine-events", false),
                c.getString("logging.log-file", "audit/incidents.log"),
                c.getLong("noise-control.world-change-grace-ticks", 40),
                c.getLong("noise-control.gamemode-change-grace-ticks", 20),
                c.getLong("noise-control.join-grace-ticks", 60),
                c.getLong("noise-control.respawn-grace-ticks", 40),
                c.getLong("noise-control.duplicate-window-ticks", 100),
                c.getLong("noise-control.per-player-code-cooldown-seconds", 30),
                c.getLong("noise-control.remembered-context-minutes", 30),
                c.getBoolean("checks.old-hash-in-same-context", true),
                c.getBoolean("checks.shulker-content-overlap", true),
                c.getInt("checks.minimum-shulker-overlap-items", 16),
                c.getInt("checks.minimum-single-fingerprint-mirror-items", 256),
                c.getBoolean("checks.respawn-destination-change", true),
                Math.max(20L, c.getLong("performance.cleanup-period-ticks", 1200)),
                c.getBoolean("performance.async-file-writer", true),
                Math.max(100, c.getInt("performance.max-file-queue", 10000))
        );
    }
}
