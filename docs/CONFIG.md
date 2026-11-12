# Configuration

The main Paper file is `plugins/ERRSA-MC-Core/config.yml`. Module-specific files live at `plugins/ERRSA-MC-Core/<module>/config.yml`.

## Main settings

| Key | Default | Function |
|---|---|---|
| `modules.<id>.enabled` | `true` for eight bundled modules | Loads the named module at startup |
| `economy.diamond-sell-price` | `50.0` | Diamond value used by `/sell` |
| `reset.date` | `August 2027` | Date text shown by `/resetschedule` |
| `reset.cycle` | `3` | Reset cycle text shown by `/resetschedule` |
| `moderation.discord-webhook` | empty | Core moderation webhook setting |
| `links.discord` | ERRSA Discord invite | `/discord` |
| `links.feedback` | Feedback form | `/feedback` |
| `links.report` | Report form | `/report` |
| `links.website` | ERRSA handbook URL | `/website` |
| `links.map` | Handbook map URL | `/map` |

The `modules` section is read during module initialization. Each module's configuration is separate from the main switch.

## Module configurations

Each page contains the source defaults, setting names and feature-specific behavior:

- [custom-portals](modules/custom-portals.md)
- [end-entity-reroute](modules/end-entity-reroute.md)
- [auto-unlock-pvp-drops](modules/auto-unlock-pvp-drops.md)
- [combat-diagnostics](modules/combat-diagnostics.md)
- [inventory-audit](modules/inventory-audit.md)
- [compatibility-paper](modules/compatibility-paper.md)
- [web-bridge](modules/web-bridge.md)
- [wiki-sync](modules/wiki-sync.md)
