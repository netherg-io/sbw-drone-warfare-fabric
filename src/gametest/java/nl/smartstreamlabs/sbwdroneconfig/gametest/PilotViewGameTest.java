package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import nl.smartstreamlabs.sbwdroneconfig.DroneWarfare;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Pilot beyond the server view distance (sbw-drone-warfare-fabric#1, 2026-09-25): the stand runs
 * 6 chunks (~96 m). A pilot flies an FPV drone through an active monitor from 60, 120 and 300 m;
 * his client must keep the drone (entity pairing and its chunk), then get his own view back and the
 * drone's chunk ticket must expire after he stops. The mock client acknowledges chunk batches and sends
 * a position every 20 ticks like a real one; nothing else about it is faked.
 */
public final class PilotViewGameTest implements FabricGameTest {
    private static final Logger LOG = LogUtils.getLogger();
    static final int VIEW_CHUNKS = 6;

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pilotAt60m(GameTestHelper helper) { pilotAt(helper, 60); }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pilotAt120m(GameTestHelper helper) { pilotAt(helper, 120); }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pilotAt300m(GameTestHelper helper) { pilotAt(helper, 300); }

    private static void pilotAt(GameTestHelper helper, int metres) {
        ServerLevel level = helper.getLevel();
        level.getServer().getPlayerList().setViewDistance(VIEW_CHUNKS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        ClientInformation d = ClientInformation.createDefault();
        pilot.updateOptions(new ClientInformation(d.language(), VIEW_CHUNKS, d.chatVisibility(), d.chatColors(),
                d.modelCustomisation(), d.mainHand(), d.textFilteringEnabled(), d.allowsListing()));
        helper.onEachTick(() -> {
            pilot.connection.chunkSender.onChunkBatchReceivedByClient(64);
            // A real client sends its position at least every 20 ticks; each packet re-pairs it with every entity.
            if (helper.getTick() % 20 == 0) level.getChunkSource().move(pilot);
        });

        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        FpvDrone drone = DroneWarfare.FPV.create(level);
        drone.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        level.addFreshEntity(drone);
        pilot.teleportTo(level, base.getX() + 0.5 + metres, base.getY(), base.getZ() + 0.5, 0, 0);

        ItemStack monitor = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("superbwarfare:monitor")));
        pilot.setItemInHand(InteractionHand.MAIN_HAND, monitor);
        drone.claimBy(pilot);
        pilot.getMainHandItem().use(level, pilot, InteractionHand.MAIN_HAND);

        helper.runAfterDelay(60, () -> {
            boolean server = com.atsuishio.superbwarfare.control.DroneControlAccess.INSTANCE.resolve(pilot) == drone;
            boolean paired = paired(level, drone, pilot);
            boolean chunk = pilot.getChunkTrackingView().contains(drone.chunkPosition());
            LOG.info("FPV-VIEW d={}m (view {} chunks = {} m): serverControl={} clientHasDrone={} droneChunkSent={}",
                    Math.round(pilot.distanceTo(drone)), VIEW_CHUNKS, VIEW_CHUNKS * 16, server, paired, chunk);
            helper.assertTrue(server, "server must authorise the monitor at " + metres + " m");
            helper.assertTrue(paired, "pilot's client must track his drone at " + metres + " m");
            helper.assertTrue(chunk, "pilot's client must have the drone's chunk at " + metres + " m");
            // Monitor off: the next use leaves the view.
            pilot.getMainHandItem().use(level, pilot, InteractionHand.MAIN_HAND);
        });
        helper.runAfterDelay(100, () -> {
            boolean home = pilot.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view
                    && view.center().equals(pilot.chunkPosition());
            String tickets = tickets(level, drone);
            boolean ticket = tickets.contains("sbwdroneconfig_fpv_view");
            LOG.info("FPV-VIEW d={}m after monitor off: viewBackAtBody={} droneViewTicket={} ({})",
                    metres, home, ticket, tickets);
            helper.assertTrue(home, "pilot's view must return to his body");
            helper.assertFalse(ticket, "the drone view ticket must expire");
            drone.discard();
            helper.succeed();
        });
    }

    private static boolean paired(ServerLevel level, Entity entity, ServerPlayer player) {
        try {
            Field map = ChunkMap.class.getDeclaredField("entityMap");
            map.setAccessible(true);
            Object tracked = ((Int2ObjectMap<?>) map.get(level.getChunkSource().chunkMap)).get(entity.getId());
            if (tracked == null) return false;
            Field seen = tracked.getClass().getDeclaredField("seenBy");
            seen.setAccessible(true);
            return ((Set<?>) seen.get(tracked)).contains(player.connection);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String tickets(ServerLevel level, Entity entity) {
        try {
            Field f = ChunkMap.class.getDeclaredField("distanceManager");
            f.setAccessible(true);
            Object manager = f.get(level.getChunkSource().chunkMap);
            var debug = net.minecraft.server.level.DistanceManager.class.getDeclaredMethod("getTicketDebugString", long.class);
            debug.setAccessible(true);
            return (String) debug.invoke(manager, entity.chunkPosition().toLong());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
