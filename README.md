# Mad Angel Light Visuals

Accessibility-focused **cosmetic remaps** for the Mad Angel (Wyrmscraig) **Exploding Light** attack.

This plugin does **not** tell you where to stand, predict landings, or draw tile overlays. It only replaces the game’s already-spawned floor telegraph and/or light-ball projectile with brighter curated vanilla visuals on the **same** tile / trajectory.

## Features

- Amplify the Exploding Light floor shadow telegraph (brighter marker on the same tile)
- Optionally remap the flying light-ball projectile to a curated brighter vanilla projectile
- Style enum only (Magenta / Cyan / Orange / Lime) — no free-form GFX ID input

## Requirements

- RuneLite with Plugin Hub support (or run locally via Gradle)
- Access to Mad Angel (quest *Fallen From Grace* or post-quest repeatable fight)

## Local development

1. Java 11+
2. `./gradlew run` (Windows: `gradlew.bat run`)
3. Jagex accounts: follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
4. Enable **Developer Tools → Graphics Objects / Projectiles** if verifying spotanim IDs (see [docs/ID_CAPTURE.md](docs/ID_CAPTURE.md))

## Configuration

| Setting | Description |
|---------|-------------|
| Amplify floor shadow | Hide the vanilla shadow gfx and show a brighter marker |
| Remap light ball | Replace projectile spotanim `4015` with a curated style |
| Visual style | Magenta / Cyan / Orange / Lime |

## Policy notes

Intended as a visual remapping plugin in the same family as Vardorvis Projectiles / Projectile Override:

- No automatic stand / don’t-stand indicators
- No projectile landing prediction overlays
- No cleave / smite / prayer helpers

Hub acceptance is still at reviewer discretion for boss-related plugins.

## In-fight ID verification

Gameval names the bomb impact spotanims `4016` / `4017`. Confirm which is the looming shadow vs bounce/explosion during a kill — see [docs/ID_CAPTURE.md](docs/ID_CAPTURE.md).
