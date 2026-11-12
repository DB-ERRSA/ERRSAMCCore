# Known issues and release checks

1. **Version mismatch:** `paper/pom.xml` declares `2.5.6-combat-evidence-config`, but `paper/src/main/resources/plugin.yml` declares `2.5.5-portal-pvp-fixes`. Update `plugin.yml` to the intended release version before publishing a tagged release.
2. **No Java tests:** Maven reports `No tests to run` for both modules. Test deployments on staging with external integrations enabled carefully.
3. **Shading warnings:** Velocity's Maven Shade process reports duplicate META-INF resources/module descriptors; build succeeds but verify the resulting runtime plugin.
4. **Embedded binaries:** The project ZIP includes compiled SQLite classes/native libraries under Paper resources. These are source-controlled resources in the current layout; review provenance, license and necessity separately. Do not remove without testing.
5. **Credentials:** keep webhook URLs, tokens, GitHub credentials and operational data out of the public source repository. `.gitignore` cannot prevent committing a credential embedded in a tracked template; inspect `git diff --cached` before pushing.
6. **Maven release versioning:** Paper and Velocity maintain separate versions. The tag workflow takes the repository tag as the combined release label and preserves each module's own JAR filename. A tagged build does not rewrite either module version.
