# Portal Disabler

A small NeoForge 1.21.1 mod that lets a server operator disable portal travel and portal creation across all dimensions.

## What it does

When portals are disabled:

- **Dimension travel is blocked.** Nether portals, end portals, and any modded dimension portal that goes through Minecraft's standard dimension-change pipeline will refuse to take entities through.
- **Nether portals can't form.** Flint & steel and fire charges still light fire normally, but the purple portal blocks never spawn.
- **End portals don't open.** Ender eyes still slot into end portal frames and play their animation, but no portal blocks form even with all twelve eyes.
- **Existing portals are removed instantly.** When you disable, every existing nether/end portal block in every loaded chunk is replaced with air.

## Commands

| Command | Effect |
| --- | --- |
| `/disableportals` | Prints the current state ("Portals are disabled" / "Portals are enabled") |
| `/disableportals true` | Disable portals; clears existing portal blocks |
| `/disableportals false` | Re-enable portals (does not restore previously cleared portals) |

Requires permission level 2 (op). State is persisted per-world via `SavedData`.

## Compatibility

- Minecraft `1.21.1`
- NeoForge `21.1.213`+
- Java `21`

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.

## License

MIT — see [LICENSE](LICENSE).
