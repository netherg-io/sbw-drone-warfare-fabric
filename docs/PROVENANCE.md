# Source and binary audit — 2026-09-24

The selected reference remains upstream 1.0.1, commit `30e7b655e85301f7c3607d2279e8aad268dfa91d`, preserved in this fork's history. Fabric sources live in `src/fabric`; unported Forge sources and unverified assets are excluded from the artifact. The first prototype reuses the installed Superb Warfare entity/renderer/resources; it does not claim to port all 1.0.6 behavior.

## Located NeoForge 1.0.6 source tree

- Published NeoForge 1.0.6: https://modrinth.com/mod/sbw-tactical-drone/version/NeM3aBVy (2026-08-28 14:49 UTC).
- Binary: https://cdn.modrinth.com/data/WG6tgfJf/versions/NeM3aBVy/SBW-Drone-Warfare-1.21.1-NeoForge-1.0.6.jar
- SHA-256: `411c61c05450249bbde15f0153a74ea075336ae4a2f459b19b3c01e4c995801c`.
- Author's source: https://github.com/SmartStreamLabs/SBW-Drone-Warfare-Updated-8-28-2026/tree/9eb3b598083838facc16022738aa06ad48fbfb04/SBW-Drone-Range-1.20.1 (16:06 UTC).
- **Folder labels are misleading.** `SBW-Drone-Range-1.20.1` uses NeoForge ModDev, Java 21, Minecraft 1.21.1, NeoForge 21.1.228 and mod_version 1.0.6. The sibling `SBW-Drone-Warfare-1.21.1` is actually the Forge 1.20.1 tree. The earlier audit inspected the wrong sibling and incorrectly concluded that no NeoForge source existed.
- All 154 assets in the published jar match this NeoForge source tree: 145 byte-for-byte; the remaining 9 differ only in CRLF/LF line endings. All 258 top-level binary class paths have a corresponding Java source path, with no missing or extra paths.
- This establishes the matching author-published source candidate, not a reproducible binary build. The jar embeds no source commit and the repository has no release tag. Direct author confirmation / bytecode-equivalent rebuild remains outstanding; do not claim that either has happened.

The current Fabric prototype still uses the recorded 1.0.1 baseline plus Fabric SBW. No code or assets from the later tree have been imported yet.

## Published Fabric dependency

`superbwarfare-fabric` bf19: https://github.com/netherg-io/superbwarfare-fabric/releases/tag/bf19

Artifact `superbwarfare-0.8.9.1-mc1.21.1-bf19.jar`, SHA-256 `7a32396379bd6526d5d53ef487dc79bac9385f5e3272966e4d5e56bc128a487c`. Gradle downloads it anonymously and verifies this checksum before use. Wrapper distribution is also checksum-pinned. Java 21, Minecraft 1.21.1, Loom 1.11.8, Mojang+Parchment 2024.11.17, Fabric Loader 0.19.3/API 0.116.15.

## Asset decisions

The Fabric artifact contains no upstream binary textures, sounds, OBJ or geometry. Existing SBW resources are referenced from the required installed mod (LGPL-3.0-only); the provisional inventory icon uses a vanilla texture.

All original assets marked “replace before release” in CREDITS.md remain excluded. Advanced models will use owned geometry or verified attributed alternatives; sounds require owned recordings/procedural synthesis or a verified compatible source. Geranium-2 and Vampire UAV NC assets remain excluded.

Signal Jammer source found: https://sketchfab.com/3d-models/signal-jammer-draft-b567fbe6f16a43969fc204ee0f90b06d by AspectStudios, CC Attribution. The OBJ header's author/title match this page; the texture's exact relationship remains unverified and it is not distributed.
