# SBW Drone Warfare — Fabric 1.21.1 port

Fabric 1.21.1 port of [[SBW] Drone Warfare](https://github.com/SmartStreamLabs/-SBW-Drone-Warfare)
by Team SmartStreamLabs, as an addon for
[superbwarfare-fabric](https://github.com/netherg-io/superbwarfare-fabric) (Superb Warfare ported
to Fabric). Maintained by [netherg-io](https://github.com/netherg-io) for the Blockfield server.
Not affiliated with SmartStreamLabs or the Superb Warfare team.

Tracking issue: netherg-io/blockfield-releases#4.

## Status

**S0: provenance only, no Fabric code yet.** This branch still contains the upstream Forge 1.20.1
sources of 1.0.1 unchanged. Branches:

- `main` mirrors upstream (`30e7b655e85301f7c3607d2279e8aad268dfa91d`, 1.0.1).
- `fabric-1.21.1` (default) carries the port.

Provenance and modifications: [NOTICE](./NOTICE). Asset sources and license status:
[CREDITS.md](./CREDITS.md). Upstream README: [README-upstream.md](./README-upstream.md).

## Goal

First public release: FPV drone with monitor, operator sessions and camera, Angle/Acro flight
modes, payload mass, battery, radio link and EW jamming, fiber-optic control, contact detonation,
engine sound. The operator stays physically in the world and vulnerable; control and camera always
terminate cleanly.

## Build plan

1. **S0** (this state): fork, license, NOTICE, asset audit.
2. **S1**: Fabric Loom 1.21.1 build (Java 21, Mojang mappings + Parchment, same toolchain as
   superbwarfare-fabric), depending on a published superbwarfare-fabric release instead of local
   SBW paths; choose the source base (1.0.1 here vs. the later NeoForge 1.21.1 tree in
   [SBW-Drone-Warfare-Updated-8-28-2026](https://github.com/SmartStreamLabs/SBW-Drone-Warfare-Updated-8-28-2026)).
3. **S2+**: port gameplay systems, replace assets marked "replace before release" in CREDITS.md.

Planned build, once code exists:

```sh
git clone -b fabric-1.21.1 https://github.com/netherg-io/sbw-drone-warfare-fabric.git
cd sbw-drone-warfare-fabric
./gradlew build --no-daemon   # JDK 21
```

No private repositories, tokens or local jars will be required.

## License

GNU Affero General Public License v3.0, see [LICENSE](./LICENSE), inherited from upstream.
Port changes by netherg-io are released under the same license.

**AGPL-3.0 §13 (network use):** servers running this mod, including the Blockfield server, offer
players the Corresponding Source through this repository: the `fabric-1.21.1` branch and the tag
matching each released jar.

Third-party assets keep their own licenses (see CREDITS.md). Assets under non-commercial licenses,
such as the Geranium-2 model (CC BY-NC-SA 4.0), are not included.
