# ERRSA Velocity Core 1.0.0 — modular source integration

One Velocity plugin with **four modules**:

- **compatibility-velocity**: periodically reports Geyser, Floodgate, ViaVersion and plugin inventory to the existing compatibility API.
- **web-bridge**: publishes the proxy player list and original **base64-encoded** RankProgression/EventBridge data sources to the existing site API.
- **wiki-sync**: startup-only GitHub snapshot of Velocity plugins to `docs/assets/wiki-sync/velocity.json`.
- **auto-via-updater**: ports the supplied AutoViaUpdater Java source, preserving staging, backups, install-on-subsequent-restart logic, and version checks.

## Build

Open this directory as a Maven project in IntelliJ IDEA with Java 17+ and run:

```bash
mvn clean package
```

The shaded plugin JAR should be `target/ERRSA-Velocity-Core-1.0.0.jar`.
**This JAR was not built or tested in a running Velocity proxy in the creation environment.**

## Install and migration

1. Back up the **entire Velocity `plugins/` directory**, the old JARs, and all four configs.
2. Stop Velocity. Copy only the new shaded Core JAR to `plugins/` and **remove all four standalone JARs** to avoid duplicate reports and writes. Do not remove ViaVersion itself.
3. Start once with the main `plugins/errsa-velocity-core/config.properties`. Main settings default to compatibility + WebBridge on; WikiSync + auto-updating off for a safe first start.
4. Existing config migration uses the original paths, without overwriting user settings:
   - `plugins/errsa-compat-velocity.properties` -> `plugins/errsa-velocity-core/compatibility-velocity/config.properties`
   - `plugins/ERRSAWebBridgeVelocity/config.properties` -> `plugins/errsa-velocity-core/web-bridge/config.properties`
   - `plugins/errsa-wiki-sync/config.properties` -> `plugins/errsa-velocity-core/wiki-sync/config.properties`
   - `plugins/autoviaupdater/config.toml` -> `plugins/errsa-velocity-core/auto-via-updater/config.toml`
5. Verify endpoints, token/secret, Github branch, and ViaUpdater file paths. Enable WikiSync only on the intended destination; enable auto-update after checking ViaVersion is working.
6. Restart and inspect module startup messages. Validate external reports, payload schema, GitHub output, update staging, and rollback behavior on a TEST proxy before production.

## Important limitations

- This is a **source integration**, not a production-verified binary. No proxy integration test was run.
- WikiSync and WebBridge payloads are reconstructed from the supplied JAR bytecode and preserve the observed schemas/paths; compare output to the deployed services before production.
- `auto-via-updater` updates **ViaVersion**, not Velocity itself.
- The four old JARs must not remain enabled alongside this plugin.
- No credentials from the supplied configs are included in the distributed source ZIP.
- `config.toml` uses module-specific path, so AutoViaUpdater's pre-existing state/backups may need manual migration. Do not delete old backups.
