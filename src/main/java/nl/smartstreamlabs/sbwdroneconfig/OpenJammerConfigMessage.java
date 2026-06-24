package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenJammerConfigMessage(BlockPos blockPos, int range, List<String> whitelistedGamertags) {
    public static void encode(OpenJammerConfigMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.blockPos);
        buffer.writeVarInt(message.range);
        buffer.writeCollection(message.whitelistedGamertags, FriendlyByteBuf::writeUtf);
    }

    public static OpenJammerConfigMessage decode(FriendlyByteBuf buffer) {
        return new OpenJammerConfigMessage(buffer.readBlockPos(), buffer.readVarInt(), buffer.readList(FriendlyByteBuf::readUtf));
    }

    public static void handle(OpenJammerConfigMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> JammerScreen.open(message)));
        context.setPacketHandled(true);
    }
}
