# Custom portals

Region-triggered teleports replacing the previous portal Skript. Triggers can be WorldGuard regions or cuboids. The listener checks entry, applies the configured teleport delay, and can cancel the pending teleport when the player leaves the region.

## Current portal IDs

| ID | WorldGuard region | Destination |
|---|---|---|
| `tutorial-dojo-entrance` | `tutorial_dojo_entrance` | world: -53.5, -35, 4.5 |
| `tutorial-dojo-exit` | `tutorial_dojo` | world: 59.5, 15, 15.5 |
| `legacy-lake-entrance` | `legacy_lake_entrance` | world: 0.5, -34, -1.5 |
| `legacy-lake-exit` | `legacy_lake` | world: 8.5, 0, 60.5 |
| `guild-hall-entrance` | `guild_hall_entrance` | world: 59.5, -53, 31.5 |
| `guild-hall-exit` | `guild_hall_exit` | world: -39.5, 11, -33.5 |

Legacy Lake entrance uses a zero-tick delay; Legacy Lake exit sets `forward-velocity: 2.5`. The common defaults use a 20-tick delay and 40-tick cooldown. `/coreportal` supports creating cuboids from two selected corners; see [commands](../COMMANDS.md#portal-commands).

Source: `CustomPortalModule`, `PortalAdminCommand`, `PortalPlayerListener`, `PortalRegionResolver`.

## Settings

Default values from `paper/src/main/resources/modules/custom-portals/config.yml`:

```yaml
settings:
  check-interval-ticks: 1
  default-delay-ticks: 20
  cooldown-ticks: 40
  cancel-on-exit: true
  entry-sound: ENTITY_EXPERIENCE_ORB_PICKUP
  teleport-sound: ENTITY_ENDERMAN_TELEPORT
  cancel-sound: ITEM_AXE_STRIP
  particles-enabled: true
  arrival-effects-delay-ticks: 2
  particle: HAPPY_VILLAGER
  particle-count: 30
  warp-bypass-ticks: 40
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
