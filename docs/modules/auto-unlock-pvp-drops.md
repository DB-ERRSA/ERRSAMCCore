# Auto-unlock PvP drops

When a player dies in eligible PvP, this module retries the GriefPrevention drop unlock after the death event. The default retries are at 5, 20 and 40 ticks. The victim must have PvP enabled according to PvPToggle, and the killer must be a player.

Source: `com.errsa.AutoUnlockPvPDrops`.

## Settings

Default values from `paper/src/main/resources/modules/auto-unlock-pvp-drops/config.yml`:

```yaml
conditions:
  require-player-killer: true
  require-victim-pvp-enabled: true
unlock:
  retry-delays-ticks: [5, 20, 40]
logging:
  successful-unlocks: true
```
