# Portal Disabler

A small NeoForge 1.21.1 mod that lets a server operator disable Nether and End portals independently or together.

## What it does

When a dimension is disabled:

- **Travel to that dimension is blocked.** Players who step into a Nether portal (or fall through an End portal) stay where they are.
- **Nether portals can't form** when Nether is disabled. Flint & steel and fire charges still light fire normally, but the purple portal blocks never spawn.
- **End portals can't open** when End is disabled. Ender eyes still slot into End portal frames and play their animation, but no portal blocks form even with all twelve eyes.
- **Existing portals are removed instantly** when you disable that dimension. Every loaded Nether/End portal block is replaced with air.

Disabling a destination dimension never traps players — leaving the Nether or the End back to the Overworld still works, since that travel targets the Overworld and isn't affected.

## Commands

| Command | Effect |
| --- | --- |
| `/disable` | Print the current state of both Nether and End |
| `/disable nether true` | Disable Nether; clear existing Nether portal blocks |
| `/disable nether false` | Re-enable Nether |
| `/disable end true` | Disable End; clear existing End portal blocks |
| `/disable end false` | Re-enable End |
| `/disable all true` | Disable both; clear all existing Nether/End portal blocks |
| `/disable all false` | Re-enable both |

Requires permission level 2 (op). State is persisted per-world via `SavedData`. Re-enabling does not restore previously cleared portals.

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
