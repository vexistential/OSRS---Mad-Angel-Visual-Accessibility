# Plugin Hub submission checklist

## Prepared in this repo

- [x] Public-ready plugin source (`com.madangelvisuals`)
- [x] BSD-2 `LICENSE`
- [x] Accessibility-framed `README.md`
- [x] `icon.png` (48×48)
- [x] `runelite-plugin.properties` with `build=standard`
- [x] Marker stub at `plugin-hub/mad-angel-light-visuals`

## Steps for you

1. Create a **public** GitHub repository (suggested name: `mad-angel-light-visuals`).
2. Push this project (exclude `runs/` if desired via `.gitignore`).
3. Note the full 40-character commit SHA of the tip you want published.
4. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
5. Add `plugins/mad-angel-light-visuals` with:

```
repository=https://github.com/<your-user>/mad-angel-light-visuals.git
commit=<full-40-char-sha>
```

6. Open a PR. In the description, emphasize:
   - Cosmetic remap of **already-spawned** Exploding Light visuals
   - No tile overlays / stand indicators / landing prediction
   - Precedent: Vardorvis Projectiles / Projectile Override
7. Fix CI feedback; update `commit=` in the same PR as needed.

## Attribution footer (from example-plugin AGENTS.md)

```
Generated-by: Cursor Agent
```
