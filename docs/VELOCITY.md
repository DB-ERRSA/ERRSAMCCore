# Velocity proxy core

The `velocity/` Maven module targets Java 17 and builds `ERRSA-Velocity-Core-1.0.0.jar` in this snapshot.

It integrates four modules:

| Module | What it does | Key consideration |
|---|---|---|
| `compatibility-velocity` | Reports compatibility/plugin inventory and Geyser/Floodgate/ViaVersion information | Confirm API destination and credentials |
| `web-bridge` | Publishes proxy players and RankProgression/EventBridge sources | Compare external payload schema before production |
| `wiki-sync` | Creates a GitHub-oriented snapshot of installed proxy plugins | Only enable for intended repository and branch |
| `auto-via-updater` | Stages updates for **ViaVersion**, not the Velocity proxy | Confirm staging, backups and restart behavior |

Defaults and templates live in `velocity/src/main/resources/` (`core.properties`, `compatibility.properties`, `web-bridge.properties`, `wiki-sync.properties`, `config.toml`). Protect secrets before pushing.

**Migration** (from `velocity/README.md` in the uploaded project):

- `plugins/errsa-compat-velocity.properties` → `plugins/errsa-velocity-core/compatibility-velocity/config.properties`
- `plugins/ERRSAWebBridgeVelocity/config.properties` → `plugins/errsa-velocity-core/web-bridge/config.properties`
- `plugins/errsa-wiki-sync/config.properties` → `plugins/errsa-velocity-core/wiki-sync/config.properties`
- `plugins/autoviaupdater/config.toml` → `plugins/errsa-velocity-core/auto-via-updater/config.toml`

Back up the proxy and the prior configs. Remove the four integrated standalone plugin JARs, but keep ViaVersion itself. Start with conservative defaults, verify reports and GitHub behavior on Test, and enable auto-updating only after checking staging/rollback.

## Original source documentation

See `velocity/README.md` for extra integration notes and compatibility caveats.
