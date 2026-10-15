# Mad Angel Light Visuals

Makes the Mad Angel (Wyrmscraig) **Exploding Light** floor shadow easier to see.

When the attack targets a player-stand destination, this plugin recolors the
existing floor shadow and draws a high-contrast outline around the shadow's
footprint. The ball projectile is unchanged. Bounce-back toward the Mad Angel
is not recolored.

## Features

- High-contrast recolor of the Exploding Light floor shadow
- Floor-parallel outline of the shadow footprint (not a full-tile highlight)
- Skips bounce-back toward the Mad Angel
- Does not alter the ball projectile
- Color choices: Magenta, Cyan, Orange, Lime

## Configuration

| Setting | Description |
|---------|-------------|
| Recolor floor shadow | Enable shadow recolor and outline |
| Shadow color | Magenta / Cyan / Orange / Lime |

## Development

```powershell
.\gradlew.bat run
```

Jagex accounts: follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## License

BSD-2-Clause. See [LICENSE](LICENSE).
