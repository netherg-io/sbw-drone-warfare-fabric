package nl.smartstreamlabs.sbwdroneconfig.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import nl.smartstreamlabs.sbwdroneconfig.RemoteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A pilot's chunk cache centre and chunk tracking follow his FPV drone (see {@link RemoteView}).
 * ChunkMap.tick re-runs this for every player each tick, so the view moves with the drone and
 * returns to the body the tick after the view ends.
 */
@Mixin(ChunkMap.class)
abstract class ChunkMapMixin {
    @Redirect(method = "updateChunkTracking", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos sbwdroneconfig$viewCentre(ServerPlayer player) {
        return RemoteView.viewCentre(player);
    }
}
