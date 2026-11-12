# Paper module guide

All modules are toggled in `plugins/ERRSA-MC-Core/config.yml` under `modules.<id>.enabled`. The eight modules below are registered by `CoreModuleManager`.

## end-entity-reroute

Moves non-player End return portal arrivals to a designated destination; does not reroute players.

- **Enable flag:** `modules.end-entity-reroute.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/end-entity-reroute/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## custom-portals

Custom region-triggered portals with WorldGuard/cuboid options, delay, cancellation, sound and particle settings.

- **Enable flag:** `modules.custom-portals.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/custom-portals/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## inventory-audit

Records suspicious inventory changes for staff review; evidence-focused rather than automatic punishment.

- **Enable flag:** `modules.inventory-audit.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/inventory-audit/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## combat-diagnostics

Captures evidence around combat events, samples, packet correlation and optional Discord staff alerts; not an automatic anti-cheat.

- **Enable flag:** `modules.combat-diagnostics.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/combat-diagnostics/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## auto-unlock-pvp-drops

Unlocks eligible PvP death drops using GriefPrevention/PvPToggle conditions.

- **Enable flag:** `modules.auto-unlock-pvp-drops.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/auto-unlock-pvp-drops/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## compatibility-paper

Compatibility information reporting to an endpoint; review destination before enabling.

- **Enable flag:** `modules.compatibility-paper.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/compatibility-paper/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## web-bridge

Publishes snapshots used by website/Dynmap integrations, including QuickShop and EventBridge-related data.

- **Enable flag:** `modules.web-bridge.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/web-bridge/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## wiki-sync

Syncs a wiki snapshot using GitHub-related configuration; can write to a remote destination.

- **Enable flag:** `modules.wiki-sync.enabled` in the main configuration
- **Defaults:** `paper/src/main/resources/modules/wiki-sync/config.yml`
- **Inspect:** actual source for implementation details; check logs for `Module enabled` or startup error.

## Conflict checks

The core explicitly checks for standalone `ERRSAWebBridge` and `ERRSAWikiSync` before enabling integrated `web-bridge` and `wiki-sync`; a conflict logs an error and prevents that module from enabling. For other previously standalone modules, verify that no duplicate listeners remain installed.

## Startup and failure behavior

Module initialization is attempted independently; a failed module logs a severe error and processing continues. Modules are shut down in reverse order. Configuration flags are read at enable time.
