# SBW Drone Warfare 1.0.1 Changelog

This is the big SBW Drone Warfare update. The addon has grown from a drone range/config addon into a full tactical drone warfare expansion for Superb Warfare.

## New Drones

* Added the new FPV Drone.
* Added the new LUCAS Drone.
* FPV Drone keeps a small quadcopter-style flight feel.
* LUCAS Drone uses arcade fixed-wing flight instead of quadcopter hover physics.
* LUCAS has assisted takeoff, throttle, turning, climb, dive, fuel, stronger crash behavior, and its own UAV HUD.
* FPV and LUCAS drone items can stack up to 64.
* Added improved item icons, models, sounds, tooltips, and inventory names for the new drones.

## FPV Drone System

* Added a dedicated FPV-style OSD for the FPV Drone.
* Added FPV camera options: FOV, camera tilt, fisheye effect, and camera vibration.
* Added FPV drone engine audio and volume config.
* Added airborne volume boost config.
* Added drone monitor hum audio config.
* Added drone screenshot keybind and screenshot feedback.
* Added FPV drone speed config.
* Added better crash/explosion behavior for FPV drones.

## LUCAS Drone System

* Added LUCAS fixed-wing flight controls.
* Added assisted takeoff so LUCAS can launch from the ground.
* Added faster LUCAS flight tuning.
* Added Shift dive behavior.
* Added LUCAS-specific inventory title.
* Added LUCAS fuel system.
* Added LUCAS engine audio with configurable volume up to 500%.
* Added LUCAS UAV-style HUD.
* Added LUCAS HUD scale config.
* Added stronger LUCAS crash explosion.
* Added distant explosion sounds for drone explosions.
* Added extreme-distance explosion sound for very far explosions.
* Improved LUCAS model scale, hitbox, propeller visibility, and visual details.

## Fuel System

* Added Empty Jerrycan.
* Added Gasoline Canister.
* Added Gasoline Jerrycan.
* Added LUCAS fuel tank.
* LUCAS must be refueled on the ground.
* Gasoline Jerrycan refuels LUCAS and returns an Empty Jerrycan.
* Added low fuel and empty fuel warnings.
* LUCAS engine stops/glides when fuel runs out unless config allows fuel-free flight.
* Added config for tank size, fuel drain, climb drain, low fuel warning, and fuel-free flight.

## Fuel Mixer

* Added Fuel Mixer block.
* Fuel Mixer produces Gasoline Canisters.
* Inputs: Coal or Charcoal, Blaze Powder, and Redstone.
* Output: Gasoline Canister.
* Added Fuel Mixer GUI with input slots, output slot, and progress.
* Added Fuel Mixer particles and sound while active.
* Added clearer Fuel Mixer tooltips so players understand how to make Gasoline Canisters.
* Added Fuel Mixer process time config.
* Added Gasoline Jerrycan fuel amount config.

## Drone Modules

* Added Spotlight Module.
* Added Fiber Optic Spool Upgrade.
* Modules install through the drone inventory/module layer.
* FPV and LUCAS can use module-based systems without rewriting the base drone controls.

## Spotlight System

* Added toggleable drone spotlight.
* Spotlight requires the module if configured.
* Added spotlight HUD text.
* Added spotlight energy drain.
* Added real light projection support.
* Added spotlight range, real-light range, energy cost, and auto-off config.
* Spotlight turns off when control stops or battery is empty.

## Fiber Optic Link System

* Added WIRELESS and FIBER\_OPTIC link modes.
* Wireless remains the default behavior.
* Fiber optic activates when the Fiber Optic Spool Upgrade is installed.
* Fiber optic ignores normal jammers unless config says otherwise.
* Added cable length, tension, break delay, spool damage, and recovery behavior.
* Added fiber optic HUD data: LINK, CABLE, TENSION, and SPOOL.
* Added visible fiber optic cable rendering.
* Added damageable fiber optic cable segment entities.
* Cable can be shot, hit, or cut.
* If the cable is severed, FPV control disconnects and the spool takes damage.
* Broken fiber cable can remain in the world as recoverable cable.
* Players can recover broken fiber cable with Shift + Right Click.
* Added many fiber optic config options including cable length, break delay, battery drain multiplier, debug options, visual thickness, segment spacing, hitbox size, health, and projectile break behavior.

## Battery System

* Added drone battery recharge behavior.
* Drones can pull energy from compatible Superb Warfare batteries placed in the drone inventory.
* Added low battery and empty battery messages.
* Added battery capacity, drain, transfer, low warning, and return-home config.

## Jammer Block

* Added placeable Jammer block.
* Replaced harsh instant jamming with progressive signal interference.
* Jam progress builds up while inside range.
* Jam progress recovers after leaving range.
* Close drones jam faster than drones near the edge.
* Hard jam only happens after enough interference buildup.
* Added jammer GUI range control.
* Added gamertag whitelist for trusted players.
* Added jammer config for range, buildup, recovery, hard jam threshold, interference threshold, friendly drone behavior, and debug logging.

