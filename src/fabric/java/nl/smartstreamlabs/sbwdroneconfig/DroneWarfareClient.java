package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.client.renderer.entity.DroneRenderer;
import com.atsuishio.superbwarfare.item.misc.MonitorItem;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public final class DroneWarfareClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(DroneWarfare.FPV, DroneRenderer::new);
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            // Monitor-only tag keys; comparing against ModItems.MONITOR would need Porting Lib on the compile path.
            var tag = NBTTool.getTag(mc.player.getMainHandItem());
            if (!tag.getBoolean(MonitorItem.USING)
                    || !(EntityFindUtil.findDrone(mc.player.level(), tag.getString(MonitorItem.LINKED_DRONE)) instanceof FpvDrone drone)) return;
            String text = String.format("%s  THR %d%%  %.1f m/s", drone.isAcro() ? "ACRO" : "ANGLE",
                    Math.round(drone.throttle() * 100), drone.getDeltaMovement().length() * 20);
            graphics.drawCenteredString(mc.font, text, graphics.guiWidth() / 2, graphics.guiHeight() - 48, 0x55FF55);
        });
    }
}
