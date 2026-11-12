# Combat diagnostics

Captures combat evidence during manual watches and automatic incident windows. It records position samples, attack packet observations when ProtocolLib is available, trigger context, and incident summaries. It does not change damage or cancel attacks.

Automatic capture includes 12 seconds of buffered context and 8 seconds following a trigger by default. Packet observations are correlated to damage events within a configurable timing window. The recorded packet-processing positions and damage-event positions are separate measurements.

Command reference: [Combat diagnostics](../COMMANDS.md#combat-diagnostics). Source: `org.errsa.combatdiag`.

## Settings

Default values from `paper/src/main/resources/modules/combat-diagnostics/config.yml`:

```yaml
max-watch-minutes: 30
sample-every-ticks: 4
max-queued-records: 10000
retention-days: 7
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
```
