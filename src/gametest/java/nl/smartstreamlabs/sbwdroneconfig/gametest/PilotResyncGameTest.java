package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.DroneWarfare;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import org.slf4j.Logger;

/**
 * Pilot input after the drone leaves his client and comes back (sbw-drone-warfare-fabric#1,
 * 2026-09-25): a drone teleported past the body's view, or flown faster than the pilot's client
 * takes chunks (lag), is removed from his client and sent again. SBW's client restarts its input
 * sequence on the new entity object, so unless the session restarts too the server rejects every
 * input until the monitor is toggled.
 */
public final class PilotResyncGameTest implements FabricGameTest {
    private static final Logger LOG = LogUtils.getLogger();
    static final int VIEW_CHUNKS = 6;

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pilotInputAfterTeleport120m(GameTestHelper helper) { teleport(helper, 120); }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pilotInputAfterTeleport300m(GameTestHelper helper) { teleport(helper, 300); }

    /** A jump like a teleport or a rubber-band correction: out of the pilot's loaded chunks at once. */
    private static void teleport(GameTestHelper helper, int metres) {
        MockPilotClient client = fly(helper, 64);
        FpvDrone drone = (FpvDrone) client.drone;
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(client.accepted > 20, "input must flow before the jump: " + client.summary());
            drone.teleportTo(drone.getX() + metres, drone.getY(), drone.getZ());
        });
        helper.runAfterDelay(140, () -> {
            boolean back = client.hasDrone && client.adds >= 2;
            LOG.info("FPV-RESYNC teleport {} m: drone back on the client={} input accepted in the last {} ticks={} ({})",
                    metres, back, client.sinceAdd, client.sinceAccepted <= 2, client.summary());
            helper.assertTrue(back, "the drone must come back to the pilot's client: " + client.summary());
            helper.assertTrue(client.sinceAccepted <= 2, "input must reach the drone after it comes back: " + client.summary());
            drone.discard();
            helper.succeed();
        });
    }

    /**
     * 40 m/s straight out to 280 m while the pilot's client takes one chunk per tick (a slow, lagging
     * client): the drone may leave and re-enter his client; input must keep flowing each time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 260)
    public void pilotInputFastFlightLaggingClient(GameTestHelper helper) {
        MockPilotClient client = fly(helper, 1);
        FpvDrone drone = (FpvDrone) client.drone;
        Vec3 start = drone.position();
        double perTick = 40 / 20.0;
        helper.onEachTick(() -> {
            long t = helper.getTick();
            if (t >= 40 && t < 180) {
                drone.moveTo(start.x + (t - 40) * perTick, start.y, start.z);
                drone.setDeltaMovement(Vec3.ZERO);
            }
        });
        helper.runAfterDelay(220, () -> {
            LOG.info("FPV-RESYNC fast flight 40 m/s to {} m, client takes 1 chunk/tick: {}",
                    Math.round(drone.position().distanceTo(start)), client.summary());
            helper.assertTrue(client.hasDrone, "the pilot's client must have his drone: " + client.summary());
            helper.assertTrue(client.longestRejectedRun <= 5, "input must not be rejected for long: " + client.summary());
            helper.assertTrue(client.sinceAccepted <= 2, "input must reach the drone at the end: " + client.summary());
            drone.discard();
            helper.succeed();
        });
    }

    /** A pilot flying an FPV drone through an active monitor; his mock client acks this many chunks per tick. */
    static MockPilotClient fly(GameTestHelper helper, float chunksPerTick) {
        ServerLevel level = helper.getLevel();
        level.getServer().getPlayerList().setViewDistance(VIEW_CHUNKS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        MockPilotClient.viewDistance(pilot, VIEW_CHUNKS);
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        pilot.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        FpvDrone drone = DroneWarfare.FPV.create(level);
        drone.moveTo(base.getX() + 0.5, base.getY() + 20, base.getZ() + 4.5, 0, 0);
        level.addFreshEntity(drone);
        double height = drone.getY();
        MockPilotClient client = new MockPilotClient(pilot, drone);
        helper.onEachTick(() -> {
            pilot.connection.chunkSender.onChunkBatchReceivedByClient(chunksPerTick);
            // A real client sends its position at least every 20 ticks; each packet re-pairs it with every entity.
            if (helper.getTick() % 20 == 0) level.getChunkSource().move(pilot);
            // The drone keeps its height: this is about tracking, not flight.
            drone.setPos(drone.getX(), height, drone.getZ());
            drone.setDeltaMovement(drone.getDeltaMovement().multiply(1, 0, 1));
            client.tick(helper, helper.getTick() > 5);
        });
        ItemStack monitor = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("superbwarfare:monitor")));
        pilot.setItemInHand(InteractionHand.MAIN_HAND, monitor);
        drone.claimBy(pilot);
        pilot.getMainHandItem().use(level, pilot, InteractionHand.MAIN_HAND);
        return client;
    }
}
