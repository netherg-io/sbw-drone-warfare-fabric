# SBW Drone Warfare — Fabric 1.21.1 port

Public Fabric port of [SBW Drone Warfare](https://github.com/SmartStreamLabs/-SBW-Drone-Warfare), by Team SmartStreamLabs, maintained by netherg-io. Not affiliated with the original authors or Mojang.

## Status

S2 flight: a deployable FPV drone with its own server-side quad model (four motor thrust points, mass, inertia, drag, airmode mixer), Angle and Acro modes and a throttle axis, payload mass, a 4S LiPo battery with voltage sag, a kamikaze contact fuze with a safe arming distance, a radio link with range, obstruction and jamming (handheld Signal Jammer), a fibre-optic variant and a thrust-driven motor tone. Control, session checks, camera and HUD reuse the Superb Warfare monitor. **This is not a production replacement.** Tracking: [S1](https://github.com/netherg-io/sbw-drone-warfare-fabric/issues/1), [full port](https://github.com/netherg-io/blockfield-releases/issues/4).

### Flying the FPV drone

Place the drone, link a Superb Warfare monitor to it (right-click the drone), then use the monitor to take control. Controls are the Superb Warfare vehicle bindings:

| Input | Effect |
|---|---|
| Forward/back (W/S) | pitch: tilt angle in Angle mode, pitch rate in Acro |
| Left/right (A/D) | roll, same split |
| Mouse X | yaw rate |
| Up/down (Space/Shift) | raise/lower the throttle; it stays where you leave it (hover ≈ 25 %) |
| SBW "Ctrl" binding | toggle Angle/Acro |

Angle mode limits tilt to 45° and levels itself when the sticks are released. Acro holds the current attitude and allows flips. The FPV camera is fixed to the frame with 20° uptilt and rolls with it; the addon applies the roll itself because the SBW monitor camera skips its own roll hook on Fabric. The HUD line shows mode, throttle, speed, pack voltage and charge, and with a payload its mass and SAFE/ARMED; it turns red below 3.5 V per cell under load. Without an active control session, on an empty battery or without a link, the drone levels, descends at 90 % of its current hover throttle and stops its motors on the ground. The model runs only on the server at the 20 Hz tick, so client frame rate does not change flight results. Other players see the model's pitch interpolated linearly between ticks. SBW's fixed 150-block range and its signal-loss explosion no longer apply to these drones; the link model below decides (`FpvDrone.getMaxControlDistance()` is far beyond the link's 2 km cap, so SBW never fires its blast first).

**Pilot view past the view distance.** Vanilla sends a player only the chunks and entities within his view distance of his body; past it (96 m on a 6-chunk server) the pilot's client used to lose the drone, and SBW's monitor dropped control while the radio was still full. While a pilot flies an FPV drone through an active monitor, `RemoteView` makes the drone his viewpoint on the server: his chunk cache centre, chunk tracking and entity tracking distance follow the drone (`ChunkMapMixin`, `TrackedEntityMixin`), and a ticket loads up to 6 chunks around it. The ticket expires 20 ticks after the last refresh, and the view returns to the body when the monitor is off, the drone is gone or the pilot leaves. The body stays where it is, vulnerable and tracked by other players; the pilot does not see his body's surroundings while flying. At most 16 remote views at once. `./gradlew runGametest` checks 60, 120 and 300 m on a 6-chunk view. Model constants live in `QuadFlightModel`, `Battery`, `Payload`, `RadioLink`, `FpvLink` and `MotorSound`; `./gradlew test` checks hover, tilt limit, self-levelling, acro hold, loop continuity, camera uptilt, axes, drag, saturation, payload hover and inertia, sag, flight times, arming distance, the nose-strike rule, the link budget, jamming burn-through, failsafe timing, fibre pay-out and the motor tone mapping.

### Link: radio, jamming, fibre

- **Radio (`cubed_fpv_drone`).** The drone has two links, computed on the server:
  - a 2.4 GHz control uplink (operator → drone, 100 mW, LoRa-class, decodes down to −5 dB SNR);
  - a 5.8 GHz analog video downlink (drone → operator, 400 mW, clean from 20 dB SNR, unwatchable below 3 dB).

  Received power is TX power plus antenna gains, minus free-space path loss, minus per-block obstruction along the line of sight. Obstruction is 8 dB per opaque block for control and 12 dB for video; glass, leaves and fences count 0.3 block. It is resampled every 5 ticks with a bounded voxel walk that never loads chunks. In open air both links reach past any map; a hard 2 km cap (failsafe beyond it) bounds the walk and SBW's chunk tickets around a far drone. Behind a two-block wall the video breaks up while control holds; behind a hill or ten blocks of stone both are gone. Between −5 and 0 dB control frames drop at random: the drone holds the last sticks, as a real receiver does. After 10 ticks without a frame the receiver goes to failsafe (level, descend, land). When the link comes back, the pilot has control again. The HUD shows `LQ` (control frames) and `VID`; analog noise streaks cover the view as the video degrades, then `NO VIDEO`.
- **Signal Jammer (`signal_jammer`).** Right-click toggles it; it jams while held in either hand by a living, non-spectator player. It radiates 100 mW on both bands. Its received power adds to the noise of the drone's control receiver and of the operator's video receiver, so jamming is burn-through physics: a jammer near the drone kills control, one near the operator blinds the video, and an operator close to the drone can fly through the jamming. Against a drone flown from 40 m, a jammer ≈20 m from the drone takes control; from 150 m, ≈80 m. The placeholder item model is vanilla's lightning rod.
- **Fibre (`fibre_fpv_drone`).** Same airframe on a 3 km spool of 0.1 kg/km fibre (plus a 0.1 kg bobbin; the spool mass counts as payload and falls as it pays out). The link is ideal: jammers and walls do nothing. The fibre is laid along the flight path (not the straight-line distance), and it snaps when the path exceeds the spool, which is a failsafe. The laid path is synced as at most 128 points and drawn as a line; it belongs to the drone, so it disappears with it. HUD: `FIBRE paid-out/spool km`. Known limit: picking the drone up and deploying it again gives a fresh spool.
- **Motor sound.** SBW's drone engine loop, which derives its pitch from the synced power: the addon sets power so the pitch follows motor rpm (the square root of thrust, 0.7 at idle to 1.25 at full) and the volume follows thrust. The motors stop, and the loop with them, when the drone is disarmed on the ground or removed. Doppler is SBW's approximation. The addon adds no occlusion of its own, so Sound Physics Remastered applies occlusion once when installed. The operator's own FPV view keeps SBW's fixed pitch 1 (the listener sits in the drone).

### Payload, battery and fuze

- **Payload.** A Superb Warfare drone attachment (right-click the drone holding it) adds its real-world mass per mounted unit: C4 0.6 kg, PG-7VL warhead (`rpg_rocket_standard`) 1.5 kg, TBG 2.0 kg, 82 mm mortar shell 3.1 kg, TM-62 9.5 kg, VOG-25 0.25 kg, RGO 0.53 kg; others 0.5 kg. Mass raises the hover throttle (bare 0.8 kg ≈ 21 %, with the PG-7VL ≈ 70 %) and slows attitude changes, because the flight controller gains stay tuned for the bare frame. The 5" frame cannot lift an 82 mm shell or a TM-62. Dropping bombs makes the drone lighter.
- **Battery.** A 4S 1800 mAh LiPo with 1500 mAh usable. Motor power follows momentum theory (≈150 W at a bare hover, 1.1–1.4 kW at full throttle depending on sag). The pack sags through its internal resistance, and available thrust follows the loaded voltage squared, so a tired pack needs more throttle and a heavy drone may no longer hold altitude. A bare hover lasts ≈9 min, brisk flying ≈3 min, and hovering with a PG-7VL ≈1.7 min. When the usable capacity is gone the pilot loses control and the failsafe lands the drone on the reserve. It does not arm again; a new drone brings a full pack, and in-flight or item recharging is not ported.
- **Contact fuze.** A kamikaze attachment arms once the controlled drone is at least `max(15 m, 2 × blast radius)` from its operator. It stays armed like a real fuze (an armed drone flown home is still live) until its warhead or its operator changes. Only the nose zone (a 0.15 m sphere 0.35 m ahead of the frame centre) is a fuze, and it fires only when the nose leads the motion at ≥ 3 m/s. A flat landing, a side graze or a hull bump does nothing. The zone is swept along each tick's motion (a centre and four rim rays against block collision shapes, and entity boxes grown by the zone radius, a nose already inside one counting), so a fast drone cannot pass through a thin block or a player. On a strike the attachment's SBW hit damage goes to the struck entity, and its SBW custom explosion fires at the strike point with the operator as attacker. The payload is spent before the drone is destroyed, so it cannot explode twice. An unarmed warhead is a dud: a crash, shoot-down or the SBW fire key (blocked while SAFE) does not detonate it. An armed drone destroyed in flight still detonates as in SBW.

The `main` branch preserves upstream 1.0.1 (`30e7b655`). Only `src/fabric` is built into the Fabric jar; the original Forge sources and unverified assets under `src/main` are excluded. No later NC-licensed assets were imported.

## Build

Java 21 and Python 3; no private source, Maven credentials or local dependency jars:

```sh
./gradlew --no-daemon build
python3 scripts/check-artifact.py
./gradlew --no-daemon runGametest   # headless server scenarios (src/gametest); CI prints their FPV- lines
```

The wrapper downloads Gradle 8.14.2 with a pinned checksum. Loom 1.11.8 uses Mojang + Parchment 2024.11.17 mappings. Superb Warfare [bf18](https://github.com/netherg-io/superbwarfare-fabric/releases/tag/bf18) is downloaded from its public release and SHA-256 verified by Gradle. CI builds without repository secrets and uploads both jar and sources.

## Test installation

Use Minecraft 1.21.1, Fabric Loader 0.19.3, Fabric API 0.116.15+1.21.1, Superb Warfare bf18 and its normal runtime dependencies, including GeckoLib 4.7.5 and Fabric Language Kotlin. Install the built jar on the dedicated server and both clients.

Spawn `sbwdroneconfig:cubed_fpv_drone` or give its item. The prototype references the installed SBW drone model/texture/animation; its provisional inventory icon uses vanilla iron ingot. In Blockfield, adventure-mode loadout/deployment integration is not switched over yet; test item placement in creative mode. Existing SBW combat and scout drones are unchanged.

Smoke scenario: start dedicated server, connect two clients, summon FPV, check model/texture from both clients, give and place an FPV item, check both see the second entity. 2026-09-24: passed on Minecraft 1.21.1/Fabric with SBW bf18 and Blockfield 1.25.0; the new entity renders on both clients and creative item placement creates it. This does not accept the advanced-flight requirements.

## Provenance and remaining port work

[NOTICE](NOTICE), [asset credits](CREDITS.md), [source/binary audit](docs/PROVENANCE.md), [class/system map](docs/CLASS-MAP.md). The author’s NeoForge 1.0.6 source is in the misleadingly named `SBW-Drone-Range-1.20.1` folder; its assets and class inventory match the published jar, but an equivalent rebuild is not yet verified. Do not claim the published binary's bug fixes are present in this prototype.

Next: adapt addon systems against the pinned source and existing SBW lifecycle, replace unverifiable assets, test control/physics/radio/cleanup on two clients, then integrate stock/reward rules and ship through the versioned Blockfield modpack. Do not deploy the S1 prototype to production.

## License

AGPL-3.0-only, inherited from upstream; modifications by netherg-io. Source and modification history are public. Deployed jars must have a corresponding public tag and release with checksums and source instructions. Third-party assets retain their licenses; see CREDITS.md.
