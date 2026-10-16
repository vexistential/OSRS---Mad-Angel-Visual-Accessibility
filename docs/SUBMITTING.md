# Submitting to the Plugin Hub

## Checklist

- [x] Public-ready plugin source (`com.madangelvisuals`)
- [x] BSD-2-Clause `LICENSE`
- [x] User-facing `README.md`
- [x] `icon.png` (PNG, within hub size limits)
- [x] `runelite-plugin.properties` with `build=standard`
- [ ] Public GitHub repository
- [ ] Plugin Hub PR with `repository=` and tip `commit=`

## Steps

1. Create a **public** GitHub repository (suggested name: `mad-angel-visual-accessibility`).
2. Push this project.
3. Note the full 40-character commit SHA of the tip you want published
   (`git rev-parse HEAD`).
4. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
5. Add `plugins/mad-angel-visual-accessibility` with:

```
repository=https://github.com/<your-user>/mad-angel-visual-accessibility.git
commit=<full-40-char-sha>
```

6. Open a pull request describing the plugin:
   - Cosmetic remapping of already-spawned Exploding Light floor shadows for accessibility
   - Outline follows the shadow footprint, not a full tile fill
   - No landing prediction, prayer helpers, or combat automation
7. Address CI / reviewer feedback; update `commit=` in the same PR as needed.

A local marker stub lives at [`plugin-hub/mad-angel-visual-accessibility`](../plugin-hub/mad-angel-visual-accessibility)
for copy-paste into the hub fork.
