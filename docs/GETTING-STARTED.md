# Getting started: server admins

## Which file goes where?

| Artifact | Where to upload | Required Java |
|---|---|---|
| `ERRSA-MC-Core-*.jar` | Paper server `plugins/` | Java 21 |
| `ERRSA-Velocity-Core-*.jar` | Velocity proxy `plugins/` | Java 17 or later |

Do **not** install both on the same server type. Back up the current JARs and plugin configuration directories first. Stop the server; replace the relevant old JAR with the new one; start and inspect console startup logs. Test on Test before Survival or the live proxy.

## Paper first run

Paper requires **Essentials** according to its `plugin.yml`. It lists ProtocolLib, GriefPrevention, PvpToggle, Floodgate, EventBridge, QuickShop, Dynmap and LuckPerms as optional dependencies. An optional dependency may still be needed for its particular module to work fully.

Main settings: `plugins/ERRSA-MC-Core/config.yml`. Each active feature has a separate config under its module's folder. Main module flags apply at startup: restart Paper after changes. Run module-specific reload only where the command implements it.

## Proxy first run

Read [Velocity](VELOCITY.md) before migrating. The combined plugin replaces four integrated standalone plugins; having both the originals and combined versions present can produce duplicate reports or writes. Back up the proxy `plugins/` folder and all old configurations before removing duplicates.

## Rollback

Stop the instance, restore the previous JAR and associated configuration backup, restart, inspect logs and confirm integrations. The proxy updater has staging and backup state that may require separate restoration.
