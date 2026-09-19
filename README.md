# More Commands

Quality-of-life commands for Minecraft 26.2 (Fabric). Server-side only: install it on a server and players do not need it on their client. It works in singleplayer too.

Requires Fabric Loader 0.19.3+, Fabric API, and Java 25.

## Commands

Commands marked "op" need operator level 2 (gamemaster) or higher. Commands marked "config" are open to everyone unless `utilityCommandsRequireOp` is true, which is the default.

### Homes and teleporting

| Command | Who | What it does |
| --- | --- | --- |
| `/sethome [name]` | everyone | Save your position as a home. The default name is `home`. |
| `/home [name]` | everyone | Teleport to a saved home. |
| `/delhome [name]` | everyone | Delete a home. |
| `/homes` | everyone | List your homes. |
| `/back` | everyone | Return to where you were before your last teleport, or to where you last died. |
| `/top` | op | Teleport to the surface above you. |

Homes are saved per world in `morecommands_homes.json` inside the world folder and work across dimensions.

### Player

| Command | Who | What it does |
| --- | --- | --- |
| `/heal [targets]` | op | Restore health, food and air, and put out fires. |
| `/feed [targets]` | op | Fill the hunger bar and saturation. |
| `/god [targets]` | op | Toggle invulnerability. |
| `/repair` | op | Repair the item in your main hand. |
| `/repair all` | op | Repair everything in your inventory. |
| `/hat` | config | Wear the item in your main hand as a hat (swaps with your current helmet). |

### Portable menus (config)

`/craft` (or `/workbench`), `/anvil`, `/grindstone`, `/stonecutter`, `/loom`, `/cartography`, `/smithing`, `/enderchest` (or `/ec`).

### Info

| Command | What it does |
| --- | --- |
| `/ping` | Show your latency. |
| `/pos` | Show your coordinates, dimension and chunk. |

### World shortcuts (op)

`/day`, `/noon`, `/night`, `/midnight`, `/sun`, `/rain`, `/storm`. These run the matching vanilla `/time set` or `/weather` command.

`/butcher [radius]` kills hostile mobs around you (default radius 64, max 512). Mobs with a custom name are skipped.
`/clearitems [radius]` removes dropped items around you (same radius rules).

## Config

`config/morecommands.json` is created on first launch:

```json
{
  "homeLimit": 5,
  "utilityCommandsRequireOp": true
}
```

- `homeLimit`: maximum homes per player. `0` means unlimited.
- `utilityCommandsRequireOp`: set to `false` to let everyone use the portable menus and `/hat`.

## Building

1. Install JDK 25.
2. Copy `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` from another Fabric project (the wrapper properties file here already points at Gradle 9.5.1), or run `gradle wrapper` if you have Gradle installed.
3. Replace `YOUR_NAME` in `LICENSE` and `fabric.mod.json`, and change `maven_group` in `gradle.properties` if you want a different package prefix.
4. Run `./gradlew build`. The jar ends up in `build/libs/`.
5. Use `./gradlew runServer` or `./gradlew runClient` to test in a dev environment.

Check https://fabricmc.net/develop/ for the newest Fabric API version and update `fabric_api_version` in `gradle.properties` if a newer 26.2 build is out.

## Notes

- `/god` uses vanilla invulnerability, which is saved on the player. If you toggle it on and leave, it is still on when you return.
- Void damage and `/kill` still work while in god mode, same as vanilla.
- The portable menus are not tied to a block, so an anvil opened with `/anvil` will not wear down.

## License

All Rights Reserved. See `LICENSE`.
