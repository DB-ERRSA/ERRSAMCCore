# Commands

## Core commands

| Command | Behavior | Permission |
|---|---|---|
| `/sell` | Sells diamonds from inventory at `economy.diamond-sell-price` | `ERRSAMCCore.sell` |
| `/discord` | Discord invite from `links.discord` | `ERRSAMCCore.discord` |
| `/feedback` | Feedback form from `links.feedback` | `ERRSAMCCore.feedback` |
| `/report` | Report form from `links.report` | `ERRSAMCCore.report` |
| `/tutorial` | Teleport to configured tutorial area | `ERRSAMCCore.tutorial` |
| `/resetschedule` | Displays `reset.date` and `reset.cycle` | `ERRSAMCCore.resetschedule` |
| `/website` | Link from `links.website` | `ERRSAMCCore.website` |
| `/map` | Link from `links.map` | `ERRSAMCCore.map` |
| `/menu` | Opens the server menu | `ERRSAMCCore.menu` |

## Portal commands

Permission: `errsa.core.portals.admin`.

| Command | Behavior |
|---|---|
| `/coreportal list` | List portal IDs |
| `/coreportal reload` | Reload portal configuration |
| `/coreportal pos1 <id>` | Record first cuboid corner at your position |
| `/coreportal pos2 <id>` | Record second cuboid corner at your position |
| `/coreportal create <id>` | Create a disabled cuboid portal from the selected corners |
| `/coreportal destination <id>` | Set the portal destination to your position |
| `/coreportal enable <id>` | Enable a portal with a valid destination |
| `/coreportal disable <id>` | Disable a portal |
| `/coreportal remove <id>` | Remove a portal |

IDs use lowercase letters, digits, `_` or `-`, maximum 48 characters. The built-in WorldGuard portals are defined in [custom portals](modules/custom-portals.md).

## End exit

`/endexit set` (alias `/shulkerbay`) saves your current Overworld position as the non-player End exit destination. Permission: `ERRSAMCCore.endexit`.

## Combat diagnostics

Permission: `errsa.combatdiag.staff`. Alias: `/cdiag`.

| Command | Behavior |
|---|---|
| `/cdiag watch <player> [minutes]` | Start manual evidence capture. Maximum defaults to 30 minutes |
| `/cdiag stop <player>` | End a manual watch |
| `/cdiag mark <player> [reason]` | Record a manual incident marker |
| `/cdiag incidents` | List recent incident IDs |
| `/cdiag show <incident-id>` | Show an incident from the recent-memory index |
| `/cdiag status` | Active watches and incidents |
| `/cdiag health` | Queue and capture counters |
| `/cdiag packet` | Packet observation and correlation counters |
| `/cdiag reload` | Reload settings; sample interval still needs a restart |

Evidence records: `plugins/ERRSA-MC-Core/combat-diagnostics/logs/`.

## Inventory audit

Permission: `inventoryaudit.admin`. Alias: `/iaudit`.

| Command | Behavior |
|---|---|
| `/inventoryaudit` or `/inventoryaudit stats` | Audit state and counters |
| `/inventoryaudit reload` | Reload audit configuration |

## Web bridge

`/errsawebbridge` reports bridge status or requests a snapshot sync. Permission: `errsawebbridge.admin`.
