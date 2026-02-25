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
- Asset handoff checklist is in `docs/ASSET_CHECKLIST.md`.
- Storage backends currently available: YAML, SQLite, MySQL, PostgreSQL, MongoDB, Redis.
- Command highlights: `/csmc create <mode> [mapId]`, `/csmc maps`, `/csmc sessions`, `/csmc rules [mode]`, `/csmc scoreboard [limit]`, `/csmc buy [item]`, `/csmc view <free|next|prev|player>`, `/csmc weapon <list|key> [recoilPreviewLimit]`, `/csmc queue join [mode] [mapId]`, `/csmc queue status`, `/csmc queue votes [mode]`, `/csmc queue global [mode|detail [limit]]`, `/csmc queue clear [mode]`, `/csmc stats [player]`, `/csmc history [player|uuid] [limit]`, `/csmc top [limit]`, `/csmc ac <status|reset> [player]`, `/csmc ac top [limit]`, `/csmc ac reasons [limit]`, `/csmc ac reasonsreset`.
- Queue monitoring command: `/csmc queue list` (all mode queue sizes + needed players).
- With Redis cluster sync enabled, `/csmc queue list` shows `local/global` queue totals per mode.
- `/csmc queue global detail [limit]` shows remote source ages, per-mode counts, and per-source totals.
- `/csmc queue clear [mode]` clears queued players (permission: `csmc.queue.manage`).
- Queue clear operations now publish cluster queue snapshots immediately for faster cross-server convergence.
- Queue map preference insight command: `/csmc queue votes [mode]`.
- Queue vote output includes per-map share percentages.
- Queue join/status output now includes your selected map's current vote share.
- With Redis cluster sync enabled, queue join/status also shows global queued players for the mode.
- On server shutdown, queue snapshot is pushed as zero so global queue display converges quickly.
- Queue list/actionbar now also surfaces top-voted map share for quicker matchmaking visibility.
- Queue status/actionbar now includes `need N` (players still needed before next match assignment).
- Matchmaking now prioritizes filling existing waiting sessions before creating new sessions.
- Session diagnostic command: `/csmc info` (mode/map/state/phase/score/player split).
- Queue map preference supports `auto` (default) or explicit map IDs from `/csmc maps`.
- Queued players now get real-time action-bar status (mode/map/position).
- C4 planting now follows rules-driven plant time (`plantTimeSeconds`) and can be cancelled by movement/sneak release before completion.
- Map editor tool: `/csmcmap create`, `/csmcmap addspawn`, `/csmcmap setbomb`, `/csmcmap addbuy`, `/csmcmap save`, `/csmcmap reload`.
- Map editor now supports indexed point removal and inspection: `/csmcmap listpoints`, `/csmcmap removespawn`, `/csmcmap removebuy`.
- Map editor now supports draft cloning: `/csmcmap clone <sourceId> <targetId> [name]`.
- Map editor supports bulk draft persistence: `/csmcmap saveall`.
- Map editor supports structural checks before save: `/csmcmap validate <id>`.
- Map save commands now enforce validation by default; append `force` to override (`/csmcmap save <id> force`, `/csmcmap saveall force`).
- Cross-server stats cache invalidation is enabled automatically when `storage.type` is `REDIS`.
- Redis storage now maintains a kills leaderboard sorted-set index to speed up `/csmc top` queries.
- Bundled starter maps: `dust2`, `mirage`, `inferno`, `nuke`, `anubis`, `ancient`, `vertigo`, `summit` (original).
- Finished sessions auto-close after 15 seconds to free match slots for queueing.
- Baseline anti-cheat now flags repeated rapid-fire abuse and alerts ops (`csmc.anticheat.alert`).
- Weapon-system headshot kills are now tracked into persistent player stats.
- Baseline anti-cheat also flags abnormal movement bursts (`speed`/`fly`) during combat phases.
- Anti-cheat now escalates severe repeated violations into automatic kicks (bypass permission: `csmc.anticheat.bypass`).
- Ops can inspect/reset anti-cheat VL using `/csmc ac` (`csmc.anticheat.manage`).
- `/csmc ac top` shows highest current violation levels across tracked players.
- `/csmc ac reasons [limit]` shows top anti-cheat trigger reasons; `/csmc ac reasonsreset` clears reason counters.
- Weapon firing now uses per-weapon/fallback recoil patterns and applies recoil offsets together with spread.
- Weapon inaccuracy now penalizes sprinting/airborne states and rewards sneaking stability.
- `/csmc weapon list` shows available weapon keys; `/csmc weapon <key> [recoilPreviewLimit]` shows live profile + spread profile + recoil preview.
- Default shop catalog now includes expanded CS-style weapon pool and side-restricted purchases (`sides: [t|ct|both]` in `shop.yml`).
- `/csmc buy` now prints a side-filtered shop list with price/affordability hints; `/csmc buy <item>` performs purchase.
- Armor logic is helmet-aware for headshots; added `kevlar_helmet` shop item to equip vest + helmet together.
- Match history is persisted to `plugins/CSMC/data/match-history.yml` and queryable via `/csmc history`.
- Stats persistence is now buffered and flushed asynchronously in batches for smoother tick performance.
- Per-match live scoreboard is available via `/csmc scoreboard`.
- End-of-round top players and match MVP are broadcast to session chat automatically.
- Assist credit is now constrained to hits from the same round/session to avoid stale carryover.
- `/csmc` now includes tab-completion for modes/maps/session IDs/view targets.
- `/csmcmap` now includes tab-completion for map IDs, sides, bomb sites, and removable point indexes.
- Friendly fire is mode-aware: enabled for competitive/wingman/demolition, disabled for casual/deathmatch-style modes.
