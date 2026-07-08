# Spleef

A classic Spleef minigame plugin for Paper Minecraft servers. Break snow beneath other players to eliminate them — last player (or team) standing wins.

## Features

- **Multiple game modes** — FFA, Solos, Duos, Trios, Quads
- **Arena management** — Create arenas in-game with selection tools
- **Team support** — Color-coded teams with friendly-fire immunity
- **Block-based economy** — Break a block → get a snowball
- **Snowball knockback** — Snowballs knock opponents away (no friendly fire)
- **Full arena snapshot** — Entire arena volume is saved and restored after each game
- **Glass platforms** — Players stand on glass above the snow floor; platforms removed at start
- **Player freeze** — Horizontal movement locked during lobby/countdown (jumping allowed)
- **Auto floor fill** — Snow floor auto-generates on first join, clears on last leave

## Commands

All commands use the `/spleef` namespace. Admin commands require `spleef.admin` permission (default: OP).

### Player Commands

| Command | Description |
|---------|-------------|
| `/spleef join [arena]` | Join a game (auto-creates if none exists) |
| `/spleef leave` | Leave your current game |
| `/spleef list` | List all arenas and their status |
| `/spleef info <arena>` | Show arena details |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/spleef arena create <name>` | Create a new arena |
| `/spleef arena pos1 <name>` | Set first corner of arena bounds |
| `/spleef arena pos2 <name>` | Set second corner (auto-finalizes bounds) |
| `/spleef arena addspawn <name>` | Add a spawn point at your location |
| `/spleef arena type <name> <type>` | Set game type (ffa/solos/duos/trios/quads) |
| `/spleef arena players-needed <name> <n>` | Override minimum players |
| `/spleef start [arena]` | Force-start a game |
| `/spleef stop [arena]` | Stop a game |

## Game Types

| Type | Min Players | Team Size | Description |
|------|-------------|-----------|-------------|
| `ffa` | 2 | 1 | Free-for-all |
| `solos` | 2 | 1 | Free-for-all (alias) |
| `duos` | 4 | 2 | Teams of 2 |
| `trios` | 6 | 3 | Teams of 3 |
| `quads` | 8 | 4 | Teams of 4 |

## How to Play

1. **Set up an arena:**
   ```
   /spleef arena create myarena
   /spleef arena pos1 myarena     (stand at one corner)
   /spleef arena pos2 myarena     (stand at opposite corner)
   /spleef arena addspawn myarena (stand where players should spawn)
   /spleef arena type myarena duos
   ```

2. **Add more spawn points** — one per player (run `/spleef arena addspawn myarena` at each spawn location)

3. **Join and play:**
   ```
   /spleef join myarena
   ```
   The game auto-starts when enough players join. An admin can also `/spleef start myarena`.

4. **Break snow blocks** beneath opponents to make them fall. Each broken block gives you a snowball.

5. **Hit opponents with snowballs** to knock them back (horizontal knockback, 1.2 strength). No friendly fire on teams.

6. **Last player/team standing wins!**

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `spleef.admin` | OP | Admin commands (arena setup, start/stop) |

## Installation

1. Download the JAR from [Modrinth](https://modrinth.com/project/spleef) or [GitHub Releases](https://github.com/Speedymr01/spleef/releases)
2. Place in your server's `plugins/` folder
3. Restart the server (or `/reload`)
4. Configure arenas using `/spleef arena create`

## Building from Source

```bash
git clone https://github.com/Speedymr01/spleef.git
cd spleef
./build.bat   # Windows
# or
./build.sh    # Linux/Mac
```

Requires JDK 21+ and Maven 3.8+.

## Technical Details

- Built on Paper API 1.21+
- Full 3D arena snapshot at game start restores all block changes on game end
- No natural block drops — blocks broken in the arena give snowballs directly
- Void damage eliminated players; all other damage is cancelled
- Player locations are saved on join and restored on leave/game end
- Arena data persists to `config.yml` via the Paper configuration API
