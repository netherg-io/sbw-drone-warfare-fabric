package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FiberOpticCableVisualSourceTest {
    @Test
    void configDefaultsSeparateThinVisualWireFromGameplayHitboxes() throws Exception {
        Path configSource = Path.of(
                "src",
                "main",
                "java",
                "nl",
                "smartstreamlabs",
                "sbwdroneconfig",
                "AddonConfig.java"
        );
        String source = Files.readString(configSource, StandardCharsets.UTF_8);

        assertTrue(source.contains(".defineInRange(\"fiberCableVisualThickness\", 0.012D"),
                "Fiber cable visuals should default to a hair-thin render thickness instead of the current thick pipe.");
        assertTrue(source.contains(".defineInRange(\"fiberCableVisualAlpha\", 0.45D"),
                "Fiber cable visuals should expose a separate alpha control so the visible wire can stay faint and realistic.");
        assertTrue(source.contains(".define(\"fiberCableVisualColor\", \"light_gray\""),
                "Fiber cable visuals should expose a pale default color instead of hardcoded black beam rendering.");
        assertTrue(source.contains(".defineInRange(\"fiberCableDebugVisualThickness\", 0.08D"),
                "Debug mode should keep its own thicker visual without affecting the normal gameplay wire thickness.");
        assertTrue(source.contains("public static double fiberCableVisualAlpha()"),
                "Runtime config access should expose the new fiber cable alpha value.");
        assertTrue(source.contains("public static String fiberCableVisualColor()"),
                "Runtime config access should expose the new fiber cable color preset.");
        assertTrue(source.contains("public static double fiberCableDebugVisualThickness()"),
                "Runtime config access should expose the new debug-only fiber cable thickness.");
    }

    @Test
    void rendererUsesThinRibbonStyleAndLeavesSegmentEntitiesInvisibleByDefault() throws Exception {
        Path cableRendererSource = Path.of(
                "src",
                "main",
                "java",
                "nl",
                "smartstreamlabs",
                "sbwdroneconfig",
                "FiberOpticCableRenderer.java"
        );
        String cableRenderer = Files.readString(cableRendererSource, StandardCharsets.UTF_8);

        assertTrue(cableRenderer.contains("RenderType.leash()"),
                "The visible fiber cable should render as a thin colored ribbon/line instead of block cuboids.");
        assertTrue(cableRenderer.contains("AddonConfig.fiberCableVisualAlpha()"),
                "The visible fiber cable should use the new alpha config in normal mode.");
        assertTrue(cableRenderer.contains("AddonConfig.fiberCableDebugVisualThickness()"),
                "The debug render path should use a separate thicker config value.");
        assertTrue(!cableRenderer.contains("renderSingleBlock("),
                "The visible fiber cable should no longer render as tiny concrete blocks that look like a pipe.");

        Path segmentRendererSource = Path.of(
                "src",
                "main",
                "java",
                "nl",
                "smartstreamlabs",
                "sbwdroneconfig",
                "FiberOpticCableSegmentRenderer.java"
        );
        String segmentRenderer = Files.readString(segmentRendererSource, StandardCharsets.UTF_8);

        assertTrue(segmentRenderer.contains("if (!AddonConfig.debugFiberCableSegments())"),
                "Cable segment entities should stay invisible during normal play and only show markers in debug mode.");
        assertTrue(segmentRenderer.contains("LevelRenderer.renderLineBox"),
                "Debug cable segment rendering should use hitbox markers instead of reusing the thick visible cable mesh.");
    }
}
