package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubedFpvHudSeparationSourceTest {
    private static final Path MAIN = Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig");
    private static final Path MIXIN = MAIN.resolve("mixin/DroneHudOverlayMixin.java");
    private static final Path LANG = Path.of("src/main/resources/assets/sbwdroneconfig/lang/en_us.json");

    @Test
    void cubedFpvHasDedicatedOsdAndDoesNotRenderMilitaryLabels() throws Exception {
        String source = Files.readString(MAIN.resolve("CubedFpvHudOverlay.java"));

        assertTrue(source.contains("BAT:"), "FPV OSD should show Betaflight-style battery voltage.");
        assertTrue(source.contains("LINK: FIBER"), "FPV OSD should show fiber link state inline.");
        assertTrue(source.contains("LAT:"), "FPV OSD should show wireless latency.");
        assertTrue(source.contains("REC"), "FPV OSD should show a small recording indicator.");
        assertTrue(source.contains("SPD:"), "FPV OSD should show speed.");
        assertTrue(source.contains("ALT:"), "FPV OSD should show altitude.");
        assertTrue(source.contains("DIST:"), "FPV OSD should show pilot distance.");
        assertTrue(source.contains("HDG:"), "FPV OSD should show heading.");

        assertFalse(source.contains("HEALTH"), "Cubed FPV HUD must not show military health labels.");
        assertFalse(source.contains("AMMO"), "Cubed FPV HUD must not show military ammo labels.");
        assertFalse(source.contains("TARGET RANGE"), "Cubed FPV HUD must not show military target range.");
        assertFalse(source.contains("ZOOM"), "Cubed FPV HUD must not show UAV zoom labels.");
    }

    @Test
    void defaultSbwHudIsCancelledOnlyForCubedFpvDrone() throws Exception {
        String mixin = Files.readString(MIXIN);

        assertTrue(mixin.contains("sbwdroneconfig$replaceCubedFpvHud"));
        assertTrue(mixin.contains("CubedFpvHudOverlay.shouldReplaceDefaultSbwHud()"));
        assertTrue(mixin.contains("ci.cancel()"));
    }

    @Test
    void lucasKeepsSeparateUavHudAddition() throws Exception {
        String source = Files.readString(MAIN.resolve("LucasDroneHudOverlay.java"));

        assertTrue(source.contains("LucasDroneEntity"));
        assertTrue(source.contains("FUEL:"));
        assertTrue(source.contains("SPD:"));
        assertTrue(source.contains("ALT:"));
        assertFalse(source.contains("BAT:"), "LUCAS HUD should not reuse the Cubed FPV battery OSD.");
    }

    @Test
    void fpvCameraOptionsAreRegisteredInConfigAndGui() throws Exception {
        String config = Files.readString(MAIN.resolve("AddonConfig.java"));
        String draft = Files.readString(MAIN.resolve("DroneConfigDraft.java"));
        String screen = Files.readString(MAIN.resolve("DroneConfigScreen.java"));
        String lang = Files.readString(LANG);
        String bootstrap = Files.readString(MAIN.resolve("SbwDroneRangeConfig.java"));

        assertTrue(config.contains("FPV_CAMERA_FOV_VALUE"));
        assertTrue(config.contains("defineInRange(\"fpvCameraFov\", 135.0D, 120.0D, 155.0D)"));
        assertTrue(config.contains("FPV_CAMERA_TILT_DEGREES_VALUE"));
        assertTrue(config.contains("defineInRange(\"fpvCameraTiltDegrees\", 20.0D"));
        assertTrue(config.contains("ENABLE_FPV_FISHEYE_EFFECT_VALUE"));
        assertTrue(config.contains("ENABLE_FPV_CAMERA_VIBRATION_VALUE"));
        assertTrue(draft.contains("fpvCameraFov"));
        assertTrue(screen.contains("addFpvCameraPage"));
        assertTrue(screen.contains("case FPV_DRONE -> 2"));
        assertTrue(lang.contains("screen.sbwdroneconfig.config.label.fpv_camera_fov"));
        assertTrue(bootstrap.contains("CubedFpvHudOverlay.init(modBus);"));
        assertTrue(bootstrap.contains("CubedFpvCameraEffectsClient.init(modBus);"));
        assertTrue(bootstrap.contains("LucasDroneHudOverlay.init(modBus);"));
    }

    @Test
    void fpvCameraEffectsUseForgeViewportEvents() throws Exception {
        String source = Files.readString(MAIN.resolve("CubedFpvCameraEffectsClient.java"));

        assertTrue(source.contains("ViewportEvent.ComputeFov"));
        assertTrue(source.contains("setFOV(AddonConfig.fpvCameraFov())"));
        assertTrue(source.contains("ViewportEvent.ComputeCameraAngles"));
        assertTrue(source.contains("AddonConfig.fpvCameraTiltDegrees()"));
    }
}
