# Configuration reference

Source defaults from this project snapshot. **Never paste secrets from a live server into GitHub.** Paths below are under `paper/src/main/resources`; deployed configuration locations are controlled by the plugin implementation.

## Main Paper config (`config.yml`)

```yaml
# ERRSA MC Core | Main Settings
# Switches below apply at startup. Restart Paper after changing them.
# Each enabled feature has its own config.yml in the matching folder.
# Disable integrations you do not want running on the Testing Server.
modules:
  inventory-audit:
    enabled: true # Records suspicious inventory changes; does not punish players.
  combat-diagnostics:
    enabled: true # Evidence-only PvP review; does not block attacks.
  auto-unlock-pvp-drops:
    enabled: true # Cooperates with GriefPrevention and PvPToggle.
  compatibility-paper:
    enabled: true # Sends reports to your bot. Check test/production endpoint.
  web-bridge:
    enabled: true # Writes website/Dynmap JSON snapshots.
  wiki-sync:
    enabled: true # WARNING: may publish live GitHub content on startup.
  end-entity-reroute:
    enabled: true # Moves non-player End exit arrivals; players remain unaffected.
  custom-portals:
    enabled: true # WorldGuard/cuboid staff portals; replaces portal Skript.

# Public links displayed in Core commands and menus.
links:
  discord: "https://discord.com/invite/XvKYDe3pGX"
  feedback: "https://forms.fillout.com/t/s2oKYJtMGDus"
  report: "https://forms.fillout.com/t/eRqvr1KnD4us"
  website: "https://errsa-minecraft-handbook.onrender.com/"
  map: "https://errsa-minecraft-handbook.onrender.com/map.html"

# Diamond sell command value, in server economy dollars.
economy:
  diamond-sell-price: 50.0

# Display text for /resetschedule; these are labels, not automated reset times.
reset:
  date: "August 2027"
  cycle: "3"

# Paste secrets in the SERVER config only. Never commit or share the webhook.
moderation:
  discord-webhook: ""
```

## end-entity-reroute

Source: `paper/src/main/resources/modules/end-entity-reroute/config.yml`

```yaml
# End Entity Reroute | Only non-player entities exiting the End.
# Keep root modules.end-entity-reroute.enabled true to start this module.
end-exit-reroute:
  enabled: true # Additional module-local switch.
  safe-destination:
    world: world # Existing loaded destination world name.
    x: 772 # Destination X coordinate.
    y: 64.0 # Destination Y coordinate.
    z: 1533 # Destination Z coordinate.
    yaw: 0.0 # Facing direction in degrees.
    pitch: 0.0 # Vertical facing direction in degrees.
  search-radius: 8 # Look for a safe landing spot near destination if blocked.
  debug: false # Log extra diagnostics during testing; disable for normal play.
```

## custom-portals

Source: `paper/src/main/resources/modules/custom-portals/config.yml`

