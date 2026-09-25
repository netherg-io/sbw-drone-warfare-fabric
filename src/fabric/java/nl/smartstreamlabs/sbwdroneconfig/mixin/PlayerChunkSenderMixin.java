package nl.smartstreamlabs.sbwdroneconfig.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import nl.smartstreamlabs.sbwdroneconfig.RemoteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A pilot's pending chunks go out nearest to his drone first, not to his body (see {@link RemoteView}).
 * A client that takes chunks slowly otherwise gets the drone's own chunk last, and until then the
 * server does not send him the drone at all.
 */
@Mixin(PlayerChunkSender.class)
abstract class PlayerChunkSenderMixin {
    @Redirect(method = "sendNextChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos sbwdroneconfig$nearestToView(ServerPlayer player) {
        return RemoteView.viewCentre(player);
    }
}
