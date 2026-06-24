package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiberOpticCableSegmentConfigTest {
    @Test
    void exposesFiberCableSegmentConfigAccessors() throws Exception {
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableVisualThickness"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableVisualAlpha"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableVisualColor"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableDebugVisualThickness"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableSegmentSpacing"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableSegmentHealth"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableSegmentHitboxSize"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableCanBeDamagedByPlayers"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableCanBeDamagedByProjectiles"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableCanBeDamagedByExplosions"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableProjectileRaycastBreak"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("fiberCableBreaksWhenAnySegmentDestroyed"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("maxFiberCableSegmentsPerDrone"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("debugFiberCableRender"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("debugFiberCableSegments"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("renderRedDebugCable"));
        assertNotNull(AddonConfig.class.getDeclaredMethod("renderLegacyFiberLine"));
    }

    @Test
    void commonConfigDefinesCableSegmentDefaultsAndDisablesLegacyLineByDefault() throws Exception {
        Path configSource = Path.of("src", "main", "java", "nl", "smartstreamlabs", "sbwdroneconfig", "AddonConfig.java");
        String source = Files.readString(configSource, StandardCharsets.UTF_8);

        assertTrue(source.contains("defineInRange(\"fiberCableVisualThickness\", 0.012D"), "Fiber cable visual thickness should default to a very thin 0.012.");
        assertTrue(source.contains("defineInRange(\"fiberCableVisualAlpha\", 0.45D"), "Fiber cable visual alpha should default to 0.45.");
        assertTrue(source.contains("define(\"fiberCableVisualColor\", \"light_gray\""), "Fiber cable visual color should default to light_gray.");
        assertTrue(source.contains("defineInRange(\"fiberCableDebugVisualThickness\", 0.08D"), "Fiber cable debug visual thickness should default to 0.08.");
        assertTrue(source.contains("defineInRange(\"fiberCableSegmentSpacing\", 3.0D"), "Fiber cable segment spacing should default to 3.0 blocks.");
        assertTrue(source.contains("defineInRange(\"fiberCableSegmentHealth\", 4.0D"), "Fiber cable segment health should default to 4.0.");
        assertTrue(source.contains("defineInRange(\"fiberCableSegmentHitboxSize\", 0.55D"), "Fiber cable segment hitbox size should default to 0.55.");
        assertTrue(source.contains("define(\"fiberCableCanBeDamagedByPlayers\", true)"), "Players should be able to damage fiber cable segments by default.");
        assertTrue(source.contains("define(\"fiberCableCanBeDamagedByProjectiles\", true)"), "Projectiles should be able to damage fiber cable segments by default.");
        assertTrue(source.contains("define(\"fiberCableCanBeDamagedByExplosions\", true)"), "Explosions should be able to damage fiber cable segments by default.");
        assertTrue(source.contains("define(\"fiberCableProjectileRaycastBreak\", true)"), "Projectile raycast backup breaking should be enabled by default.");
        assertTrue(source.contains("define(\"fiberCableBreaksWhenAnySegmentDestroyed\", true)"), "Any destroyed segment should sever the cable by default.");
        assertTrue(source.contains("defineInRange(\"maxFiberCableSegmentsPerDrone\", 96"), "Fiber cable segments should clamp to 96 per drone by default.");
        assertTrue(source.contains("define(\"debugFiberCableRender\", false)"), "Fiber cable render debug logs should stay off by default.");
        assertTrue(source.contains("define(\"debugFiberCableSegments\", false)"), "Fiber cable segment debug logs should stay off by default.");
        assertTrue(source.contains("define(\"renderRedDebugCable\", false)"), "The red debug cable should be disabled by default.");
        assertTrue(source.contains("define(\"renderLegacyFiberLine\", false)"), "The old legacy client-side line should be disabled by default.");
    }

    @Test
    void legacyClientLineIsGatedBehindTheNewConfigToggle() throws Exception {
        Path clientSource = Path.of("src", "main", "java", "nl", "smartstreamlabs", "sbwdroneconfig", "DroneFiberOpticClient.java");
        String source = Files.readString(clientSource, StandardCharsets.UTF_8);

        assertTrue(source.contains("AddonConfig.renderLegacyFiberLine()"),
                "The old client-only cable line should only render when the new legacy toggle is enabled.");
    }
}
