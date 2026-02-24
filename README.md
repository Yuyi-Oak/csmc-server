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
- Command highlights: `/csmc create <mode> [mapId]`, `/csmc maps`, `/csmc sessions`, `/csmc scoreboard [limit]`, `/csmc view <free|next|prev|player>`, `/csmc queue join [mode] [mapId]`, `/csmc queue status`, `/csmc stats [player]`, `/csmc history [player|uuid] [limit]`, `/csmc top`.
- Queue monitoring command: `/csmc queue list` (all mode queue sizes).
- Queue status/actionbar now includes `need N` (players still needed before next match assignment).
- Matchmaking now prioritizes filling existing waiting sessions before creating new sessions.
- Session diagnostic command: `/csmc info` (mode/map/state/phase/score/player split).
- Queue map preference supports `auto` (default) or explicit map IDs from `/csmc maps`.
- Queued players now get real-time action-bar status (mode/map/position).
- Map editor tool: `/csmcmap create`, `/csmcmap addspawn`, `/csmcmap setbomb`, `/csmcmap addbuy`, `/csmcmap save`, `/csmcmap reload`.
- Map editor now supports indexed point removal and inspection: `/csmcmap listpoints`, `/csmcmap removespawn`, `/csmcmap removebuy`.
- Cross-server stats cache invalidation is enabled automatically when `storage.type` is `REDIS`.
- Bundled starter maps: `dust2`, `mirage`, `inferno`, `nuke`, `anubis`, `ancient`, `vertigo`, `summit` (original).
- Finished sessions auto-close after 15 seconds to free match slots for queueing.
- Baseline anti-cheat now flags repeated rapid-fire abuse and alerts ops (`csmc.anticheat.alert`).
- Weapon-system headshot kills are now tracked into persistent player stats.
- Baseline anti-cheat also flags abnormal movement bursts (`speed`/`fly`) during combat phases.
- Match history is persisted to `plugins/CSMC/data/match-history.yml` and queryable via `/csmc history`.
- Stats persistence is now buffered and flushed asynchronously in batches for smoother tick performance.
- Per-match live scoreboard is available via `/csmc scoreboard`.
- End-of-round top players and match MVP are broadcast to session chat automatically.
- `/csmc` now includes tab-completion for modes/maps/session IDs/view targets.
- Friendly fire is mode-aware: enabled for competitive/wingman/demolition, disabled for casual/deathmatch-style modes.
