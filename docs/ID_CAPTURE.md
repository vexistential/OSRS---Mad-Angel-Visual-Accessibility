# Mad Angel Exploding Light — notes

## Current behavior

- On player-stand bomb projectile (`4015`): use `ProjectileMoved`’s `LocalPoint` (instance-safe).
- Track loom gfx on that tile (pinned Mad Angel bomb spotanims, or unknown gfx in the first ~1s after throw).
- Each `BeforeRender`, recolor that GraphicsObject’s model **in place** (untexture + opaque + max sat/lum).
- Bounce-back to Mad Angel: skipped. Ball / unrelated late VFX: unchanged.

## Why not hide + RuneLiteObject?

`GraphicsObject.getModel()` returns a **shared temporary model buffer**. Assigning it to a `RuneLiteObject` made other actors (player, boss, random NPCs) appear on the shadow tile. In-place mutation for the same frame’s draw avoids that.

## Why not ModelOutlineRenderer / tile poly?

- Full-tile `getCanvasTilePoly` highlights the square, not the shadow mesh.
- `ModelOutlineRenderer.drawOutline(GraphicsObject)` often draws nothing visible for flat floor shadows (face culling / software raster path).
- Spec path: project the shadow model to canvas (`getClickbox` or `modelToCanvas` + `Jarvis.convexHull`) and stroke that hull.

## Pinned shadow candidates

| Constant | ID | Role |
|----------|----|------|
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW` | 4014 | Cast / ground candidate |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_01` | 4016 | Impact / loom candidate |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_02` | 4017 | Alternate impact / loom candidate |

## Capture

INFO logs: `Shadow capture gfx id=...` and `Tracking floor shadow gfx id=...`. Pin a confirmed loom ID into `SHADOW_SPOTANIMS` if needed; exclude dust IDs if the early window catches them.
