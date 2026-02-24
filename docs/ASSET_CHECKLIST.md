# Asset Checklist

This checklist defines placeholder asset slots expected by the current plugin.
Use it to replace temporary resources with production-quality art/audio later.

## 1. Weapon Models / Textures

Provide one set per weapon key in `shop-*.yml` and weapon registry keys.

- `glock`
- `usp`
- `deagle`
- `p250`
- `fiveseven`
- `tec9`
- `ak47`
- `m4a1`
- `famas`
- `galil`
- `awp`
- `ssg08`
- `mac10`
- `mp9`
- `ump45`
- `nova`
- `xm1014`
- `knife`

Recommended deliverables per weapon:

- first-person model/texture
- world model/texture
- icon (shop + hud)
- firing/reload/empty sounds

## 2. Grenades / Utilities

- `flashbang`
- `smoke`
- `hegrenade`
- `molotov`
- `incendiary`
- `decoy`
- `defuse_kit`
- `kevlar`

Recommended deliverables:

- inventory icon
- throwable model
- detonation/loop sound sets
- particle/sprite sheets (if used by client resource pack/mod)

## 3. HUD / UI

Current gameplay expects HUD layers for:

- round timer (freeze/buy/live/bomb/defuse)
- team score (`T`, `CT`)
- economy
- armor
- ammo
- kill feed
- match scoreboard
- queue status

Recommended files:

- `hud/main_bar.*`
- `hud/scoreboard_panel.*`
- `hud/killfeed_panel.*`
- `hud/queue_panel.*`
- `hud/icons/*.png` (or equivalent)

## 4. Audio

Required categories:

- weapon fire/reload/bolt/empty
- grenade throw/bounce/explode
- bomb plant/beep/explode/defuse
- round start/end announcements
- ui click/open/confirm

## 5. Map Visual Packs

For each map ID (`dust2`, `mirage`, `inferno`, `nuke`, `anubis`, `ancient`, `vertigo`, `summit`):

- signage/locale decals
- unique environment texture set
- buy zone and bomb site markers (`A`, `B`)
- optional ambient loops

## 6. Naming Convention

Use stable IDs to avoid config rewrites:

- weapon: `weapon.<key>.*`
- grenade: `grenade.<key>.*`
- ui: `ui.<layer>.*`
- map: `map.<mapId>.*`
- sound: `sound.<category>.<key>.*`

## 7. Delivery Format

- textures/icons: PNG (power-of-two preferred)
- sounds: OGG/WAV
- metadata: JSON/YAML mapping table
- optional source files: PSD/AI/BLEND for future edits

