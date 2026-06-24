package nl.smartstreamlabs.sbwdroneconfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix4f;

import java.util.Locale;

public final class FiberOpticCableRenderer {
    private static final int MIN_VISUAL_SEGMENTS = 12;
    private static final int MAX_VISUAL_SEGMENTS = 20;
    private static final long DEBUG_INTERVAL_TICKS = 20L;

    private static long lastDebugTick = Long.MIN_VALUE;

    private FiberOpticCableRenderer() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(FiberOpticCableRenderer::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(FiberOpticCableRenderer.class);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Vec3 cameraPos = event.getCamera().getPosition();

        if (!DroneFiberOpticClient.hasSyncedFiberState()) {
            DroneFiberOpticClient.updateRenderDebugState(true, false, "No client fiber state found.");
            maybeLogMissingState(level, cameraPos);
            return;
        }

        DroneFiberOpticClient.FiberHudState state = DroneFiberOpticClient.currentState();
        if (state == null || state.linkMode() != DroneLinkMode.FIBER_OPTIC || level == null) {
            DroneFiberOpticClient.updateRenderDebugState(true, false, "No client fiber state found.");
            maybeLogMissingState(level, cameraPos);
            return;
        }

        Entity drone = DroneFiberOpticClient.findActiveDroneEntity(level);
        if (drone == null || !drone.isAlive()) {
            DroneFiberOpticClient.updateRenderDebugState(true, false, "Drone entity not found on client.");
            maybeLogDebug(level, state, null, cameraPos, null, null, 0, true);
            return;
        }

        Vec3 anchorPos = new Vec3(state.anchorX(), state.anchorY(), state.anchorZ());
        Vec3 dronePos = drone.position();
        Vec3 renderStart = FiberOpticCableRenderMath.toRenderAnchor(anchorPos);
        Vec3 renderEnd = FiberOpticCableRenderMath.toRenderEnd(dronePos, drone.getYRot(), drone.getBbWidth(), drone.getBbHeight());
        int segmentCount = computeVisualSegmentCount(state.cableLength());
        Vec3[] cablePoints = FiberOpticCableRenderMath.buildCableCurvePoints(renderStart, renderEnd, segmentCount);
        CableRenderStyle style = resolveCableRenderStyle();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        for (int index = 0; index < cablePoints.length - 1; index++) {
            renderCableSegment(consumer, matrix, level, cameraPos, cablePoints[index], cablePoints[index + 1], style);
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.leash());

        DroneFiberOpticClient.updateRenderDebugState(true, true, null);
        maybeLogDebug(level, state, drone, cameraPos, renderStart, renderEnd, segmentCount, true);
    }

