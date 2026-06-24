package nl.smartstreamlabs.sbwdroneconfig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneRadarSourceIntegrationTest {
    @Test
    void handheldRadarUsesDedicatedRadarConfigAndFiberLabels() throws Exception {
        String source = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/DroneJammerClient.java"));

        assertTrue(source.contains("droneRadarDetectionRange()"));
        assertTrue(source.contains("droneRadarBeepEnabled()"));
        assertTrue(source.contains("droneRadarOnlyBeepWhenHeld()"));
        assertTrue(source.contains("DroneFiberOpticClient.isFiberHudActiveFor"));
        assertTrue(source.contains("overlay.sbwdroneconfig.radar_fiber"));
        assertTrue(source.contains("drawNearestDirectionIndicator"));
        assertTrue(source.contains("SoundEvents.NOTE_BLOCK_HAT"));
    }

    @Test
    void handheldRadarHudUsesCompactTopRightConfigurableLayout() throws Exception {
        String client = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/DroneJammerClient.java"));
        String config = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/AddonConfig.java"));

        assertTrue(config.contains("DRONE_RADAR_HUD_SCALE_VALUE"), "Radar HUD scale should be configurable.");
        assertTrue(config.contains("defineInRange(\"droneRadarHudScale\", 0.55D"), "Default radar HUD scale should be compact.");
        assertTrue(config.contains("defineInRange(\"droneRadarHudXOffset\", 8"), "Top-right X offset should be configurable.");
        assertTrue(config.contains("defineInRange(\"droneRadarHudYOffset\", 8"), "Top-right Y offset should be configurable.");
        assertTrue(config.contains("define(\"droneRadarCompactMode\", true)"), "Compact mode should be enabled by default.");

        assertTrue(client.contains("AddonConfig.droneRadarHudScale()"), "Renderer should use configured scale.");
        assertTrue(client.contains("AddonConfig.droneRadarHudXOffset()"), "Renderer should use configured X offset.");
        assertTrue(client.contains("AddonConfig.droneRadarHudYOffset()"), "Renderer should use configured Y offset.");
        assertTrue(client.contains("AddonConfig.droneRadarCompactMode()"), "Renderer should switch to compact labels/layout.");
        assertTrue(client.contains("COMPACT_PANEL_WIDTH = 230"), "Compact panel width should be about 230 px before scaling.");
        assertTrue(client.contains("COMPACT_PANEL_HEIGHT = 120"), "Compact panel height should be about 120 px before scaling.");
        assertTrue(client.contains("COMPACT_RADAR_SIZE = 90"), "Compact radar circle should be about 90 px before scaling.");
        assertTrue(client.contains("TEXT_SCALE = 0.65F"), "Compact radar text should be scaled smaller.");
        assertTrue(client.contains("pushPose()") && client.contains("scale(scale, scale, 1.0F)"),
                "Renderer should scale the whole panel instead of manually inflating dimensions.");
        assertTrue(client.contains("DRONE RF"), "Compact title should use short labels.");
        assertTrue(client.contains("COUNT:"), "Compact labels should be short.");
        assertTrue(client.contains("SIG:"), "Compact labels should be short.");
        assertTrue(client.contains("NEAR:"), "Compact labels should be short.");
    }

    @Test
    void radarConfigIsExposedInConfigScreenSecondJammerPage() throws Exception {
        String config = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/AddonConfig.java"));
        String draft = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/DroneConfigDraft.java"));
        String screen = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/DroneConfigScreen.java"));

        assertTrue(config.contains("defineInRange(\"droneRadarDetectionRange\", 256"));
        assertTrue(config.contains("define(\"droneRadarBeepEnabled\", true)"));
        assertTrue(config.contains("defineInRange(\"droneRadarBeepVolume\", 0.45D"));
        assertTrue(config.contains("define(\"droneRadarOnlyBeepWhenHeld\", true)"));
        assertTrue(config.contains("define(\"debugDroneRadar\", false)"));
        assertTrue(config.contains("defineInRange(\"droneRadarHudScale\", 0.55D"));
        assertTrue(config.contains("defineInRange(\"droneRadarHudXOffset\", 8"));
        assertTrue(config.contains("defineInRange(\"droneRadarHudYOffset\", 8"));
        assertTrue(config.contains("define(\"droneRadarCompactMode\", true)"));
        assertTrue(draft.contains("droneRadarDetectionRange"));
        assertTrue(draft.contains("droneRadarBeepVolume"));
        assertTrue(draft.contains("droneRadarHudScale"));
        assertTrue(draft.contains("droneRadarCompactMode"));
        assertTrue(screen.contains("case JAMMER -> 2"));
        assertTrue(screen.contains("screen.sbwdroneconfig.config.label.radar_detection_range"));
        assertTrue(screen.contains("screen.sbwdroneconfig.config.label.radar_hud_scale"));
    }

    @Test
    void radarSoundAndTranslationsAreRegistered() throws Exception {
        String sounds = Files.readString(Path.of("src/main/resources/assets/sbwdroneconfig/sounds.json"));
        String soundRegistry = Files.readString(Path.of("src/main/java/nl/smartstreamlabs/sbwdroneconfig/AddonSounds.java"));

        assertTrue(soundRegistry.contains("DRONE_RADAR_BEEP"));
        assertTrue(sounds.contains("\"drone_radar_beep\""));
        assertTrue(sounds.contains("subtitles.sbwdroneconfig.drone_radar_beep"));

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("assets/sbwdroneconfig/lang/en_us.json")) {
            assertNotNull(stream, "English language file should be packaged.");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(root.has("overlay.sbwdroneconfig.radar_title"));
            assertTrue(root.has("overlay.sbwdroneconfig.radar_fiber"));
            assertTrue(root.has("screen.sbwdroneconfig.config.label.radar_beep_volume"));
        }
    }
}
