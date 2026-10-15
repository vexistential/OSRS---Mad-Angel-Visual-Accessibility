# Verification

## Automated / static (done)

- [x] `gradlew compileJava` — BUILD SUCCESSFUL
- [x] No `Perspective` / `getCanvasTilePoly` / overlay tile drawing in `src/`
- [x] No free-form ID config fields (style enum only)
- [x] Projectile remap uses Vardorvis/`createProjectile` pattern
- [x] Shadow amplify hides original gfx + same-tile `RuneLiteObject` marker

## In-game (requires your account — cannot be automated)

Jagex rules forbid automating game input. Please:

1. `gradlew.bat run` from the project root
2. Log in ([Jagex Accounts wiki](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) if needed)
3. Enable the plugin **Mad Angel Light Visuals**
4. Fight Mad Angel and confirm:
   - [ ] Floor shadow is clearly brighter for all **3** bounces
   - [ ] Ball remap (if enabled) is visible
   - [ ] Enrage still looks correct
   - [ ] Quest and/or post-quest NPC forms work
   - [ ] Disabling the plugin restores vanilla visuals
5. Optional: DevTools capture per [ID_CAPTURE.md](ID_CAPTURE.md) to narrow 4016 vs 4017
