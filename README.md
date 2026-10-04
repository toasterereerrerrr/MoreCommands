# More Commands

Quality-of-life commands for Minecraft 26.2 (Fabric). Server-side only: install it on a server and players do not need it on their client. It works in singleplayer too.

Requires Fabric Loader 0.19.3+, Fabric API, and Java 25.

## Commands

Permissions levels:
- **Everyone**: Available to all players.
- **Utility**: Available to everyone unless `utilityCommandsRequireOp` is true (default: `true`).
- **Moderator**: Requires operator level 1 or higher.
- **Gamemaster**: Requires operator level 2 or higher (standard "op").

### Teleportation

| Command | Permission | What it does |
| --- | --- | --- |
| `/sethome [name]` | Everyone | Save your position as a home. The default name is `home`. |
| `/home [name]` | Everyone | Teleport to a saved home. |
| `/delhome [name]` | Everyone | Delete a home. |
| `/homes` | Everyone | List your homes. |
| `/back` | Everyone | Return to where you were before your last teleport, or to where you last died. |
| `/tpa <player>` | Everyone | Request to teleport to another player. |
| `/tpahere <player>` | Everyone | Request another player to teleport to you. |
| `/tpaccept` | Everyone | Accept a pending teleport request. |
| `/tpdeny` / `/tpcancel` | Everyone | Deny or cancel a teleport request. |
| `/rtp` | Everyone | Randomly teleport within a configured range. |
| `/warp <name>` | Utility | Teleport to a global server warp. |
| `/warps` | Utility | List all global warps. |
| `/top` | Gamemaster | Teleport to the surface above you. |
| `/setwarp <name>` | Moderator | Create a global warp at your position. |
| `/delwarp <name>` | Moderator | Delete a global warp. |
| `/tpall` | Gamemaster | Teleport all players to your location. |

### Player Utilities

| Command | Permission | What it does |
| --- | --- | --- |
| `/heal [targets]` | Gamemaster | Restore health, food, air, and put out fires. |
| `/feed [targets]` | Gamemaster | Fill the hunger bar and saturation. |
| `/god [targets]` | Gamemaster | Toggle invulnerability. |
| `/fly [player]` | Moderator | Toggle flight mode. |
| `/speed <0-10> [player]` | Moderator | Set walk or fly speed. |
| `/repair [all]` | Gamemaster | Repair held item or full inventory. |
| `/hat` | Utility | Wear the held item as a hat. |
| `/invsee <player>` | Gamemaster | View and interact with another player's inventory. |
| `/sudo <player> <cmd>` | Gamemaster | Force another player to run a command. |
| `/extinguish [player]` | Moderator | Put out a player who is on fire. |

### Social and Chat

| Command | Permission | What it does |
| --- | --- | --- |
| `/msg <player> <text>` | Everyone | Send a private message (`/w`, `/tell`). |
| `/r <text>` | Everyone | Reply to the last private message. |
| `/nick <name>` | Moderator* | Change your display name. (*Everyone if `allowNicknames` is true) |
| `/realname <nick>` | Moderator | See the true username behind a nickname. |
| `/socialspy` | Gamemaster | See all private messages sent on the server. |
| `/broadcast <text>` | Gamemaster | Send a high-visibility server announcement. |
| `/list` | Utility | List all online players. |
| `/near` | Utility | See players within 100 blocks. |
| `/whois <player>` | Moderator | Show detailed info (IP, UUID, Location) about a player. |

### Shortcuts and Fun

- **Gamemodes (Gamemaster)**: `/gmc` (Creative), `/gms` (Survival), `/gma` (Adventure), `/gmsp` (Spectator).
- **Time & Weather (Gamemaster)**: `/day`, `/night`, `/sun`, `/rain`, `/storm`.
- **Menus (Utility)**: `/craft` (or `/workbench`), `/anvil`, `/grindstone`, `/stonecutter`, `/loom`, `/cartography`, `/smithing`, `/enderchest` (or `/ec`).
- **Info (Everyone)**: `/ping` (latency), `/pos` (coordinates/chunk).
- **Misc**: `/suicide` (Everyone), `/lightning [player]` (Gamemaster), `/burn <player> <sec>` (Gamemaster).
- **Admin**: `/butcher [radius]` (Gamemaster), `/clearitems [radius]` (Gamemaster).

## Config

`config/morecommands.json` options:

- `homeLimit`: (int) Max homes per player. `0` = unlimited.
- `utilityCommandsRequireOp`: (bool) If `true`, utility commands need op level 2.
- `tpaTimeoutSeconds`: (int) Seconds before a TPA request expires.
- `rtpRange`: (int) Maximum distance for `/rtp`.
- `allowNicknames`: (bool) If `true`, all players can use `/nick`.

## Building

1. Install JDK 25.
2. Run `./gradlew build`. The jar ends up in `build/libs/`.
3. Use `./gradlew runServer` or `./gradlew runClient` to test.

## Notes

- `/god` uses vanilla invulnerability, which is saved on the player.
- Global warps are saved in `morecommands_warps.json` in the world folder.
- Portable menus are not tied to a block; anvils will not wear down.

## License

All Rights Reserved. See `LICENSE`.
