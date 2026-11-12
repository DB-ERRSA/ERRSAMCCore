# Wiki sync

Publishes the server's plugin and permission inventory to a configured GitHub repository and branch. The `github` settings select the destination and authentication token.

Source: `edu.errsa.wikisync.WikiSyncModule`.

## Settings

Default values from `paper/src/main/resources/modules/wiki-sync/config.yml`:

```yaml
github:
  owner: "" # GitHub owner/user or organization.
  repo: "" # Destination repository name.
  branch: "main" # Existing target branch; use a dedicated testing branch.
  token: "" # Personal access token with minimum necessary permissions; private.
```
