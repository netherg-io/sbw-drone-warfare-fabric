package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Own item icons from tools/item_icons.py: checks they stay in sync with the item models and CREDITS.md. */
class ItemIconsTest {
    private static final Path ASSETS = Path.of("src/fabric/resources/assets/sbwdroneconfig");
    private static final Path TEXTURES = ASSETS.resolve("textures/item");
    private static final Path MODELS = ASSETS.resolve("models/item");
    private static final Path CREDITS = Path.of("CREDITS.md");

    private static final List<String> ICONS = List.of("cubed_fpv_drone", "fibre_fpv_drone", "signal_jammer");

    @Test
    void iconsAre16x16WithAlpha() throws IOException {
        for (String name : ICONS) {
            BufferedImage img = ImageIO.read(TEXTURES.resolve(name + ".png").toFile());
            assertNotNull(img, name);
            assertEquals(16, img.getWidth(), name + " width");
            assertEquals(16, img.getHeight(), name + " height");
            assertTrue(img.getColorModel().hasAlpha(), name + " needs an alpha channel");
        }
    }

    @Test
    void modIconIs128x128WithAlpha() throws IOException {
        BufferedImage img = ImageIO.read(ASSETS.resolve("icon.png").toFile());
        assertNotNull(img);
        assertEquals(128, img.getWidth());
        assertEquals(128, img.getHeight());
        assertTrue(img.getColorModel().hasAlpha());
    }

    private static final Pattern LAYER0 = Pattern.compile("\"layer0\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    void itemModelsReferenceExistingTextures() throws IOException {
        for (String item : ICONS) {
            String json = Files.readString(MODELS.resolve(item + ".json"));
            Matcher m = LAYER0.matcher(json);
            assertTrue(m.find(), item + " model has no layer0 texture");
            String texture = m.group(1);
            assertEquals("sbwdroneconfig:item/" + item, texture, item + " should reference its own icon");
            Path textureFile = TEXTURES.resolve(item + ".png");
            assertTrue(Files.exists(textureFile), texture + " texture file missing");
        }
    }

    @Test
    void shippedIconsAreListedInCredits() throws IOException {
        String credits = Files.readString(CREDITS);
        assertTrue(credits.contains("assets/sbwdroneconfig/icon.png"), "mod icon missing from CREDITS.md");
        for (String item : ICONS) {
            String path = "assets/sbwdroneconfig/textures/item/" + item + ".png";
            assertTrue(credits.contains(path), path + " missing from CREDITS.md");
        }
    }
}
