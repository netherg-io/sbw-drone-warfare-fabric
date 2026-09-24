package nl.smartstreamlabs.sbwdroneconfig.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import nl.smartstreamlabs.sbwdroneconfig.DroneWarfareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rolls the FPV view with the frame. SBW's monitor camera cancels Camera.setup, which also skips
 * the Porting Lib camera-angles event its own drone roll relies on, so the roll is applied here,
 * at the same projection hook SBW uses.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void sbwdroneconfig$fpvRoll(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        var drone = DroneWarfareClient.viewedDrone();
        if (drone != null) poseStack.mulPose(Axis.ZP.rotationDegrees(drone.cameraRoll(partialTick)));
    }
}
