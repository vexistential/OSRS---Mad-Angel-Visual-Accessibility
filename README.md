# Mad Angel Light Visuals

Accessibility-focused **recolor** of the Mad Angel (Wyrmscraig) **Exploding Light** floor shadow.

When the ball is aimed at a **player-stand destination**, the existing floor shadow spotanim on that tile is tinted a bright curated color. The ball projectile is unchanged. Bounce-back toward the boss is not recolored.

## Why not a full tile highlight?

Jagex’s third-party client guidelines and RuneLite Hub policy treat automatic stand-tile / projectile-landing highlights as prohibited for bosses. Recoloring the game’s own shadow telegraph is the Hub-safer approach (same family as cosmetic projectile remaps).

## Features

- Recolor the Exploding Light floor shadow on stand destinations
- Skip bounce-back toward the Mad Angel
- Vanilla ball projectile
- Color enum (Magenta / Cyan / Orange / Lime)

## Local development

```powershell
.\gradlew.bat run
```

## Configuration

| Setting | Description |
|---------|-------------|
| Recolor floor shadow | Enable shadow tinting |
| Shadow color | Magenta / Cyan / Orange / Lime |