```yaml
# Custom Portals | Replaces the previous Custom_Portal.sk.
# Create/edit staff portals with /coreportal; restart after manual YAML edits.
# WorldGuard trigger needs a matching region name in the stated world.
# The six preconfigured destinations are intentionally unchanged.
# All delay values are in server ticks (20 ticks ~= 1 second at 20 TPS).
# Trigger type: cuboid or worldguard (requires WorldGuard installed).
# New portals can be added using /coreportal commands.
settings:
  # Periodic scan fallback; 1 tick helps narrow regions. Movement events are also monitored.
  check-interval-ticks: 1
  default-delay-ticks: 20
  cooldown-ticks: 40
  # Cancel a pending delayed teleport if the player leaves its portal region.
  cancel-on-exit: true
  entry-sound: ENTITY_EXPERIENCE_ORB_PICKUP
  teleport-sound: ENTITY_ENDERMAN_TELEPORT
  cancel-sound: ITEM_AXE_STRIP
  # Spawn particles when teleporting; arrival effects can be delayed to reach clients.
  particles-enabled: true
  arrival-effects-delay-ticks: 2
  particle: HAPPY_VILLAGER
  particle-count: 30
  # Prevent teleport loops or unintended activation immediately after /warp.
  warp-bypass-ticks: 40
# Portal IDs can be changed only if staff commands and references are updated.
# Each portal supports enabled, trigger, region, world, delay-ticks,
# destination (world/x/y/z/yaw/pitch), enter-message and forward-velocity.
portals:
  tutorial-dojo-entrance:
    enabled: true
    trigger: worldguard
    region: tutorial_dojo_entrance
    world: world
    destination:
      world: world
      x: -53.5
      y: -35
      z: 4.5
      yaw: -180
      pitch: 0
    enter-message: "&aEntering Tutorial Dojo..."
  tutorial-dojo-exit:
    enabled: true
    trigger: worldguard
    region: tutorial_dojo
    world: world
    destination:
      world: world
      x: 59.5
      y: 15
      z: 15.5
      yaw: -180
      pitch: 0
    enter-message: "&cLeaving Tutorial Dojo..."
  legacy-lake-entrance:
    enabled: true
    trigger: worldguard
    region: legacy_lake_entrance
    world: world
    delay-ticks: 0 # Immediate teleport for narrow Legacy Lake entrance.
    destination:
      world: world
      x: 0.5
      y: -34
      z: -1.5
      yaw: 0
      pitch: 0
    enter-message: "&aEntering Legacy Lake..."
  legacy-lake-exit:
    enabled: true
    trigger: worldguard
    region: legacy_lake
    world: world
    destination:
      world: world
      x: 8.5
      y: 0
      z: 60.5
      yaw: 180
      pitch: 0
    enter-message: "&cLeaving Legacy Lake..."
    forward-velocity: 2.5 # Push player forward after Legacy Lake exit.
  guild-hall-entrance:
    enabled: true
    trigger: worldguard
    region: guild_hall_entrance
    world: world
    destination:
      world: world
      x: 59.5
      y: -53
      z: 31.5
      yaw: -90
      pitch: -2
    enter-message: "&aEntering Guild Hall..."
  guild-hall-exit:
    enabled: true
    trigger: worldguard
    region: guild_hall_exit
    world: world
    destination:
      world: world
      x: -39.5
      y: 11
      z: -33.5
      yaw: 0
      pitch: 10
    enter-message: "&cLeaving Guild Hall..."
```

## inventory-audit

Source: `paper/src/main/resources/modules/inventory-audit/config.yml`

```yaml
# Inventory Audit | Evidence-only inventory change investigation.
# The HIGH threshold keeps routine observations out of the console.
logging:
  console-min-severity: HIGH # Print HIGH/CRITICAL incidents to console.
  file-min-severity: MEDIUM # Persist MEDIUM and above to the log file.
  write-routine-events: false # Set true temporarily while debugging.
  log-file: audit/incidents.log # Relative to inventory-audit folder.

noise-control:
  world-change-grace-ticks: 40 # Allow inventory swap plugins to finish after world changes.
  gamemode-change-grace-ticks: 20 # Avoid false alerts after game mode swaps.
  join-grace-ticks: 60 # Allow other plugins to restore inventory on login.
  respawn-grace-ticks: 40 # Let death/respawn plugins complete changes.
  duplicate-window-ticks: 100 # Collapse repeated observations to one incident.
  per-player-code-cooldown-seconds: 30 # Prevent alert spam by one player/code.
  remembered-context-minutes: 30 # Prune old comparison hashes from RAM.

checks:
  old-hash-in-same-context: true # Advisory: older inventory fingerprint reappeared.
  shulker-content-overlap: true # Compare shulker contents against loose items.
  minimum-shulker-overlap-items: 16 # Threshold of overlap before review.
  minimum-single-fingerprint-mirror-items: 256 # Higher bar for single-item shulkers.
  respawn-destination-change: true # Informational respawn location changes.

performance:
  cleanup-period-ticks: 1200 # Remove stale state approximately every minute.
  async-file-writer: true # Avoid disk writing on server tick thread.
  max-file-queue: 10000 # Upper bound for pending incident writes.
```

## combat-diagnostics

Source: `paper/src/main/resources/modules/combat-diagnostics/config.yml`

