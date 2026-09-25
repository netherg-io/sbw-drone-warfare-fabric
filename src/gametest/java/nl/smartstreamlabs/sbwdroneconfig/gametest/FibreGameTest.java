package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.DroneWarfare;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import org.slf4j.Logger;

import java.util.List;

/**
 * The fibre variant (blockfield-releases#4, stage 3): laid fibre is cut by a player's swing through
 * it (the real swing packet), not through a wall or from afar; the cable keeps a bounded number of
 * elements over a long flight; and a picked-up fibre drone does not get a fresh 3 km spool.
 */
public final class FibreGameTest implements FabricGameTest {
    private static final Logger LOG = LogUtils.getLogger();

    /** A fibre drone that has flown 40 m east at y+3, laying fibre. */
    private static FpvDrone laid(GameTestHelper helper, ServerPlayer operator) {
        ServerLevel level = helper.getLevel();
        Runnable unforce = MockPilotClient.forceChunks(helper, 0, 0, 46, 10);
        helper.runAfterDelay(110, unforce::run);
        FpvDrone drone = DroneWarfare.FPV_FIBRE.create(level);
        Vec3 start = helper.absoluteVec(new Vec3(2.5, 3, 2.5));
        drone.moveTo(start.x, start.y, start.z, 0, 0);
        // The operator stands at the spool end; SBW blows up a drone whose operator is out of range.
        operator.teleportTo(level, start.x - 1, start.y - 2, start.z, 0, 0);
        level.addFreshEntity(drone);
        drone.claimBy(operator);
        helper.onEachTick(() -> {
            long t = helper.getTick();
            if (t <= 40) drone.moveTo(start.x + t, start.y, start.z);
            else drone.setPos(start.x + 40, start.y, start.z);
            drone.setDeltaMovement(Vec3.ZERO);
        });
        return drone;
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void fibreCutBySwing(GameTestHelper helper) {
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        FpvDrone drone = laid(helper, operator);
        ServerPlayer far = helper.makeMockServerPlayerInLevel();
        ServerPlayer walled = helper.makeMockServerPlayerInLevel();
        ServerPlayer cutter = helper.makeMockServerPlayerInLevel();
        float[] lq = new float[4];
        helper.runAfterDelay(50, () -> {
            lq[0] = drone.linkQuality();
            // 6 m from the fibre: out of reach.
            look(far, helper.absoluteVec(new Vec3(22.5, 3.5, 8.5)), helper.absoluteVec(new Vec3(22.5, 3.2, 2.5)));
            swing(far);
            lq[1] = drone.linkQuality();
            // Right next to it but behind a glass wall.
            for (int y = 2; y <= 5; y++) helper.setBlock(new BlockPos(12, y, 3), Blocks.GLASS);
            look(walled, helper.absoluteVec(new Vec3(12.5, 3.5, 4.2)), helper.absoluteVec(new Vec3(12.5, 3.2, 2.5)));
            swing(walled);
            lq[2] = drone.linkQuality();
            look(cutter, helper.absoluteVec(new Vec3(22.5, 3.5, 4.0)), helper.absoluteVec(new Vec3(22.5, 3.2, 2.5)));
            swing(cutter);
            lq[3] = drone.linkQuality();
        });
        helper.runAfterDelay(80, () -> {
            List<Float> cable = drone.cable();
            float lastX = cable.isEmpty() ? Float.NaN : cable.get(cable.size() - 3) - (float) helper.absoluteVec(Vec3.ZERO).x;
            LOG.info("FPV-FIBRE link quality: laid {} / swing 6 m away {} / swing behind glass {} / swing through it {}; "
                    + "cable now ends at x={} (cut at 22.5), points {}, throttle {}", lq[0], lq[1], lq[2], lq[3],
                    lastX, cable.size() / 3, drone.throttle());
            helper.assertTrue(lq[0] == 1 && lq[1] == 1 && lq[2] == 1, "only a swing through the fibre cuts it");
            helper.assertTrue(lq[3] == 0 && drone.linkQuality() == 0, "the swing through the fibre must cut it");
            helper.assertTrue(Math.abs(lastX - 22.5f) < 0.6f, "the fibre ends at the cut: " + lastX);
            drone.discard();
            helper.succeed();
        });
    }

    /** Sneak + empty hand on a fibre drone that has laid 40 m: the item keeps them, and the next drone starts at 40 m. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void pickedUpFibreDroneKeepsItsSpool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        FpvDrone drone = laid(helper, operator);
        double[] paid = new double[2];
        helper.runAfterDelay(50, () -> {
            paid[0] = drone.fibrePaidOut();
            operator.getInventory().clearContent();
            operator.setShiftKeyDown(true);
            drone.interact(operator, InteractionHand.MAIN_HAND);
            operator.setShiftKeyDown(false);
        });
        helper.runAfterDelay(55, () -> {
            ItemStack item = operator.getInventory().items.stream().filter(s -> s.is(DroneWarfare.FPV_FIBRE_ITEM)).findFirst().orElse(ItemStack.EMPTY);
            helper.assertFalse(item.isEmpty(), "the pickup must return the fibre drone");
            helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
            operator.setItemInHand(InteractionHand.MAIN_HAND, item.copyWithCount(1));
            BlockPos floor = helper.absolutePos(new BlockPos(6, 1, 6));
            operator.getMainHandItem().useOn(new UseOnContext(operator, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        });
        helper.runAfterDelay(60, () -> {
            FpvDrone next = level.getEntities(DroneWarfare.FPV_FIBRE, d -> d != drone && d.distanceToSqr(helper.absoluteVec(new Vec3(6.5, 2, 6.5))) < 4)
                    .stream().findFirst().orElse(null);
            helper.assertTrue(next != null, "the item must deploy a fibre drone");
            CompoundTag tag = next.saveWithoutId(new CompoundTag());
            paid[1] = tag.getDouble("FpvFibrePaidM");
            LOG.info("FPV-FIBRE picked up after {} m of fibre; the redeployed drone starts at {} m (spool {} m); old drone removed={}",
                    Math.round(paid[0]), Math.round(paid[1]), 3000, drone.isRemoved());
            helper.assertTrue(drone.isRemoved(), "the picked-up drone is gone");
            helper.assertTrue(paid[0] > 30 && Math.abs(paid[1] - paid[0]) < 1e-3, "the spool must stay used: " + paid[0] + " -> " + paid[1]);
            next.discard();
            helper.succeed();
        });
    }

    private static void look(ServerPlayer player, Vec3 from, Vec3 at) {
        player.teleportTo(player.serverLevel(), from.x, from.y - player.getEyeHeight(), from.z, 0, 0);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, at);
    }

    /** The client's swing packet, handled by the real server listener. */
    private static void swing(ServerPlayer player) {
        player.connection.handleAnimate(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }
}
