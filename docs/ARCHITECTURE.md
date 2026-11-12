# Code layout

```text
pom.xml                       Maven parent/aggregator
paper/
  pom.xml                     Paper module (Java 21)
  src/main/java/
  src/main/resources/
    plugin.yml                Command registration, dependencies and plugin version
    config.yml                Root feature switches and core settings
    modules/<module>/config.yml
velocity/
  pom.xml                     Velocity module (Java 17 target)
  src/main/java/
  src/main/resources/         Proxy configuration templates
```

## Paper startup

`ERRSAMCCore` is the plugin entrypoint. `CoreCommandRegistrar` handles core command registration. `CoreModuleManager` loads enabled modules in this order:

1. End entity reroute
2. Custom portals
3. Inventory audit
4. Combat diagnostics
5. Auto-unlock PvP drops
6. Compatibility Paper
7. Web bridge
8. Wiki sync

Module configuration is written on first initialization through `saveDefaultConfig()`. Each enabled module receives `onEnable()`. Modules shut down in reverse order. A module startup exception is logged without stopping initialization of subsequent modules.

The integrated WebBridge and WikiSync modules are skipped if their corresponding standalone plugins (`ERRSAWebBridge` and `ERRSAWikiSync`) are present.

## Main code locations

| Feature | Source package / class |
|---|---|
| Root lifecycle | `me.RedEagle3.eRRSAMCCore` |
| Module manager | `...lifecycle.CoreModuleManager` |
| Core commands | `...Commands` |
| Custom portals | `...modules.CustomPortalModule`, `PortalAdminCommand`, `PortalPlayerListener`, `PortalRegionResolver` |
| End rerouting | `...Listeners.EndExitRerouteListener` |
| PvP drop unlock | `com.errsa.AutoUnlockPvPDrops` |
| Combat diagnostics | `org.errsa.combatdiag` |
| Inventory audit | `com.errsa.inventory` |
| Web bridge | `com.errsa.webbridge` |
| Compatibility reports | `com.errsa.compat.PaperReporter` |
| Wiki sync | `edu.errsa.wikisync.WikiSyncModule` |

## Build versions

Paper Maven version in this snapshot: `2.5.6-combat-evidence-config`. The `plugin.yml` version is still `2.5.5-portal-pvp-fixes`; the in-game version comes from `plugin.yml`, not the JAR filename. Velocity version: `1.0.0`.
