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
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class DroneWarfareClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(DroneWarfare.FPV, DroneRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            FpvDrone drone = viewedDrone();
            FpvDrone.viewedId = drone == null ? -1 : drone.getId();
        });
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            FpvDrone drone = viewedDrone();
            if (drone == null || mc.options.hideGui) return;
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
