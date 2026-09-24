# Port map

Names below refer to the preserved 1.0.1 source in `src/main/java/nl/smartstreamlabs/sbwdroneconfig`. Fabric classes live in `src/fabric/java` and the dependency is pinned to SBW bf18. “Present” does not imply runtime acceptance of the final addon.

| System | Upstream | Fabric SBW bf18 | Required addon work |
|---|---|---|---|
| Entity/deployer | `CubedFpvDroneEntity`, `CubedFpvDroneItem`, `AddonEntities`, `AddonItems` | `DroneEntity`, `AbstractDeployerItem` | `FpvDrone`, registry and item are the S1 prototype; port specialized behavior later |
| Session/packets | `DroneControlEvents`, `AddonNetwork`, operator visibility messages | `control/DroneControlSession`, `DroneControlEvents`, owner/generation/sequence checks | Reuse the server-authoritative lifecycle; validate new inputs against it |
| Camera/HUD | `DroneClientViewContext`, `CubedFpvHudOverlay`, `CubedFpvCameraEffectsClient` | monitor camera and drone HUD | Adapt addon overlays without hiding or teleporting the operator |
| Physics/payload | `DroneSpeedSystem`, `DronePayloadMounts`, `LucasFixedWingFlightController` | base drone flight and attachment data | New server-ticked Angle/Acro quad model; do not apply it to the scout |
| Battery | `DroneBatterySystem` | vehicle energy data; drone has MaxEnergy=0 | Adapt battery drain and payload mass |
| Radio/EW | `DroneJammerSystem`, `DroneJammerEvents`, `DroneJammerMode` | distance limits | Adapt LOS/NLOS, jammers and bounded signal sampling |
| Fiber | `FiberOpticLinkSystem`, `FiberOpticLinkMath`, cable entities/renderers | no addon fiber | Adapt with bounded segments and lifecycle cleanup |
| Sound | `DroneEngineSoundClient`, `LucasDroneSoundManager`, `AddonSounds` | base drone sound | Verify occlusion/Doppler/cleanup; replace unverifiable upstream audio |
| Chunks | `ChunkTicketManager`, `DroneClientChunkSyncManager`, `ChunkMapTrackedEntityMixin` | session-bound drone tracking/tickets | Validate budgets, forward loading and release for the new entity |

The Fabric prototype inherits SBW behavior rather than importing Forge event handlers or duplicating session state. The original scout entity and item remain untouched. Blockfield inventory/reward integration is deferred until advanced flight is implemented and accepted; the prototype is not installed into the public modpack.
