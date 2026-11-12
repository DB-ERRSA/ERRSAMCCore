# Troubleshooting

| Symptom | Likely cause | What to check |
|---|---|---|
| `not a git repository` | Running Git from old folder | Switch to `ERRSA-MC-Core-GitHub` cloned working directory |
| `Child module ... velocity/pom.xml does not exist` | Incomplete checkout/project folder | Confirm `paper/pom.xml`, `velocity/pom.xml` and root `pom.xml` exist |
| `No tests to run` | No compiled Java test sources | This is a warning about coverage, not a build failure |
| Build succeeds with Shade warnings | Duplicate dependency metadata | Test startup and check plugin compatibility; avoid indiscriminate exclusion |
| Actions finds zero JARs | Missing module, failed compile, wrong path | Inspect Maven logs, check `paper/target` and `velocity/target` |
| Plugin loads, module does not | Module disabled or initialization error | Review `modules.<name>.enabled` and severe startup log messages |
| Wiki/Web bridge silent | Standalone duplicate or bad credentials | Check integrated module conflict log and secret destinations |
| Missing external updates | Test/production endpoints differ | Check proxy/Paper module configs and API response status |
| Paper version in `/plugins` differs from JAR | `plugin.yml` not aligned with Paper POM | Correct `plugin.yml` version and rebuild |
| Can't download from Actions | Run failed, artifacts expired, or access restricted | Choose successful run; use Releases for permanent downloads |

For crash reports, preserve the relevant console stack trace, server version, Java version, plugin list, and **redacted** config. Do not share webhooks or API secrets.
