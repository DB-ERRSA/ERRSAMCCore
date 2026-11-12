# Architecture

```text
GitHub source repository
├── pom.xml                 parent aggregator (packaging=pom)
├── paper/pom.xml           Paper JAR, Java 21
│   ├── plugin.yml         command registration and dependencies
│   ├── config.yml         links, economy, reset labels, module switches
│   └── modules/           one default configuration per modular feature
└── velocity/pom.xml        Velocity JAR, Java 17
    └── src/main/resources  configuration templates
```

Paper entrypoint: `me.RedEagle3.eRRSAMCCore.ERRSAMCCore`. `CoreModuleManager` enables configured modules, handles per-module exceptions and disables modules in reverse order. `CoreCommandRegistrar` registers core commands. Paper has standalone command implementations and module-specific handlers.

The built-in Paper features include portals, non-player End entity rerouting, PvP drop unlocking, inventory auditing, combat diagnostics, compatibility reporting, web snapshots and wiki synchronization. Core command sources reside under `.../Commands`.

For the website integration, inspect `com.errsa.webbridge` packages, including event bridge, player, quickshop and snapshot services. For combat evidence, inspect `org.errsa.combatdiag` and its packet-correlation/JSONL writer classes.

Velocity combines proxy compatibility reporting, web snapshots, wiki sync and a ViaVersion updater. See `velocity/README.md` and the proxy implementation source before altering API or update behavior.

No automated Java unit tests ran in the supplied Maven build log (`No tests to run`). A successful compilation does not verify server behavior or external integrations.
