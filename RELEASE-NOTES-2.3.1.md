# ERRSA-MC-Core v2.3.1 — Console Moderation Compatibility

## Changes from v2.3.0

- Fixed null-issuer exceptions when EssentialsX mute/jail events are raised from console or automatic expiration.
- Added `ServerCommandEvent` handling for commands from the console / RCON, and attribution of actions as `CONSOLE`.
- Preserved usernames for player-issued actions. For unprompted automatic mute/jail expirations, issuer is `SYSTEM`.
- Extended successful ban/unban logging to commands issued by console/RCON, not just players. A scheduled ban-list check is used to avoid logging a failed unban.
- Replaced unreliable EssentialsX `UserKickEvent` auditing with a confirmed Bukkit `PlayerKickEvent` correlated with a recently observed `kick`/`ekick` command. EssentialsX v2.21.0 passes the arguments to its UserKickEvent constructor in the inverse order from its API accessor names, causing reversed attribution and console null exceptions.
- Prevented a null `discord-webhook` configuration value from causing a `trim()` exception.
- Updated bundled default `config.yml` URLs, reset date, empty webhook, and preserved End-return portal rerouting configuration based on the supplied server configuration.

## What is not changed

- `ERRSA-MC-Core` moderation logger does not execute or authorize moderation commands; this patch only changes audit listeners. Permissions and punishment semantics remain in the actual command-owning plugins.
- End-exit destination remains `world: 772,64,1533`, search radius 8. `/endexit set` and `/shulkerbay set` remain.
- An empty `discord-webhook:` disables Discord action logging. Set a real webhook URL to enable it.

## Validation

- `javac --release 21`: updated AdminCommandListener compiled using Paper/Essentials API-compatible local stubs.
- Focused regression harness passed: console/RCON mute, console jail, player and console kicks, ban processing, null-controller events, automatic-expiry attribution and short-lived attribution cleanup.
- YAML files parsed successfully.
- No full Maven build or live Paper 1.21.4 runtime test has been performed. Compile with Maven and validate on the test server before production rollout.

## Build

1. Open a terminal in this source directory.
2. Run `mvn clean package` with Java 21 and Maven installed, with Maven repository access.
3. Use the shaded `target/ERRSA-MC-Core-2.3.1.jar` (as produced by the Maven project); **back up the original JAR** and config first.
4. Stop the Paper server; replace ONLY the Core JAR, preserving `plugins/ERRSA-MC-Core/config.yml`; restart.
5. On a test account, try console `mute`, `unmute`, `kick`, `ban`, `pardon` and compare with a player-issued command. Test jail and automatic expiration independently. Confirm no `Could not pass event ... to ERRSA-MC-Core` errors.

## Scope / limitations

- `ServerCommandEvent` captures direct console/RCON command entry. Commands run internally via `Bukkit.dispatchCommand(...)` may bypass that event, so automatic attribution can fall back to `SYSTEM`; their punishments themselves are unaffected.
- Kick notifications are limited to observed kick commands, and do not log unrelated disconnects, automatic idle kicks, or anti-cheat kicks.
- The old WorldGuard-dependent ShulkerReroute plugin should remain removed when using Core's End-return redirection.
