# Windows GitHub upload and release instructions

## Initial setup

Use the **cloned** working copy: `C:\Users\Stephen\IdeaProjects\ERRSA-MC-Core-GitHub`. It already has `.git` and `origin/master`, unlike the previous local folder. Copy this package's files into that cloned directory (merge, do not replace the source folders). The initial source copy was done with Robocopy; check status before committing.

```powershell
cd "C:\Users\Stephen\IdeaProjects\ERRSA-MC-Core-GitHub"
git config core.pager cat
git status
git add .github .gitignore README.md docs
git status --short
git diff --cached --stat
```

If the source changes are not yet committed, stage those only after checking for secrets and compiled/operational data with `git status --short`. When ready:

```powershell
git add -A
git diff --cached --stat
git commit -m "Add two-module CI and Core documentation"
git push origin master
```

**Do not force-push.** If the commit command says `nothing to commit`, check whether the preceding commit already included the files.

## Every update

```powershell
git add -A
git diff --cached --stat
git commit -m "Explain what changed"
git push
```

## Download development JARs

Open `https://github.com/DB-ERRSA/ERRSAMCCore/actions/workflows/build.yml`, choose latest successful run, and download the `ERRSA-Core-dev-N` artifact ZIP. It contains two deployable JARs. Run artifacts expire after 30 days; for long-term storage, use Releases.

## Official tagged release

After validating a release on Test, update the Maven/plugin version metadata and push your changes. Tag the tested commit with a meaningful repository release identifier, for example:

```powershell
git tag v2.5.6-combat-evidence-config
git push origin v2.5.6-combat-evidence-config
```

The release workflow compiles both modules, checks exactly one deployable JAR per module and uploads both to GitHub Releases, including `SHA256SUMS.txt`. Tag names label the combined repository snapshot; Paper/Velocity filenames retain their independent module versions.

**Important:** `release.yml` also supports manual runs; those create downloadable build artifacts but not an official GitHub Release.
