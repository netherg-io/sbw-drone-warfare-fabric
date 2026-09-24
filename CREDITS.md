# Asset credits and license status

Audit of every file under `src/main/resources/assets` (plus root resources) at upstream commit
`30e7b655e85301f7c3607d2279e8aad268dfa91d` (1.0.1), done 2026-09-14.

Status:

- **port**: author-created data/JSON covered by the repository AGPL-3.0, or third-party asset
  with a license compatible with redistribution (attribution kept).
- **replace before release**: no recorded source or license. Upstream ships no credit file for
  it (the 1.0.1 changelog mentions "third-party asset credit documentation", but none is in this
  tree or in the published 1.0.1 jar). Do not ship in a public release until provenance is confirmed
  by SmartStreamLabs or the asset is replaced with our own.
- **exclude**: license incompatible with a public release (non-commercial, etc.). Not copied.

## Summary

96 files under `assets/`, plus 33 root/data resources.

| Status | `assets/` files | Other resources |
|--------|------:|------:|
| port (AGPL-3.0 JSON by SmartStreamLabs) | 44 | 32 |
| port (CC BY 4.0, attribution required; verify source URL) | 3 | 0 |
| replace before release (unknown provenance: 36 PNG, 7 OGG, 3 geo, 1 OBJ, 2 MTL) | 49 | 1 (`cacc.png`) |
| exclude | 0 present in 1.0.1 | 0 |

## Excluded (not present in 1.0.1; must not be imported from later upstream versions)

