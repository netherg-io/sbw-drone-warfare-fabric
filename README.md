# SBW Drone Warfare — Fabric 1.21.1 port

Public Fabric port of [SBW Drone Warfare](https://github.com/SmartStreamLabs/-SBW-Drone-Warfare), by Team SmartStreamLabs, maintained by netherg-io. Not affiliated with the original authors or Mojang.

## Status

A Fabric 1.21.1 addon to [Superb Warfare](https://github.com/netherg-io/superbwarfare-fabric) with an FPV drone that flies on its own server-side quad model: four motor thrust points, mass, inertia, drag and an airmode mixer; Angle and Acro modes and a throttle axis; payload mass; a 4S LiPo battery with voltage sag; a kamikaze contact fuze with a safe arming distance; a radio link with range, obstruction and jamming (handheld Signal Jammer); a fibre-optic variant that players can cut; a thrust-driven motor tone with Doppler. Control, session checks, camera and HUD reuse the Superb Warfare monitor. It ships in the [Blockfield](https://github.com/netherg-io/blockfield-releases) modpack as the FPV drones of the drone-operator class. Tracking: [S1](https://github.com/netherg-io/sbw-drone-warfare-fabric/issues/1), [full port](https://github.com/netherg-io/blockfield-releases/issues/4).

## Versions

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.116.15+1.21.1 |
| Superb Warfare (Fabric fork) | [bf19](https://github.com/netherg-io/superbwarfare-fabric/releases/tag/bf19) |
| GeckoLib | 4.7.5 |
| Fabric Language Kotlin | 1.13.7+kotlin.2.2.21 |
| Java | 21 |
| Sound Physics Remastered (optional, client) | tested with 1.21.1-1.5.1 |

## Install

Put the release jar on the dedicated server **and** every client, next to Superb Warfare bf19 and its dependencies (Porting Lib, Forge Config API Port, Accessories, SimpleBedrockModel; see SBW's release notes). Blockfield players get it through the modpack; nobody copies jars by hand there. Sound Physics Remastered is optional; without it the addon muffles the motors behind blocks itself.

Items: `sbwdroneconfig:cubed_fpv_drone` (radio FPV), `sbwdroneconfig:fibre_fpv_drone` (fibre FPV), `sbwdroneconfig:signal_jammer`. Right-click a block to place a drone, link a Superb Warfare monitor to it (right-click the drone with the monitor), then use the monitor to fly.

### Flying the FPV drone

Place the drone, link a Superb Warfare monitor to it (right-click the drone), then use the monitor to take control. Controls are the Superb Warfare vehicle bindings:

| Input | Effect |
|---|---|
| Forward/back (W/S) | pitch: tilt angle in Angle mode, pitch rate in Acro |
| Left/right (A/D) | roll, same split |
| Mouse X | yaw rate |
| Up/down (Space/Shift) | raise/lower the throttle; it stays where you leave it (hover ≈ 25 %) |
| SBW "Ctrl" binding | toggle Angle/Acro |

Angle mode limits tilt to 45° and levels itself when the sticks are released. Acro holds the current attitude and allows flips. The FPV camera is fixed to the frame with 20° uptilt and rolls with it; the addon applies the roll itself because the SBW monitor camera skips its own roll hook on Fabric. The HUD line shows mode, throttle, speed, pack voltage and charge, and with a payload its mass and SAFE/ARMED; it turns red below 3.5 V per cell under load. Without an active control session, on an empty battery or without a link, the drone levels, descends at 90 % of its current hover throttle and stops its motors on the ground. The model runs only on the server at the 20 Hz tick, so client frame rate does not change flight results. Other players see the model's attitude smoothed over three client ticks (`AttitudeLerp`, like vanilla's position lerp), so attitude updates that reach a tick in bunches do not make the tilt stall and jump; the pilot's own view is not smoothed. SBW's fixed 150-block range and its signal-loss explosion no longer apply to these drones; the link model below decides (`FpvDrone.getMaxControlDistance()` is far beyond the link's 2 km cap, so SBW never fires its blast first).

**Pilot view past the view distance.** Vanilla sends a player only the chunks and entities within his view distance of his body; past it (96 m on a 6-chunk server) the pilot's client used to lose the drone, and SBW's monitor dropped control while the radio was still full. While a pilot flies an FPV drone through an active monitor, `RemoteView` makes the drone his viewpoint on the server: his chunk cache centre, chunk tracking and entity tracking distance follow the drone (`ChunkMapMixin`, `TrackedEntityMixin`), and a ticket loads up to 6 chunks around it. The ticket expires 20 ticks after the last refresh, and the view returns to the body when the monitor is off, the drone is gone or the pilot leaves. The body stays where it is, vulnerable and tracked by other players; the pilot does not see his body's surroundings while flying. At most 16 remote views at once. A second, smaller ticket sits where the drone will be 2 s ahead (capped at 96 m), so fast flight does not outrun chunk loading; tickets are placed only inside the world border. The pilot's pending chunks are sent nearest to the drone first (`PlayerChunkSenderMixin`). A drone that jumps (a teleport, a lag correction) into loaded but non-ticking chunks is re-centred from the server tick; one that jumps into unloaded chunks takes its ticket with it, the chunks load and the flight goes on. SBW's client keeps its input sequence on the drone entity object, so when the drone leaves the pilot's client and comes back, a new control session starts on both ends (`FpvDrone.startSeenByPlayer`); before that fix input was rejected until the monitor was toggled. Only a flown drone loads chunks: SBW's per-vehicle keep-loaded tickets are off for the FPV (`KeepChunkLoaded: false`), so an idle drone does not keep its area loaded.

**Ending a flight.** Every exit goes through SBW's teardown (`DroneControlAccess.resetInput`/`stopMonitor`): monitor off, drone destroyed or removed, operator death, disconnect, dimension change (which is also how a Blockfield round end or room close reaches the pilot). The client is told to reset its camera, the monitor is off, the drone has no session and rejects any further input (a stale client cannot revive the flight), the view returns to the body and the drone's tickets expire within 20 ticks. The drone then levels and lands on failsafe if it is still there. A lost link is not an exit: the view stays on the drone with `NO VIDEO`/`LQ 0`, the drone ignores the held sticks in failsafe, and it obeys again as soon as the link returns. A reconnecting operator gets no session back; his monitor starts one new session on the same drone. Model constants live in `QuadFlightModel`, `Battery`, `Payload`, `RadioLink`, `FpvLink` and `MotorSound`; `./gradlew test` checks hover, tilt limit, self-levelling, acro hold, loop continuity, camera uptilt, axes, drag, saturation, payload hover and inertia, sag, flight times, arming distance, the nose-strike rule, the link budget, jamming burn-through, failsafe timing, fibre pay-out and the motor tone mapping.

### Link: radio, jamming, fibre

- **Radio (`cubed_fpv_drone`).** The drone has two links, computed on the server:
  - a 2.4 GHz control uplink (operator → drone, 100 mW, LoRa-class, decodes down to −5 dB SNR);
  - a 5.8 GHz analog video downlink (drone → operator, 400 mW, clean from 20 dB SNR, unwatchable below 3 dB).

  Received power is TX power plus antenna gains, minus free-space path loss, minus per-block obstruction along the line of sight. Obstruction is 8 dB per opaque block for control and 12 dB for video; glass, leaves and fences count 0.3 block. It is resampled every 5 ticks with a bounded voxel walk that never loads chunks. In open air both links reach past any map; a hard 2 km cap (failsafe beyond it) bounds the walk and SBW's chunk tickets around a far drone. Behind a two-block wall the video breaks up while control holds; behind a hill or ten blocks of stone both are gone. Between −5 and 0 dB control frames drop at random: the drone holds the last sticks, as a real receiver does. After 10 ticks without a frame the receiver goes to failsafe (level, descend, land). When the link comes back, the pilot has control again. The HUD shows `LQ` (control frames) and `VID`; analog noise streaks cover the view as the video degrades, then `NO VIDEO`.
- **Signal Jammer (`signal_jammer`).** Right-click toggles it; it jams while held in either hand by a living, non-spectator player. It radiates 100 mW on both bands. Its received power adds to the noise of the drone's control receiver and of the operator's video receiver, so jamming is burn-through physics: a jammer near the drone kills control, one near the operator blinds the video, and an operator close to the drone can fly through the jamming. Against a drone flown from 40 m, a jammer ≈20 m from the drone takes control; from 150 m, ≈80 m. Its icon is the addon's own (CREDITS.md).
- **Fibre (`fibre_fpv_drone`).** Same airframe on a 3 km spool of 0.1 kg/km fibre (plus a 0.1 kg bobbin; the spool mass counts as payload and falls as it pays out). The link is ideal: jammers and walls do nothing. The fibre is laid along the flight path (not the straight-line distance), and it snaps when the path exceeds the spool, which is a failsafe. The laid path is synced as at most 128 points (older points are thinned) and drawn as a line; it belongs to the drone, so it disappears with it, and there are no cable entities. HUD: `FIBRE paid-out/spool km`. **Cutting:** any arm swing a player makes (attack, mining, placing; operator and teammates included) that passes within 0.4 m of the laid fibre or its free end, within reach and with nothing solid in between, cuts it there for good; the operator sees `FPV: FIBRE CUT` and the drone goes to failsafe. **Used spool:** picking up a fibre drone that has laid fibre (sneak + empty hand or crowbar) returns an item that carries the paid-out length; deploying it restores that length (the tooltip shows the fibre left), so a pickup never gives a fresh 3 km spool.
- **Motor sound.** SBW's drone engine loop (128-block attenuation), which derives its pitch from the synced power: the addon sets power so the pitch follows motor rpm (the square root of thrust, 0.7 at idle to 1.25 at full) and the volume follows thrust. The motors stop, and the loop with them, when the drone is disarmed on the ground or removed. On the client (`VehicleSoundInstanceMixin`, FPV loops only):
  - Doppler is physical: pitch × c / (c − closing speed), c = 343 m/s, from the change of the listener–drone distance (so the listener's own motion counts); a camera jump is not motion.
  - The pilot flying through his monitor hears his own motor tone (SBW pinned it to 1 inside the drone's camera), without Doppler or obstruction.
  - Obstruction is applied once. With Sound Physics Remastered, which by default (`update_moving_sounds=false`) evaluates a looping sound only when it starts, the addon asks it to re-evaluate each FPV loop every 5 ticks through its `SoundPhysics.processSound` (called reflectively: no dependency, no crash without it). Without it the addon muffles 6 dB per solid block between drone and listener (16 blocks at most, loaded chunks only), so a drone overhead is silent deep underground (upstream issue #9).

### Payload, battery and fuze

- **Payload.** A Superb Warfare drone attachment (right-click the drone holding it) adds its real-world mass per mounted unit: C4 0.6 kg, PG-7VL warhead (`rpg_rocket_standard`) 1.5 kg, TBG 2.0 kg, 82 mm mortar shell 3.1 kg, TM-62 9.5 kg, VOG-25 0.25 kg, RGO 0.53 kg; others 0.5 kg. Mass raises the hover throttle (bare 0.8 kg ≈ 21 %, with the PG-7VL ≈ 70 %) and slows attitude changes, because the flight controller gains stay tuned for the bare frame. The 5" frame cannot lift an 82 mm shell or a TM-62. Dropping bombs makes the drone lighter.
- **Battery.** A 4S 1800 mAh LiPo with 1500 mAh usable. Motor power follows momentum theory (≈150 W at a bare hover, 1.1–1.4 kW at full throttle depending on sag). The pack sags through its internal resistance, and available thrust follows the loaded voltage squared, so a tired pack needs more throttle and a heavy drone may no longer hold altitude. A bare hover lasts ≈9 min, brisk flying ≈3 min, and hovering with a PG-7VL ≈1.7 min. When the usable capacity is gone the pilot loses control and the failsafe lands the drone on the reserve. It does not arm again; a new drone brings a full pack, and in-flight or item recharging is not ported.
- **Durability.** 1 HP, as upstream 1.0.6: any hit, SBW's collision damage from about 10 m/s into a wall (1.2 at 12 m/s), or water brings the 5" frame down. Upstream's ×8 incoming-damage multiplier is not ported; with 1 HP it would only make 7 m/s bumps fatal.
- **Contact fuze.** A kamikaze attachment arms once the controlled drone is at least `max(15 m, 2 × blast radius)` from its operator. It stays armed like a real fuze (an armed drone flown home is still live) until its warhead or its operator changes. Only the nose zone (a 0.15 m sphere 0.35 m ahead of the frame centre) is a fuze, and it fires only when the nose leads the motion at ≥ 3 m/s. A flat landing, a side graze or a hull bump does nothing. The zone is swept along each tick's motion (a centre and four rim rays against block collision shapes, and entity boxes grown by the zone radius, a nose already inside one counting), so a fast drone cannot pass through a thin block or a player. The sweep runs before the move, so SBW's collision damage cannot break the 1 HP frame first. On a strike the attachment's SBW hit damage goes to the struck entity, and its SBW custom explosion fires at the strike point; both name the drone itself as the direct source and the operator as the attacker, so a server can tell a drone strike from a fired rocket. The payload is spent before the drone is destroyed, and SBW's 5 m "wreck" blast (fired when a vehicle dies with health at or below minus its maximum) cannot happen because the frame's health stops at 0, so a strike explodes exactly once. **Contact zone:** the nose is the only contact zone, as the piezo nose fuze of a real FPV warhead (PG-7VL or TBG on a 5" frame): a hit by the body, landing gear or props is a crash, not a detonation. `FuzeGameTest` flies armed drones at 30, 60 and 100 m/s into a glass pane wall, iron bars, a fence, one block of stone and a mob: one explosion each time, at the obstacle. An unarmed warhead is a dud: a crash, shoot-down or the SBW fire key (blocked while SAFE) does not detonate it. An armed drone destroyed in flight still detonates as in SBW.

## Settings

There is no config file. Server owners tune through what the game already has:
- **Server view distance** sets how far a pilot sees around his drone (the view ticket radius is min(6, view distance)).
- **Blockfield** (`classes/drone.json` → `drone_settings`): `initial_fpv`, `initial_fibre_fpv`, `max_active_fpv`; launch, damage and statistics rules live in Blockfield, not here.
- **Sound Physics Remastered**: works as installed; `update_moving_sounds` does not need to be on for FPV motors.
- **Model constants** (balance knobs, change in code with their tests): `QuadFlightModel`, `Battery`, `Payload`, `RadioLink`, `FpvLink` (spool, cable budget, 2 km radio cap), `MotorSound`, `RemoteView` (view radius, 16 views, look-ahead).

## Differences from upstream 1.0.6

- Fabric 1.21.1 on the Superb Warfare Fabric fork instead of NeoForge; only `src/fabric` is built.
- Flight: an own server-side quad model (Angle/Acro, throttle axis, payload mass, battery) instead of upstream's arcade drone flight.
- Link: a radio budget with obstruction and burn-through jamming instead of fixed ranges; SBW's 150-block range explosion does not apply.
- Fuze: a nose contact fuze with a safe arming distance; unarmed warheads are duds; one explosion per strike.
- Fibre: a synced path of at most 128 points drawn as a line, no cable entities; cut by player swings; the used spool stays with a picked-up drone.
- Pilot view past the view distance, session restart on re-pairing, chunk loading only for a flown drone.
- Sound: physical Doppler, the pilot hears his motors, Sound Physics re-evaluation or own occlusion.
- Not ported: the Lucas fixed-wing drone, placeable/vehicle jammers, the drone detector and siren, anti-drone nets, the fuel system, spotlight, extraction crate, the config screens. No upstream model, texture or sound is shipped (docs/ASSET-PLAN.md): the FPV uses Superb Warfare's drone model and engine loop by reference, the item icons are our own.

## Known limits

- The FPV entity uses Superb Warfare's drone model, not a dedicated 5" quad model.
- Only the handheld Signal Jammer exists; no placeable or vehicle-mounted jammers, no detector.
- Explosions do not cut fibre; only player swings do.
- A drone that jumps into unloaded chunks leaves the pilot's client for the time its chunks take to load.
- At most 16 pilots at once get the view past the view distance; more keep the vanilla view from the body.
- With Sound Physics installed but disabled in its config, FPV motors get no occlusion at all.
- The Blockfield game rules (who may launch, whom a drone may hurt, stock, statistics) are in Blockfield, not in this addon.

## Build and test

Java 21 and Python 3; no private source, Maven credentials or local dependency jars:

```sh
./gradlew --no-daemon build                # jar, sources jar, unit tests (src/fabricTest)
python3 scripts/check-artifact.py          # the jar ships only allowed assets
./gradlew --no-daemon runGametest          # headless dedicated server with SBW (src/gametest)
```

CI (`.github/workflows/build.yml`) runs all three on every push and prints the gametests' `FPV-` result lines as annotations; the `gametest` artifact has the server log and `junit.xml`.

Game tests (each logs one `FPV-…` line with its numbers):
- `PilotViewGameTest`: the pilot keeps his drone and its chunk at 60/120/300 m on a 6-chunk view; after the monitor is off the view returns and the ticket expires.
- `PilotResyncGameTest`: input after a jump into loaded, non-ticking and unloaded areas, and at 40 m/s with a client taking one chunk per tick. `MockPilotClient` reads the packets the server really sends to a mock player and sends inputs the way SBW's client does.
- `SessionLifecycleGameTest`: every exit path (camera reset, monitor off, no session, stale input rejected, view back, no tickets, drone count), link loss and reconnect.
- `FuzeGameTest`: the fuze at speed into thin blocks and mobs, one explosion.
- `FpvStrikeGameTest`: 1 HP, strike attribution to the drone and its operator.
- `FibreGameTest`: cutting by a real swing packet (reach, walls), the used spool after a pickup.

Releases: a tag `bfN` on `fabric-1.21.1` (pushed, or created by running the Release workflow manually on `fabric-1.21.1` with input `tag`) runs `.github/workflows/release.yml`, which publishes `sbw-drone-warfare-fabric-<version>-bfN.jar`, its sources jar and `SHA256SUMS` as a GitHub Release; the tag is the corresponding source. Blockfield pins a release with `scripts/bump-fork.sh sbwdrone bfN` in blockfield-modpack.

The wrapper downloads Gradle 8.14.2 with a pinned checksum. Loom 1.11.8 uses Mojang + Parchment 2024.11.17 mappings. Superb Warfare [bf19](https://github.com/netherg-io/superbwarfare-fabric/releases/tag/bf19) is downloaded from its public release and SHA-256 verified by Gradle. CI builds without repository secrets and uploads both jar and sources.

## Multiplayer regression scenarios

Run on a dedicated server with two rendering clients (for Blockfield: the rig in blockfield-mod `dev/rig`, full pack, server view distance 6) before a release that touches flight, link, session or sound:
1. **Past the view distance:** fly out to 300 m holding W; throttle and sticks answer the whole way; terrain around the drone is drawn.
2. **Jump:** teleport the flown drone 120 m and 300 m; Space still changes the throttle without touching the monitor.
3. **Exits:** monitor off, drone shot, operator killed by the other player, disconnect and reconnect, dimension change / round end. Each time: the camera is back at the body, no `drone_engine` loop is left for a removed drone, W moves the body.
4. **Observer:** the second client, through a delay proxy (100 ms one way), watches the drone come in from 150 m and cross chunk borders: it appears once in range and moves without backward steps or jumps.
5. **Link:** a jammer at the drone gives `NO VIDEO`/failsafe and the flight resumes when it is off; a fibre drone is immune; the other player's swing cuts its fibre.
6. **Sound:** the pilot's motor pitch follows the throttle; a pass-by shows Doppler; with Sound Physics a listener 30 m underground hears far less than one 30 m away in the open.
7. **Fuze:** an armed strike on a wall and on the other player explodes once.

## For AI agents

- Build and test only with the commands above; run `runGametest` in CI rather than locally on a busy machine. New server behaviour gets a gametest that logs one `FPV-` line with the numbers a reviewer needs, registered in `src/gametest/resources/fabric.mod.json` and in the allow-list of `GameTestRegistryMixin`. Tests that fly a pilot use `PilotResyncGameTest.fly` and `MockPilotClient`; keep concurrent pilots in one batch under the 16-view cap (`RemoteView.MAX_VIEWS`), release forced chunks before the final assertions, and teleport mock players next to the test (they spawn at world spawn, millions of blocks away, where SBW blows the drone up).
- A gametest only ticks entities near its own structure: force the chunks a test flies through (`MockPilotClient.forceChunks`).
- SBW's `setDeltaMovement` refuses an acceleration over 2.83 blocks/tick; build speed up in steps.
- Client behaviour (camera, HUD, sound) cannot be gametested on 1.21.1; prove it on two real clients (the Blockfield rig testbot has `sounds`, `frames` and `state`).
- Do not ship upstream assets: `scripts/check-artifact.py` allows exactly the listed own icons; any new asset needs a CREDITS.md entry with source and licence (no NC/ND).
- Keep Blockfield game rules out of this addon; they belong in blockfield-mod's `drone/` package.
- Every deployed jar must be a `bfN` release of this repository (AGPL §13 source offer).

## Provenance

[NOTICE](NOTICE), [asset credits](CREDITS.md), [asset plan](docs/ASSET-PLAN.md), [source/binary audit](docs/PROVENANCE.md), [class/system map](docs/CLASS-MAP.md). The `main` branch preserves upstream 1.0.1 (`30e7b655`); only `src/fabric` is built into the Fabric jar, and the original Forge sources and unverified assets under `src/main` are excluded. The author's NeoForge 1.0.6 source is in the misleadingly named `SBW-Drone-Range-1.20.1` folder; its assets and class inventory match the published jar, but an equivalent rebuild is not verified, so the published binary's bug fixes are not claimed here. No NC-licensed assets were imported.

## License

AGPL-3.0-only, inherited from upstream; modifications by netherg-io. Source and modification history are public. Deployed jars must have a corresponding public tag and release with checksums and source instructions. Third-party assets retain their licenses; see CREDITS.md.
