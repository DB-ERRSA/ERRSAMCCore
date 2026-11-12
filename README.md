# ERRSA-MC-Core

Two plugins, one source repository: the Paper server core and Velocity proxy core for the ERRSA Minecraft network.

**Build:** `mvn clean verify` from the repository root (JDK 21, Maven installed). The deployable artifacts are `paper/target/ERRSA-MC-Core-*.jar` and `velocity/target/ERRSA-Velocity-Core-*.jar` (do not deploy `original-*` jars).

**Download without building locally:** open [Actions](https://github.com/DB-ERRSA/ERRSAMCCore/actions/workflows/build.yml), select the latest successful run, and download the artifact ZIP containing both JARs. For tagged releases, use [Releases](https://github.com/DB-ERRSA/ERRSAMCCore/releases).

## Documentation

- [Quick start, installing and rollback](docs/GETTING-STARTED.md)
- [Paper module reference](docs/PAPER-MODULES.md)
- [Paper commands and permissions](docs/COMMANDS-AND-PERMISSIONS.md)
- [Configuration reference](docs/CONFIGURATION.md)
- [Velocity core](docs/VELOCITY.md)
- [Architecture and integration](docs/ARCHITECTURE.md)
- [Builds, releases, GitHub upload](docs/RELEASING.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)

> Check `docs/KNOWN-ISSUES.md` before deploying. Do not commit actual production credentials or database snapshots.
