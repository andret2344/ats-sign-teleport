# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Keep it up to date — when making changes that affect architecture, data flow, build setup, testing conventions, or key constraints, update the relevant sections here.

## Build commands

```bash
./gradlew build          # compile, test, coverage verification, shadow JAR
./gradlew test           # run all tests
./gradlew test --tests "eu.andret.ats.signteleport.SignTeleportServiceTest.parseSignDataWithCoords"  # single test
./gradlew clean build    # full clean build
```

JaCoCo generates an XML coverage report after tests. **100% instruction coverage** is enforced by Codecov (configured in `codecov.yml`), not locally. `SignTeleportPlugin` is excluded from coverage via `exclude("**/*Plugin.*")` in `build.gradle.kts` and `ignore` in `codecov.yml`.

The output JAR is `build/libs/atsSignTeleport-<version>.jar` (shadow JAR with bStats relocated).

## Architecture

Three-layer structure:

- **`SignTeleportPlugin`** — entry point only; wires the other classes together in `onEnable()`.
- **`SignTeleportListener`** — thin Bukkit event router. Handles `PlayerInteractEvent`, `BlockBreakEvent`, `SignChangeEvent`, `ChunkLoadEvent`. Does permission checks and block-state extraction, then delegates all business logic to the service.
- **`SignTeleportService`** — all business logic. Owns the six `NamespacedKey` fields (`world`, `x`, `y`, `z`, `yaw`, `pitch`) used to read/write teleport destinations from Bukkit's `PersistentDataContainer`. Key public methods: `getWorldName`, `isTeleportSign`, `buildLocation`, `parseSignData`, `applySignData`, `updateChunk`, `updateSigns`.
- **`SignTeleportCommand`** — handles `/signteleport reload`: calls `plugin.reloadConfig()` then `service.updateSigns()`.

## Data flow

**Sign creation** (`SignChangeEvent`): `parseSignData` validates line 0 is `[TELEPORT]`, line 1 is either `[worldName]` or `@playerName` (online player snapshot), line 2 is `[x, y, z]` or `[x, y, z, yaw, pitch]`. On success, `applySignData` writes coords to the block's PDC and re-renders lines using config templates.

**Teleportation** (`PlayerInteractEvent`, right-click): reads world/coords from PDC, calls `player.teleport()`.

**Sign display refresh**: on plugin start and `/signteleport reload`, `updateSigns()` iterates all loaded chunks. `updateChunk()` is also called on `ChunkLoadEvent` to lazily refresh signs as chunks load. Both re-apply the `config.yml` `lines` templates (supports `%WORLD%`, `%X%`, `%Y%`, `%Z%` and `&`-color codes).

## Testing conventions

- `SignTeleportServiceTest` — tests business logic directly; mocks `SignTeleportPlugin`, `Server`, etc.
- `SignTeleportListenerTest` — mocks `SignTeleportService`; tests only event routing, permission checks, and event cancellation.
- `SignTeleportCommandTest` — mocks both `SignTeleportPlugin` and `SignTeleportService`.
- All tests use `MockitoExtension` (STRICT_STUBS). Do not add unnecessary stubs.

## Code style

- Always use braces `{}` for all control flow blocks (`if`, `for`, `while`, etc.), even single-line bodies.
- 100% instruction coverage is required — every new branch must have a test. Codecov enforces this on CI.

## Key constraints

- Java 25, Spigot API (Spigot 1.21.4 / API `26.1-R0.1-SNAPSHOT`).
- `ChatColor.translateAlternateColorCodes` is used for sign display lines.
