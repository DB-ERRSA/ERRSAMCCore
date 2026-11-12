# Inventory audit

Tracks inventory-state changes and writes incidents based on configured severity. Current checks cover previously observed inventory hashes returning in the same context, shulker contents overlapping loose items, and respawn destination changes.

The module applies grace periods after joins, respawns, world changes and gamemode changes, and collapses repeat observations. Default file threshold is `MEDIUM`; default console threshold is `HIGH`.

`/inventoryaudit stats` shows state counts; `/inventoryaudit reload` reloads settings. Source: `com.errsa.inventory`.

## Settings

Default values from `paper/src/main/resources/modules/inventory-audit/config.yml`:

```yaml
logging:
  console-min-severity: HIGH # Print HIGH/CRITICAL incidents to console.
  file-min-severity: MEDIUM # Persist MEDIUM and above to the log file.
  write-routine-events: false # Set true temporarily while debugging.
  log-file: audit/incidents.log # Relative to inventory-audit folder.
noise-control:
  world-change-grace-ticks: 40 # Allow inventory swap plugins to finish after world changes.
  gamemode-change-grace-ticks: 20 # Avoid false alerts after game mode swaps.
  join-grace-ticks: 60 # Allow other plugins to restore inventory on login.
  respawn-grace-ticks: 40 # Let death/respawn plugins complete changes.
  duplicate-window-ticks: 100 # Collapse repeated observations to one incident.
  per-player-code-cooldown-seconds: 30 # Prevent alert spam by one player/code.
  remembered-context-minutes: 30 # Prune old comparison hashes from RAM.
checks:
  old-hash-in-same-context: true # Advisory: older inventory fingerprint reappeared.
  shulker-content-overlap: true # Compare shulker contents against loose items.
  minimum-shulker-overlap-items: 16 # Threshold of overlap before review.
  minimum-single-fingerprint-mirror-items: 256 # Higher bar for single-item shulkers.
  respawn-destination-change: true # Informational respawn location changes.
performance:
  cleanup-period-ticks: 1200 # Remove stale state approximately every minute.
  async-file-writer: true # Avoid disk writing on server tick thread.
  max-file-queue: 10000 # Upper bound for pending incident writes.
```
