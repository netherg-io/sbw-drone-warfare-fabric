package nl.smartstreamlabs.sbwdroneconfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix4f;

import java.util.Locale;
import java.util.UUID;

public final class DroneFiberOpticClient {
    private static final int CABLE_SEGMENTS = 10;
    private static final int CABLE_RED = 28;
    private static final int CABLE_GREEN = 28;
    private static final int CABLE_BLUE = 30;
    private static final int CABLE_ALPHA = 115;
    private static final long RENDER_DEBUG_INTERVAL_TICKS = 20L;

    private static FiberHudState state;
    private static long lastRenderDebugTick = Long.MIN_VALUE;
    private static boolean rendererCalled;
    private static boolean droneFoundClient;
    private static String renderWarning;

    private DroneFiberOpticClient() {
    }

    public static void init(net.minecraftforge.eventbus.api.IEventBus modBus) {
        modBus.addListener(DroneFiberOpticClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(DroneFiberOpticClient.class);
    }

    public static void handleSync(DroneFiberOpticSyncMessage message) {
        if (message == null) {
            return;
        }

        state = new FiberHudState(
                message.droneId(),
                message.droneEntityId(),
                message.linkMode(),
                message.fiberSessionActive(),
                message.cableLength(),
                message.maxCableLength(),
                message.cableTension(),
                message.spoolPercent(),
                message.segmentCount(),
                message.anchorX(),
                message.anchorY(),
                message.anchorZ()
        );
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        rendererCalled = false;
        droneFoundClient = false;
        renderWarning = null;

        if (minecraft.player == null || minecraft.level == null) {
            state = null;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == null || !"hotbar".equals(event.getOverlay().id().getPath()) || !shouldRenderHud()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = 10;
        int y = 36;

        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.sbwdroneconfig.link_fiber"), x, y, 0x8ED6FF, true);
        guiGraphics.drawString(
                minecraft.font,
                Component.translatable(
                        "overlay.sbwdroneconfig.fiber_cable",
                        formatCableLength(state.cableLength()),
                        state.maxCableLength()
                ),
                x,
                y + 12,
                0xD8F5FF,
                true
        );
        guiGraphics.drawString(
                minecraft.font,
                Component.translatable(
                        "overlay.sbwdroneconfig.fiber_tension",
                        formatPercent(state.cableTension())
                ),
                x,
                y + 24,
                0xFFE28A,
                true
        );
        guiGraphics.drawString(
                minecraft.font,
                Component.translatable(
                        "overlay.sbwdroneconfig.fiber_spool",
                        state.spoolPercent()
                ),
                x,
                y + 36,
                state.spoolPercent() <= 20 ? 0xFF6B6B : 0xA8FF9E,
                true
        );

        if (AddonConfig.debugFiberCableRender()) {
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("fiber state active: " + state.fiberSessionActive()),
                    x,
                    y + 48,
                    state.fiberSessionActive() ? 0xA8FF9E : 0xFF9A9A,
                    true
            );
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("anchorPos: " + formatVec3(state.anchorX(), state.anchorY(), state.anchorZ())),
                    x,
                    y + 60,
                    0xD8F5FF,
                    true
            );
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("droneId: " + abbreviateUuid(state.droneId())),
                    x,
                    y + 72,
                    0xD8F5FF,
                    true
            );
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("droneFoundClient: " + droneFoundClient),
                    x,
                    y + 84,
                    droneFoundClient ? 0xA8FF9E : 0xFF9A9A,
                    true
            );
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("currentLength: " + formatCableLength(state.cableLength()) + " / " + state.maxCableLength()),
                    x,
                    y + 96,
                    0xD8F5FF,
                    true
            );
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("segmentCount: " + state.segmentCount()),
                    x,
                    y + 108,
                    0xD8F5FF,
                    true
            );
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal("rendererCalled: " + rendererCalled),
                    x,
                    y + 120,
                    rendererCalled ? 0xA8FF9E : 0xFF9A9A,
                    true
            );

            if (renderWarning != null && !renderWarning.isBlank()) {
                guiGraphics.drawString(minecraft.font, Component.literal(renderWarning), x, y + 132, 0xFF6B6B, true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !shouldRenderCable()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        Entity drone = findEntity(level, state.droneId);
        if (drone == null || !drone.isAlive()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        renderCableLine(poseStack, lines, drone);
        poseStack.popPose();

        buffers.endBatch(RenderType.lines());
    }

    private static boolean shouldRenderHud() {
        if (!AddonConfig.enableFiberOpticMode() || state == null || state.linkMode() != DroneLinkMode.FIBER_OPTIC) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID)) {
            return false;
        }
        if (DroneClientViewContext.isActiveCubedFpvView()) {
            return false;
        }

        ItemStack activeMonitor = SbwCompat.getActiveLinkedMonitor(player);
        if (activeMonitor.isEmpty()) {
            return false;
        }

        UUID linkedDroneId = SbwCompat.getLinkedDroneUuid(activeMonitor);
        return linkedDroneId != null && linkedDroneId.equals(state.droneId());
    }

    private static boolean shouldRenderCable() {
        return AddonConfig.renderLegacyFiberLine()
                && hasSyncedFiberState()
                && (Math.abs(state.anchorX) > 1.0E-4D || Math.abs(state.anchorY) > 1.0E-4D || Math.abs(state.anchorZ) > 1.0E-4D);
    }

    private static String formatCableLength(double cableLength) {
        return String.format(Locale.ROOT, "%.1f", cableLength);
    }

    private static int formatPercent(float progress) {
        return Math.max(0, Math.min(100, Math.round(progress * 100.0F)));
    }

    private static void renderCableLine(PoseStack poseStack, VertexConsumer lines, Entity drone) {
        Vec3 anchorPos = new Vec3(state.anchorX, state.anchorY, state.anchorZ);
        Vec3 dronePos = drone.position();
        Vec3 renderStart = FiberOpticCableRenderMath.toRenderAnchor(anchorPos);
        Vec3 renderEnd = FiberOpticCableRenderMath.toRenderEnd(dronePos, drone.getYRot(), drone.getBbWidth(), drone.getBbHeight());
        Vec3[] cablePoints = FiberOpticCableRenderMath.buildCableCurvePoints(renderStart, renderEnd, CABLE_SEGMENTS);

        maybeLogRenderDebug(drone, anchorPos, dronePos, renderStart, renderEnd);
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < cablePoints.length - 1; i++) {
            drawLine(lines, matrix, cablePoints[i], cablePoints[i + 1], CABLE_RED, CABLE_GREEN, CABLE_BLUE, CABLE_ALPHA);
        }
    }

    private static void drawLine(VertexConsumer consumer, Matrix4f matrix, Vec3 from, Vec3 to, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) from.x, (float) from.y, (float) from.z).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) to.x, (float) to.y, (float) to.z).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    private static void maybeLogRenderDebug(Entity drone, Vec3 anchorPos, Vec3 dronePos, Vec3 renderStart, Vec3 renderEnd) {
        if (!AddonConfig.debugFiberOptic()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime - lastRenderDebugTick < RENDER_DEBUG_INTERVAL_TICKS) {
            return;
        }

        lastRenderDebugTick = gameTime;
        SbwDroneRangeConfig.LOGGER.info(
                "Fiber optic render drone={} anchorPos=[{}, {}, {}] dronePos=[{}, {}, {}] currentLength={} renderStart=[{}, {}, {}] renderEnd=[{}, {}, {}]",
                drone.getUUID(),
                formatDebug(anchorPos.x),
                formatDebug(anchorPos.y),
                formatDebug(anchorPos.z),
                formatDebug(dronePos.x),
                formatDebug(dronePos.y),
                formatDebug(dronePos.z),
                formatDebug(state.cableLength),
                formatDebug(renderStart.x),
                formatDebug(renderStart.y),
                formatDebug(renderStart.z),
                formatDebug(renderEnd.x),
                formatDebug(renderEnd.y),
                formatDebug(renderEnd.z)
        );
    }

    static boolean hasSyncedFiberState() {
        return state != null && state.linkMode() == DroneLinkMode.FIBER_OPTIC && state.fiberSessionActive();
    }

    static boolean isFiberHudActiveFor(UUID droneId) {
        return droneId != null
                && state != null
                && state.linkMode() == DroneLinkMode.FIBER_OPTIC
                && state.fiberSessionActive()
                && droneId.equals(state.droneId());
    }

    static FiberHudState currentState() {
        return state;
    }

    static void updateRenderDebugState(boolean wasRendererCalled, boolean wasDroneFoundClient, String warning) {
        rendererCalled = wasRendererCalled;
        droneFoundClient = wasDroneFoundClient;
        renderWarning = warning;
    }

    static Entity findActiveDroneEntity(ClientLevel level) {
        if (level == null || state == null) {
            return null;
        }

        Entity byEntityId = level.getEntity(state.droneEntityId);
        if (byEntityId != null && state.droneId.equals(byEntityId.getUUID())) {
            return byEntityId;
        }

        return findEntity(level, state.droneId);
    }

    private static Entity findEntity(ClientLevel level, UUID uuid) {
        for (Entity entity : level.entitiesForRendering()) {
            if (uuid.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    static record FiberHudState(
            UUID droneId,
            int droneEntityId,
            DroneLinkMode linkMode,
            boolean fiberSessionActive,
            double cableLength,
            int maxCableLength,
            float cableTension,
            int spoolPercent,
            int segmentCount,
            double anchorX,
            double anchorY,
            double anchorZ
    ) {
    }

    private static String formatDebug(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatVec3(double x, double y, double z) {
        return formatDebug(x) + ", " + formatDebug(y) + ", " + formatDebug(z);
    }

    private static String abbreviateUuid(UUID uuid) {
        if (uuid == null) {
            return "none";
        }
        String full = uuid.toString();
        return full.length() <= 12 ? full : full.substring(0, 12);
    }
}
