# Paper commands and permissions

Source: `paper/src/main/resources/plugin.yml`. These are registered command roots; subcommands need inspecting the appropriate command handler.

| Command | Purpose | Permission | Alias |
|---|---|---|---|
| `/errsawebbridge` | Show WebBridge status or force a snapshot sync. | `errsawebbridge.admin` | — |
| `/inventoryaudit` | Inventory audit status and module reload | `inventoryaudit.admin` | [iaudit] |
| `/combatdiag` | Combat diagnostics incident capture | `errsa.combatdiag.staff` | [cdiag] |
| `/sell` | Sell diamonds from your inventory. | `ERRSAMCCore.sell` | — |
| `/discord` | Get the Discord server link. | `ERRSAMCCore.discord` | — |
| `/feedback` | Get the feedback form link. | `ERRSAMCCore.feedback` | — |
| `/report` | Get the report form link. | `ERRSAMCCore.report` | — |
| `/tutorial` | Teleport to the tutorial area. | `ERRSAMCCore.tutorial` | — |
| `/resetschedule` | View the planned date of the next server reset. | `ERRSAMCCore.resetschedule` | — |
| `/website` | Get the server website link. | `ERRSAMCCore.website` | — |
| `/map` | View the live online world map. | `ERRSAMCCore.map` | — |
| `/menu` | Opens the server menu. | `ERRSAMCCore.menu` | — |
| `/coreportal` | Configure custom portal regions and destinations. | `errsa.core.portals.admin` | — |
| `/endexit` | Set the non-player End return-portal destination. | `ERRSAMCCore.endexit` | [shulkerbay] |

Permissions shown above are taken from `plugin.yml` and should not be assumed available to all players. Operator defaults are declared for `errsawebbridge.admin`, `inventoryaudit.admin`, `errsa.combatdiag.staff`, `ERRSAMCCore.endexit`, and `errsa.core.portals.admin`.

### Important
Do not grant staff diagnostic/admin permissions to normal player ranks. For nested subcommands such as `/coreportal` and `/combatdiag`, use the plugin help command and inspect the handler before using in production.
