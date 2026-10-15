# Mad Angel Light Visuals

Accessibility-focused **recolor** of the Mad Angel (Wyrmscraig) **Exploding Light** floor shadow.

When the ball is aimed at a **player-stand destination**, the existing floor shadow is hidden and redrawn as the **same mesh** in a high-contrast curated color. The ball projectile is unchanged. Bounce-back toward the boss is not recolored.

## Features

- High-contrast recolor of the Exploding Light floor shadow (same shape)
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
| Recolor floor shadow | Enable shadow recolor |
| Shadow color | Magenta / Cyan / Orange / Lime |
