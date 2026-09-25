package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.embedded.EmbeddedChannel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import nl.smartstreamlabs.sbwdroneconfig.DroneWarfare;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import nl.smartstreamlabs.sbwdroneconfig.RemoteView;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Every way a flight ends (blockfield-releases#4, stage 1): the monitor is switched off, the drone is
 * destroyed or removed, the operator dies, leaves or changes dimension. Each must leave the server
 * in one state: the pilot's client told to reset its camera (it cannot be told once he has left),
 * the monitor off, the drone without a session, no input accepted any more (a stale client cannot
 * revive the flight), the view back at the body and no chunk ticket left for the drone.
 * A lost link is not an exit: the view stays (no video), the drone ignores the held sticks in
 * failsafe and obeys them again when the link returns. A reconnect gets neither a second session
 * nor a second drone.
 */
public final class SessionLifecycleGameTest implements FabricGameTest {
    private static final Logger LOG = LogUtils.getLogger();
    static final int TRIGGER = 30;
    static final int CHECK = TRIGGER + 45;

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void exitMonitorOff(GameTestHelper helper) {
        exit(helper, "monitor off", c -> c.player.getMainHandItem().use(helper.getLevel(), c.player, InteractionHand.MAIN_HAND), true, true);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void exitDroneDestroyed(GameTestHelper helper) {
        exit(helper, "drone destroyed", c -> c.drone.hurt(helper.getLevel().damageSources().generic(), 100), true, false);
    }

    /** Removal without damage: a round's cleanup, /kill, a despawn. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void exitDroneRemoved(GameTestHelper helper) {
        exit(helper, "drone removed", c -> c.drone.discard(), true, false);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void exitOperatorDeath(GameTestHelper helper) {
        exit(helper, "operator death", c -> c.player.kill(), true, true);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void exitOperatorDisconnect(GameTestHelper helper) {
        exit(helper, "operator disconnect", c -> disconnect(c.player), false, true);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void exitDimensionChange(GameTestHelper helper) {
        exit(helper, "dimension change", c -> {
            ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            c.player.changeDimension(new DimensionTransition(nether, new Vec3(0.5, 100, 0.5), Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
        }, true, true);
    }

    private static void exit(GameTestHelper helper, String name, Consumer<MockPilotClient> trigger, boolean cameraReset, boolean droneStays) {
        MockPilotClient client = PilotResyncGameTest.fly(helper, 64);
        ServerLevel level = helper.getLevel();
        DroneEntity drone = (DroneEntity) client.drone;
        int[] acceptedAtExit = {0};
        helper.runAfterDelay(TRIGGER, () -> {
            helper.assertTrue(client.accepted > 10 && tickets(level, drone) > 0,
                    "a live flight first: tickets=" + tickets(level, drone) + " " + client.summary());
            acceptedAtExit[0] = client.accepted;
            trigger.accept(client);
        });
        helper.runAfterDelay(CHECK, () -> {
            ServerPlayer pilot = client.player;
            boolean online = level.getServer().getPlayerList().getPlayer(pilot.getUUID()) == pilot;
            boolean using = online && NBTTool.getTag(pilot.getMainHandItem()).getBoolean("Using");
            String session = drone.isRemoved() ? "(removed)" : drone.getEntityData().get(DroneEntity.SESSION);
            int acceptedAfter = client.accepted - acceptedAtExit[0];
            boolean viewAtBody = !online || RemoteView.viewpoint(pilot) == null && pilot.getChunkTrackingView() instanceof ChunkTrackingView.Positioned v
                    && v.center().equals(pilot.chunkPosition());
            int tickets = tickets(level, drone);
            long drones = level.getEntities(DroneWarfare.FPV, e -> true).size();
            LOG.info("FPV-EXIT {}: cameraReset={} monitorUsing={} session={} inputsAcceptedAfter={} viewAtBody={} droneTickets={} fpvInWorld={} ({})",
                    name, client.resetCamera, using, session, acceptedAfter, viewAtBody, tickets, drones, client.summary());
            helper.assertTrue(!cameraReset || client.resetCamera >= 1, name + ": the client must be told to reset its camera");
            helper.assertFalse(using, name + ": the monitor must be off");
            helper.assertTrue(drone.isRemoved() || "none".equals(session), name + ": the drone must have no session");
            helper.assertTrue(acceptedAfter == 0, name + ": no input may be accepted after the exit: " + acceptedAfter);
            helper.assertTrue(viewAtBody, name + ": the view must be back at the body");
            helper.assertTrue(tickets == 0, name + ": no chunk ticket may stay for the drone: " + tickets);
            helper.assertTrue(drones == (droneStays ? 1 : 0), name + ": FPV drones in the world: " + drones);
            if (!drone.isRemoved()) drone.discard();
            helper.succeed();
        });
    }

    /**
     * A jammer at the drone takes the control link. The view stays on the drone (the HUD shows the lost
     * link and no video), held Space does not raise the throttle in failsafe, and once the jammer is
     * off the pilot flies again without touching the monitor.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void linkLossKeepsViewNoStuckInput(GameTestHelper helper) {
        MockPilotClient client = PilotResyncGameTest.fly(helper, 64);
        FpvDrone drone = (FpvDrone) client.drone;
        ServerPlayer jammer = helper.makeMockServerPlayerInLevel();
        ItemStack device = new ItemStack(DroneWarfare.JAMMER);
        device.set(DataComponents.CUSTOM_DATA, CustomData.of(activeTag(true)));
        float[] thr = new float[4];
        helper.runAfterDelay(20, () -> {
            client.keys = 0b10000;
            thr[0] = drone.throttle();
        });
        helper.runAfterDelay(40, () -> {
            thr[1] = drone.throttle();
            jammer.teleportTo(helper.getLevel(), drone.getX() + 1, drone.getY(), drone.getZ(), 0, 0);
            jammer.setItemInHand(InteractionHand.MAIN_HAND, device);
        });
        helper.runAfterDelay(90, () -> {
            thr[2] = drone.throttle();
            helper.assertTrue(drone.linkQuality() < 0.05f, "the jammer must take the link: LQ " + drone.linkQuality());
            jammer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        helper.runAfterDelay(150, () -> {
            thr[3] = drone.throttle();
            boolean using = NBTTool.getTag(client.player.getMainHandItem()).getBoolean("Using");
            String session = drone.getEntityData().get(DroneEntity.SESSION);
            LOG.info("FPV-EXIT link loss (jammer at the drone), Space held: throttle {} -> {} (link up) -> {} (jammed) -> {} (link back); "
                            + "LQ now {} cameraReset={} monitorUsing={} sessionKept={} ({})",
                    thr[0], thr[1], thr[2], thr[3], drone.linkQuality(), client.resetCamera, using, !"none".equals(session), client.summary());
            helper.assertTrue(thr[1] > thr[0] + 0.1f, "held Space must raise the throttle while the link is up");
            helper.assertTrue(thr[2] < thr[1], "in failsafe the held Space must not raise the throttle");
            helper.assertTrue(thr[3] > thr[2] + 0.1f, "the pilot must fly again when the link returns");
            helper.assertTrue(client.resetCamera == 0 && using && !"none".equals(session), "a lost link keeps the view and the session");
            drone.discard();
            helper.succeed();
        });
    }

    /**
     * The operator leaves mid-flight and comes back (his saved inventory, the same drone in the world).
     * No session survives the disconnect; switching the monitor on again gives one new session on the
     * same drone, never a second drone.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
    public void reconnectNoSecondSessionOrDrone(GameTestHelper helper) {
        MockPilotClient client = PilotResyncGameTest.fly(helper, 64);
        ServerLevel level = helper.getLevel();
        DroneEntity drone = (DroneEntity) client.drone;
        GameProfile profile = client.player.getGameProfile();
        String[] sessions = new String[3];
        ServerPlayer[] back = new ServerPlayer[1];
        helper.runAfterDelay(TRIGGER, () -> {
            sessions[0] = drone.getEntityData().get(DroneEntity.SESSION);
            disconnect(client.player);
        });
        helper.runAfterDelay(TRIGGER + 20, () -> {
            back[0] = join(level, profile);
            sessions[1] = drone.getEntityData().get(DroneEntity.SESSION);
            helper.assertFalse(NBTTool.getTag(back[0].getMainHandItem()).getBoolean("Using"), "the monitor comes back off");
            back[0].getMainHandItem().use(level, back[0], InteractionHand.MAIN_HAND);
            sessions[2] = drone.getEntityData().get(DroneEntity.SESSION);
        });
        helper.runAfterDelay(TRIGGER + 60, () -> {
            long drones = level.getEntities(DroneWarfare.FPV, e -> true).size();
            boolean flying = com.atsuishio.superbwarfare.control.DroneControlAccess.INSTANCE.resolve(back[0]) == drone;
            LOG.info("FPV-RECONNECT session before={} after reconnect={} after monitor on={} fpvInWorld={} controlsSameDrone={}",
                    sessions[0], sessions[1], sessions[2], drones, flying);
            helper.assertTrue("none".equals(sessions[1]), "no session may survive the disconnect");
            helper.assertTrue(!"none".equals(sessions[2]) && !sessions[2].equals(sessions[0]), "the monitor starts one new session");
            helper.assertTrue(drones == 1, "exactly one FPV drone: " + drones);
            helper.assertTrue(flying, "the returning operator flies the same drone");
            drone.discard();
            helper.succeed();
        });
    }

    static net.minecraft.nbt.CompoundTag activeTag(boolean on) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("Active", on);
        return tag;
    }

    static void disconnect(ServerPlayer player) {
        player.connection.onDisconnect(new DisconnectionDetails(Component.literal("game test")));
    }

    /** A player logging in again with {@code profile}: the saved player data comes back, as on a real join. */
    static ServerPlayer join(ServerLevel level, GameProfile profile) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    /** Chunk tickets held for this drone: the addon's view ticket and SBW's per-vehicle keep-loaded ones. */
    static int tickets(ServerLevel level, Entity drone) {
        try {
            Field dm = ChunkMap.class.getDeclaredField("distanceManager");
            dm.setAccessible(true);
            Field all = DistanceManager.class.getDeclaredField("tickets");
            all.setAccessible(true);
            Field key = Ticket.class.getDeclaredField("key");
            key.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>>) all.get(dm.get(level.getChunkSource().chunkMap));
            int n = 0;
            for (SortedArraySet<Ticket<?>> set : map.values()) {
                for (Ticket<?> t : set) {
                    if (Integer.valueOf(drone.getId()).equals(key.get(t))
                            && (t.getType().toString().equals("sbwdroneconfig_fpv_view") || t.getType().toString().equals("post_teleport"))) n++;
                }
            }
            return n;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
