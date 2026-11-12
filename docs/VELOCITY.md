# Velocity Core

One proxy plugin containing four modules. Maven artifact: `velocity/target/ERRSA-Velocity-Core-1.0.0.jar`.

| Module | Function | Config location |
|---|---|---|
| `compatibility-velocity` | Reports plugin inventory and Geyser/Floodgate/ViaVersion information | `plugins/errsa-velocity-core/compatibility-velocity/config.properties` |
| `web-bridge` | Publishes proxy players and RankProgression/EventBridge payloads | `plugins/errsa-velocity-core/web-bridge/config.properties` |
| `wiki-sync` | Writes a snapshot of proxy plugins to `docs/assets/wiki-sync/velocity.json` on startup | `plugins/errsa-velocity-core/wiki-sync/config.properties` |
| `auto-via-updater` | Stages ViaVersion updates using version checks and backup state | `plugins/errsa-velocity-core/auto-via-updater/config.toml` |

The main Velocity configuration is `plugins/errsa-velocity-core/config.properties`. The initial default enables compatibility reporting and WebBridge; wiki sync and the ViaVersion updater are initially disabled.

## Previous standalone settings

| Previous path | Core path |
|---|---|
| `plugins/errsa-compat-velocity.properties` | `compatibility-velocity/config.properties` |
| `plugins/ERRSAWebBridgeVelocity/config.properties` | `web-bridge/config.properties` |
| `plugins/errsa-wiki-sync/config.properties` | `wiki-sync/config.properties` |
| `plugins/autoviaupdater/config.toml` | `auto-via-updater/config.toml` |

New paths in the right column are relative to `plugins/errsa-velocity-core/`. Compatibility and website payloads retain the source integration's original data formats. `auto-via-updater` updates ViaVersion, not Velocity.
