# Replacement plan for unverified upstream assets

S1 item of [#1](https://github.com/netherg-io/sbw-drone-warfare-fabric/issues/1). It covers every file marked
**replace before release** in [CREDITS.md](../CREDITS.md) (upstream 1.0.1, `30e7b655`). Checked 2026-09-25.

**Release status.** None of these files is in the Fabric jar. `scripts/check-artifact.py` fails the build on any
`.png/.ogg/.obj/.mtl`, and `build.gradle` builds only `src/fabric`. The Fabric features use vanilla or installed
Superb Warfare (LGPL-3.0-only) assets by reference. So these files do not block a release of the current feature
set. They only matter when the feature that needs them is ported.

No embedded provenance was found. The OGGs carry only ffmpeg encoder tags (`Lavf58.76.100`, and libVorbis
20200704 for the Lucas engine), and the PNGs carry no author or software chunks.

## Per asset

| Upstream file(s) | Needed by | Fabric today | Plan |
|---|---|---|---|
| `geo/cubed_fpv_drone.geo.json`, `textures/entity/cubed_fpv_drone.png`, `textures/item/cubed_fpv_drone.png` | FPV drone model and icon | SBW `drone` model, texture and animation by reference; vanilla iron ingot icon | Keep SBW by reference for the first release. Later, optionally, our own 5" quad in Blockbench (X frame, 4 props, camera pod, payload rail), 32×32 texture and a 16×16 icon, licensed CC BY 4.0 by netherg-io and listed in CREDITS. |
| `geo/drone.geo.json` (`realistic_fpv_drone`), `textures/entity/drone.png` | second FPV skin | not used | Drop. One FPV model is enough. |
| `geo/lucas_drone.geo.json` (1 MB, converted mesh), `textures/entity/lucas_drone.png`, `textures/item/lucas_drone.png`, `sounds/{entity/,}lucas_drone_engine.ogg` | Lucas fixed-wing UAV | not ported | Out of scope until the fixed-wing is ported. Then build our own low-poly model rather than trace this mesh. Use an engine loop from a CC0 source (record the URL and licence per file) or SBW's plane loop by reference. |
| `models/item/jerrycan.obj`, `empty_jerrycan.mtl`, `gasoline_jerrycan.mtl`, `textures/item/{empty,gasoline}_jerrycan_{3d,icon}.png`, `textures/item/gasoline_canister.png`, `textures/block/fuel_mixer_*.png` | fuel/fuel-mixer system | not ported | Drop. The FPV runs on the `Battery` model; there is no fuel system. |
| `textures/item/spotlight_module.png` (1.2 MB) | spotlight attachment | not ported | Drop. If ever ported, draw a 16×16 icon ourselves. |
| `textures/item/fiber_optic_spool_upgrade.png` | fibre upgrade item | `fibre_fpv_drone` uses the vanilla string icon | Draw our own 16×16 spool icon (CC BY 4.0, netherg-io) when the item gets its own texture. |
| `textures/item/jammer.png`, `textures/block/jammer_*.png`, `textures/block/drone_jammer.png` | placeable jammer block | handheld Signal Jammer uses the vanilla lightning rod model | Placeable jammers are not ported. When they are, make 16×16 block textures in the vanilla style ourselves. The photo-like 100+ KB faces are not reusable. |
| `textures/item/drone_jammer.png` (32×32 icon) | handheld jammer icon | vanilla lightning rod | This is **not** part of the Signal Jammer model below: the MTL has no texture maps. Treat it as unknown and replace it with our own 16×16 icon. |
| `textures/block/anti_drone_net.png`, `textures/item/anti_drone_net.png` | anti-drone net | not ported | Draw our own when it is ported. It is a simple net pattern. |
| `textures/block/drone_detection_siren_*.png`, `sounds/block/drone_detection_siren.ogg` | detector siren block | not ported (listed as "not done" in stage 3) | Our own 16×16 textures. For the siren, use a CC0 recording or vanilla/SBW alarm by reference; record the licence per file. |
| `textures/block/extraction_crate_*.png` | extraction crate | not ported | Drop, or our own textures if ported. |
| `sounds/entity/cubed_fpv_drone_engine.ogg`, `assets/superbwarfare/sounds/vehicle/drone/drone_engine.ogg` (override) | FPV motor | SBW `drone_engine` by reference, pitch/volume from `MotorSound` | Keep SBW by reference. Do not ship the override: it replaced SBW's own sound for every drone. |
| `sounds/drone_distant_explosion.ogg`, `sounds/drone_5000_block_explosion.ogg` | far-away blast cue | SBW explosion sounds | Keep SBW/vanilla. If a distant-blast layer is wanted, use a CC0 recording with its URL and licence in CREDITS. |
| `cacc.png` (mod logo) | `fabric.mod.json` icon | no icon | Optional: our own 128×128 icon. Without one, Mod Menu shows the default. |

## Signal Jammer {Draft} (CC BY 4.0), source check

- Source: [Signal Jammer {Draft}](https://sketchfab.com/3d-models/signal-jammer-draft-b567fbe6f16a43969fc204ee0f90b06d), model id `b567fbe6f16a43969fc204ee0f90b06d`, by AspectStudios (`@AspectStudio`). The listing gives "Creative Commons Attribution", published 2025-08-08, 7.9k triangles and 3.7k vertices. Checked through a web search result for that page on 2026-09-25; the sandbox cannot open sketchfab.com directly.
- Upstream files: `drone_jammer.obj` and `.mtl`. Header: `Converted from signal_jammer_draft.glb … Source asset: Signal Jammer {Draft} by AspectStudios, CC-BY-4.0`. Title and author match.
- Geometry: 6,642 triangles and 7,665 OBJ vertices, against the listed 7.9k triangles. The OBJ vertex count is higher because unwelded glTF vertices get split. The triangle gap points to a reduced or partial conversion. That is a modification, which CC BY 4.0 allows as long as it is indicated.
- Materials: five flat-colour materials (`Touched_Plastic_Smooth`, `Dark_Metal_Marked`, `Plastic_with_scratches`, `High_Gloss_Red_Plastic`, `Hi_Gloss_Green_Plastic`), and no texture maps. The model carries no image. The 32×32 `textures/item/drone_jammer.png` is a separate icon of unknown origin; see the table.
- Verdict: the OBJ/MTL can ship if the Signal Jammer gets its model back. The required attribution is: title, author, link, "CC BY 4.0" with a licence link, and "converted to OBJ, modified". Put it in CREDITS.md and in the jar. Before shipping, the owner should confirm once, while logged in to Sketchfab, that the page still shows CC BY 4.0 and that the download matches, because a licence change applies only to later downloads. The icon is replaced as above.

## Rule for new assets

Every new file needs one of two things: (a) it is our own work, recorded in CREDITS.md as CC BY 4.0 by netherg-io (or AGPL for data/JSON), or (b) it comes from a third party under a licence compatible with redistribution (CC0 / CC BY / CC BY-SA / LGPL), with its URL, author, licence and modifications listed. NC and ND licences are excluded (Geranium-2 and Vampire UAV stay excluded). `scripts/check-artifact.py` must be relaxed per path only when a file is added this way.
