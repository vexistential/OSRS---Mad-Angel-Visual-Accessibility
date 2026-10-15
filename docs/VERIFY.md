# Verification

## Automated / static

- [x] `compileJava` BUILD SUCCESSFUL
- [x] No `TelegraphOverlay` / tile polygon fill
- [x] No projectile remap
- [x] Bounce-back skip retained
- [x] Every-frame `ShadowRecolor` via `BeforeRender`
- [x] Hide vanilla shadow + same-shape `RuneLiteObject` clone
- [x] No arbitrary tile-VFX recolor fallback

## In-game (you)

1. Relaunch `.\gradlew.bat run`
2. Confirm:
   - [ ] Stand destinations: floor shadow itself is neon / high-contrast (same footprint as vanilla loom)
   - [ ] Bounce-back under the angel is not recolored
   - [ ] Ball looks vanilla
   - [ ] Sword-slash dust is not recolored
3. If shadow unchanged, check debug logs for `Shadow capture gfx id=` on the destination tile and pin that ID.
