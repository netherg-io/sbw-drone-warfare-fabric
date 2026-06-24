package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Locale;

public final class CubedFpvHudOverlay {
    private static final int FPV_WHITE = 0xEAF7F2;
    private static final int FPV_DIM = 0xB9C9C4;
    private static final int FPV_WARNING = 0xFF8C8C;
    private static int osdTick;

    private CubedFpvHudOverlay() {
    }

    public static void init(net.minecraftforge.eventbus.api.IEventBus modBus) {
        modBus.addListener(CubedFpvHudOverlay::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CubedFpvHudOverlay.class);
    }

    public static boolean shouldReplaceDefaultSbwHud() {
        return DroneClientViewContext.activeCubedFpvDrone(Minecraft.getInstance()) != null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            osdTick++;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == null || !"hotbar".equals(event.getOverlay().id().getPath())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        CubedFpvDroneEntity drone = DroneClientViewContext.activeCubedFpvDrone(minecraft);
        if (drone == null || minecraft.player == null) {
            return;
        }

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        float jamProgress = DroneJamOverlayClient.getJamProgressFor(drone.getUUID());

        if (AddonConfig.enableFpvFisheyeEffect()) {
            renderSubtleFpvLens(guiGraphics, width, height);
        }

        renderTopLeft(guiGraphics, minecraft, drone, jamProgress);
        renderTopRight(guiGraphics, minecraft, width);
        renderCenterCrosshair(guiGraphics, width, height, jamProgress);
        renderBottomLeft(guiGraphics, minecraft, drone, height);
        renderBottomRight(guiGraphics, minecraft, drone, width, height);
        renderJamVideoInterference(guiGraphics, width, height, jamProgress, DroneJamOverlayClient.isHardJammed(drone.getUUID()));
    }

    private static void renderTopLeft(GuiGraphics guiGraphics, Minecraft minecraft, CubedFpvDroneEntity drone, float jamProgress) {
        int x = 8;
        int y = 8;
        draw(guiGraphics, minecraft, "BAT: " + formatBatteryVoltage(DroneEnergyClient.getBatteryVoltageFor(drone.getUUID())), x, y, FPV_WHITE);

        if (DroneFiberOpticClient.isFiberHudActiveFor(drone.getUUID())) {
            DroneFiberOpticClient.FiberHudState fiberState = DroneFiberOpticClient.currentState();
            draw(guiGraphics, minecraft, "LINK: FIBER", x, y + 10, 0x9CE6FF);
            draw(guiGraphics, minecraft, "CABLE: " + formatMeters(fiberState.cableLength()) + "m", x, y + 20, FPV_WHITE);
            draw(guiGraphics, minecraft, "TENSION: " + formatPercent(fiberState.cableTension()) + "%", x, y + 30, 0xFFE28A);
            draw(guiGraphics, minecraft, "SPOOL: " + fiberState.spoolPercent() + "%", x, y + 40, fiberState.spoolPercent() <= 20 ? FPV_WARNING : 0xA8FF9E);
        } else {
            int linkQuality = Mth.clamp(Math.round((1.0F - jamProgress) * 100.0F), 0, 100);
            int latency = estimateLatencyMs(drone, jamProgress);
            draw(guiGraphics, minecraft, "LINK: " + linkQuality + "%", x, y + 10, linkQuality <= 35 ? FPV_WARNING : FPV_WHITE);
            draw(guiGraphics, minecraft, "LAT: " + latency + " ms", x, y + 20, latency >= 120 ? FPV_WARNING : FPV_DIM);
        }
    }

    private static void renderTopRight(GuiGraphics guiGraphics, Minecraft minecraft, int width) {
        String rec = "REC";
        int textWidth = minecraft.font.width(rec);
        int x = width - textWidth - 20;
        int y = 8;
        draw(guiGraphics, minecraft, rec, x, y, FPV_WHITE);
        if ((osdTick / 10) % 2 == 0) {
            guiGraphics.fill(width - 12, y + 3, width - 8, y + 7, 0xFFE53935);
        }
    }

    private static void renderCenterCrosshair(GuiGraphics guiGraphics, int width, int height, float jamProgress) {
        int centerX = width / 2;
        int centerY = height / 2;
        int color = jamProgress > 0.65F ? FPV_WARNING : 0xDDEEEA;
        guiGraphics.fill(centerX - 7, centerY, centerX - 2, centerY + 1, 0xFF000000 | color);
        guiGraphics.fill(centerX + 3, centerY, centerX + 8, centerY + 1, 0xFF000000 | color);
        guiGraphics.fill(centerX, centerY - 7, centerX + 1, centerY - 2, 0xFF000000 | color);
        guiGraphics.fill(centerX, centerY + 3, centerX + 1, centerY + 8, 0xFF000000 | color);
    }