```yaml
# Combat Diagnostics | Evidence-only PvP investigation (v0.5.1 logic)
# No bans, cancellations or damage changes. Evidence requires staff interpretation.
# Commands: /cdiag health | packet | incidents | watch | mark | reload.

# Upper bound for manual /cdiag watch <player> <minutes>.
max-watch-minutes: 30
# Capture one position sample every N server ticks (20 ticks ~= 1 second at 20 TPS).
sample-every-ticks: 4
# Bound RAM used by asynchronous JSONL evidence writing.
max-queued-records: 10000
# Retention for files in combat-diagnostics/logs/.
retention-days: 7
# Staff review threshold for current server hitbox distance, NOT client reach.
review-distance-blocks: 3.2

automatic:
  enabled: true # Enable automatic incident capture; never enforces punishment.
  capture-console-alerts: true # Best effort for Themis/ExploitFixer logger text.
  pre-seconds: 12 # Buffered evidence before a trigger.
  post-seconds: 8 # Additional evidence after a trigger.
  cooldown-seconds: 20 # Prevent repeated incident alerts for the same cause.
  max-buffered-players: 100 # Bound memory for online player samples.
  trigger-hit-distance: 3.6 # Advisory server-side distance threshold in blocks.
  rapid-firework-click-ms: 160 # Flag unusually quick inventory-to-firework use.
  hit-dedup-ms: 40 # Suppress duplicate hit paths observed close together.
  notify-staff: true # Notify online staff with errsa.combatdiag.staff permission.

discord:
  enabled: false # Leave false unless you want limited alert notifications.
  webhook-url: "" # Private Discord webhook. Keep out of source control and ZIPs.
  min-seconds-between-alerts: 30 # Rate limit outgoing notifications.

packet-observation:
  enabled: true # Requires ProtocolLib 5.x; otherwise Bukkit-only records.
  match-window-ms: 400 # Max age of packet receipt relative to damage event.
  reorder-tolerance-ms: 50 # Allow small reversed receipt/damage timestamp order.
  correlation-delay-ticks: 2 # Wait for packet callbacks queued to the main thread.
  require-match-for-distance-trigger: false # True suppresses uncorrelated distance triggers.
  # Quality values: packet-correlated / timing-ambiguous / uncorrelated.
  # Packet positions are captured on the MAIN-THREAD PROCESSING tick,
  # not at exact network receipt time; damage positions are captured separately.
  # None of these measurements are rollback or lag-compensated evidence.
```

## auto-unlock-pvp-drops

Source: `paper/src/main/resources/modules/auto-unlock-pvp-drops/config.yml`

```yaml
# Auto-Unlock PvP Drops
# This module integrates PvPToggle and GriefPrevention.
# The module enable switch remains in ERRSA-MC-Core/config.yml.

conditions:
  # Only unlock when a player killed the victim (not mobs, void or falls).
  require-player-killer: true

  # Check that PvPToggle reports the victim had PvP enabled.
  # Keep true for normal ERRSA PvP rules.
  require-victim-pvp-enabled: true

unlock:
  # Retry after GriefPrevention records the death. 20 ticks = 1 second.
  # The original behavior was 5, 20 and 40 ticks.
  retry-delays-ticks: [5, 20, 40]

logging:
  # Write one message per successful retry to the server log.
  successful-unlocks: true
```

## compatibility-paper

Source: `paper/src/main/resources/modules/compatibility-paper/config.yml`

```yaml
# Compatibility Paper | Sends compatibility reports to the ERRSA bot.
# Configure the TEST endpoint before enabling on Testing Server.
# Endpoint must be HTTPS and accept the report format expected by your bot.
endpoint: "https://YOUR-BOT-HOST/compat/report"
# Must match COMPAT_REPORT_SECRET on the receiving bot; never share this secret.
secret: "REPLACE_WITH_LONG_RANDOM_SECRET"
# Time between scheduled reports in seconds (300 = 5 minutes).
report-interval-seconds: 300
```

## web-bridge

Source: `paper/src/main/resources/modules/web-bridge/config.yml`

```yaml
# WebBridge | Publishes website data consumed through Dynmap.
# Interval in seconds. More frequent updates create more disk I/O.
sync-interval-seconds: 15

# Paths from the SERVER ROOT, not from this module folder.
# Ensure the associated plugins have actually created these databases.
guilds-db: plugins/Guilds/guilds.db
quests-db: plugins/PlayerQuests/quests.db

# Dynmap-facing player/guild/quest JSON snapshot output.
output-file: plugins/dynmap/web/errsa-paper.json

# Other outputs generated by this module:
#   plugins/dynmap/web/errsa-eventbridge.json
#   plugins/dynmap/web/errsa-shop-stock.json
# QuickShop cache is kept in web-bridge/shop-stock-cache.properties.
```

## wiki-sync

Source: `paper/src/main/resources/modules/wiki-sync/config.yml`

```yaml
# Wiki Sync | Publishes server plugin/permission inventory to GitHub.
# WARNING: Enabling on Testing can overwrite the production wiki snapshot.
# Use a separate testing branch/repository until the integration is verified.
github:
  owner: "" # GitHub owner/user or organization.
  repo: "" # Destination repository name.
  branch: "main" # Existing target branch; use a dedicated testing branch.
  token: "" # Personal access token with minimum necessary permissions; private.
```
