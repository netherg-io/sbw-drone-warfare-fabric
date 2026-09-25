package nl.smartstreamlabs.sbwdroneconfig.mixin;

import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every left-click swing a client reports may cut laid fibre ({@link FpvDrone#swing}). */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class SwingMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void sbwdroneconfig$cutFibre(ServerboundSwingPacket packet, CallbackInfo ci) {
        FpvDrone.swing(player);
    }
}
