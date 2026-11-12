# Web bridge

Writes website-facing snapshots to Dynmap's web directory. The main snapshot includes player/guild/quest data from configured local sources. Separate outputs cover EventBridge and QuickShop stock.

| Data | Output or input |
|---|---|
| Main Paper snapshot | `plugins/dynmap/web/errsa-paper.json` |
| EventBridge snapshot | `plugins/dynmap/web/errsa-eventbridge.json` |
| QuickShop stock | `plugins/dynmap/web/errsa-shop-stock.json` |
| Guilds database | `plugins/Guilds/guilds.db` |
| PlayerQuests database | `plugins/PlayerQuests/quests.db` |
| Stock cache | `web-bridge/shop-stock-cache.properties` |

The default sync interval is 15 seconds. `/errsawebbridge` provides status and snapshot sync functions.

Source: `com.errsa.webbridge`.

## Settings

Default values from `paper/src/main/resources/modules/web-bridge/config.yml`:

```yaml
sync-interval-seconds: 15
guilds-db: plugins/Guilds/guilds.db
quests-db: plugins/PlayerQuests/quests.db
output-file: plugins/dynmap/web/errsa-paper.json
```