    private static void renderBottomLeft(GuiGraphics guiGraphics, Minecraft minecraft, CubedFpvDroneEntity drone, int height) {
        int x = 8;
        int y = height - 34;
        draw(guiGraphics, minecraft, "SPD: " + formatSpeedKmh(drone) + " km/h", x, y, FPV_WHITE);
        draw(guiGraphics, minecraft, "ALT: " + Math.round(drone.getY()) + " m", x, y + 10, FPV_WHITE);
    }

    private static void renderBottomRight(GuiGraphics guiGraphics, Minecraft minecraft, CubedFpvDroneEntity drone, int width, int height) {
        Entity player = minecraft.player;
        if (player == null) {
            return;
        }

        String distance = "DIST: " + Math.round(player.distanceTo(drone)) + " m";
        String heading = "HDG: " + headingDegrees(drone) + " deg";
        int x = width - Math.max(minecraft.font.width(distance), minecraft.font.width(heading)) - 8;
        int y = height - 34;
        draw(guiGraphics, minecraft, distance, x, y, FPV_WHITE);
        draw(guiGraphics, minecraft, heading, x, y + 10, FPV_WHITE);
    }

    private static void renderSubtleFpvLens(GuiGraphics guiGraphics, int width, int height) {
        int edge = Math.max(18, Math.min(width, height) / 12);
        guiGraphics.fill(0, 0, width, edge, 0x16000000);
        guiGraphics.fill(0, height - edge, width, height, 0x16000000);
        guiGraphics.fill(0, 0, edge, height, 0x12000000);
        guiGraphics.fill(width - edge, 0, width, height, 0x12000000);
    }

    private static void renderJamVideoInterference(GuiGraphics guiGraphics, int width, int height, float jamProgress, boolean hardJammed) {
        if (jamProgress < AddonConfig.overlayStartThreshold()) {
            return;
        }

        float intensity = Mth.clamp((jamProgress - (float) AddonConfig.overlayStartThreshold()) / (1.0F - (float) AddonConfig.overlayStartThreshold()), 0.0F, 1.0F);
        int specks = 6 + Mth.floor(intensity * 70.0F);
        for (int i = 0; i < specks; i++) {
            int x = Math.floorMod(osdTick * 29 + i * 43, Math.max(1, width));
            int y = Math.floorMod(osdTick * 13 + i * 31, Math.max(1, height));
            int alpha = Mth.clamp((int) (35 + intensity * 95), 0, 155);
            guiGraphics.fill(x, y, Math.min(width, x + 1), Math.min(height, y + 1), (alpha << 24) | 0xEDEDED);
        }

        int breakupLines = 1 + Mth.floor(intensity * 5.0F);
        for (int i = 0; i < breakupLines; i++) {
            int y = Math.floorMod(osdTick * 7 + i * 53, Math.max(1, height));
            int alpha = Mth.clamp((int) (18 + intensity * 55), 0, 110);
            int inset = Math.floorMod(osdTick * 11 + i * 19, 32);
            guiGraphics.fill(inset, y, Math.max(inset + 1, width - inset), Math.min(height, y + 1), (alpha << 24) | 0xF2F2F2);
        }

        if (hardJammed || jamProgress > 0.85F) {
            int alpha = hardJammed ? 95 : 48;
            guiGraphics.fill(0, 0, width, height, (alpha << 24) | 0x808080);
            draw(guiGraphics, Minecraft.getInstance(), "PACKET LOSS", 8, 40, FPV_WARNING);
        }
    }

    private static void draw(GuiGraphics guiGraphics, Minecraft minecraft, String text, int x, int y, int color) {
        guiGraphics.drawString(minecraft.font, Component.literal(text), x, y, color, true);
    }

    private static String formatBatteryVoltage(double voltage) {
        if (Double.isNaN(voltage)) {
            return "--.-V";
        }
        return String.format(Locale.ROOT, "%.1fV", voltage);
    }

    private static String formatMeters(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int formatPercent(float value) {
        return Mth.clamp(Math.round(value * 100.0F), 0, 100);
    }

    private static int estimateLatencyMs(CubedFpvDroneEntity drone, float jamProgress) {
        double movement = drone.getDeltaMovement().length();
        return Mth.clamp(Math.round((float) (24.0D + movement * 18.0D + jamProgress * 210.0D)), 18, 300);
    }

    private static int formatSpeedKmh(CubedFpvDroneEntity drone) {
        Vec3 delta = drone.getDeltaMovement();
        return Math.max(0, Math.round((float) (delta.length() * 72.0D)));
    }

    private static int headingDegrees(CubedFpvDroneEntity drone) {
        int heading = Math.round(drone.getYRot()) % 360;
        return heading < 0 ? heading + 360 : heading;
    }
}
