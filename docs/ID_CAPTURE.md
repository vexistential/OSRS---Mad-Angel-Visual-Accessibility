# Mad Angel Exploding Light — ID capture notes

## Gameval (fact — RuneLite SpotanimID)

| Constant | ID | Likely role |
|----------|----|-------------|
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW` | 4014 | Cast VFX |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE` | 4015 | Flying ball (**remapped by plugin**) |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_01` | 4016 | Ground impact / shadow candidate (**amplified**) |
| `VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_02` | 4017 | Alternate impact / bounce candidate (**amplified**) |

Animations (reference): `NPC_MAD_ANGEL_ATTACK_BOMB_THROW` = 14443, VFX 14444–14447.

NPC combat IDs reported on wiki / earlier gameval dumps: post-quest **16305** / **16307**, quest **16309** / **16311**. Plugin gates on **spotanim IDs only** (Mad Angel–specific), so NPC constants are not required.

## In-fight checklist (DevTools)

Do this once in a live fight to confirm shadow IDs:

1. Enable **Developer Tools → Graphics Objects** and **Projectiles**.
2. Trigger **Exploding Light** (blast / bouncing ball).
3. For each of the three bounces, note:
   - Projectile ID (expect **4015**)
   - GraphicsObject ID(s) on the destination tile when the looming shadow appears
   - Whether **4016**, **4017**, both, or another ID is the shadow
4. Enrage: confirm the same IDs still apply when the ball is faster.
5. If only one of 4016/4017 is the shadow, narrow `SHADOW_SPOTANIMS` in `MadAngelVisualsPlugin` to that ID so explosion VFX is left vanilla.

## Capture log template

```
Date:
Client version:
Quest / post-quest:
Bounce 1: projectile=  gfx on shadow tile=  notes=
Bounce 2: projectile=  gfx on shadow tile=  notes=
Bounce 3: projectile=  gfx on shadow tile=  notes=
Enrage same IDs? Y/N
```

## Status

- **Projectile 4015:** high confidence (gameval name `..._PROJECTILE`)
- **Shadow = 4016 and/or 4017:** medium confidence until DevTools capture confirms; plugin currently amplifies both impact spotanims as the Hub-safe “existing telegraph” remap
