# Source and binary audit — 2026-09-24

The selected reference remains upstream 1.0.1, commit `30e7b655e85301f7c3607d2279e8aad268dfa91d`, preserved in this fork's history. Fabric sources live in `src/fabric`; unported Forge sources and unverified assets are excluded from the artifact. The first prototype reuses the installed Superb Warfare entity/renderer/resources; it does not claim to port all 1.0.6 behavior.

## Why the later folder is not a verified NeoForge source release

- Published NeoForge 1.0.6: https://modrinth.com/mod/sbw-tactical-drone/version/NeM3aBVy (2026-08-28 14:49 UTC).
- Binary: https://cdn.modrinth.com/data/WG6tgfJf/versions/NeM3aBVy/SBW-Drone-Warfare-1.21.1-NeoForge-1.0.6.jar
- SHA-256: `411c61c05450249bbde15f0153a74ea075336ae4a2f459b19b3c01e4c995801c`.
- Candidate source: https://github.com/SmartStreamLabs/SBW-Drone-Warfare-Updated-8-28-2026/tree/9eb3b598083838facc16022738aa06ad48fbfb04 (16:06 UTC).
- Its `SBW-Drone-Warfare-1.21.1/build.gradle` declares version 1.0.6, **Forge 1.20.1**, Java 17 and local Windows SBW paths. `AddonNetwork` imports `net.minecraftforge.network.simple.SimpleChannel`.
- The distributed jar instead contains `META-INF/neoforge.mods.toml`, Minecraft 1.21.1 and NeoForge >=21.1.228.
- Of matching resource paths, 152 files match byte for byte and 55 differ. Date/version agreement and partial resource matches do not establish the exact source of the released classes. No source archive or commit identifier is embedded in the jar; the candidate repository exposes only main and no tags.

Author confirmation or a matching source tree remains outstanding. Do not call this candidate the verified NeoForge 1.0.6 source. No later assets were imported.

## Published Fabric dependency

`superbwarfare-fabric` bf18: https://github.com/netherg-io/superbwarfare-fabric/releases/tag/bf18

Artifact `superbwarfare-0.8.9.1-mc1.21.1-bf18.jar`, SHA-256 `9ca5d4901e7e1cd283ba688651e1c5ba88ef72463b5ecf732ad995c2efa4c03a`. Gradle downloads it anonymously and verifies this checksum before use. Wrapper distribution is also checksum-pinned. Java 21, Minecraft 1.21.1, Loom 1.11.8, Mojang+Parchment 2024.11.17, Fabric Loader 0.19.3/API 0.116.15.

## Asset decisions

The Fabric artifact contains no upstream binary textures, sounds, OBJ or geometry. Existing SBW resources are referenced from the required installed mod (LGPL-3.0-only); the provisional inventory icon uses a vanilla texture.

All original assets marked “replace before release” in CREDITS.md remain excluded. Advanced models will use owned geometry or verified attributed alternatives; sounds require owned recordings/procedural synthesis or a verified compatible source. Geranium-2 and Vampire UAV NC assets remain excluded.

Signal Jammer source found: https://sketchfab.com/3d-models/signal-jammer-draft-b567fbe6f16a43969fc204ee0f90b06d by AspectStudios, CC Attribution. The OBJ header's author/title match this page; the texture's exact relationship remains unverified and it is not distributed.
