# atsSignTeleport

A Spigot plugin that lets players place signs which teleport anyone who right-clicks them to a stored location,
including yaw and pitch.

## Creating a teleport sign

Write on the sign while placing it. Line 1 must always be `[TELEPORT]` (case-insensitive).
There are two ways to specify the destination on line 2:

### Explicit coordinates

```
[TELEPORT]
[worldName]
[x, y, z]
[yaw, pitch]
```

Yaw and pitch on line 4 are optional — omitting them (or leaving line 4 blank) defaults both to `0`.
The legacy single-line form is also accepted:

```
[x, y, z, yaw, pitch]
```

All values accept decimals and negative numbers.

### Player snapshot (`@mention`)

```
[TELEPORT]
@playerName
         <- lines 3–4 are ignored
```

The plugin looks up the named player (must be online at sign-placement time), reads their current location, and uses
it as the destination. Coordinates (x, y, z) are rounded to the nearest `0.5`. Yaw and pitch are stored as-is.

## Editing a teleport sign

Shift-right-click a teleport sign (requires `ats.signteleport.create`) to open the sign editor pre-filled with
the current stored coordinates:

```
[TELEPORT]
[worldName]
[x, y, z]
[yaw, pitch]
```

Submit the editor to update the destination. The sign display refreshes automatically.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `ats.signteleport.use` | everyone | Right-click a teleport sign to be teleported |
| `ats.signteleport.create` | op | Place or edit a teleport sign |
| `ats.signteleport.break` | op | Break a teleport sign (breaking is cancelled without this) |
| `ats.signteleport.reload` | op | Run `/signteleport reload` |
| `ats.signteleport.*` | op | Grants create, break, and reload; use is inherited as true |

## Commands

| Command | Permission | Description |
|---|---|---|
| `/signteleport reload` | `ats.signteleport.reload` | Reloads `config.yml` and refreshes all loaded signs |

## Configuration (`config.yml`)

Controls how the sign looks after successful creation. Requires exactly 4 lines — fewer silently disables
sign rendering.

```yaml
lines:
  - '&b[TELEPORT]'   # line 1
  - ''               # line 2
  - '%WORLD%'        # line 3
  - '%X%, %Y%, %Z%'  # line 4
```

Available placeholders: `%WORLD%`, `%X%`, `%Y%`, `%Z%`, `%YAW%`, `%PITCH%`.
Color codes (`&a`, `&b`, …) are supported on any line.

## Data storage

Destinations are stored using Bukkit's `PersistentDataContainer` on the sign's `BlockState`, keyed under the
`atssignteleport` namespace:

| Key | Type | Description |
|---|---|---|
| `atssignteleport:world` | `STRING` | World name |
| `atssignteleport:x` | `DOUBLE` | X coordinate |
| `atssignteleport:y` | `DOUBLE` | Y coordinate |
| `atssignteleport:z` | `DOUBLE` | Z coordinate |
| `atssignteleport:yaw` | `FLOAT` | Yaw |
| `atssignteleport:pitch` | `FLOAT` | Pitch |

Data persists across server restarts because it is stored in the world's chunk data, not in plugin memory.

## Building

Requires Java 25 and Gradle. The shadow JAR bundles bStats (relocated to `eu.andret.ats.signteleport.bstats`).

```
./gradlew build
```

Output: `build/libs/atsSignTeleport-<version>.jar`

## CI / CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | Every push and pull request | Compiles and runs tests with Jacoco coverage verification (100% required) |
| `deploy.yml` | Tag `v*` on `main` | Builds the JAR, creates a GitHub Release with it attached, publishes to GitHub Packages |
