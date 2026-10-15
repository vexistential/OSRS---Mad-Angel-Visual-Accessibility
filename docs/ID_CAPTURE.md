# Mad Angel Exploding Light — notes

## Current behavior

- On player-stand bomb projectile (`4015`): track destination tile; attach high-contrast clone for pinned shadow spotanims on that tile.
- Vanilla shadow draw is hidden (`RenderCallback.addEntity`); same animated mesh is tinted every `BeforeRender` and shown via `RuneLiteObject`.
- Bounce-back to Mad Angel: skipped (`targetActor` or footprint).
- Ball projectile and unrelated VFX: unchanged (no “any gfx on tile” fallback).

## Pinned shadow candidates

| Constant | ID | Role |
|----------|----|------|
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW` | 4014 | Cast / ground candidate |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_01` | 4016 | Impact / loom candidate |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_02` | 4017 | Alternate impact / loom candidate |

## Capture (if shadow still dark)

With `--debug` / developer mode, look for `Shadow capture gfx id=... onPendingDestination=true` during a stand-destination bomb. If the loom shadow uses an ID outside the table above, pin that ID into `SHADOW_SPOTANIMS` in `MadAngelVisualsPlugin`.
