package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SbwDroneRangeConfig.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AddonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DRONE_MAX_RANGE_VALUE;
    public static final ForgeConfigSpec.DoubleValue DRONE_SPEED_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DRONE_CHUNK_LOADING_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_CHUNK_LOAD_RADIUS_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DRONE_PLAYER_ANCHOR_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DRONE_CRASH_EXPLOSION_DAMAGE_VALUE;
    public static final ForgeConfigSpec.IntValue LUCAS_TAKEOFF_ASSIST_TICKS_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_TAKEOFF_FORWARD_BOOST_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_TAKEOFF_UP_BOOST_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_MIN_SPEED_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_MAX_SPEED_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_ACCELERATION_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_DRAG_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_TURN_RATE_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_PITCH_RATE_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_CLIMB_SPEED_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_DESCEND_SPEED_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_LIFT_STRENGTH_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_ROLL_VISUAL_AMOUNT_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_MIN_LIFT_SPEED_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_STALL_GRAVITY_VALUE;
    public static final ForgeConfigSpec.BooleanValue LUCAS_STALL_ENABLED_VALUE;
    public static final ForgeConfigSpec.BooleanValue LUCAS_CAN_HOVER_VALUE;
    public static final ForgeConfigSpec.IntValue LUCAS_MAX_FUEL_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_FUEL_DRAIN_IDLE_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_FUEL_DRAIN_FLYING_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_FUEL_DRAIN_CLIMB_VALUE;
    public static final ForgeConfigSpec.IntValue LUCAS_LOW_FUEL_WARNING_PERCENT_VALUE;
    public static final ForgeConfigSpec.BooleanValue LUCAS_CAN_FLY_WITHOUT_FUEL_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_HUD_SCALE_VALUE;
    public static final ForgeConfigSpec.IntValue FUEL_MIXER_PROCESS_TIME_VALUE;
    public static final ForgeConfigSpec.IntValue GASOLINE_JERRYCAN_FUEL_AMOUNT_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_THERMAL_VISION_VALUE;
    public static final ForgeConfigSpec.IntValue THERMAL_VISION_RANGE_VALUE;
    public static final ForgeConfigSpec.BooleanValue THERMAL_VISION_KEYBIND_ENABLED_VALUE;
    public static final ForgeConfigSpec.BooleanValue THERMAL_VISION_NOISE_EFFECT_VALUE;
    public static final ForgeConfigSpec.BooleanValue THERMAL_HIGHLIGHT_PLAYERS_VALUE;
    public static final ForgeConfigSpec.BooleanValue THERMAL_HIGHLIGHT_HOSTILE_MOBS_VALUE;
    public static final ForgeConfigSpec.BooleanValue THERMAL_HIGHLIGHT_ANIMALS_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BATTERY_SYSTEM_VALUE;
    public static final ForgeConfigSpec.IntValue MAX_ENERGY_VALUE;
    public static final ForgeConfigSpec.IntValue ENERGY_USE_PER_TICK_VALUE;
    public static final ForgeConfigSpec.IntValue IDLE_ENERGY_USE_PER_TICK_VALUE;
    public static final ForgeConfigSpec.IntValue LOW_ENERGY_THRESHOLD_VALUE;
    public static final ForgeConfigSpec.BooleanValue RETURN_HOME_ON_EMPTY_VALUE;
    public static final ForgeConfigSpec.IntValue BATTERY_TRANSFER_RATE_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DRONE_SIREN_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_SIREN_RANGE_VALUE;
    public static final ForgeConfigSpec.BooleanValue DRONE_SIREN_REDSTONE_OUTPUT_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_SIREN_COOLDOWN_TICKS_VALUE;
    public static final ForgeConfigSpec.BooleanValue DETECT_FRIENDLY_DRONES_VALUE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORED_DRONE_OWNER_NAMES_VALUE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORED_DRONE_OWNER_UUIDS_VALUE;
    public static final ForgeConfigSpec.BooleanValue IGNORE_OWNERLESS_DRONES_VALUE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORED_DRONE_NAMES_VALUE;
    public static final ForgeConfigSpec.BooleanValue IGNORE_UNNAMED_DRONES_VALUE;
    public static final ForgeConfigSpec.BooleanValue BLACKLIST_MATCH_CASE_INSENSITIVE_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DRONE_JAMMER_VALUE;
    public static final ForgeConfigSpec.IntValue JAMMER_RANGE_VALUE;
    public static final ForgeConfigSpec.IntValue JAMMER_BUILD_UP_TICKS_VALUE;
    public static final ForgeConfigSpec.IntValue JAMMER_RECOVERY_TICKS_VALUE;
    public static final ForgeConfigSpec.DoubleValue HARD_JAM_THRESHOLD_VALUE;
    public static final ForgeConfigSpec.DoubleValue CONTROL_INTERFERENCE_THRESHOLD_VALUE;
    public static final ForgeConfigSpec.DoubleValue OVERLAY_START_THRESHOLD_VALUE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_JAMMER_VALUE;
    public static final ForgeConfigSpec.EnumValue<DroneJammerMode> JAMMER_MODE_VALUE;
    public static final ForgeConfigSpec.BooleanValue AFFECT_FRIENDLY_DRONES_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_RADAR_DETECTION_RANGE_VALUE;
    public static final ForgeConfigSpec.BooleanValue DRONE_RADAR_BEEP_ENABLED_VALUE;
    public static final ForgeConfigSpec.DoubleValue DRONE_RADAR_BEEP_VOLUME_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_RADAR_MIN_BEEP_INTERVAL_TICKS_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_RADAR_MAX_BEEP_INTERVAL_TICKS_VALUE;
    public static final ForgeConfigSpec.BooleanValue DRONE_RADAR_ONLY_BEEP_WHEN_HELD_VALUE;
    public static final ForgeConfigSpec.DoubleValue DRONE_RADAR_HUD_SCALE_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_RADAR_HUD_X_OFFSET_VALUE;
    public static final ForgeConfigSpec.IntValue DRONE_RADAR_HUD_Y_OFFSET_VALUE;
    public static final ForgeConfigSpec.BooleanValue DRONE_RADAR_COMPACT_MODE_VALUE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_DRONE_RADAR_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FIBER_OPTIC_MODE_VALUE;
    public static final ForgeConfigSpec.IntValue FIBER_OPTIC_CABLE_LENGTH_VALUE;
    public static final ForgeConfigSpec.IntValue FIBER_OPTIC_CABLE_BREAK_DELAY_TICKS_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_OPTIC_EXTRA_BATTERY_DRAIN_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.BooleanValue NORMAL_JAMMERS_AFFECT_FIBER_OPTIC_VALUE;
    public static final ForgeConfigSpec.BooleanValue ANTI_DRONE_NET_CUTS_FIBER_VALUE;
    public static final ForgeConfigSpec.BooleanValue ANTI_DRONE_NET_PANEL_REQUIRES_SUPPORT_VALUE;
    public static final ForgeConfigSpec.DoubleValue ANTI_DRONE_NET_PANEL_TRIGGER_THICKNESS_VALUE;
    public static final ForgeConfigSpec.DoubleValue ANTI_DRONE_NET_PANEL_VISUAL_THICKNESS_VALUE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_FIBER_OPTIC_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_CABLE_VISUAL_THICKNESS_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_CABLE_VISUAL_ALPHA_VALUE;
    public static final ForgeConfigSpec.ConfigValue<String> FIBER_CABLE_VISUAL_COLOR_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_CABLE_DEBUG_VISUAL_THICKNESS_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_CABLE_SEGMENT_SPACING_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_CABLE_SEGMENT_HEALTH_VALUE;
    public static final ForgeConfigSpec.DoubleValue FIBER_CABLE_SEGMENT_HITBOX_SIZE_VALUE;
    public static final ForgeConfigSpec.BooleanValue FIBER_CABLE_CAN_BE_DAMAGED_BY_PLAYERS_VALUE;
    public static final ForgeConfigSpec.BooleanValue FIBER_CABLE_CAN_BE_DAMAGED_BY_PROJECTILES_VALUE;
    public static final ForgeConfigSpec.BooleanValue FIBER_CABLE_CAN_BE_DAMAGED_BY_EXPLOSIONS_VALUE;
    public static final ForgeConfigSpec.BooleanValue FIBER_CABLE_PROJECTILE_RAYCAST_BREAK_VALUE;
    public static final ForgeConfigSpec.BooleanValue FIBER_CABLE_BREAKS_WHEN_ANY_SEGMENT_DESTROYED_VALUE;
    public static final ForgeConfigSpec.IntValue MAX_FIBER_CABLE_SEGMENTS_PER_DRONE_VALUE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_FIBER_CABLE_RENDER_VALUE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_FIBER_CABLE_SEGMENTS_VALUE;
    public static final ForgeConfigSpec.BooleanValue RENDER_RED_DEBUG_CABLE_VALUE;
    public static final ForgeConfigSpec.BooleanValue RENDER_LEGACY_FIBER_LINE_VALUE;
    public static final ForgeConfigSpec.BooleanValue WEATHER_EFFECTS_ENABLED_VALUE;
    public static final ForgeConfigSpec.DoubleValue RAIN_VISIBILITY_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.DoubleValue STORM_INSTABILITY_STRENGTH_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DRONE_SCREENSHOT_SOUND_VALUE;
    public static final ForgeConfigSpec.DoubleValue DRONE_ENGINE_VOLUME_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.DoubleValue LUCAS_ENGINE_VOLUME_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.DoubleValue DRONE_MONITOR_HUM_VOLUME_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.DoubleValue DRONE_AIRBORNE_VOLUME_MULTIPLIER_VALUE;
    public static final ForgeConfigSpec.DoubleValue FPV_CAMERA_FOV_VALUE;
    public static final ForgeConfigSpec.DoubleValue FPV_CAMERA_TILT_DEGREES_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FPV_FISHEYE_EFFECT_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FPV_CAMERA_VIBRATION_VALUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPOTLIGHT_MODULE_VALUE;
    public static final ForgeConfigSpec.IntValue SPOTLIGHT_RANGE_VALUE;
    public static final ForgeConfigSpec.IntValue SPOTLIGHT_ENERGY_COST_PER_SECOND_VALUE;
    public static final ForgeConfigSpec.BooleanValue SPOTLIGHT_REQUIRES_MODULE_VALUE;
    public static final ForgeConfigSpec.BooleanValue SPOTLIGHT_VISIBLE_TO_OTHER_PLAYERS_VALUE;
    public static final ForgeConfigSpec.BooleanValue SPOTLIGHT_AUTO_OFF_WHEN_BATTERY_EMPTY_VALUE;
    public static final ForgeConfigSpec.BooleanValue SPOTLIGHT_REAL_LIGHT_ENABLED_VALUE;
    public static final ForgeConfigSpec.IntValue SPOTLIGHT_LIGHT_RANGE_VALUE;
    public static final ForgeConfigSpec.IntValue SPOTLIGHT_LIGHT_LEVEL_VALUE;
    public static final ForgeConfigSpec.IntValue SPOTLIGHT_LIGHT_UPDATE_INTERVAL_TICKS_VALUE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_PAYLOAD_MOUNTS_VALUE;

    private static volatile int droneMaxRange;
    private static volatile double droneSpeedMultiplier;
    private static volatile boolean enableDroneChunkLoading;
    private static volatile int droneChunkLoadRadius;
    private static volatile boolean enableDronePlayerAnchor;
    private static volatile boolean enableDroneCrashExplosionDamage;
    private static volatile int lucasTakeoffAssistTicks;
    private static volatile double lucasTakeoffForwardBoost;
    private static volatile double lucasTakeoffUpBoost;
    private static volatile double lucasMinSpeed;
    private static volatile double lucasMaxSpeed;
    private static volatile double lucasAcceleration;
    private static volatile double lucasDrag;
    private static volatile double lucasTurnRate;
    private static volatile double lucasPitchRate;
    private static volatile double lucasClimbSpeed;
    private static volatile double lucasDescendSpeed;
    private static volatile double lucasLiftStrength;
    private static volatile double lucasRollVisualAmount;
    private static volatile double lucasMinLiftSpeed;
    private static volatile double lucasStallGravity;
    private static volatile boolean lucasStallEnabled;
    private static volatile boolean lucasCanHover;
    private static volatile int lucasMaxFuel;
    private static volatile double lucasFuelDrainIdle;
    private static volatile double lucasFuelDrainFlying;
    private static volatile double lucasFuelDrainClimb;
    private static volatile int lucasLowFuelWarningPercent;
    private static volatile boolean lucasCanFlyWithoutFuel;
    private static volatile double lucasHudScale;
    private static volatile int fuelMixerProcessTime;
    private static volatile int gasolineJerrycanFuelAmount;
    private static volatile boolean enableThermalVision;
    private static volatile int thermalVisionRange;
    private static volatile boolean thermalVisionKeybindEnabled;
    private static volatile boolean thermalVisionNoiseEffect;
    private static volatile boolean thermalHighlightPlayers;
    private static volatile boolean thermalHighlightHostileMobs;
    private static volatile boolean thermalHighlightAnimals;
    private static volatile boolean enableBatterySystem;
    private static volatile int maxEnergy;
    private static volatile int energyUsePerTick;
    private static volatile int idleEnergyUsePerTick;
    private static volatile int lowEnergyThreshold;
    private static volatile boolean returnHomeOnEmpty;
    private static volatile int batteryTransferRate;
    private static volatile boolean enableDroneSiren;
    private static volatile int droneSirenRange;
    private static volatile boolean droneSirenRedstoneOutput;
    private static volatile int droneSirenCooldownTicks;
    private static volatile boolean detectFriendlyDrones;
    private static volatile Set<String> ignoredDroneOwnerNames = Set.of();
    private static volatile Set<UUID> ignoredDroneOwnerUUIDs = Set.of();
    private static volatile boolean ignoreOwnerlessDrones;
    private static volatile Set<String> ignoredDroneNames = Set.of();
    private static volatile boolean ignoreUnnamedDrones;
    private static volatile boolean blacklistMatchCaseInsensitive;
    private static volatile boolean enableDroneJammer;
    private static volatile int jammerRange;
    private static volatile int jammerBuildUpTicks;
    private static volatile int jammerRecoveryTicks;
    private static volatile double hardJamThreshold;
    private static volatile double controlInterferenceThreshold;
    private static volatile double overlayStartThreshold;
    private static volatile boolean debugJammer;
    private static volatile DroneJammerMode jammerMode;
    private static volatile boolean affectFriendlyDrones;
    private static volatile int droneRadarDetectionRange;
    private static volatile boolean droneRadarBeepEnabled;
    private static volatile double droneRadarBeepVolume;
    private static volatile int droneRadarMinBeepIntervalTicks;
    private static volatile int droneRadarMaxBeepIntervalTicks;
    private static volatile boolean droneRadarOnlyBeepWhenHeld;
    private static volatile double droneRadarHudScale;
    private static volatile int droneRadarHudXOffset;
    private static volatile int droneRadarHudYOffset;
    private static volatile boolean droneRadarCompactMode;
    private static volatile boolean debugDroneRadar;
    private static volatile boolean enableFiberOpticMode;
    private static volatile int fiberOpticCableLength;
    private static volatile int fiberOpticCableBreakDelayTicks;
    private static volatile double fiberOpticExtraBatteryDrainMultiplier;
    private static volatile boolean normalJammersAffectFiberOptic;
    private static volatile boolean antiDroneNetCutsFiber;
    private static volatile boolean antiDroneNetPanelRequiresSupport;
    private static volatile double antiDroneNetPanelTriggerThickness;
    private static volatile double antiDroneNetPanelVisualThickness;
    private static volatile boolean debugFiberOptic;
    private static volatile double fiberCableVisualThickness;
    private static volatile double fiberCableVisualAlpha;
    private static volatile String fiberCableVisualColor = "light_gray";
    private static volatile double fiberCableDebugVisualThickness;
    private static volatile double fiberCableSegmentSpacing;
    private static volatile double fiberCableSegmentHealth;
    private static volatile double fiberCableSegmentHitboxSize;
    private static volatile boolean fiberCableCanBeDamagedByPlayers;
    private static volatile boolean fiberCableCanBeDamagedByProjectiles;
    private static volatile boolean fiberCableCanBeDamagedByExplosions;
    private static volatile boolean fiberCableProjectileRaycastBreak;
    private static volatile boolean fiberCableBreaksWhenAnySegmentDestroyed;
    private static volatile int maxFiberCableSegmentsPerDrone;
    private static volatile boolean debugFiberCableRender;
    private static volatile boolean debugFiberCableSegments;
    private static volatile boolean renderRedDebugCable;
    private static volatile boolean renderLegacyFiberLine;
    private static volatile boolean weatherEffectsEnabled;
    private static volatile double rainVisibilityMultiplier;
    private static volatile double stormInstabilityStrength;
    private static volatile boolean enableDroneScreenshotSound;
    private static volatile double droneEngineVolumeMultiplier;
    private static volatile double lucasEngineVolumeMultiplier;
    private static volatile double droneMonitorHumVolumeMultiplier;
    private static volatile double droneAirborneVolumeMultiplier;
    private static volatile double fpvCameraFov;
    private static volatile double fpvCameraTiltDegrees;
    private static volatile boolean enableFpvFisheyeEffect;
    private static volatile boolean enableFpvCameraVibration;
    private static volatile boolean enableSpotlightModule;
    private static volatile int spotlightRange;
    private static volatile int spotlightEnergyCostPerSecond;
    private static volatile boolean spotlightRequiresModule;
    private static volatile boolean spotlightVisibleToOtherPlayers;
    private static volatile boolean spotlightAutoOffWhenBatteryEmpty;
    private static volatile boolean spotlightRealLightEnabled;
    private static volatile int spotlightLightRange;
    private static volatile int spotlightLightLevel;
    private static volatile int spotlightLightUpdateIntervalTicks;
    private static volatile boolean debugPayloadMounts;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("drone");
        DRONE_MAX_RANGE_VALUE = builder
                .comment("Maximum allowed distance in blocks between the player and a linked Superb Warfare drone.")
                .defineInRange("droneMaxRange", 100, 1, 30000);
        DRONE_SPEED_MULTIPLIER_VALUE = builder
                .comment("Extra flight speed multiplier applied to FPV drone travel input.")
                .defineInRange("droneSpeedMultiplier", 1.0D,
                        DroneSpeedConfigLimits.MIN_DRONE_SPEED_MULTIPLIER,
                        DroneSpeedConfigLimits.MAX_DRONE_SPEED_MULTIPLIER);
        ENABLE_DRONE_CHUNK_LOADING_VALUE = builder
                .comment("Keep a small area around an actively controlled drone loaded.")
                .define("enableDroneChunkLoading", false);
        DRONE_CHUNK_LOAD_RADIUS_VALUE = builder
                .comment("Chunk radius kept loaded around the drone when chunk loading is enabled.")
                .defineInRange("droneChunkLoadRadius", 1, 0, 4);
        ENABLE_DRONE_PLAYER_ANCHOR_VALUE = builder
                .comment("Move the controlling player with the drone while using the monitor so vanilla chunk loading follows the drone.")
                .define("enableDronePlayerAnchor", true);
        ENABLE_DRONE_CRASH_EXPLOSION_DAMAGE_VALUE = builder
                .comment("Allow drone crash explosions to damage entities and destroy blocks.")
                .define("enableDroneCrashExplosionDamage", true);
        builder.pop();

        builder.push("lucas");
        LUCAS_TAKEOFF_ASSIST_TICKS_VALUE = builder
                .comment("How long the LUCAS drone gets assisted launch thrust after a new control session begins.")
                .defineInRange("lucasTakeoffAssistTicks", 60, 0, 200);
        LUCAS_TAKEOFF_FORWARD_BOOST_VALUE = builder
                .comment("Minimum forward speed injected during the LUCAS drone takeoff assist window.")
                .defineInRange("lucasTakeoffForwardBoost", 1.15D, 0.0D, 4.0D);
        LUCAS_TAKEOFF_UP_BOOST_VALUE = builder
                .comment("Upward lift injected each tick during the LUCAS drone takeoff assist window.")
                .defineInRange("lucasTakeoffUpBoost", 0.24D, 0.0D, 1.0D);
        LUCAS_MIN_SPEED_VALUE = builder
                .comment("Minimum forward cruise speed for the LUCAS fixed-wing drone while throttle is engaged.")
                .defineInRange("lucasMinSpeed", 0.85D, 0.05D, 5.0D);
        LUCAS_MAX_SPEED_VALUE = builder
                .comment("Maximum forward speed for the LUCAS fixed-wing drone.")
                .defineInRange("lucasMaxSpeed", 3.20D, 0.10D, 8.0D);
        LUCAS_ACCELERATION_VALUE = builder
                .comment("How quickly the LUCAS fixed-wing drone blends toward its target forward speed.")
                .defineInRange("lucasAcceleration", 0.085D, 0.001D, 1.0D);
        LUCAS_DRAG_VALUE = builder
                .comment("Drag multiplier applied to LUCAS movement each tick. Lower values bleed speed faster.")
                .defineInRange("lucasDrag", 0.992D, 0.50D, 0.999D);
        LUCAS_TURN_RATE_VALUE = builder
                .comment("Yaw turn rate for the LUCAS fixed-wing drone.")
                .defineInRange("lucasTurnRate", 2.0D, 0.05D, 10.0D);
        LUCAS_PITCH_RATE_VALUE = builder
                .comment("Mouse pitch response rate for the LUCAS fixed-wing drone.")
                .defineInRange("lucasPitchRate", 0.60D, 0.05D, 10.0D);
        LUCAS_CLIMB_SPEED_VALUE = builder
                .comment("Extra upward climb speed while holding Space with the LUCAS drone.")
                .defineInRange("lucasClimbSpeed", 0.08D, 0.0D, 1.0D);
        LUCAS_DESCEND_SPEED_VALUE = builder
                .comment("Extra downward speed while holding Shift with the LUCAS drone.")
                .defineInRange("lucasDescendSpeed", 0.07D, 0.0D, 1.0D);
        LUCAS_LIFT_STRENGTH_VALUE = builder
                .comment("Base arcade lift strength applied when the LUCAS drone has enough throttle to stay airborne.")
                .defineInRange("lucasLiftStrength", 0.060D, 0.0D, 1.0D);
        LUCAS_ROLL_VISUAL_AMOUNT_VALUE = builder
                .comment("Maximum banking roll shown while the LUCAS drone turns.")
                .defineInRange("lucasRollVisualAmount", 25.0D, 0.0D, 90.0D);
        LUCAS_MIN_LIFT_SPEED_VALUE = builder
                .comment("Speed threshold below which the LUCAS drone begins to stall and lose lift.")
                .defineInRange("lucasMinLiftSpeed", 0.45D, 0.05D, 6.0D);
        LUCAS_STALL_GRAVITY_VALUE = builder
                .comment("Extra downward pull applied while the LUCAS drone is stalled.")
                .defineInRange("lucasStallGravity", 0.04D, 0.0D, 1.0D);
        LUCAS_STALL_ENABLED_VALUE = builder
                .comment("Enable harsher fixed-wing stall behavior for the LUCAS drone. Disabled by default for arcade flight.")
                .define("lucasStallEnabled", false);
        LUCAS_CAN_HOVER_VALUE = builder
                .comment("If true, the LUCAS drone can slow to a hover. Intended to stay false for fixed-wing behavior.")
                .define("lucasCanHover", false);
        LUCAS_MAX_FUEL_VALUE = builder
                .comment("Maximum gasoline fuel stored by the LUCAS drone.")
                .defineInRange("lucasMaxFuel", 1000, 1, 100000);
        LUCAS_FUEL_DRAIN_IDLE_VALUE = builder
                .comment("Fuel drained per tick while the LUCAS engine is active on the ground.")
                .defineInRange("lucasFuelDrainIdle", 0.02D, 0.0D, 100.0D);
        LUCAS_FUEL_DRAIN_FLYING_VALUE = builder
                .comment("Fuel drained per tick while the LUCAS drone is flying.")
                .defineInRange("lucasFuelDrainFlying", 0.08D, 0.0D, 100.0D);
        LUCAS_FUEL_DRAIN_CLIMB_VALUE = builder
                .comment("Extra fuel drained per tick while the LUCAS drone climbs or uses takeoff assist.")
                .defineInRange("lucasFuelDrainClimb", 0.14D, 0.0D, 100.0D);
        LUCAS_LOW_FUEL_WARNING_PERCENT_VALUE = builder
                .comment("Fuel percentage at or below which the LUCAS fuel warning appears.")
                .defineInRange("lucasLowFuelWarningPercent", 15, 0, 100);
        LUCAS_CAN_FLY_WITHOUT_FUEL_VALUE = builder
                .comment("If true, LUCAS can keep powered flight with an empty tank. Intended to stay false for gasoline gameplay.")
                .define("lucasCanFlyWithoutFuel", false);
        LUCAS_HUD_SCALE_VALUE = builder
                .comment("Scale multiplier for the LUCAS military UAV camera HUD.")
                .defineInRange("lucasHudScale", 1.0D, 0.60D, 1.50D);
        builder.pop();

        builder.push("fuelMixer");
        FUEL_MIXER_PROCESS_TIME_VALUE = builder
                .comment("Ticks required for the Fuel Mixer to refine coal/charcoal, blaze powder, and redstone into one gasoline canister.")
                .defineInRange("fuelMixerProcessTime", 200, 1, 12000);
        GASOLINE_JERRYCAN_FUEL_AMOUNT_VALUE = builder
                .comment("Fuel added to a grounded LUCAS drone by one Gasoline Jerrycan.")
                .defineInRange("gasolineJerrycanFuelAmount", 1000, 1, 100000);
        builder.pop();

        builder.push("thermalVision");
        ENABLE_THERMAL_VISION_VALUE = builder
                .comment("Enable the Thermal Vision drone overlay toggle on the client.")
                .define("enableThermalVision", true);
        THERMAL_VISION_RANGE_VALUE = builder
                .comment("Maximum range in blocks for Thermal Vision entity highlighting while using the drone monitor.")
                .defineInRange("thermalVisionRange", 64, 8, 256);
        THERMAL_VISION_KEYBIND_ENABLED_VALUE = builder
                .comment("Allow toggling Thermal Vision with the client keybind while using the upgraded drone terminal.")
                .define("thermalVisionKeybindEnabled", true);
        THERMAL_VISION_NOISE_EFFECT_VALUE = builder
                .comment("Render subtle scanline and noise effects while Thermal Vision is enabled.")
                .define("thermalVisionNoiseEffect", true);
        THERMAL_HIGHLIGHT_PLAYERS_VALUE = builder
                .comment("Highlight player entities in Thermal Vision.")
                .define("thermalHighlightPlayers", true);
        THERMAL_HIGHLIGHT_HOSTILE_MOBS_VALUE = builder
                .comment("Highlight hostile mobs in Thermal Vision.")
                .define("thermalVisionHighlightHostileMobs", true);
        THERMAL_HIGHLIGHT_ANIMALS_VALUE = builder
                .comment("Highlight animals and passive mobs in Thermal Vision.")
                .define("thermalVisionHighlightAnimals", true);
        builder.pop();

        builder.push("battery");
        ENABLE_BATTERY_SYSTEM_VALUE = builder
                .comment("Enable the drone battery / energy system.")
                .define("enableBatterySystem", true);
        MAX_ENERGY_VALUE = builder
                .comment("Maximum battery capacity for a drone.")
                .defineInRange("maxEnergy", 1200, 1, 1000000);
        ENERGY_USE_PER_TICK_VALUE = builder
                .comment("Energy consumed each server tick while the drone is actively flying or being controlled.")
                .defineInRange("energyUsePerTick", 1, 0, 100000);
        IDLE_ENERGY_USE_PER_TICK_VALUE = builder
                .comment("Energy consumed each server tick while the drone is idle.")
                .defineInRange("idleEnergyUsePerTick", 0, 0, 100000);
        LOW_ENERGY_THRESHOLD_VALUE = builder
                .comment("Threshold where the controller starts receiving low battery warnings.")
                .defineInRange("lowEnergyThreshold", 200, 0, 1000000);
        RETURN_HOME_ON_EMPTY_VALUE = builder
                .comment("If true, an empty drone drifts back toward its controller while descending.")
                .define("returnHomeOnEmpty", false);
        BATTERY_TRANSFER_RATE_VALUE = builder
                .comment("Maximum energy transferred from an SBW battery item to the drone per right-click.")
                .defineInRange("batteryTransferRate", 100, 1, 1000000);
        builder.pop();

        builder.push("droneSiren");
        ENABLE_DRONE_SIREN_VALUE = builder
                .comment("Enable the Drone Detection Siren block.")
                .define("enableDroneSiren", true);
        DRONE_SIREN_RANGE_VALUE = builder
                .comment("Detection radius in blocks for nearby Superb Warfare drones.")
                .defineInRange("droneSirenRange", 32, 4, 256);
        DRONE_SIREN_REDSTONE_OUTPUT_VALUE = builder
                .comment("Emit a redstone signal while the siren is active.")
                .define("droneSirenRedstoneOutput", true);
        DRONE_SIREN_COOLDOWN_TICKS_VALUE = builder
                .comment("Ticks the siren remains active after losing drone contact.")
                .defineInRange("droneSirenCooldownTicks", 20, 1, 1200);
        DETECT_FRIENDLY_DRONES_VALUE = builder
                .comment("If false, sirens ignore drones owned by the player who placed the siren.")
                .define("detectFriendlyDrones", true);
        IGNORED_DRONE_OWNER_NAMES_VALUE = builder
                .comment("Player names whose owned drones should never trigger any siren.")
                .defineListAllowEmpty("ignoredDroneOwnerNames", List.of(), value -> value instanceof String);
        IGNORED_DRONE_OWNER_UUIDS_VALUE = builder
                .comment("Player UUIDs whose owned drones should never trigger any siren.")
                .defineListAllowEmpty("ignoredDroneOwnerUUIDs", List.of(), value -> value instanceof String);
        IGNORE_OWNERLESS_DRONES_VALUE = builder
                .comment("If true, drones without a stored owner are ignored by sirens.")
                .define("ignoreOwnerlessDrones", false);
        IGNORED_DRONE_NAMES_VALUE = builder
                .comment("Custom drone names that should never trigger any siren.")
                .defineListAllowEmpty("ignoredDroneNames", List.of(), value -> value instanceof String);
        IGNORE_UNNAMED_DRONES_VALUE = builder
                .comment("If true, drones without a custom name are ignored by sirens.")
                .define("ignoreUnnamedDrones", false);
        BLACKLIST_MATCH_CASE_INSENSITIVE_VALUE = builder
                .comment("If true, siren drone-name blacklist checks ignore case.")
                .define("blacklistMatchCaseInsensitive", true);
        builder.pop();

        builder.push("droneJammer");
        ENABLE_DRONE_JAMMER_VALUE = builder
                .comment("Enable the handheld Drone Jammer radar.")
                .define("enableDroneJammer", true);
        JAMMER_RANGE_VALUE = builder
                .comment("Detection and disruption radius in blocks for active drone jammers.")
                .defineInRange("jammerRange", 32, 4, 256);
        JAMMER_BUILD_UP_TICKS_VALUE = builder
                .comment("Ticks a drone at maximum jammer strength needs to build from 0 to full jam progress.")
                .defineInRange("jammerBuildUpTicks", 80, 10, 2400);
        JAMMER_RECOVERY_TICKS_VALUE = builder
                .comment("Ticks a drone takes to recover from full jam progress after leaving jammer coverage.")
                .defineInRange("jammerRecoveryTicks", 120, 10, 2400);
        HARD_JAM_THRESHOLD_VALUE = builder
                .comment("Jam progress threshold where a short hard-lock countdown begins.")
                .defineInRange("hardJamThreshold", 0.95D, 0.50D, 1.0D);
        CONTROL_INTERFERENCE_THRESHOLD_VALUE = builder
                .comment("Jam progress threshold where drone controls start stuttering.")
                .defineInRange("controlInterferenceThreshold", 0.50D, 0.0D, 0.99D);
        OVERLAY_START_THRESHOLD_VALUE = builder
                .comment("Jam progress threshold where the FPV static overlay becomes visible.")
                .defineInRange("overlayStartThreshold", 0.15D, 0.0D, 0.99D);
        DEBUG_JAMMER_VALUE = builder
                .comment("Enable verbose progressive jammer debug logging.")
                .define("debugJammer", false);
        JAMMER_MODE_VALUE = builder
                .comment("Legacy instant jammer behavior mode. Progressive jammer logic now ignores this value.")
                .defineEnum("jammerMode", DroneJammerMode.HARD);
        AFFECT_FRIENDLY_DRONES_VALUE = builder
                .comment("If false, jammers ignore drones owned by the player who placed the jammer.")
                .define("affectFriendlyDrones", true);
        DRONE_RADAR_DETECTION_RANGE_VALUE = builder
                .comment("Detection range in blocks for the handheld Drone Radar / RF Detector display.")
                .defineInRange("droneRadarDetectionRange", 256, 16, 1024);
        DRONE_RADAR_BEEP_ENABLED_VALUE = builder
                .comment("Play pulsing RF detector beeps when the handheld radar detects drones.")
                .define("droneRadarBeepEnabled", true);
        DRONE_RADAR_BEEP_VOLUME_VALUE = builder
                .comment("Base volume for handheld drone radar beeps.")
                .defineInRange("droneRadarBeepVolume", 0.45D, 0.0D, 2.0D);
        DRONE_RADAR_MIN_BEEP_INTERVAL_TICKS_VALUE = builder
                .comment("Fastest allowed handheld radar beep interval in ticks.")
                .defineInRange("droneRadarMinBeepIntervalTicks", 5, 1, 100);
        DRONE_RADAR_MAX_BEEP_INTERVAL_TICKS_VALUE = builder
                .comment("Slowest allowed handheld radar beep interval in ticks.")
                .defineInRange("droneRadarMaxBeepIntervalTicks", 40, 5, 200);
        DRONE_RADAR_ONLY_BEEP_WHEN_HELD_VALUE = builder
                .comment("If true, only play radar beeps while the active Drone Jammer item is held in hand.")
                .define("droneRadarOnlyBeepWhenHeld", true);
        DRONE_RADAR_HUD_SCALE_VALUE = builder
                .comment("Scale for the handheld Drone Radar / RF Detector HUD.")
                .defineInRange("droneRadarHudScale", 0.55D, 0.25D, 1.25D);
        DRONE_RADAR_HUD_X_OFFSET_VALUE = builder
                .comment("Top-right horizontal offset in pixels for the handheld Drone Radar HUD.")
                .defineInRange("droneRadarHudXOffset", 8, 0, 256);
        DRONE_RADAR_HUD_Y_OFFSET_VALUE = builder
                .comment("Top-right vertical offset in pixels for the handheld Drone Radar HUD.")
                .defineInRange("droneRadarHudYOffset", 8, 0, 256);
        DRONE_RADAR_COMPACT_MODE_VALUE = builder
                .comment("Use compact short-label layout for the handheld Drone Radar HUD.")
                .define("droneRadarCompactMode", true);
        DEBUG_DRONE_RADAR_VALUE = builder
                .comment("Enable verbose handheld drone radar debug logging.")
                .define("debugDroneRadar", false);
        builder.pop();

        builder.push("fiberOptic");
        ENABLE_FIBER_OPTIC_MODE_VALUE = builder
                .comment("Enable the optional Fiber Optic spool module for FPV drones.")
                .define("enableFiberOpticMode", true);
        FIBER_OPTIC_CABLE_LENGTH_VALUE = builder
                .comment("Maximum cable length in blocks before a Fiber Optic link starts building tension.")
                .defineInRange("fiberOpticCableLength", 256, 16, 4096);
        FIBER_OPTIC_CABLE_BREAK_DELAY_TICKS_VALUE = builder
                .comment("Ticks an overstretched Fiber Optic cable can stay beyond its length limit before it snaps.")
                .defineInRange("fiberOpticCableBreakDelayTicks", 60, 1, 2400);
        FIBER_OPTIC_EXTRA_BATTERY_DRAIN_MULTIPLIER_VALUE = builder
                .comment("Additional active-flight battery drain multiplier while controlling a drone through Fiber Optic mode.")
                .defineInRange("fiberOpticExtraBatteryDrainMultiplier", 1.15D, 1.0D, 10.0D);
        NORMAL_JAMMERS_AFFECT_FIBER_OPTIC_VALUE = builder
                .comment("Allow normal jammer interference to affect drones using Fiber Optic mode.")
                .define("normalJammersAffectFiberOptic", false);
        ANTI_DRONE_NET_CUTS_FIBER_VALUE = builder
                .comment("If true, anti-drone nets sever Fiber Optic drone links and damage the installed spool.")
                .define("antiDroneNetCutsFiber", true);
        ANTI_DRONE_NET_PANEL_REQUIRES_SUPPORT_VALUE = builder
                .comment("Require Anti-Drone Net Panels to have a solid attached face. Keep false for free-floating panels.")
                .define("antiDroneNetPanelRequiresSupport", false);
        ANTI_DRONE_NET_PANEL_TRIGGER_THICKNESS_VALUE = builder
                .comment("Anti-Drone Net Panel trigger thickness in blocks. This can be thicker than the visual panel so drones reliably trigger it.")
                .defineInRange("antiDroneNetPanelTriggerThickness", 0.25D, 0.0625D, 0.5D);
        ANTI_DRONE_NET_PANEL_VISUAL_THICKNESS_VALUE = builder
                .comment("Anti-Drone Net Panel visual thickness in blocks.")
                .defineInRange("antiDroneNetPanelVisualThickness", 0.0625D, 0.03125D, 0.125D);
        DEBUG_FIBER_OPTIC_VALUE = builder
                .comment("Enable verbose Fiber Optic cable debug logging.")
                .define("debugFiberOptic", false);
        FIBER_CABLE_VISUAL_THICKNESS_VALUE = builder
                .comment("Visible thickness of the rendered Fiber Optic cable wire.")
                .defineInRange("fiberCableVisualThickness", 0.012D, 0.008D, 0.25D);
        FIBER_CABLE_VISUAL_ALPHA_VALUE = builder
                .comment("Alpha transparency used by the rendered Fiber Optic cable wire.")
                .defineInRange("fiberCableVisualAlpha", 0.45D, 0.05D, 1.0D);
        FIBER_CABLE_VISUAL_COLOR_VALUE = builder
                .comment("Visible color preset for the Fiber Optic cable wire. Supported: light_gray, off_white, pale_gray.")
                .define("fiberCableVisualColor", "light_gray", value -> value instanceof String stringValue && isSupportedFiberCableVisualColor(stringValue));
        FIBER_CABLE_DEBUG_VISUAL_THICKNESS_VALUE = builder
                .comment("Debug-only thickness used when Fiber Optic cable render debugging is enabled.")
                .defineInRange("fiberCableDebugVisualThickness", 0.08D, 0.01D, 0.50D);
        FIBER_CABLE_SEGMENT_SPACING_VALUE = builder
                .comment("Distance in blocks between maintained Fiber Optic cable segment entities.")
                .defineInRange("fiberCableSegmentSpacing", 3.0D, 0.5D, 16.0D);
        FIBER_CABLE_SEGMENT_HEALTH_VALUE = builder
                .comment("Health for each physical Fiber Optic cable segment entity.")
                .defineInRange("fiberCableSegmentHealth", 4.0D, 0.5D, 100.0D);
        FIBER_CABLE_SEGMENT_HITBOX_SIZE_VALUE = builder
                .comment("Hitbox width/height for each Fiber Optic cable segment entity.")
                .defineInRange("fiberCableSegmentHitboxSize", 0.55D, 0.1D, 2.0D);
        FIBER_CABLE_CAN_BE_DAMAGED_BY_PLAYERS_VALUE = builder
                .comment("Allow players to damage Fiber Optic cable segment entities with melee hits.")
                .define("fiberCableCanBeDamagedByPlayers", true);
        FIBER_CABLE_CAN_BE_DAMAGED_BY_PROJECTILES_VALUE = builder
                .comment("Allow projectiles to damage Fiber Optic cable segment entities.")
                .define("fiberCableCanBeDamagedByProjectiles", true);
        FIBER_CABLE_CAN_BE_DAMAGED_BY_EXPLOSIONS_VALUE = builder
                .comment("Allow explosions to damage Fiber Optic cable segment entities.")
                .define("fiberCableCanBeDamagedByExplosions", true);
        FIBER_CABLE_PROJECTILE_RAYCAST_BREAK_VALUE = builder
                .comment("Use a projectile movement raycast fallback to sever Fiber Optic cables when small segment hitboxes are missed.")
                .define("fiberCableProjectileRaycastBreak", true);
        FIBER_CABLE_BREAKS_WHEN_ANY_SEGMENT_DESTROYED_VALUE = builder
                .comment("If true, destroying any Fiber Optic cable segment immediately severs the link.")
                .define("fiberCableBreaksWhenAnySegmentDestroyed", true);
        MAX_FIBER_CABLE_SEGMENTS_PER_DRONE_VALUE = builder
                .comment("Maximum amount of physical Fiber Optic cable segment entities maintained per drone.")
                .defineInRange("maxFiberCableSegmentsPerDrone", 96, 1, 256);
        DEBUG_FIBER_CABLE_RENDER_VALUE = builder
                .comment("Enable verbose client-side Fiber Optic cable render debug logging.")
                .define("debugFiberCableRender", false);
        DEBUG_FIBER_CABLE_SEGMENTS_VALUE = builder
                .comment("Enable verbose Fiber Optic cable segment lifecycle debug logging.")
                .define("debugFiberCableSegments", false);
        RENDER_RED_DEBUG_CABLE_VALUE = builder
                .comment("Render the visible Fiber Optic cable in bright red while debugFiberCableRender is enabled.")
                .define("renderRedDebugCable", false);
        RENDER_LEGACY_FIBER_LINE_VALUE = builder
                .comment("Render the old client-only Fiber Optic line in addition to the physical cable segments.")
                .define("renderLegacyFiberLine", false);
        builder.pop();

        builder.push("weather");
        WEATHER_EFFECTS_ENABLED_VALUE = builder
                .comment("Enable rain and storm effects for drones.")
                .define("weatherEffectsEnabled", true);
        RAIN_VISIBILITY_MULTIPLIER_VALUE = builder
                .comment("Multiplier applied to effective drone visibility and control clarity during rain.")
                .defineInRange("rainVisibilityMultiplier", 0.8D, 0.1D, 1.0D);
        STORM_INSTABILITY_STRENGTH_VALUE = builder
                .comment("Strength of random wind instability applied to drones during thunderstorms.")
                .defineInRange("stormInstabilityStrength", 0.2D, 0.0D, 2.0D);
        builder.pop();

        builder.push("droneScreenshot");
        ENABLE_DRONE_SCREENSHOT_SOUND_VALUE = builder
                .comment("Play a shutter sound when taking a drone FPV screenshot.")
                .define("enableDroneScreenshotSound", true);
        builder.pop();

        builder.push("fpvCamera");
        FPV_CAMERA_FOV_VALUE = builder
                .comment("Wide FPV camera field of view for the Cubed FPV Drone.")
                .defineInRange("fpvCameraFov", 135.0D, 120.0D, 155.0D);
        FPV_CAMERA_TILT_DEGREES_VALUE = builder
                .comment("Forward camera tilt applied while controlling the Cubed FPV Drone.")
                .defineInRange("fpvCameraTiltDegrees", 20.0D, 0.0D, 45.0D);
        ENABLE_FPV_FISHEYE_EFFECT_VALUE = builder
                .comment("Render a subtle FPV lens vignette/fisheye-style screen edge effect.")
                .define("enableFpvFisheyeEffect", true);
        ENABLE_FPV_CAMERA_VIBRATION_VALUE = builder
                .comment("Add throttle-scaled camera vibration while flying the Cubed FPV Drone.")
                .define("enableFpvCameraVibration", true);
        builder.pop();

        builder.push("audio");
        DRONE_ENGINE_VOLUME_MULTIPLIER_VALUE = builder
                .comment("Overall volume multiplier for the drone engine sound.")
                .defineInRange("droneEngineVolumeMultiplier", 1.0D,
                        DroneAudioConfigLimits.MIN_ENGINE_VOLUME_MULTIPLIER,
                        DroneAudioConfigLimits.MAX_ENGINE_VOLUME_MULTIPLIER);
        LUCAS_ENGINE_VOLUME_MULTIPLIER_VALUE = builder
                .comment("Dedicated volume multiplier for the LUCAS drone engine sound.")
                .defineInRange("lucasEngineVolumeMultiplier", 5.0D,
                        DroneAudioConfigLimits.MIN_ENGINE_VOLUME_MULTIPLIER,
                        DroneAudioConfigLimits.MAX_ENGINE_VOLUME_MULTIPLIER);
        DRONE_MONITOR_HUM_VOLUME_MULTIPLIER_VALUE = builder
                .comment("Volume multiplier for the monitor control hum while using the linked drone screen.")
                .defineInRange("droneMonitorHumVolumeMultiplier", 1.0D,
                        DroneAudioConfigLimits.MIN_MONITOR_HUM_VOLUME_MULTIPLIER,
                        DroneAudioConfigLimits.MAX_MONITOR_HUM_VOLUME_MULTIPLIER);
        DRONE_AIRBORNE_VOLUME_MULTIPLIER_VALUE = builder
                .comment("Extra engine volume multiplier applied while the drone is airborne.")
                .defineInRange("droneAirborneVolumeMultiplier", 1.35D,
                        DroneAudioConfigLimits.MIN_AIRBORNE_VOLUME_MULTIPLIER,
                        DroneAudioConfigLimits.MAX_AIRBORNE_VOLUME_MULTIPLIER);
        builder.pop();

        builder.push("spotlight");
        ENABLE_SPOTLIGHT_MODULE_VALUE = builder
                .comment("Enable the drone Spotlight Module upgrade system.")
                .define("enableSpotlightModule", true);
        SPOTLIGHT_RANGE_VALUE = builder
                .comment("Maximum render/search distance of the drone spotlight beam.")
                .defineInRange("spotlightRange", 48, 8, 100);
        SPOTLIGHT_ENERGY_COST_PER_SECOND_VALUE = builder
                .comment("Extra drone battery energy consumed per second while the spotlight is active.")
                .defineInRange("spotlightEnergyCostPerSecond", 2, 0, 1000);
        SPOTLIGHT_REQUIRES_MODULE_VALUE = builder
                .comment("Require a Spotlight Module item to be installed in the drone inventory before use.")
                .define("spotlightRequiresModule", true);
        SPOTLIGHT_VISIBLE_TO_OTHER_PLAYERS_VALUE = builder
                .comment("If true, other players can also see spotlight beams from active drones.")
                .define("spotlightVisibleToOtherPlayers", true);
        SPOTLIGHT_AUTO_OFF_WHEN_BATTERY_EMPTY_VALUE = builder
                .comment("Automatically disable the spotlight when the drone battery runs empty.")
                .define("spotlightAutoOffWhenBatteryEmpty", true);
        SPOTLIGHT_REAL_LIGHT_ENABLED_VALUE = builder
                .comment("Create temporary real light blocks in front of the drone spotlight.")
                .define("spotlightRealLightEnabled", true);
        SPOTLIGHT_LIGHT_RANGE_VALUE = builder
                .comment("Maximum distance used by the real spotlight light positions.")
                .defineInRange("spotlightLightRange", 48, 8, 100);
        SPOTLIGHT_LIGHT_LEVEL_VALUE = builder
                .comment("Base light level used for the nearest spotlight light source.")
                .defineInRange("spotlightLightLevel", 15, 1, 15);
        SPOTLIGHT_LIGHT_UPDATE_INTERVAL_TICKS_VALUE = builder
                .comment("How often the spotlight real light positions update while active.")
                .defineInRange("spotlightLightUpdateIntervalTicks", 2, 1, 10);
        builder.pop();

        builder.push("payload");
        DEBUG_PAYLOAD_MOUNTS_VALUE = builder
                .comment("Render colored payload mount debug markers and log payload mount transforms every second.")
                .define("debugPayloadMounts", false);
        builder.pop();

        SPEC = builder.build();
        bake();
    }

    private AddonConfig() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            bake();
            if (ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID)) {
                DroneTrackingRangeOverride.applyConfiguredRange();
            }
        }
    }

    private static void bake() {
        droneMaxRange = DRONE_MAX_RANGE_VALUE.get();
        droneSpeedMultiplier = DRONE_SPEED_MULTIPLIER_VALUE.get();
        enableDroneChunkLoading = ENABLE_DRONE_CHUNK_LOADING_VALUE.get();
        droneChunkLoadRadius = DRONE_CHUNK_LOAD_RADIUS_VALUE.get();
        enableDronePlayerAnchor = ENABLE_DRONE_PLAYER_ANCHOR_VALUE.get();
        enableDroneCrashExplosionDamage = ENABLE_DRONE_CRASH_EXPLOSION_DAMAGE_VALUE.get();
        lucasTakeoffAssistTicks = LUCAS_TAKEOFF_ASSIST_TICKS_VALUE.get();
        lucasTakeoffForwardBoost = LUCAS_TAKEOFF_FORWARD_BOOST_VALUE.get();
        lucasTakeoffUpBoost = LUCAS_TAKEOFF_UP_BOOST_VALUE.get();
        lucasMinSpeed = LUCAS_MIN_SPEED_VALUE.get();
        lucasMaxSpeed = Math.max(lucasMinSpeed, LUCAS_MAX_SPEED_VALUE.get());
        lucasAcceleration = LUCAS_ACCELERATION_VALUE.get();
        lucasDrag = LUCAS_DRAG_VALUE.get();
        lucasTurnRate = LUCAS_TURN_RATE_VALUE.get();
        lucasPitchRate = LUCAS_PITCH_RATE_VALUE.get();
        lucasClimbSpeed = LUCAS_CLIMB_SPEED_VALUE.get();
        lucasDescendSpeed = LUCAS_DESCEND_SPEED_VALUE.get();
        lucasLiftStrength = LUCAS_LIFT_STRENGTH_VALUE.get();
        lucasRollVisualAmount = LUCAS_ROLL_VISUAL_AMOUNT_VALUE.get();
        lucasMinLiftSpeed = LUCAS_MIN_LIFT_SPEED_VALUE.get();
        lucasStallGravity = LUCAS_STALL_GRAVITY_VALUE.get();
        lucasStallEnabled = LUCAS_STALL_ENABLED_VALUE.get();
        lucasCanHover = LUCAS_CAN_HOVER_VALUE.get();
        lucasMaxFuel = LUCAS_MAX_FUEL_VALUE.get();
        lucasFuelDrainIdle = LUCAS_FUEL_DRAIN_IDLE_VALUE.get();
        lucasFuelDrainFlying = LUCAS_FUEL_DRAIN_FLYING_VALUE.get();
        lucasFuelDrainClimb = LUCAS_FUEL_DRAIN_CLIMB_VALUE.get();
        lucasLowFuelWarningPercent = LUCAS_LOW_FUEL_WARNING_PERCENT_VALUE.get();
        lucasCanFlyWithoutFuel = LUCAS_CAN_FLY_WITHOUT_FUEL_VALUE.get();
        lucasHudScale = LUCAS_HUD_SCALE_VALUE.get();
        fuelMixerProcessTime = FUEL_MIXER_PROCESS_TIME_VALUE.get();
        gasolineJerrycanFuelAmount = GASOLINE_JERRYCAN_FUEL_AMOUNT_VALUE.get();
        enableThermalVision = ENABLE_THERMAL_VISION_VALUE.get();
        thermalVisionRange = THERMAL_VISION_RANGE_VALUE.get();
        thermalVisionKeybindEnabled = THERMAL_VISION_KEYBIND_ENABLED_VALUE.get();
        thermalVisionNoiseEffect = THERMAL_VISION_NOISE_EFFECT_VALUE.get();
        thermalHighlightPlayers = THERMAL_HIGHLIGHT_PLAYERS_VALUE.get();
        thermalHighlightHostileMobs = THERMAL_HIGHLIGHT_HOSTILE_MOBS_VALUE.get();
        thermalHighlightAnimals = THERMAL_HIGHLIGHT_ANIMALS_VALUE.get();
        enableBatterySystem = ENABLE_BATTERY_SYSTEM_VALUE.get();
        maxEnergy = MAX_ENERGY_VALUE.get();
        energyUsePerTick = ENERGY_USE_PER_TICK_VALUE.get();
        idleEnergyUsePerTick = IDLE_ENERGY_USE_PER_TICK_VALUE.get();
        lowEnergyThreshold = LOW_ENERGY_THRESHOLD_VALUE.get();
        returnHomeOnEmpty = RETURN_HOME_ON_EMPTY_VALUE.get();
        batteryTransferRate = BATTERY_TRANSFER_RATE_VALUE.get();
        enableDroneSiren = ENABLE_DRONE_SIREN_VALUE.get();
        droneSirenRange = DRONE_SIREN_RANGE_VALUE.get();
        droneSirenRedstoneOutput = DRONE_SIREN_REDSTONE_OUTPUT_VALUE.get();
        droneSirenCooldownTicks = DRONE_SIREN_COOLDOWN_TICKS_VALUE.get();
        detectFriendlyDrones = DETECT_FRIENDLY_DRONES_VALUE.get();
        ignoredDroneOwnerNames = normalizeOwnerNames(IGNORED_DRONE_OWNER_NAMES_VALUE.get());
        ignoredDroneOwnerUUIDs = parseOwnerUuids(IGNORED_DRONE_OWNER_UUIDS_VALUE.get());
        ignoreOwnerlessDrones = IGNORE_OWNERLESS_DRONES_VALUE.get();
        blacklistMatchCaseInsensitive = BLACKLIST_MATCH_CASE_INSENSITIVE_VALUE.get();
        ignoredDroneNames = normalizeConfiguredNames(IGNORED_DRONE_NAMES_VALUE.get(), blacklistMatchCaseInsensitive);
        ignoreUnnamedDrones = IGNORE_UNNAMED_DRONES_VALUE.get();
        enableDroneJammer = ENABLE_DRONE_JAMMER_VALUE.get();
        jammerRange = JAMMER_RANGE_VALUE.get();
        jammerBuildUpTicks = JAMMER_BUILD_UP_TICKS_VALUE.get();
        jammerRecoveryTicks = JAMMER_RECOVERY_TICKS_VALUE.get();
        overlayStartThreshold = clampThreshold(OVERLAY_START_THRESHOLD_VALUE.get(), 0.0D, 0.99D);
        controlInterferenceThreshold = Math.max(
                overlayStartThreshold,
                clampThreshold(CONTROL_INTERFERENCE_THRESHOLD_VALUE.get(), 0.0D, 0.99D)
        );
        hardJamThreshold = Math.max(
                controlInterferenceThreshold,
                clampThreshold(HARD_JAM_THRESHOLD_VALUE.get(), 0.50D, 1.0D)
        );
        debugJammer = DEBUG_JAMMER_VALUE.get();
        jammerMode = JAMMER_MODE_VALUE.get();
        affectFriendlyDrones = AFFECT_FRIENDLY_DRONES_VALUE.get();
        droneRadarDetectionRange = DRONE_RADAR_DETECTION_RANGE_VALUE.get();
        droneRadarBeepEnabled = DRONE_RADAR_BEEP_ENABLED_VALUE.get();
        droneRadarBeepVolume = DRONE_RADAR_BEEP_VOLUME_VALUE.get();
        droneRadarMinBeepIntervalTicks = DRONE_RADAR_MIN_BEEP_INTERVAL_TICKS_VALUE.get();
        droneRadarMaxBeepIntervalTicks = Math.max(
                droneRadarMinBeepIntervalTicks,
                DRONE_RADAR_MAX_BEEP_INTERVAL_TICKS_VALUE.get()
        );
        droneRadarOnlyBeepWhenHeld = DRONE_RADAR_ONLY_BEEP_WHEN_HELD_VALUE.get();
        droneRadarHudScale = DRONE_RADAR_HUD_SCALE_VALUE.get();
        droneRadarHudXOffset = DRONE_RADAR_HUD_X_OFFSET_VALUE.get();
        droneRadarHudYOffset = DRONE_RADAR_HUD_Y_OFFSET_VALUE.get();
        droneRadarCompactMode = DRONE_RADAR_COMPACT_MODE_VALUE.get();
        debugDroneRadar = DEBUG_DRONE_RADAR_VALUE.get();
        enableFiberOpticMode = ENABLE_FIBER_OPTIC_MODE_VALUE.get();
        fiberOpticCableLength = FIBER_OPTIC_CABLE_LENGTH_VALUE.get();
        fiberOpticCableBreakDelayTicks = FIBER_OPTIC_CABLE_BREAK_DELAY_TICKS_VALUE.get();
        fiberOpticExtraBatteryDrainMultiplier = FIBER_OPTIC_EXTRA_BATTERY_DRAIN_MULTIPLIER_VALUE.get();
        normalJammersAffectFiberOptic = NORMAL_JAMMERS_AFFECT_FIBER_OPTIC_VALUE.get();
        antiDroneNetCutsFiber = ANTI_DRONE_NET_CUTS_FIBER_VALUE.get();
        antiDroneNetPanelRequiresSupport = ANTI_DRONE_NET_PANEL_REQUIRES_SUPPORT_VALUE.get();
        antiDroneNetPanelTriggerThickness = ANTI_DRONE_NET_PANEL_TRIGGER_THICKNESS_VALUE.get();
        antiDroneNetPanelVisualThickness = ANTI_DRONE_NET_PANEL_VISUAL_THICKNESS_VALUE.get();
        debugFiberOptic = DEBUG_FIBER_OPTIC_VALUE.get();
        fiberCableVisualThickness = FIBER_CABLE_VISUAL_THICKNESS_VALUE.get();
        fiberCableVisualAlpha = FIBER_CABLE_VISUAL_ALPHA_VALUE.get();
        fiberCableVisualColor = normalizeFiberCableVisualColor(FIBER_CABLE_VISUAL_COLOR_VALUE.get());
        fiberCableDebugVisualThickness = FIBER_CABLE_DEBUG_VISUAL_THICKNESS_VALUE.get();
        fiberCableSegmentSpacing = FIBER_CABLE_SEGMENT_SPACING_VALUE.get();
        fiberCableSegmentHealth = FIBER_CABLE_SEGMENT_HEALTH_VALUE.get();
        fiberCableSegmentHitboxSize = FIBER_CABLE_SEGMENT_HITBOX_SIZE_VALUE.get();
        fiberCableCanBeDamagedByPlayers = FIBER_CABLE_CAN_BE_DAMAGED_BY_PLAYERS_VALUE.get();
        fiberCableCanBeDamagedByProjectiles = FIBER_CABLE_CAN_BE_DAMAGED_BY_PROJECTILES_VALUE.get();
        fiberCableCanBeDamagedByExplosions = FIBER_CABLE_CAN_BE_DAMAGED_BY_EXPLOSIONS_VALUE.get();
        fiberCableProjectileRaycastBreak = FIBER_CABLE_PROJECTILE_RAYCAST_BREAK_VALUE.get();
        fiberCableBreaksWhenAnySegmentDestroyed = FIBER_CABLE_BREAKS_WHEN_ANY_SEGMENT_DESTROYED_VALUE.get();
        maxFiberCableSegmentsPerDrone = MAX_FIBER_CABLE_SEGMENTS_PER_DRONE_VALUE.get();
        debugFiberCableRender = DEBUG_FIBER_CABLE_RENDER_VALUE.get();
        debugFiberCableSegments = DEBUG_FIBER_CABLE_SEGMENTS_VALUE.get();
        renderRedDebugCable = RENDER_RED_DEBUG_CABLE_VALUE.get();
        renderLegacyFiberLine = RENDER_LEGACY_FIBER_LINE_VALUE.get();
        weatherEffectsEnabled = WEATHER_EFFECTS_ENABLED_VALUE.get();
        rainVisibilityMultiplier = RAIN_VISIBILITY_MULTIPLIER_VALUE.get();
        stormInstabilityStrength = STORM_INSTABILITY_STRENGTH_VALUE.get();
        enableDroneScreenshotSound = ENABLE_DRONE_SCREENSHOT_SOUND_VALUE.get();
        fpvCameraFov = FPV_CAMERA_FOV_VALUE.get();
        fpvCameraTiltDegrees = FPV_CAMERA_TILT_DEGREES_VALUE.get();
        enableFpvFisheyeEffect = ENABLE_FPV_FISHEYE_EFFECT_VALUE.get();
        enableFpvCameraVibration = ENABLE_FPV_CAMERA_VIBRATION_VALUE.get();
        droneEngineVolumeMultiplier = DRONE_ENGINE_VOLUME_MULTIPLIER_VALUE.get();
        lucasEngineVolumeMultiplier = LUCAS_ENGINE_VOLUME_MULTIPLIER_VALUE.get();
        droneMonitorHumVolumeMultiplier = DRONE_MONITOR_HUM_VOLUME_MULTIPLIER_VALUE.get();
        droneAirborneVolumeMultiplier = DRONE_AIRBORNE_VOLUME_MULTIPLIER_VALUE.get();
        enableSpotlightModule = ENABLE_SPOTLIGHT_MODULE_VALUE.get();
        spotlightRange = SPOTLIGHT_RANGE_VALUE.get();
        spotlightEnergyCostPerSecond = SPOTLIGHT_ENERGY_COST_PER_SECOND_VALUE.get();
        spotlightRequiresModule = SPOTLIGHT_REQUIRES_MODULE_VALUE.get();
        spotlightVisibleToOtherPlayers = SPOTLIGHT_VISIBLE_TO_OTHER_PLAYERS_VALUE.get();
        spotlightAutoOffWhenBatteryEmpty = SPOTLIGHT_AUTO_OFF_WHEN_BATTERY_EMPTY_VALUE.get();
        spotlightRealLightEnabled = SPOTLIGHT_REAL_LIGHT_ENABLED_VALUE.get();
        spotlightLightRange = SPOTLIGHT_LIGHT_RANGE_VALUE.get();
        spotlightLightLevel = SPOTLIGHT_LIGHT_LEVEL_VALUE.get();
        spotlightLightUpdateIntervalTicks = SPOTLIGHT_LIGHT_UPDATE_INTERVAL_TICKS_VALUE.get();
        debugPayloadMounts = DEBUG_PAYLOAD_MOUNTS_VALUE.get();
    }

    public static int droneMaxRange() {
        return droneMaxRange;
    }

    public static double droneSpeedMultiplier() {
        return droneSpeedMultiplier;
    }

    public static boolean enableDroneChunkLoading() {
        return enableDroneChunkLoading;
    }

    public static int droneChunkLoadRadius() {
        return droneChunkLoadRadius;
    }

    public static boolean enableDronePlayerAnchor() {
        return enableDronePlayerAnchor;
    }

    public static boolean enableDroneCrashExplosionDamage() {
        return enableDroneCrashExplosionDamage;
    }

    public static int lucasTakeoffAssistTicks() {
        return lucasTakeoffAssistTicks;
    }

    public static double lucasTakeoffForwardBoost() {
        return lucasTakeoffForwardBoost;
    }

    public static double lucasTakeoffUpBoost() {
        return lucasTakeoffUpBoost;
    }

    public static double lucasMinSpeed() {
        return lucasMinSpeed;
    }

    public static double lucasMaxSpeed() {
        return lucasMaxSpeed;
    }

    public static double lucasAcceleration() {
        return lucasAcceleration;
    }

    public static double lucasDrag() {
        return lucasDrag;
    }

    public static double lucasTurnRate() {
        return lucasTurnRate;
    }

    public static double lucasPitchRate() {
        return lucasPitchRate;
    }

    public static double lucasClimbSpeed() {
        return lucasClimbSpeed;
    }

    public static double lucasDescendSpeed() {
        return lucasDescendSpeed;
    }

    public static double lucasLiftStrength() {
        return lucasLiftStrength;
    }

    public static double lucasRollVisualAmount() {
        return lucasRollVisualAmount;
    }

    public static double lucasMinLiftSpeed() {
        return lucasMinLiftSpeed;
    }

    public static double lucasStallGravity() {
        return lucasStallGravity;
    }

    public static boolean lucasStallEnabled() {
        return lucasStallEnabled;
    }

    public static boolean lucasCanHover() {
        return lucasCanHover;
    }

    public static int lucasMaxFuel() {
        return lucasMaxFuel;
    }

    public static double lucasFuelDrainIdle() {
        return lucasFuelDrainIdle;
    }

    public static double lucasFuelDrainFlying() {
        return lucasFuelDrainFlying;
    }

    public static double lucasFuelDrainClimb() {
        return lucasFuelDrainClimb;
    }

    public static int lucasLowFuelWarningPercent() {
        return lucasLowFuelWarningPercent;
    }

    public static boolean lucasCanFlyWithoutFuel() {
        return lucasCanFlyWithoutFuel;
    }

    public static double lucasHudScale() {
        return lucasHudScale;
    }

    public static int fuelMixerProcessTime() {
        return fuelMixerProcessTime;
    }

    public static int gasolineJerrycanFuelAmount() {
        return gasolineJerrycanFuelAmount;
    }

    public static boolean enableThermalVision() {
        return enableThermalVision;
    }

    public static int thermalVisionRange() {
        return thermalVisionRange;
    }

    public static boolean thermalVisionKeybindEnabled() {
        return thermalVisionKeybindEnabled;
    }

    public static boolean thermalVisionNoiseEffect() {
        return thermalVisionNoiseEffect;
    }

    public static boolean thermalHighlightPlayers() {
        return thermalHighlightPlayers;
    }

    public static boolean thermalHighlightHostileMobs() {
        return thermalHighlightHostileMobs;
    }

    public static boolean thermalHighlightAnimals() {
        return thermalHighlightAnimals;
    }

    public static boolean thermalHighlightMobs() {
        return thermalHighlightHostileMobs || thermalHighlightAnimals;
    }

    public static boolean enableBatterySystem() {
        return enableBatterySystem;
    }

    public static int maxEnergy() {
        return maxEnergy;
    }

    public static int energyUsePerTick() {
        return energyUsePerTick;
    }

    public static int idleEnergyUsePerTick() {
        return idleEnergyUsePerTick;
    }

    public static int lowEnergyThreshold() {
        return lowEnergyThreshold;
    }

    public static boolean returnHomeOnEmpty() {
        return returnHomeOnEmpty;
    }

    public static int batteryTransferRate() {
        return batteryTransferRate;
    }

    public static boolean enableDroneSiren() {
        return enableDroneSiren;
    }

    public static int droneSirenRange() {
        return droneSirenRange;
    }

    public static boolean droneSirenRedstoneOutput() {
        return droneSirenRedstoneOutput;
    }

    public static int droneSirenCooldownTicks() {
        return droneSirenCooldownTicks;
    }

    public static boolean detectFriendlyDrones() {
        return detectFriendlyDrones;
    }

    public static Set<String> ignoredDroneOwnerNames() {
        return ignoredDroneOwnerNames;
    }

    public static Set<UUID> ignoredDroneOwnerUUIDs() {
        return ignoredDroneOwnerUUIDs;
    }

    public static boolean ignoreOwnerlessDrones() {
        return ignoreOwnerlessDrones;
    }

    public static Set<String> ignoredDroneNames() {
        return ignoredDroneNames;
    }

    public static boolean ignoreUnnamedDrones() {
        return ignoreUnnamedDrones;
    }

    public static boolean blacklistMatchCaseInsensitive() {
        return blacklistMatchCaseInsensitive;
    }

    public static boolean enableDroneJammer() {
        return enableDroneJammer;
    }

    public static int jammerRange() {
        return jammerRange;
    }

    public static int jammerBuildUpTicks() {
        return jammerBuildUpTicks;
    }

    public static int jammerRecoveryTicks() {
        return jammerRecoveryTicks;
    }

    public static double hardJamThreshold() {
        return hardJamThreshold;
    }

    public static double controlInterferenceThreshold() {
        return controlInterferenceThreshold;
    }

    public static double overlayStartThreshold() {
        return overlayStartThreshold;
    }

    public static boolean debugJammer() {
        return debugJammer;
    }

    public static DroneJammerMode jammerMode() {
        return jammerMode;
    }

    public static boolean affectFriendlyDrones() {
        return affectFriendlyDrones;
    }

    public static int droneRadarDetectionRange() {
        return droneRadarDetectionRange;
    }

    public static boolean droneRadarBeepEnabled() {
        return droneRadarBeepEnabled;
    }

    public static double droneRadarBeepVolume() {
        return droneRadarBeepVolume;
    }

    public static int droneRadarMinBeepIntervalTicks() {
        return droneRadarMinBeepIntervalTicks;
    }

    public static int droneRadarMaxBeepIntervalTicks() {
        return droneRadarMaxBeepIntervalTicks;
    }

    public static boolean droneRadarOnlyBeepWhenHeld() {
        return droneRadarOnlyBeepWhenHeld;
    }

    public static double droneRadarHudScale() {
        return droneRadarHudScale;
    }

    public static int droneRadarHudXOffset() {
        return droneRadarHudXOffset;
    }

    public static int droneRadarHudYOffset() {
        return droneRadarHudYOffset;
    }

    public static boolean droneRadarCompactMode() {
        return droneRadarCompactMode;
    }

    public static boolean debugDroneRadar() {
        return debugDroneRadar;
    }

    public static boolean enableFiberOpticMode() {
        return enableFiberOpticMode;
    }

    public static int fiberOpticCableLength() {
        return fiberOpticCableLength;
    }

    public static int fiberOpticCableBreakDelayTicks() {
        return fiberOpticCableBreakDelayTicks;
    }

    public static double fiberOpticExtraBatteryDrainMultiplier() {
        return fiberOpticExtraBatteryDrainMultiplier;
    }

    public static boolean normalJammersAffectFiberOptic() {
        return normalJammersAffectFiberOptic;
    }

    public static boolean antiDroneNetCutsFiber() {
        return antiDroneNetCutsFiber;
    }

    public static boolean antiDroneNetPanelRequiresSupport() {
        return antiDroneNetPanelRequiresSupport;
    }

    public static double antiDroneNetPanelTriggerThickness() {
        return antiDroneNetPanelTriggerThickness;
    }

    public static double antiDroneNetPanelVisualThickness() {
        return antiDroneNetPanelVisualThickness;
    }

    public static boolean debugFiberOptic() {
        return debugFiberOptic;
    }

    public static double fiberCableVisualThickness() {
        return fiberCableVisualThickness;
    }

    public static double fiberCableVisualAlpha() {
        return fiberCableVisualAlpha;
    }

    public static String fiberCableVisualColor() {
        return fiberCableVisualColor;
    }

    public static double fiberCableDebugVisualThickness() {
        return fiberCableDebugVisualThickness;
    }

    public static double fiberCableSegmentSpacing() {
        return fiberCableSegmentSpacing;
    }

    public static double fiberCableSegmentHealth() {
        return fiberCableSegmentHealth;
    }

    public static double fiberCableSegmentHitboxSize() {
        return fiberCableSegmentHitboxSize;
    }

    public static boolean fiberCableCanBeDamagedByPlayers() {
        return fiberCableCanBeDamagedByPlayers;
    }

    public static boolean fiberCableCanBeDamagedByProjectiles() {
        return fiberCableCanBeDamagedByProjectiles;
    }

    public static boolean fiberCableCanBeDamagedByExplosions() {
        return fiberCableCanBeDamagedByExplosions;
    }

    public static boolean fiberCableProjectileRaycastBreak() {
        return fiberCableProjectileRaycastBreak;
    }

    public static boolean fiberCableBreaksWhenAnySegmentDestroyed() {
        return fiberCableBreaksWhenAnySegmentDestroyed;
    }

    public static int maxFiberCableSegmentsPerDrone() {
        return maxFiberCableSegmentsPerDrone;
    }

    public static boolean debugFiberCableRender() {
        return debugFiberCableRender;
    }

    public static boolean debugFiberCableSegments() {
        return debugFiberCableSegments;
    }

    public static boolean renderRedDebugCable() {
        return renderRedDebugCable;
    }

    public static boolean renderLegacyFiberLine() {
        return renderLegacyFiberLine;
    }

    public static boolean weatherEffectsEnabled() {
        return weatherEffectsEnabled;
    }

    public static double rainVisibilityMultiplier() {
        return rainVisibilityMultiplier;
    }

    public static double stormInstabilityStrength() {
        return stormInstabilityStrength;
    }

    public static boolean enableDroneScreenshotSound() {
        return enableDroneScreenshotSound;
    }

    public static double fpvCameraFov() {
        return fpvCameraFov;
    }

    public static double fpvCameraTiltDegrees() {
        return fpvCameraTiltDegrees;
    }

    public static boolean enableFpvFisheyeEffect() {
        return enableFpvFisheyeEffect;
    }

    public static boolean enableFpvCameraVibration() {
        return enableFpvCameraVibration;
    }

    public static double droneEngineVolumeMultiplier() {
        return droneEngineVolumeMultiplier;
    }

    public static double lucasEngineVolumeMultiplier() {
        return lucasEngineVolumeMultiplier;
    }

    public static double droneMonitorHumVolumeMultiplier() {
        return droneMonitorHumVolumeMultiplier;
    }

    public static double droneAirborneVolumeMultiplier() {
        return droneAirborneVolumeMultiplier;
    }

    public static boolean enableSpotlightModule() {
        return enableSpotlightModule;
    }

    public static int spotlightRange() {
        return spotlightRange;
    }

    public static int spotlightEnergyCostPerSecond() {
        return spotlightEnergyCostPerSecond;
    }

    public static boolean spotlightRequiresModule() {
        return spotlightRequiresModule;
    }

    public static boolean spotlightVisibleToOtherPlayers() {
        return spotlightVisibleToOtherPlayers;
    }

    public static boolean spotlightAutoOffWhenBatteryEmpty() {
        return spotlightAutoOffWhenBatteryEmpty;
    }

    public static boolean spotlightRealLightEnabled() {
        return spotlightRealLightEnabled;
    }

    public static int spotlightLightRange() {
        return spotlightLightRange;
    }

    public static int spotlightLightLevel() {
        return spotlightLightLevel;
    }

    public static int spotlightLightUpdateIntervalTicks() {
        return spotlightLightUpdateIntervalTicks;
    }

    public static boolean debugPayloadMounts() {
        return debugPayloadMounts;
    }

    private static Set<String> normalizeOwnerNames(List<? extends String> rawNames) {
        return normalizeConfiguredNames(rawNames, true);
    }

    private static Set<String> normalizeConfiguredNames(List<? extends String> rawNames, boolean caseInsensitive) {
        LinkedHashSet<String> normalizedNames = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            if (rawName == null) {
                continue;
            }
            String normalized = normalizeName(rawName, caseInsensitive);
            if (!normalized.isEmpty()) {
                normalizedNames.add(normalized);
            }
        }
        return Set.copyOf(normalizedNames);
    }

    private static Set<UUID> parseOwnerUuids(List<? extends String> rawUuids) {
        LinkedHashSet<UUID> uuids = new LinkedHashSet<>();
        for (String rawUuid : rawUuids) {
            if (rawUuid == null) {
                continue;
            }
            String trimmed = rawUuid.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                uuids.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Set.copyOf(uuids);
    }

    public static String normalizeName(String rawName, boolean caseInsensitive) {
        if (rawName == null) {
            return "";
        }
        String normalized = rawName.trim();
        if (caseInsensitive) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    private static double clampThreshold(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isSupportedFiberCableVisualColor(String rawValue) {
        String normalized = normalizeFiberCableVisualColor(rawValue);
        return "light_gray".equals(normalized)
                || "off_white".equals(normalized)
                || "pale_gray".equals(normalized);
    }

    private static String normalizeFiberCableVisualColor(String rawValue) {
        if (rawValue == null) {
            return "light_gray";
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "off_white", "pale_gray", "light_gray" -> normalized;
            default -> "light_gray";
        };
    }

    public static void save() {
        SPEC.save();
        bake();
        if (ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID)) {
            DroneTrackingRangeOverride.applyConfiguredRange();
        }
    }
}
