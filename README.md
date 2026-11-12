# ERRSA MC Core

ERRSA's shared Paper and Velocity plugin. Core holds the features we maintain ourselves instead of running them as separate plugins.

| Component | Contents |
|---|---|
| `paper/` | Core commands, custom portals, End entity rerouting, PvP drops, combat diagnostics, inventory audit, compatibility reporting, website snapshots, wiki sync |
| `velocity/` | Compatibility reporting, website bridge, wiki sync, ViaVersion updater |

## Reference

- [Paper modules](docs/PAPER.md): what is included and where it lives
- [Commands](docs/COMMANDS.md): syntax, purpose, and permissions
- [Configuration](docs/CONFIG.md): root settings and module defaults
- [Velocity](docs/VELOCITY.md): integrated proxy features and paths
- [Architecture](docs/ARCHITECTURE.md): startup, configuration and source layout
- [Builds](docs/BUILDS.md): Maven outputs, GitHub artifacts and releases

## Build

From the repository root:

```powershell
mvn clean package
```

| Output | Runtime |
|---|---|
| `paper/target/ERRSA-MC-Core-2.5.6-combat-evidence-config.jar` | Paper, Java 21 |
| `velocity/target/ERRSA-Velocity-Core-1.0.0.jar` | Velocity, Java 17+ |

Development JARs: [GitHub Actions](https://github.com/DB-ERRSA/ERRSAMCCore/actions). Versioned builds: [Releases](https://github.com/DB-ERRSA/ERRSAMCCore/releases).
