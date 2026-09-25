package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.atsuishio.superbwarfare.control.DroneControlAccess;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The client side of a pilot, driven from the packets the server really sends to a mock player.
 * It mirrors what SBW's client does with them: a drone entity that arrives (again) is a new client
 * object whose input sequence starts at 0 ({@code DroneEntity.nextClientSequence}); the counter also
 * restarts when the synced session id changes. Every tick it sends one input the way SBW's monitor
 * does (MouseMoveMessage each frame) and counts what the server accepts.
 */
final class MockPilotClient {
    final ServerPlayer player;
    final Entity drone;
    private final EmbeddedChannel channel;
    boolean hasDrone;
    int adds, removes, accepted, rejected, resetCamera;
    /** Ticks since the drone last (re)appeared on the client, and ticks since an input was last accepted. */
    int sinceAdd, sinceAccepted;
    /** Longest run of ticks the client had the drone but every input was rejected. */
    int longestRejectedRun;
    private int rejectedRun;
    private String session = "none";
    private String counterSession;
    private long counter;
    final List<String> events = new ArrayList<>();

    MockPilotClient(ServerPlayer player, Entity drone) {
        this.player = player;
        this.drone = drone;
        this.channel = channel(player);
    }

    static void viewDistance(ServerPlayer player, int chunks) {
        ClientInformation d = ClientInformation.createDefault();
        player.updateOptions(new ClientInformation(d.language(), chunks, d.chatVisibility(), d.chatColors(),
                d.modelCustomisation(), d.mainHand(), d.textFilteringEnabled(), d.allowsListing()));
    }

    /** Reads what the server sent this tick; then, holding the drone, sends one input. */
    void tick(GameTestHelper helper, boolean sendInput) {
        channel.flush();
        for (Object msg; (msg = channel.readOutbound()) != null; ) receive(msg, helper);
        sinceAdd++;
        sinceAccepted++;
        if (!hasDrone || !sendInput) return;
        if (!session.equals(counterSession)) {
            counterSession = session;
            counter = 0;
        }
        long sequence = counter++;
        // The server half of VehicleMovementMessage/MouseMoveMessage: live ownership, then the session sequence.
        DroneEntity target = DroneControlAccess.INSTANCE.resolve(player);
        boolean ok = target == drone && DroneControlAccess.INSTANCE.acceptsSequence(target, session, sequence);
        if (ok) {
            accepted++;
            sinceAccepted = 0;
            rejectedRun = 0;
        } else {
            rejected++;
            longestRejectedRun = Math.max(longestRejectedRun, ++rejectedRun);
        }
    }

    private void receive(Object msg, GameTestHelper helper) {
        if (msg instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> p : bundle.subPackets()) receive(p, helper);
        } else if (msg instanceof ClientboundAddEntityPacket add && add.getId() == drone.getId()) {
            // A new client entity: its sequence counter starts again, whatever the session.
            hasDrone = true;
            counterSession = null;
            adds++;
            sinceAdd = 0;
            events.add("t" + helper.getTick() + " add");
        } else if (msg instanceof ClientboundRemoveEntitiesPacket remove && remove.getEntityIds().contains(drone.getId())) {
            hasDrone = false;
            removes++;
            rejectedRun = 0;
            events.add("t" + helper.getTick() + " remove");
        } else if (msg instanceof ClientboundSetEntityDataPacket data && data.id() == drone.getId()) {
            for (var value : data.packedItems()) {
                if (value.id() == DroneEntity.SESSION.id()) session = (String) value.value();
            }
        } else if (msg instanceof ClientboundCustomPayloadPacket custom
                && custom.payload().getClass().getSimpleName().contains("ResetCameraType")) {
            resetCamera++;
            events.add("t" + helper.getTick() + " resetCamera");
        }
    }

    String summary() {
        return String.format("adds=%d removes=%d accepted=%d rejected=%d longestRejectedRun=%d resetCamera=%d events=%s",
                adds, removes, accepted, rejected, longestRejectedRun, resetCamera, events);
    }

    /** Chunk tickets held for this entity: the addon's view tickets and SBW's per-vehicle keep-loaded ones. */
    static int tickets(net.minecraft.server.level.ServerLevel level, Entity entity) {
        try {
            Field dm = net.minecraft.server.level.ChunkMap.class.getDeclaredField("distanceManager");
            dm.setAccessible(true);
            Field all = net.minecraft.server.level.DistanceManager.class.getDeclaredField("tickets");
            all.setAccessible(true);
            Field key = net.minecraft.server.level.Ticket.class.getDeclaredField("key");
            key.setAccessible(true);
            var map = (it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<?>) all.get(dm.get(level.getChunkSource().chunkMap));
            int n = 0;
            for (Object set : map.values()) {
                for (Object o : (Iterable<?>) set) {
                    var t = (net.minecraft.server.level.Ticket<?>) o;
                    String type = t.getType().toString();
                    if (Integer.valueOf(entity.getId()).equals(key.get(t))
                            && (type.equals("sbwdroneconfig_fpv_view") || type.equals("post_teleport"))) n++;
                }
            }
            return n;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static EmbeddedChannel channel(ServerPlayer player) {
        try {
            Field conn = net.minecraft.server.network.ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
            conn.setAccessible(true);
            Field f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
            return (EmbeddedChannel) (Channel) f.get(conn.get(player.connection));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
