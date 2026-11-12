# Paper modules

Eight modules are loaded by `CoreModuleManager` when enabled in `config.yml`. Each module has its own configuration under `plugins/ERRSA-MC-Core/<module>/config.yml`.

| Module | Purpose | Reference |
|---|---|---|
| `custom-portals` | Region-based teleports; WorldGuard or cuboid triggers | [Details](modules/custom-portals.md) |
| `end-entity-reroute` | Route non-player entities leaving the End to the configured destination | [Details](modules/end-entity-reroute.md) |
| `auto-unlock-pvp-drops` | Release eligible PvP death drops in GriefPrevention | [Details](modules/auto-unlock-pvp-drops.md) |
| `combat-diagnostics` | Record combat incidents and packet correlation for review | [Details](modules/combat-diagnostics.md) |
| `inventory-audit` | Record inventory-state anomalies and shulker overlap | [Details](modules/inventory-audit.md) |
| `compatibility-paper` | Send periodic compatibility reports to the ERRSA bot | [Details](modules/compatibility-paper.md) |
| `web-bridge` | Write player, guild, quest, EventBridge, and shop data for the website | [Details](modules/web-bridge.md) |
| `wiki-sync` | Export plugin and permission inventory to GitHub | [Details](modules/wiki-sync.md) |

Core also registers the player-facing `/sell`, `/discord`, `/feedback`, `/report`, `/tutorial`, `/resetschedule`, `/website`, `/map`, and `/menu` commands.
