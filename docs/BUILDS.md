# Builds and releases

## Local build

```powershell
mvn clean package
```

The parent POM builds both Maven modules. The shaded deployable JARs are:

- `paper/target/ERRSA-MC-Core-2.5.6-combat-evidence-config.jar`
- `velocity/target/ERRSA-Velocity-Core-1.0.0.jar`

## GitHub

| Workflow | Trigger | Output |
|---|---|---|
| `build.yml` | Push to `master` / manual run | Downloadable build artifact containing both JARs |
| `release.yml` | Version tag beginning with `v` | GitHub Release with both JARs and SHA-256 checksums |

[Actions](https://github.com/DB-ERRSA/ERRSAMCCore/actions) · [Releases](https://github.com/DB-ERRSA/ERRSAMCCore/releases)

To publish source changes:

```powershell
cd "C:\Users\Stephen\IdeaProjects\ERRSA-MC-Core-GitHub"
git add -A
git commit -m "Describe the change"
git push origin master
```

Version tags are separate from the Maven versions in `paper/pom.xml` and `velocity/pom.xml`.
