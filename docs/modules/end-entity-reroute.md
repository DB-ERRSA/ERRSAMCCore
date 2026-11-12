# End entity reroute

Routes non-player entities leaving the End to a configured Overworld position. The default destination is `world` at `772, 64, 1533`. The search radius is 8 blocks. Players are not rerouted by this module.

`/endexit set` saves the player's current Overworld position to the module configuration. Alias: `/shulkerbay`.

Source: `EndEntityRerouteModule`, `EndExitRerouteListener`, `EndExitCommand`.

## Settings

Default values from `paper/src/main/resources/modules/end-entity-reroute/config.yml`:

```yaml
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
