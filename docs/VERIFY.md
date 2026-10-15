# Verification

## Automated / static

- [x] `compileJava` BUILD SUCCESSFUL
- [x] No tile polygon fill
- [x] No projectile remap
- [x] No `RuneLiteObject` / shared-`getModel()` clone
- [x] In-place `BeforeRender` recolor (opaque faces + boosted HSL brightness)
- [x] Shadow mesh outline via vertex convex hull, stroke only (no fill; not clickbox AABB / tile poly / ModelOutlineRenderer)
- [x] Bounce-back skip retained
- [x] Instance-safe projectile `LocalPoint`

## In-game (you)

1. Relaunch `.\gradlew.bat run`
2. Confirm:
   - [ ] No player/NPC clones spawning on the shadow tile
   - [ ] Stand-destination floor shadow is recolored and outlined along its mesh (not a full tile square)
   - [ ] Bounce-back under the angel is not recolored/outlined
   - [ ] Ball looks vanilla
   - [ ] Sword-slash dust is not recolored (or note ID if early window catches it)
