# Port map

Names below refer to the preserved 1.0.1 source in `src/main/java/nl/smartstreamlabs/sbwdroneconfig`. Fabric classes live in `src/fabric/java` and the dependency is pinned to SBW bf18. “Present” does not imply runtime acceptance of the final addon.

| System | Upstream | Fabric SBW bf18 | Required addon work |
|---|---|---|---|
| Entity/deployer | `CubedFpvDroneEntity`, `CubedFpvDroneItem`, `AddonEntities`, `AddonItems` | `DroneEntity`, `AbstractDeployerItem` | `FpvDrone` (own flight, SBW lifecycle); upstream 1 HP/damage multiplier and crowbar pickup not ported |
| Session/packets | `DroneControlEvents`, `AddonNetwork`, operator visibility messages | `control/DroneControlSession`, `DroneControlEvents`, owner/generation/sequence checks | Reuse the server-authoritative lifecycle; validate new inputs against it |
| Camera/HUD | `DroneClientViewContext`, `CubedFpvHudOverlay`, `CubedFpvCameraEffectsClient` | monitor camera and drone HUD | FPV: SBW monitor camera with 20° uptilt, roll via `GameRendererMixin`, text HUD line; adapt the remaining addon overlays without hiding or teleporting the operator |
| Physics/payload | `DroneSpeedSystem`, `DronePayloadMounts`, `LucasFixedWingFlightController` | base drone flight and attachment data | Done for the FPV: `QuadFlightModel` (Angle/Acro, throttle axis, four thrust points) driven by SBW session-checked inputs in `FpvDrone.travel`; scout untouched. Payload mass from SBW attachments (`Payload`), contact fuze with safe arming distance in `FpvDrone.move` |
| Battery | `DroneBatterySystem` | vehicle energy data; drone has MaxEnergy=0 | Done for the FPV as a new model (`Battery`: 4S LiPo, drain from motor power, sag, thrust limit, empty → failsafe); upstream SBW-battery recharge not ported |
| Radio/EW | `DroneJammerSystem`, `DroneJammerEvents`, `DroneJammerMode` | distance limits | Done for the FPV as a link budget (`RadioLink`, `FpvLink`): range, obstruction via bounded voxel walk every 5 ticks, handheld Signal Jammer as receiver interference, frame loss and failsafe; placeable jammer blocks/detector not ported |
| Fiber | `FiberOpticLinkSystem`, `FiberOpticLinkMath`, cable entities/renderers | no addon fiber | Done as `fibre_fpv_drone`: path pay-out, spool mass, snap; ≤128 synced points drawn as lines, no segment entities, gone with the drone; cutting by other players not ported |
| Sound | `DroneEngineSoundClient`, `LucasDroneSoundManager`, `AddonSounds` | base drone sound | FPV motor tone via SBW's engine loop (`MotorSound`: pitch ∝ √thrust, volume by thrust, stops when disarmed); no upstream audio imported; true Doppler and operator-position listening not done |
| Chunks | `ChunkTicketManager`, `DroneClientChunkSyncManager`, `ChunkMapTrackedEntityMixin` | session-bound drone tracking/tickets | Done for the FPV pilot as `RemoteView`: chunk cache centre, chunk tracking and entity tracking distance follow the piloted drone (`ChunkMapMixin`, `TrackedEntityMixin`), a self-expiring ticket of at most 6 chunks around it, at most 16 views; game test at 60/120/300 m |

The Fabric prototype inherits SBW behavior rather than importing Forge event handlers or duplicating session state. The original scout entity and item remain untouched. Blockfield inventory/reward integration is deferred until advanced flight is implemented and accepted; the prototype is not installed into the public modpack.
