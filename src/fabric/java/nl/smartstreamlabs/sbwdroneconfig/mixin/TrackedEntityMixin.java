package nl.smartstreamlabs.sbwdroneconfig.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.RemoteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity tracking distance for a pilot is measured from his FPV drone (see {@link RemoteView}), so he
 * keeps the drone itself and sees what is around it; the chunk check that follows already uses the
 * drone-centred view.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
abstract class TrackedEntityMixin {
    @Redirect(method = "updatePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sbwdroneconfig$viewPosition(ServerPlayer player) {
        Entity drone = RemoteView.viewpoint(player);
        return drone != null ? drone.position() : player.position();
    }
}
