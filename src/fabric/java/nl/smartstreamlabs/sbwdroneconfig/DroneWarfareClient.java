package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.client.renderer.entity.DroneRenderer;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.item.misc.MonitorItem;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class DroneWarfareClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(DroneWarfare.FPV, DroneRenderer::new);
        EntityRendererRegistry.register(DroneWarfare.FPV_FIBRE, DroneRenderer::new);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> renderCables(context.matrixStack(), context.camera().getPosition(),
                context.tickCounter().getGameTimeDeltaPartialTick(false)));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            FpvDrone drone = viewedDrone();
            FpvDrone.viewedId = drone == null ? -1 : drone.getId();
        });
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            FpvDrone drone = viewedDrone();
            if (drone == null || mc.options.hideGui) return;
            videoNoise(graphics, drone.videoQuality());
            String link = drone.fibre
                    ? String.format("FIBRE %.2f/%.1f km", drone.fibrePaidOut() / 1000, FpvLink.SPOOL_M / 1000)
                    : String.format("LQ %d%%  VID %d%%", Math.round(drone.linkQuality() * 100), Math.round(drone.videoQuality() * 100));
            graphics.drawCenteredString(mc.font, link, graphics.guiWidth() / 2, graphics.guiHeight() - 58,
                    drone.linkQuality() < 1 || drone.videoQuality() < 0.5 ? 0xFF5555 : 0x55FF55);
            String text = String.format("%s  THR %d%%  %.1f m/s  %.1fV %d%%", drone.isAcro() ? "ACRO" : "ANGLE",
                    Math.round(drone.throttle() * 100), drone.getDeltaMovement().length() * 20,
                    drone.volts(), Math.round(drone.charge() * 100));
            if (drone.payloadKg() > 0) {
                text += String.format("  %.2f kg", drone.payloadKg());
                if (drone.getEntityData().get(DroneEntity.IS_KAMIKAZE)) text += drone.isArmed() ? "  ARMED" : "  SAFE";
            }
            boolean low = drone.volts() < Battery.LOW_VOLTS || drone.charge() <= 0;
            graphics.drawCenteredString(mc.font, text, graphics.guiWidth() / 2, graphics.guiHeight() - 48, low ? 0xFF5555 : 0x55FF55);
        });
    }

    private static final RandomSource NOISE = RandomSource.create();

    /** Analog video breaking up: grey streaks grow with 1 - quality; no picture at all when it is gone. */
    private static void videoNoise(GuiGraphics graphics, float quality) {
        if (quality >= 1) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        float noise = 1 - quality;
        if (quality <= 0) graphics.fill(0, 0, w, h, 0xE0101010);
        for (int y = 0; y < h; y += 2) {
            if (NOISE.nextFloat() >= noise) continue;
            int x = NOISE.nextInt(w), len = 8 + NOISE.nextInt(Math.max(1, (int) (w * noise)));
            int grey = 80 + NOISE.nextInt(176), alpha = (int) (255 * Math.min(1, 0.3f + noise));
            graphics.fill(x - len / 2, y, x + len / 2, y + 1 + NOISE.nextInt(2), alpha << 24 | grey << 16 | grey << 8 | grey);
        }
        if (quality <= 0) {
            var font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, "NO VIDEO", w / 2, h / 2 - 20, 0xFFFFFF);
        }
    }

    /** Fibre laid by each fibre drone: the synced path points, then up to the drone while the fibre holds. */
    private static void renderCables(com.mojang.blaze3d.vertex.PoseStack poses, Vec3 cam, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || poses == null) return;
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        var pose = poses.last();
        boolean drew = false;
        for (var e : mc.level.entitiesForRendering()) {
            if (!(e instanceof FpvDrone drone) || !drone.fibre) continue;
            List<Float> c = drone.cable();
            List<Vec3> points = new java.util.ArrayList<>();
            for (int i = 0; i + 2 < c.size(); i += 3) points.add(new Vec3(c.get(i), c.get(i + 1), c.get(i + 2)));
            if (drone.linkQuality() > 0) points.add(drone.getPosition(partial).add(0, drone.getBbHeight() / 2, 0));
            for (int i = 1; i < points.size(); i++) {
                Vec3 a = points.get(i - 1).subtract(cam), b = points.get(i).subtract(cam), n = b.subtract(a).normalize();
                lines.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(230, 230, 220, 255).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
                lines.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(230, 230, 220, 255).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
                drew = true;
            }
        }
        if (drew) buffers.endBatch(RenderType.lines());
    }

    /** The FPV drone the local player is flying through an active SBW monitor, or null. */
    public static FpvDrone viewedDrone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) return null;
        // Monitor-only tag keys; comparing against ModItems.MONITOR would need Porting Lib on the compile path.
        var tag = NBTTool.getTag(mc.player.getMainHandItem());
        if (!tag.getBoolean(MonitorItem.USING)) return null;
        return EntityFindUtil.findDrone(mc.player.level(), tag.getString(MonitorItem.LINKED_DRONE)) instanceof FpvDrone drone ? drone : null;
    }
}
