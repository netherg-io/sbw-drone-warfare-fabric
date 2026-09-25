package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Contact fuze at speed (blockfield-releases#4, stage 2): an armed FPV flown nose first into a thin
 * block (a glass pane, iron bars, a fence, one block of stone) or a mob at 30, 60 and 100 m/s must
 * go off exactly once, at the obstacle, never behind it. The shots run one after another in their own
 * batch, so every SBW explosion recorded meanwhile is this test's.
 */
public final class FuzeGameTest implements FabricGameTest {
    private static final Logger LOG = LogUtils.getLogger();
    /** Every SBW custom explosion, recorded by ExplosionCounterMixin. */
    public static final List<Vec3> EXPLOSIONS = new CopyOnWriteArrayList<>();
    static final int[] SPEEDS = {30, 60, 100};
    static final Block[] THIN = {Blocks.GLASS_PANE, Blocks.IRON_BARS, Blocks.OAK_FENCE, Blocks.STONE};
    static final int START_Z = 1, OBSTACLE_Z = 26, LANE_SPACING = 7, SHOT_TICKS = 30;
    /** SBW's setDeltaMovement refuses a jump of more than 2.83 blocks/tick; build the speed up in steps. */
    static final double RAMP = 2.5;

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 700, batch = "fuze")
    public void fuzeAtSpeed(GameTestHelper helper) {
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        Runnable unforce = MockPilotClient.forceChunks(helper, 0, -1, 6, OBSTACLE_Z + 4);
        List<String> results = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int shot = 0;
        for (Block block : THIN) {
            for (int speed : SPEEDS) shoot(helper, operator, shot++, block, speed, results, failures);
        }
        for (int speed : SPEEDS) shoot(helper, operator, shot++, null, speed, results, failures);
        // A mob right behind a pane: two things to hit in one tick, still one detonation.
        shoot(helper, operator, shot++, Blocks.GLASS_PANE, 100, results, failures, true);
        helper.runAfterDelay((long) shot * SHOT_TICKS + 10, () -> {
            unforce.run();
            results.forEach(r -> LOG.info("FPV-FUZE {}", r));
            helper.assertTrue(failures.isEmpty(), "fuze failures: " + failures);
            helper.succeed();
        });
    }

    private static void shoot(GameTestHelper helper, ServerPlayer operator, int shot, Block block, int speed,
                              List<String> results, List<String> failures) {
        shoot(helper, operator, shot, block, speed, results, failures, false);
    }

    private static void shoot(GameTestHelper helper, ServerPlayer operator, int shot, Block block, int speed,
                              List<String> results, List<String> failures, boolean mobBehind) {
        // One column of lanes: the test's own chunks are the ticking ones.
        int x = 3;
        int y = 4 + shot * LANE_SPACING;
        long at = (long) shot * SHOT_TICKS + 5;
        FpvDrone[] drone = new FpvDrone[1];
        Zombie[] target = new Zombie[1];
        int[] before = new int[1];
        List<String> track = new ArrayList<>();
        for (int k = 1; k <= 8; k++) {
            helper.runAfterDelay(at + 2 + k, () -> {
                if (drone[0] != null) track.add(String.format("%.1f/%.1f%s", helper.relativeVec(drone[0].position()).z,
                        drone[0].getDeltaMovement().z * 20, drone[0].isRemoved() ? "x" : ""));
            });
        }
        helper.runAfterDelay(at, () -> {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                for (int z = START_Z - 1; z <= OBSTACLE_Z + 3; z++) helper.setBlock(new BlockPos(x + dx, y + dy, z), Blocks.AIR);
                if (block != null) helper.setBlock(new BlockPos(x + dx, y + dy, OBSTACLE_Z), block);
            }
            if (block == null || mobBehind) {
                target[0] = helper.spawn(EntityType.ZOMBIE, new BlockPos(x, y - 1, block == null ? OBSTACLE_Z : OBSTACLE_Z + 1));
                target[0].setNoAi(true);
                target[0].setNoGravity(true);
            }
        });
        helper.runAfterDelay(at + 2, () -> {
            before[0] = EXPLOSIONS.size();
            drone[0] = FpvStrikeGameTest.armed(helper, operator, new BlockPos(x, y, START_Z));
        });
        // A sustained dive: the drone is held at the test speed (reached in steps) until it strikes.
        for (int k = 0; k < SHOT_TICKS - 6; k++) {
            helper.runAfterDelay(at + 2 + k, () -> {
                if (drone[0] == null || drone[0].isRemoved()) return;
                double v = Math.min(speed / 20.0, drone[0].getDeltaMovement().z + RAMP);
                drone[0].setDeltaMovement(0, 0, v);
            });
        }
        helper.runAfterDelay(at + SHOT_TICKS - 3, () -> {
            // Only this lane's blasts; anything else that explodes meanwhile is not this shot.
            net.minecraft.world.phys.AABB lane = new net.minecraft.world.phys.AABB(helper.absoluteVec(new Vec3(x - 3, y - 3, START_Z - 3)),
                    helper.absoluteVec(new Vec3(x + 4, y + 4, OBSTACLE_Z + 5)));
            List<Vec3> blasts = EXPLOSIONS.subList(before[0], EXPLOSIONS.size()).stream().filter(lane::contains).toList();
            double obstacle = helper.absoluteVec(new Vec3(0, 0, block == null ? OBSTACLE_Z : OBSTACLE_Z)).z;
            String where = blasts.isEmpty() ? "-" : String.format("%.2f", blasts.get(0).z - obstacle);
            String name = block == null ? "zombie" : block.getDescriptionId().replace("block.minecraft.", "") + (mobBehind ? "+zombie behind" : "");
            String line = String.format("%s at %d m/s: explosions=%d, first at %s m from the obstacle's near face, drone removed=%s, z/speed by tick %s%s",
                    name, speed, blasts.size(), where, drone[0].isRemoved(), track,
                    target[0] == null ? "" : String.format(", zombie health %.1f", target[0].getHealth()));
            results.add(line);
            // The obstacle block spans [obstacle, obstacle + 1); a blast past its far face means the drone went through.
            boolean ok = blasts.size() == 1 && drone[0].isRemoved() && blasts.get(0).z - obstacle <= 1.0 && blasts.get(0).z - obstacle >= -1.0;
            if (!ok) failures.add(line);
            if (!drone[0].isRemoved()) drone[0].discard();
            if (target[0] != null) target[0].discard();
        });
    }
}
