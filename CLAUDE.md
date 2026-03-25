# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```bash
./gradlew build          # compile, test, coverage verification, shadow JAR
./gradlew test           # run all tests
./gradlew test --tests "eu.andret.ats.signteleport.SignTeleportServiceTest.parseSignDataWithCoords"  # single test
./gradlew clean build    # full clean build
```

Jacoco coverage verification runs automatically after tests and requires **100% instruction coverage**. `SignTeleportPlugin` is excluded from coverage via `exclude("**/*Plugin.*")` in `build.gradle.kts`.

The output JAR is `build/libs/atsSignTeleport-<version>.jar` (shadow JAR with bStats relocated).

## Architecture

Three-layer structure:

- **`SignTeleportPlugin`** — entry point only; wires the other classes together in `onEnable()`.
- **`SignTeleportListener`** — thin Bukkit event router. Handles `PlayerInteractEvent`, `BlockBreakEvent`, `SignChangeEvent`, `ChunkLoadEvent`. Does permission checks and block-state extraction, then delegates all business logic to the service.
- **`SignTeleportService`** — all business logic. Owns the six `NamespacedKey` fields (`world`, `x`, `y`, `z`, `yaw`, `pitch`) used to read/write teleport destinations from Bukkit's `PersistentDataContainer`. Key public methods: `getWorldName`, `isTeleportSign`, `buildLocation`, `buildEditorLines`, `parseSignData`, `applySignData`, `updateChunk`, `updateSigns`.
- **`SignTeleportCommand`** — handles `/signteleport reload`: calls `plugin.reloadConfig()` then `service.updateSigns()`.

## Data flow

**Sign creation** (`SignChangeEvent`): `parseSignData` validates line 0 is `[TELEPORT]`, line 1 is either `[worldName]` or `@playerName` (online player snapshot), line 2 is `[x, y, z]` or `[x, y, z, yaw, pitch]`. On success, `applySignData` writes coords to the block's PDC and re-renders lines using config templates.

**Teleportation** (`PlayerInteractEvent`, right-click): reads world/coords from PDC, calls `player.teleport()`. Shift-right-click by a player with `ats.signteleport.create` opens the sign editor pre-filled with raw coordinates: `buildEditorLines` returns the 4-line raw array, the scheduler task calls `player.sendSignChange(location, lines)` then `player.openSign(sign, Side.FRONT)` in the same tick so the data update packet reaches the client before the editor-open packet (avoids the client using stale cached sign text).

**Sign display refresh**: on plugin start and `/signteleport reload`, `updateSigns()` iterates all loaded chunks. `updateChunk()` is also called on `ChunkLoadEvent` to lazily refresh signs as chunks load. Both re-apply the `config.yml` `lines` templates (supports `%WORLD%`, `%X%`, `%Y%`, `%Z%` and `&`-color codes).

## Testing conventions

- `SignTeleportServiceTest` — tests business logic directly; mocks `SignTeleportPlugin`, `Server`, etc.
- `SignTeleportListenerTest` — mocks `SignTeleportService`; tests only event routing, permission checks, and event cancellation.
- `SignTeleportCommandTest` — mocks both `SignTeleportPlugin` and `SignTeleportService`.
- All tests use `MockitoExtension` (STRICT_STUBS). Do not add unnecessary stubs.
- For the scheduler-delayed `player.openSign` call, tests use `ArgumentCaptor<Runnable>` to capture and manually invoke the task.

## Code style

- Always use braces `{}` for all control flow blocks (`if`, `for`, `while`, etc.), even single-line bodies.
- 100% instruction coverage is required — every new branch must have a test. The build fails without it.

## Key constraints

- Java 25, Spigot API (Spigot 1.21.4 / API `26.1-R0.1-SNAPSHOT`).
- `ChatColor.translateAlternateColorCodes` is used for sign display lines; `String.format("%.1f", ...)` is used when writing raw coordinates to the sign editor.
