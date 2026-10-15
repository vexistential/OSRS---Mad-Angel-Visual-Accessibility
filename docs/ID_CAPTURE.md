# Mad Angel Exploding Light — notes

## Hub stance

| Approach | Hub outlook |
|----------|-------------|
| Full tile fill / stand polygon on destination | **Likely rejected** — automatic stand / projectile-target indicator |
| Recolor existing floor shadow spotanim | **Preferred** — cosmetic remap of vanilla telegraph |

## Current behavior

- On player-stand bomb projectile (`4015`): find `GraphicsObject` on that tile (prefer Mad Angel bomb spotanims `4014`/`4016`/`4017`, else any gfx on the tile) and recolor its model faces to the style color each tick until finished.
- Bounce-back to Mad Angel: skipped (`targetActor` or footprint).
- Ball projectile: unchanged.

## If recolor does nothing in-fight

The looming shadow may not be a `GraphicsObject` on that tile. Check DevTools → Graphics Objects while the shadow is visible and note the ID on the destination tile; then we can pin `PREFERRED_SHADOW_SPOTANIMS` to that ID.