Credit files in the later upstream source
([SBW-Drone-Warfare-Updated-8-28-2026@9eb3b59](https://github.com/SmartStreamLabs/SBW-Drone-Warfare-Updated-8-28-2026/tree/9eb3b598083838facc16022738aa06ad48fbfb04))
declare these models:

| Asset (later upstream) | Source | License | Status |
|---|---|---|---|
| Geranium-2 (`geranium_2.*`) | [Karosio, Sketchfab](https://skfb.ly/pztpW) | CC BY-NC-SA 4.0 | **exclude** |
| Vampire UAV / Heavy Bomber (`vampire_uav_babayaga`) | [Karosio, Sketchfab](https://skfb.ly/pGIZV) | CC BY-NC-SA 4.0 | **exclude** |
| FPV Delivery Drone (`delivery_drone_uav`) | ["uAV" by Phoenix..., Sketchfab](https://skfb.ly/ory8t) | CC BY 4.0 | port with attribution if imported |
| FPV Goggles | [Meshy FPV Goggles by LINE-O](https://skfb.ly/ptSTu) | CC BY 4.0 | port with attribution if imported |
| GP-4 Glide Bomb | [KillCaptureDestroy, Sketchfab](https://skfb.ly/pGOr6) | CC BY 4.0 | port with attribution if imported |
| Bayraktar TB2 | [42manako, Sketchfab](https://skfb.ly/pwWNv) | CC BY 4.0 | port with attribution if imported |

## Third-party with attribution

| File | Source | License | Status |
|---|---|---|---|
| `assets/sbwdroneconfig/models/item/drone_jammer.obj` | "Signal Jammer {Draft}" by AspectStudios (header comment in file; no URL) | CC BY 4.0 | port (CC BY 4.0, verify source URL) |
| `assets/sbwdroneconfig/models/item/drone_jammer.mtl` | same | CC BY 4.0 | port (CC BY 4.0, verify source URL) |
| `assets/sbwdroneconfig/textures/item/drone_jammer.png` | probably same model; not stated | CC BY 4.0? | port (CC BY 4.0, verify source URL) |

## Models and geometry

| File | Source | License | Status |
|---|---|---|---|
| `geo/cubed_fpv_drone.geo.json` (270 KB) | unknown | unknown | replace before release |
| `geo/drone.geo.json` (`realistic_fpv_drone`) | unknown | unknown | replace before release |
| `geo/lucas_drone.geo.json` (1 MB, likely converted mesh) | unknown | unknown | replace before release |
| `models/item/jerrycan.obj` ("Converted from metal_jerrycan.glb", no author) | unknown | unknown | replace before release |
| `models/item/empty_jerrycan.mtl`, `models/item/gasoline_jerrycan.mtl` | belong to `jerrycan.obj` | unknown | replace before release |
| `animations/fpv_drone.animation.json` | SmartStreamLabs | AGPL-3.0 | port |
| `blockstates/*.json` (8) | SmartStreamLabs | AGPL-3.0 | port |
| `models/block/*.json` (17) | SmartStreamLabs | AGPL-3.0 | port |
| `models/item/*.json` (15) | SmartStreamLabs | AGPL-3.0 | port |

## Textures

| File | Source | License | Status |
|---|---|---|---|
| `textures/entity/cubed_fpv_drone.png`, `drone.png`, `lucas_drone.png` | unknown | unknown | replace before release |
| `textures/item/cubed_fpv_drone.png`, `lucas_drone.png`, `spotlight_module.png`, `fiber_optic_spool_upgrade.png`, `gasoline_canister.png`, `anti_drone_net.png`, `jammer.png` | unknown | unknown | replace before release |
| `textures/item/empty_jerrycan_3d.png`, `empty_jerrycan_icon.png`, `gasoline_jerrycan_3d.png`, `gasoline_jerrycan_icon.png` | unknown (jerrycan mesh) | unknown | replace before release |
| `textures/block/anti_drone_net.png`, `drone_jammer.png` | unknown | unknown | replace before release |
| `textures/block/drone_detection_siren_{back,bottom,front,side,top}.png` | unknown (photo-like, non-power-of-two) | unknown | replace before release |
| `textures/block/extraction_crate_{back,bottom,front,side,top}.png` | unknown (photo-like, non-power-of-two) | unknown | replace before release |
| `textures/block/jammer_{back,bottom,front,side_left,side_right,top}.png` | unknown (photo-like, non-power-of-two) | unknown | replace before release |
| `textures/block/fuel_mixer_{bottom,front,side,top}.png` | unknown | unknown | replace before release |

## Sounds

| File | Source | License | Status |
|---|---|---|---|
| `sounds/entity/cubed_fpv_drone_engine.ogg` | unknown | unknown | replace before release |
| `sounds/entity/lucas_drone_engine.ogg`, `sounds/lucas_drone_engine.ogg` (duplicate) | unknown | unknown | replace before release |
| `sounds/drone_distant_explosion.ogg`, `sounds/drone_5000_block_explosion.ogg` | unknown | unknown | replace before release |
| `sounds/block/drone_detection_siren.ogg` | unknown | unknown | replace before release |
| `assets/superbwarfare/sounds/vehicle/drone/drone_engine.ogg` (overrides Superb Warfare's sound) | unknown | unknown | replace before release |
| `sounds.json`, `assets/superbwarfare/sounds.json` | SmartStreamLabs | AGPL-3.0 | port |

## Other resources

| File | Source | License | Status |
|---|---|---|---|
| `assets/sbwdroneconfig/lang/en_us.json` | SmartStreamLabs | AGPL-3.0 | port |
| `cacc.png` (mod logo) | unknown | unknown | replace before release |
| `data/**` (recipes, loot tables, tags, vehicle data; 29 JSON files) | SmartStreamLabs | AGPL-3.0 | port |
| `pack.mcmeta`, `sbwdroneconfig.mixins.json`, `META-INF/mods.toml` | SmartStreamLabs | AGPL-3.0 | port (Fabric equivalents replace Forge metadata) |

## Fabric prototype — 2026-09-24

No files from `src/main/resources` are included in the Fabric artifact. The renderer references the installed LGPL-3.0-only `superbwarfare-fabric` drone model/texture/animation and engine sound, without redistributing them; the prototype item icon references vanilla iron ingot.

Signal Jammer source: [AspectStudios, Signal Jammer {Draft}](https://sketchfab.com/3d-models/signal-jammer-draft-b567fbe6f16a43969fc204ee0f90b06d), CC Attribution. Model title and author match the OBJ header; unverified texture provenance is still not cleared. Neither is included.

## Fabric FPV flight — 2026-09-24

No assets were added. The flight HUD is plain text drawn with the vanilla font; flight code is original AGPL-3.0 work.

## Fabric FPV payload, battery and fuze — 2026-09-24

No assets were added. HUD additions are vanilla-font text; payload, battery and fuze code is original AGPL-3.0 work. Payload models and the explosion come from the installed Superb Warfare jar and are not redistributed.

## Fabric FPV link, jammer, fibre and motor tone — 2026-09-24

No assets were added. The Signal Jammer item uses vanilla's lightning rod model and the fibre drone's icon vanilla string (both referenced from Minecraft, not copied); the upstream Signal Jammer OBJ/texture stays excluded. The fibre is drawn as vanilla line geometry, the video noise as filled GUI rectangles, and the motor tone is Superb Warfare's installed `drone_engine` sound. Link, jammer and fibre code is original AGPL-3.0 work.