    private static void renderCableSegment(
            VertexConsumer consumer,
            Matrix4f matrix,
            ClientLevel level,
            Vec3 cameraPos,
            Vec3 start,
            Vec3 end,
            CableRenderStyle style
    ) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-6D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        Vec3 midpoint = start.lerp(end, 0.5D);
        Vec3 toCamera = cameraPos.subtract(midpoint);
        Vec3 primaryOffset = perpendicularOffset(direction, toCamera, style.halfThickness());
        Vec3 secondaryOffset = perpendicularOffset(direction, primaryOffset, style.halfThickness() * 0.55D);
        int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(midpoint));

        renderRibbonQuad(consumer, matrix, start, end, primaryOffset, style, packedLight);
        renderRibbonQuad(consumer, matrix, start, end, secondaryOffset, style, packedLight);
    }

    private static Vec3 perpendicularOffset(Vec3 direction, Vec3 reference, double halfThickness) {
        Vec3 side = direction.cross(reference);
        if (side.lengthSqr() <= 1.0E-8D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() <= 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return side.normalize().scale(halfThickness);
    }

    private static void renderRibbonQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 start,
            Vec3 end,
            Vec3 offset,
            CableRenderStyle style,
            int packedLight
    ) {
        Vec3 startLeft = start.subtract(offset);
        Vec3 startRight = start.add(offset);
        Vec3 endRight = end.add(offset);
        Vec3 endLeft = end.subtract(offset);

        putVertex(consumer, matrix, startLeft, style, packedLight);
        putVertex(consumer, matrix, startRight, style, packedLight);
        putVertex(consumer, matrix, endRight, style, packedLight);
        putVertex(consumer, matrix, endLeft, style, packedLight);

        putVertex(consumer, matrix, endLeft, style, packedLight);
        putVertex(consumer, matrix, endRight, style, packedLight);
        putVertex(consumer, matrix, startRight, style, packedLight);
        putVertex(consumer, matrix, startLeft, style, packedLight);
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 point, CableRenderStyle style, int packedLight) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(style.red(), style.green(), style.blue(), style.alpha())
                .uv2(packedLight)
                .endVertex();
    }

    private static int computeVisualSegmentCount(double cableLength) {
        int dynamicCount = (int) Math.ceil(Math.max(1.0D, cableLength) / 3.0D);
        return Math.max(MIN_VISUAL_SEGMENTS, Math.min(MAX_VISUAL_SEGMENTS, dynamicCount));
    }

    private static CableRenderStyle resolveCableRenderStyle() {
        double thickness = AddonConfig.debugFiberCableRender()
                ? AddonConfig.fiberCableDebugVisualThickness()
                : AddonConfig.fiberCableVisualThickness();
        int alpha = clampColorChannel(Math.round((float) (AddonConfig.fiberCableVisualAlpha() * 255.0D)));

        if (AddonConfig.debugFiberCableRender() && AddonConfig.renderRedDebugCable()) {
            return new CableRenderStyle(thickness, 230, 48, 48, Math.max(alpha, 200));
        }

        return switch (AddonConfig.fiberCableVisualColor()) {
            case "off_white" -> new CableRenderStyle(thickness, 210, 210, 200, alpha);
            case "pale_gray" -> new CableRenderStyle(thickness, 180, 180, 170, alpha);
            default -> new CableRenderStyle(thickness, 192, 192, 184, alpha);
        };
    }

    private static int clampColorChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static void maybeLogMissingState(ClientLevel level, Vec3 cameraPos) {
        if (!AddonConfig.debugFiberCableRender() || level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime - lastDebugTick < DEBUG_INTERVAL_TICKS) {
            return;
        }

        lastDebugTick = gameTime;
        SbwDroneRangeConfig.LOGGER.info("No client fiber state found.");
        SbwDroneRangeConfig.LOGGER.info(
                "Fiber cable renderer called=true syncedFiberState=false cameraPos=[{}, {}, {}]",
                format(cameraPos.x),
                format(cameraPos.y),
                format(cameraPos.z)
        );
    }

    private static void maybeLogDebug(
            ClientLevel level,
            DroneFiberOpticClient.FiberHudState state,
            Entity drone,
            Vec3 cameraPos,
            Vec3 renderStart,
            Vec3 renderEnd,
            int segmentCount,
            boolean rendererCalled
    ) {
        if (!AddonConfig.debugFiberCableRender() || level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime - lastDebugTick < DEBUG_INTERVAL_TICKS) {
            return;
        }

        lastDebugTick = gameTime;
        SbwDroneRangeConfig.LOGGER.info(
                "Fiber cable renderer called={} syncedFiberState={} linkMode={} anchorPos=[{}, {}, {}] dronePos=[{}, {}, {}] renderStart=[{}, {}, {}] renderEnd=[{}, {}, {}] cameraPos=[{}, {}, {}] currentLength={} segmentCount={}",
                rendererCalled,
                state != null,
                state != null ? state.linkMode() : "none",
                state != null ? format(state.anchorX()) : "n/a",
                state != null ? format(state.anchorY()) : "n/a",
                state != null ? format(state.anchorZ()) : "n/a",
                drone != null ? format(drone.getX()) : "n/a",
                drone != null ? format(drone.getY()) : "n/a",
                drone != null ? format(drone.getZ()) : "n/a",
                renderStart != null ? format(renderStart.x) : "n/a",
                renderStart != null ? format(renderStart.y) : "n/a",
                renderStart != null ? format(renderStart.z) : "n/a",
                renderEnd != null ? format(renderEnd.x) : "n/a",
                renderEnd != null ? format(renderEnd.y) : "n/a",
                renderEnd != null ? format(renderEnd.z) : "n/a",
                format(cameraPos.x),
                format(cameraPos.y),
                format(cameraPos.z),
                state != null ? format(state.cableLength()) : "n/a",
                segmentCount
        );
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private record CableRenderStyle(double thickness, int red, int green, int blue, int alpha) {
        private double halfThickness() {
            return thickness * 0.5D;
        }
    }
}
