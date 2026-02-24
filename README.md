# CSMC Server

Paper plugin (Java 21) that recreates CS:GO gameplay in Minecraft 1.21+.

## Modules
- `csmc-api`: shared interfaces and data contracts
- `csmc-core`: core game engine (rules, economy, ballistics)
- `csmc-storage`: storage abstraction and providers
- `csmc-paper`: Paper implementation and plugin entrypoint

## Gradle tasks
- `shadowJar`: builds the shaded plugin jar
- `runPaper`: starts a local Paper test server (uses run-paper)

## Notes
- Configuration templates and language files are under `csmc-paper/src/main/resources/`.
- Storage backends currently available: YAML, SQLite, MySQL, PostgreSQL, MongoDB, Redis.
- Command highlights: `/csmc create <mode> [mapId]`, `/csmc maps`, `/csmc view <free|player>`, `/csmc queue join [mode]`, `/csmc queue status`, `/csmc stats [player]`, `/csmc top`.
- Map editor tool: `/csmcmap create`, `/csmcmap addspawn`, `/csmcmap setbomb`, `/csmcmap addbuy`, `/csmcmap save`, `/csmcmap reload`.
- Cross-server stats cache invalidation is enabled automatically when `storage.type` is `REDIS`.
- Bundled starter maps: `dust2`, `mirage`, `inferno`, `nuke`, `anubis`, `ancient`, `vertigo`, `summit` (original).
- Finished sessions auto-close after 15 seconds to free match slots for queueing.
