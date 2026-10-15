# Verification

## Automated / static

- [x] `compileJava` BUILD SUCCESSFUL
- [x] No `TelegraphOverlay` / tile polygon fill
- [x] No projectile remap
- [x] Bounce-back skip retained
- [x] Shadow recolor via GraphicsObject model face tint

## In-game (you)

1. Relaunch `.\gradlew.bat run`
2. Confirm:
   - [ ] Floor shadow on stand destinations looks brightly recolored (not a full tile square overlay)
   - [ ] Bounce-back under the angel is not highlighted
   - [ ] Ball looks vanilla
   - [ ] If shadow is unchanged, note DevTools GraphicsObject ID on that tile
