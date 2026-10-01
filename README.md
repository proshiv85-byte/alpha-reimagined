# Alpha Reimagined

Real survival multiplayer for **Minecraft Alpha 1.1.2_01**.

The original Alpha multiplayer server was only a building test: players couldn't take damage, fight, die or respawn. Alpha Reimagined is a patched server (based on server 0.2.1) and a matching patched client that add normal survival multiplayer, plus single-file launchers that include Java, so there's nothing else to install.

> Not affiliated with Mojang or Microsoft. This is a fan project for a version of the game from 2010.

## Download

Get the latest release from the [Releases page](../../releases).

| File | What it is |
|---|---|
| `alphar-client-windows.exe` / `alphar-client-linux` | The game client |
| `alphar-server-windows.exe` / `alphar-server-linux` | The server |
| `minecraft_server-<version>.jar` | The server as a plain jar (needs Java 8 or newer) |
| `windows-<version>.zip` / `linux-<version>.zip` | Client and server together |

Everyone on a server needs the matching client: older clients are refused with "Outdated client!".

Only 64-bit (x86_64) Windows and Linux are supported.

## Playing

**Client.** Run `alphar-client-windows.exe` or `alphar-client-linux`. A small window asks for your username and, optionally, a server (`host` or `host:port`) to join directly.

- The first start unpacks the game into a cache folder, so it takes a few seconds. Later starts are instant.
- Saves and settings are kept in `%APPDATA%\.alphar` (Windows) or `~/.alphar` (Linux).
- From a terminal, the client runs in the background and writes its log to `client.log` in that folder. Add `-c` to keep it in the terminal and print the log there.
- Skip the window with `--username Name --server host:port`.
- On Linux, after downloading, you may need to run `chmod +x alphar-client-linux` first.
- On Windows the executables aren't signed: if SmartScreen warns you, click "More info" and then "Run anyway".

**Server.** Put `alphar-server-windows.exe` or `alphar-server-linux` in a folder and run it there. The world and config files are created in that folder. Add `nogui` to run without the status window. Or use the jar: `java -Xmx1G -jar minecraft_server-<version>.jar nogui`.

There are no online accounts any more, so set `online-mode=false` in `server.properties`.

## What's different from the vanilla Alpha server

**Survival**
- Player health and damage: falling, lava, fire, drowning, suffocation, cactus, the void, explosions and monsters (scaled by difficulty). Armor works.
- Death drops your items; the Respawn button takes you back to spawn.
- Attacking mobs and players, with weapon damage, knockback and hurt/death animations. Mobs drop their loot.
- Bows, arrows, snowballs, food and buckets work.
- Arrows, snowballs, primed TNT and falling sand/gravel are visible to everyone; explosions show particles and play sound.
- Furnaces smelt with a working progress bar, and furnace/chest contents stay in sync without losing or duplicating items.

**Server settings** (`server.properties`)
- `pvp` (default `true`): whether players can hurt each other.
- `difficulty` (default `1`): 0 = peaceful, 1 = easy, 2 = normal, 3 = hard.
- `monsters` (default `true`).
- `skins` (default `true`): show player skins.

**Commands**
- `skins on|off`: show or hide player skins for everyone. Use it in the server console, or as `/skins on|off` in game if you're an op.

**Fixes to Mojang bugs**
- Sheep, cows and chickens all showed up as chickens in multiplayer.
- Players spawned 1.62 blocks in the air.
- Furnaces in multiplayer froze in their open window when they lit up.
- Chest and furnace contents were sometimes sent to the wrong position.
- Some new worlds crashed the server silently while generating terrain.

## Building from source

The repository contains the patched, decompiled source (`retromcp/minecraft/src` for the client, `retromcp/minecraft_server/src` for the server), the same changes as patch files (`retromcp/patches`), and the launcher (`launcher/`).

You need Java 17 or newer, Go, `curl`, `zip`, `unzip` and Python 3, on Linux.

1. Download [RetroMCP-Java](https://github.com/MCPHackers/RetroMCP-Java/releases) (CLI, v1.2) into `retromcp/`.
2. In `retromcp/`, run `java -jar RetroMCP-Java-CLI.jar setup a1.1.2_01` and then `java -jar RetroMCP-Java-CLI.jar decompile`. This downloads the original game files.
3. Decompiling overwrites the source folders with vanilla code, so restore the patched source: `git checkout -- retromcp`.
4. Build the jars: `java -jar RetroMCP-Java-CLI.jar build -fullbuild -source=8 -target=8`.
5. Copy them where the launcher build expects them:
   ```
   mkdir -p ../survival
   cp build/minecraft.jar ../survival/client_survival.jar
   cp build/minecraft_server.jar ../survival/minecraft_server_survival.jar
   ```
6. Build the executables: `../launcher/build.sh b1.7` (use any new version name). On the first run this downloads the Java 21 runtimes that get bundled. The results end up in `survival/linux/<version>/` and `survival/windows/<version>/`.

## Credits

- [RetroMCP-Java](https://github.com/MCPHackers/RetroMCP-Java) by MCPHackers for decompiling and rebuilding the game, and its launchwrapper.
- Bundled Java runtime: [Eclipse Temurin](https://adoptium.net/) 21.
- Minecraft is © Mojang Studios.
