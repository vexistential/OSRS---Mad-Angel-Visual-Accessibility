# Mad Angel Light Visuals

Accessibility-focused **recolor** of the Mad Angel (Wyrmscraig) **Exploding Light** floor shadow.

When the ball is aimed at a **player-stand destination**, the existing floor shadow spotanim is tinted a bright curated color in place each frame, with a vibrant border around that shadow tile. The ball projectile is unchanged. Bounce-back toward the boss is not recolored.

## Features

- High-contrast in-place recolor of the Exploding Light floor shadow
- Vibrant border around tracked shadow tiles
- Skip bounce-back toward the Mad Angel
- Vanilla ball projectile
- Color enum (Magenta / Cyan / Orange / Lime)

## Local development

```powershell
.\gradlew.bat run
```

Jagex accounts: follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Configuration

| Setting | Description |
|---------|-------------|
| Recolor floor shadow | Enable shadow tinting |
| Shadow color | Magenta / Cyan / Orange / Lime |