## Handheld Drone Radar / RF Detector

* Upgraded handheld Drone Jammer into a drone radar / RF detector.
* Detects FPV Drone, LUCAS Drone, and supported Superb Warfare drones.
* Added compact radar HUD.
* Shows drone count, closest distance, signal strength, mode, and link type.
* Added directional blips based on player facing.
* Added beeping audio that speeds up when drones get closer.
* Fiber optic drones are detected but shown as resistant to normal jamming.
* Added radar range, beep, volume, HUD scale, compact mode, offset, and debug config.

## Anti-Drone Defenses

* Added Anti-Drone Net.
* Added Anti-Drone Net Carpet.
* Added Anti-Drone Net Panel.
* Anti-Drone Net uses wool-like sound.
* Added transparent/cutout net texture with see-through holes.
* Anti-Drone Net Panel is slab-style with bottom, top, and double placement.
* Anti-Drone Net Panel does not connect like fences, panes, or iron bars.
* Panels can be placed side by side cleanly.
* Anti-drone nets trap or disconnect drones that collide with or pass very close.
* Anti-drone nets can cut fiber optic cables if configured.

## Drone Detection Siren

* Added Drone Detection Siren block.
* Detects nearby drones.
* Plays alarm and can output redstone.
* Added gamertag whitelist so trusted players do not trigger the siren.

## Extraction Crate

* Added Extraction Crate block.
* Drones can land on top of it.
* The crate extracts items from the drone inventory.
* Players can open it like a chest to collect the items.

## Thermal Vision and Weather Effects

* Added thermal vision toggle for drone FPV mode.
* Thermal vision highlights living entities.
* Added thermal HUD indicator.
* Added rain droplet camera effect.
* Added storm/rain visual interference options.
* Added thermal range, noise, and highlight config.

## Camera, HUD, and UI

* Added separate FPV Drone HUD and LUCAS Drone HUD.
* FPV Drone now uses a cleaner FPV/OSD style.
* LUCAS uses a military UAV-style HUD.
* Added config GUI with categories for Drone, LUCAS Flight, Fuel, Battery, Jammer, Weather, Spotlight, and FPV Drone.
* Added multi-page config sections where needed.
* Added minimap options to hide the player marker while controlling FPV or LUCAS drones.
* Added optional Xaero's Minimap and JourneyMap compatibility without hard dependencies.

## Audio

* Added FPV drone engine sound.
* Added LUCAS engine sound.
* Added monitor hum sound.
* Added drone radar beep.
* Added Fuel Mixer active sound.
* Added distant and extreme-distance drone explosion sounds.
* Added audio sliders for FPV and LUCAS drone systems.

## Compatibility

* Added Superb Warfare compatibility abstraction layer.
* Added runtime support for Superb Warfare 0.8.8 final and 0.8.9 final.
* Added safer wrappers for drone detection, linked monitors, owners, batteries, and drone state.
* Added optional Xaero's Minimap compatibility.
* Added optional JourneyMap compatibility.
* No hard dependency on Xaero or JourneyMap.

## Recipes and Crafting

* Added recipes for new blocks/items where needed.
* Added Vehicle Assembling Table support for drone crafting.
* Added Fuel Mixer production flow for gasoline.
* Added Gasoline Canister + Empty Jerrycan recipe for Gasoline Jerrycan.
* Added recipes for modules and anti-drone systems.

## Visual and Asset Updates

* Renamed the mod to SBW Drone Warfare.
* Updated mod logo.
* Updated item icons and textures for multiple items.
* Added custom models/textures for FPV Drone, LUCAS Drone, jerrycans, jammer, spotlight module, and anti-drone net systems.
* Added third-party asset credit documentation.

## Fixes and Improvements

* Fixed player body/skin visibility while controlling drones.
* Fixed drone sound attenuation so drone audio follows the drone instead of playing globally.
* Fixed monitor exit sound behavior.
* Fixed water behavior so drones disconnect instead of exploding from water contact.
* Fixed drone scale, hitbox, model orientation, propeller placement, and propeller animation issues.
* Fixed creative tab and recipe issues.
* Fixed config button integration.
* Fixed config GUI text/layout overlap with pages and clearer sections.
* Fixed item model positioning for jerrycans and other items.
* Fixed siren/jammer whitelist behavior.
* Fixed anti-drone net behavior multiple times until it became a close-range trap instead of broken collision logic.
* Fixed fiber optic HUD staying visible after leaving drone control.
* Fixed fiber cable visual thickness and damage behavior.
* Fixed minimap player marker hiding config visibility for FPV and LUCAS.

## Notes

* This addon still depends on Superb Warfare.
* Normal wireless drone behavior remains available.
* Fiber optic mode is optional.
* Most advanced systems are configurable from the in-game config GUI.

