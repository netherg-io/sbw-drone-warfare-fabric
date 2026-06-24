package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DroneJammerClient {
    private static final int COMPACT_PANEL_WIDTH = 230;
    private static final int COMPACT_PANEL_HEIGHT = 120;
    private static final int COMPACT_RADAR_SIZE = 90;
    private static final int LEGACY_RADAR_SIZE = 104;
    private static final int LEGACY_INFO_WIDTH = 132;
    private static final int LEGACY_PANEL_WIDTH = 258;
    private static final int LEGACY_PANEL_HEIGHT = 170;
    private static final float TEXT_SCALE = 0.65F;
    private static final int OUTER_RING_COLOR = 0xAA1CFF63;
    private static final int INNER_RING_COLOR = 0x8833FF66;
    private static final int BACKGROUND_COLOR = 0xA0030A03;
    private static final int PANEL_COLOR = 0xBB020704;
    private static final int SWEEP_COLOR = 0xAA39FF74;
    private static final int DOT_COLOR = 0xFF8CFF91;
    private static final int FIBER_DOT_COLOR = 0xFF7AD7FF;
    private static final int WARNING_DOT_COLOR = 0xFFFFD35A;
    private static final int CENTER_COLOR = 0xFFCAFFD3;
    private static final int TEXT_COLOR = 0xFFD6FFE2;
    private static final int MUTED_TEXT_COLOR = 0xFF8DAA94;
    private static final ResourceLocation CUSTOM_BEEP_RESOURCE = new ResourceLocation(SbwDroneRangeConfig.MOD_ID, "sounds/drone_radar_beep.ogg");

    private static RadarSnapshot cachedSnapshot = RadarSnapshot.empty();
    private static long nextBeepTick;
    private static long lastDebugTick = Long.MIN_VALUE;
    private static long lastRenderDebugTick = Long.MIN_VALUE;

    private DroneJammerClient() {
    }

    public static void init(net.minecraftforge.eventbus.api.IEventBus modBus) {
        modBus.addListener(DroneJammerClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(DroneJammerClient.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !isRadarActive(player)) {
            cachedSnapshot = RadarSnapshot.empty();
            nextBeepTick = 0L;
            return;
        }

        cachedSnapshot = scan(player);
        handleBeep(player, cachedSnapshot);
        logScanDebug(player, cachedSnapshot);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == null || !"hotbar".equals(event.getOverlay().id().getPath())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !isRadarActive(player)) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        boolean compact = AddonConfig.droneRadarCompactMode();
        int panelWidth = compact ? COMPACT_PANEL_WIDTH : LEGACY_PANEL_WIDTH;
        int panelHeight = compact ? COMPACT_PANEL_HEIGHT : LEGACY_PANEL_HEIGHT;
        float scale = (float) Math.max(0.25D, Math.min(1.25D, AddonConfig.droneRadarHudScale()));
        int scaledPanelWidth = Math.round(panelWidth * scale);
        int scaledPanelHeight = Math.round(panelHeight * scale);
        int panelX = Math.max(4, event.getWindow().getGuiScaledWidth() - scaledPanelWidth - AddonConfig.droneRadarHudXOffset());
        int panelY = Math.max(4, AddonConfig.droneRadarHudYOffset());

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panelX, panelY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        List<RenderedBlip> renderedBlips = compact
                ? drawCompactRadarPanel(guiGraphics, minecraft, player, cachedSnapshot)
                : drawLegacyRadarPanel(guiGraphics, minecraft, player, cachedSnapshot);
        guiGraphics.pose().popPose();
        logRenderDebug(player, renderedBlips);
    }

    private static void handleBeep(LocalPlayer player, RadarSnapshot snapshot) {
        if (!AddonConfig.droneRadarBeepEnabled() || snapshot.closest() == null) {
            nextBeepTick = 0L;
            return;
        }
        if (AddonConfig.droneRadarOnlyBeepWhenHeld() && !DroneJammerItem.isActiveJammerHeld(player)) {
            nextBeepTick = 0L;
            return;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime < nextBeepTick) {
            return;
        }

        int interval = DroneRadarMath.beepIntervalTicks(
                snapshot.closest().distance(),
                AddonConfig.droneRadarMinBeepIntervalTicks(),
                AddonConfig.droneRadarMaxBeepIntervalTicks()
        );
        float pitch = DroneRadarMath.beepPitch(snapshot.closest().distance(), snapshot.range());
        float volume = DroneRadarMath.beepVolume(snapshot.closest().distance(), snapshot.range(), AddonConfig.droneRadarBeepVolume());
        SoundEvent sound = selectRadarBeepSound();
        player.level().playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch,
                false
        );
        nextBeepTick = gameTime + interval;
    }

    private static SoundEvent selectRadarBeepSound() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasBundledCustomBeep = minecraft.getResourceManager().getResource(CUSTOM_BEEP_RESOURCE).isPresent();
        if (hasBundledCustomBeep && AddonSounds.DRONE_RADAR_BEEP.isPresent()) {
            return AddonSounds.DRONE_RADAR_BEEP.get();
        }
        return SoundEvents.NOTE_BLOCK_HAT.value();
    }

    private static boolean isRadarActive(LocalPlayer player) {
        if (!AddonConfig.enableDroneJammer() || !ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID)) {
            return false;
        }
        return player != null && !DroneJammerItem.findActiveJammer(player).isEmpty();
    }

    private static RadarSnapshot scan(LocalPlayer player) {
        double range = AddonConfig.droneRadarDetectionRange();
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<RadarTarget> targets = new ArrayList<>(player.level().getEntities(player, searchBox, entity -> entity.isAlive() && SbwCompat.isDrone(entity)))
                .stream()
                .map(entity -> RadarTarget.from(player, entity))
                .filter(target -> target.distance() <= range)
                .sorted(Comparator.comparingDouble(RadarTarget::distance))
                .limit(16)
                .toList();
        return new RadarSnapshot(range, targets);
    }

    private static List<RenderedBlip> drawCompactRadarPanel(GuiGraphics guiGraphics, Minecraft minecraft, LocalPlayer player, RadarSnapshot snapshot) {
        int radarX = COMPACT_PANEL_WIDTH - COMPACT_RADAR_SIZE - 8;
        int radarY = 15;
        int centerX = radarX + (COMPACT_RADAR_SIZE / 2);
        int centerY = radarY + (COMPACT_RADAR_SIZE / 2);
        int radius = (COMPACT_RADAR_SIZE / 2) - 7;

        guiGraphics.fill(0, 0, COMPACT_PANEL_WIDTH, COMPACT_PANEL_HEIGHT, PANEL_COLOR);
        drawCompactInfoPanel(guiGraphics, minecraft, snapshot, 10, 10);
        drawRadarBackground(guiGraphics, radarX, radarY, COMPACT_RADAR_SIZE, centerX, centerY, radius);
        drawRadarSweep(guiGraphics, centerX, centerY, radius, player.level().getGameTime());
        List<RenderedBlip> renderedBlips = drawDroneDots(guiGraphics, player, snapshot.targets(), centerX, centerY, radius);
        drawNearestDirectionIndicator(guiGraphics, player, snapshot.closest(), centerX, centerY, radius);
        return renderedBlips;
    }

    private static List<RenderedBlip> drawLegacyRadarPanel(GuiGraphics guiGraphics, Minecraft minecraft, LocalPlayer player, RadarSnapshot snapshot) {
        int radarX = LEGACY_INFO_WIDTH + 14;
        int radarY = 24;
        int centerX = radarX + (LEGACY_RADAR_SIZE / 2);
        int centerY = radarY + (LEGACY_RADAR_SIZE / 2);
        int radius = (LEGACY_RADAR_SIZE / 2) - 6;

        guiGraphics.fill(0, 0, LEGACY_PANEL_WIDTH, LEGACY_PANEL_HEIGHT, PANEL_COLOR);
        drawRadarBackground(guiGraphics, radarX, radarY, LEGACY_RADAR_SIZE, centerX, centerY, radius);
        drawRadarSweep(guiGraphics, centerX, centerY, radius, player.level().getGameTime());
        List<RenderedBlip> renderedBlips = drawDroneDots(guiGraphics, player, snapshot.targets(), centerX, centerY, radius);
        drawNearestDirectionIndicator(guiGraphics, player, snapshot.closest(), centerX, centerY, radius);
        drawInfoPanel(guiGraphics, minecraft, snapshot, 8, radarY);
        return renderedBlips;
    }

    private static void drawCompactInfoPanel(GuiGraphics guiGraphics, Minecraft minecraft, RadarSnapshot snapshot, int x, int y) {
        RadarTarget closest = snapshot.closest();
        int signalStrength = closest == null ? 0 : DroneRadarMath.signalStrengthPercent(closest.distance(), snapshot.range());
        DroneRadarMath.RadarMode mode = DroneRadarMath.radarMode(snapshot.targets().size(), closest == null ? Double.MAX_VALUE : closest.distance());
        int line = Math.round(20.0F * TEXT_SCALE);

        guiGraphics.drawString(minecraft.font, Component.literal("DRONE RF"), x, y, 0xFF7BFF6B, true);
        guiGraphics.drawString(minecraft.font, Component.literal("MODE: " + mode.name()), x, y + line, colorForMode(mode), false);
        guiGraphics.drawString(minecraft.font, Component.literal("COUNT: " + snapshot.targets().size()), x, y + (line * 2), TEXT_COLOR, false);
        guiGraphics.drawString(minecraft.font, Component.literal("SIG: " + signalStrength + "%"), x, y + (line * 3), signalColor(signalStrength), false);
        guiGraphics.drawString(minecraft.font, Component.literal("NEAR: " + (closest == null ? "--" : formatDistance(closest.distance()) + "m")), x, y + (line * 4), TEXT_COLOR, false);
        guiGraphics.drawString(minecraft.font, Component.literal(compactTargetLine(closest)), x, y + (line * 5), closest != null && closest.fiberLink() ? FIBER_DOT_COLOR : MUTED_TEXT_COLOR, false);
    }

    private static void drawInfoPanel(GuiGraphics guiGraphics, Minecraft minecraft, RadarSnapshot snapshot, int x, int y) {
        RadarTarget closest = snapshot.closest();
        int signalStrength = closest == null ? 0 : DroneRadarMath.signalStrengthPercent(closest.distance(), snapshot.range());
        DroneRadarMath.RadarMode mode = DroneRadarMath.radarMode(snapshot.targets().size(), closest == null ? Double.MAX_VALUE : closest.distance());

        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.sbwdroneconfig.radar_title"), x, y - 13, 0xFF7BFF6B, true);
        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.sbwdroneconfig.radar_mode", mode.name()), x, y + 4, colorForMode(mode), true);
        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.sbwdroneconfig.radar_count", snapshot.targets().size()), x, y + 17, TEXT_COLOR, false);
        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.sbwdroneconfig.radar_signal", signalStrength), x, y + 30, signalColor(signalStrength), false);

        if (closest == null) {
            guiGraphics.drawString(minecraft.font, Component.translatable("overlay.sbwdroneconfig.radar_no_targets"), x, y + 46, MUTED_TEXT_COLOR, false);
            return;
        }

        guiGraphics.drawString(
                minecraft.font,
                Component.translatable("overlay.sbwdroneconfig.radar_closest", formatDistance(closest.distance())),
                x,
                y + 43,
                TEXT_COLOR,
                false
        );
        guiGraphics.drawString(minecraft.font, Component.literal(closest.typeLabel()), x, y + 56, MUTED_TEXT_COLOR, false);
        guiGraphics.drawString(
                minecraft.font,
                Component.translatable(closest.fiberLink() ? "overlay.sbwdroneconfig.radar_fiber" : "overlay.sbwdroneconfig.link_wireless"),
                x,
                y + 69,
                closest.fiberLink() ? FIBER_DOT_COLOR : TEXT_COLOR,
                false
        );
    }

    private static void drawRadarBackground(GuiGraphics guiGraphics, int x, int y, int radarSize, int centerX, int centerY, int radius) {
        guiGraphics.fill(x, y, x + radarSize, y + radarSize, BACKGROUND_COLOR);
        drawRing(guiGraphics, centerX, centerY, radius, OUTER_RING_COLOR);
        drawRing(guiGraphics, centerX, centerY, (int) (radius * 0.66F), INNER_RING_COLOR);
        drawRing(guiGraphics, centerX, centerY, (int) (radius * 0.33F), INNER_RING_COLOR);
        guiGraphics.hLine(x + 5, x + radarSize - 6, centerY, 0x551DFF64);
        guiGraphics.vLine(centerX, y + 5, y + radarSize - 6, 0x551DFF64);
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, CENTER_COLOR);
    }

    private static void drawRadarSweep(GuiGraphics guiGraphics, int centerX, int centerY, int radius, long gameTime) {
        double angle = Math.toRadians((gameTime * 7L) % 360L);
        int endX = centerX + (int) Math.round(Math.cos(angle) * radius);
        int endY = centerY + (int) Math.round(Math.sin(angle) * radius);
        drawLine(guiGraphics, centerX, centerY, endX, endY, SWEEP_COLOR);
    }

    private static List<RenderedBlip> drawDroneDots(GuiGraphics guiGraphics, LocalPlayer player, List<RadarTarget> targets, int centerX, int centerY, int radius) {
        List<RenderedBlip> renderedBlips = new ArrayList<>();
        int blipRadius = radius - 7;
        for (int index = 0; index < targets.size(); index++) {
            RadarTarget target = targets.get(index);
            DroneRadarMath.Blip blip = computeBlip(player, target, blipRadius);
            int dotX = centerX + blip.x();
            int dotY = centerY + blip.y();
            int color = target.fiberLink() ? FIBER_DOT_COLOR : DOT_COLOR;
            if (index == 0 && target.distance() < 20.0D) {
                color = WARNING_DOT_COLOR;
            }
            int size = index == 0 ? 3 : 2;
            guiGraphics.fill(dotX - size, dotY - size, dotX + size + 1, dotY + size + 1, color);
            renderedBlips.add(new RenderedBlip(target, dotX, dotY));
        }
        return renderedBlips;
    }

    private static void drawNearestDirectionIndicator(GuiGraphics guiGraphics, LocalPlayer player, RadarTarget nearest, int centerX, int centerY, int radius) {
        if (nearest == null) {
            return;
        }

        DroneRadarMath.Blip blip = computeBlip(player, nearest, radius - 2);
        double length = Math.max(1.0D, Math.hypot(blip.x(), blip.y()));
        int markerX = centerX + (int) Math.round((blip.x() / length) * (radius + 7));
        int markerY = centerY + (int) Math.round((blip.y() / length) * (radius + 7));
        int color = nearest.distance() < 20.0D ? WARNING_DOT_COLOR : 0xFFE8FFF0;
        drawLine(guiGraphics, markerX - 4, markerY, markerX + 4, markerY, color);
        drawLine(guiGraphics, markerX, markerY - 4, markerX, markerY + 4, color);
    }

    private static DroneRadarMath.Blip computeBlip(LocalPlayer player, RadarTarget target, int radius) {
        Vec3 offset = target.entity().position().subtract(player.position());
        return DroneRadarMath.blip(offset.x, offset.z, player.getYRot(), AddonConfig.droneRadarDetectionRange(), radius);
    }

    private static void drawRing(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        int previousX = centerX + radius;
        int previousY = centerY;
        for (int degrees = 4; degrees <= 360; degrees += 4) {
            double radians = Math.toRadians(degrees);
            int nextX = centerX + (int) Math.round(Math.cos(radians) * radius);
            int nextY = centerY + (int) Math.round(Math.sin(radians) * radius);
            drawLine(guiGraphics, previousX, previousY, nextX, nextY, color);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private static void drawLine(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            guiGraphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }

        for (int i = 0; i <= steps; i++) {
            float progress = (float) i / (float) steps;
            int x = Math.round(x0 + ((x1 - x0) * progress));
            int y = Math.round(y0 + ((y1 - y0) * progress));
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static int colorForMode(DroneRadarMath.RadarMode mode) {
        return switch (mode) {
            case SCAN -> 0xFF9DFFB1;
            case JAM -> 0xFFFFE38A;
            case OVERLOAD -> 0xFFFF6868;
        };
    }

    private static int signalColor(int signalStrength) {
        if (signalStrength >= 80) {
            return 0xFFFF6868;
        }
        if (signalStrength >= 45) {
            return 0xFFFFE38A;
        }
        return 0xFF9DFFB1;
    }

    private static String compactTargetLine(RadarTarget target) {
        if (target == null) {
            return "NO TARGET";
        }
        String type = target.typeLabel().toUpperCase(Locale.ROOT).contains("LUCAS") ? "LUCAS" : "FPV";
        String link = target.fiberLink() ? "FIBER LINK" : "WIRELESS";
        return type + " / " + link;
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.ROOT, "%.0f", distance);
    }

    private static String typeLabel(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) {
            return entity.getType().toString();
        }
        if (SbwDroneRangeConfig.CUBED_FPV_DRONE_ENTITY_ID.equals(key.toString())) {
            return "FPV Drone";
        }
        if (SbwDroneRangeConfig.LUCAS_DRONE_ENTITY_ID.equals(key.toString())) {
            return "LUCAS Drone";
        }
        return key.toString();
    }

    private static String entityTypeId(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key == null ? entity.getType().toString() : key.toString();
    }

    private static boolean isFiberLink(Entity entity) {
        return DroneFiberOpticClient.isFiberHudActiveFor(entity.getUUID());
    }

    private static void logScanDebug(LocalPlayer player, RadarSnapshot snapshot) {
        if (!AddonConfig.debugDroneRadar()) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime - lastDebugTick < 20L) {
            return;
        }
        lastDebugTick = gameTime;
        RadarTarget closest = snapshot.closest();
        int interval = closest == null
                ? 0
                : DroneRadarMath.beepIntervalTicks(
                        closest.distance(),
                        AddonConfig.droneRadarMinBeepIntervalTicks(),
                        AddonConfig.droneRadarMaxBeepIntervalTicks()
                );
        SbwDroneRangeConfig.LOGGER.info(
                "Drone radar scan count={} closest={} distance={} interval={} range={}",
                snapshot.targets().size(),
                closest == null ? "none" : closest.entity().getId(),
                closest == null ? "none" : String.format(Locale.ROOT, "%.2f", closest.distance()),
                interval,
                snapshot.range()
        );
    }

    private static void logRenderDebug(LocalPlayer player, List<RenderedBlip> renderedBlips) {
        if (!AddonConfig.debugDroneRadar() || renderedBlips.isEmpty()) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime - lastRenderDebugTick < 20L) {
            return;
        }
        lastRenderDebugTick = gameTime;
        for (RenderedBlip renderedBlip : renderedBlips) {
            RadarTarget target = renderedBlip.target();
            SbwDroneRangeConfig.LOGGER.info(
                    "Drone radar blip drone={} type={} distance={} fiber={} screenX={} screenY={}",
                    target.entity().getId(),
                    entityTypeId(target.entity()),
                    String.format(Locale.ROOT, "%.2f", target.distance()),
                    target.fiberLink(),
                    renderedBlip.screenX(),
                    renderedBlip.screenY()
            );
        }
    }

    private record RadarSnapshot(double range, List<RadarTarget> targets) {
        private static RadarSnapshot empty() {
            return new RadarSnapshot(0.0D, List.of());
        }

        private RadarTarget closest() {
            return targets.isEmpty() ? null : targets.get(0);
        }
    }

    private record RadarTarget(Entity entity, double distance, boolean fiberLink, String typeLabel) {
        private static RadarTarget from(LocalPlayer player, Entity entity) {
            return new RadarTarget(
                    entity,
                    Math.sqrt(entity.distanceToSqr(player)),
                    isFiberLink(entity),
                    DroneJammerClient.typeLabel(entity)
            );
        }
    }

    private record RenderedBlip(RadarTarget target, int screenX, int screenY) {
    }
}
