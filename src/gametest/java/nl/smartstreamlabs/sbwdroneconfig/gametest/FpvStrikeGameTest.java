package nl.smartstreamlabs.sbwdroneconfig.gametest;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.DroneWarfare;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FPV durability and strikes (blockfield-releases#4, known limits): upstream 1.0.6 gives the FPV
 * 1 HP; a hit, a wall at speed or water ends it. A strike must still go off through the nose fuze,
 * at the strike point, with the drone as the direct source and the operator as the attacker.
 */
public final class FpvStrikeGameTest implements FabricGameTest {
    private static final Logger LOG = LogUtils.getLogger();
    static final double STRIKE_MS = 12;
    private static final Map<UUID, List<String>> HITS = new ConcurrentHashMap<>();

    static {
        // Every attempt, fatal ones included (AFTER_DAMAGE skips those).
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            HITS.computeIfAbsent(entity.getUUID(), id -> new ArrayList<>()).add(describe(source) + " " + String.format("%.2f", amount));
            return true;
        });
    }

    private static String describe(DamageSource source) {
        Entity direct = source.getDirectEntity(), attacker = source.getEntity();
        return source.getMsgId() + " direct=" + (direct == null ? "-" : BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()))
                + " attacker=" + (attacker == null ? "-" : attacker instanceof ServerPlayer p ? "operator:" + p.getGameProfile().getName()
                : BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString());
    }

    /** A single 2-point hit by a mob (a rifle round does far more). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void fpvSingleHit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5));
        attacker.setNoAi(true);
        FpvDrone drone = spawn(helper, new BlockPos(2, 1, 2), 0);
        float before = drone.getHealth();
        helper.runAfterDelay(2, () -> drone.hurt(level.damageSources().mobAttack(attacker), 2));
        helper.runAfterDelay(10, () -> {
            LOG.info("FPV-HP single 2-point hit: maxHealth={} health {} -> {} destroyed={}",
                    drone.getMaxHealth(), before, drone.getHealth(), drone.isRemoved());
            helper.assertTrue(drone.isRemoved(), "one hit must bring the FPV down");
            helper.succeed();
        });
    }

    /** Unarmed FPV flown into a wall at 12 m/s. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
    public void fpvWallCrash(GameTestHelper helper) {
        wall(helper, 5);
        FpvDrone drone = spawn(helper, new BlockPos(3, 2, 1), 0);
        drone.setDeltaMovement(0, 0, STRIKE_MS / 20);
        helper.runAfterDelay(30, () -> {
            LOG.info("FPV-HP unarmed wall hit at {} m/s: health={} destroyed={} z={}",
                    STRIKE_MS, drone.getHealth(), drone.isRemoved(), String.format("%.2f", drone.getZ()));
            helper.assertTrue(drone.isRemoved(), "a 12 m/s wall hit must break the 1 HP frame");
            helper.succeed();
        });
    }

    /** Armed FPV into a zombie: fuze hit and blast name the drone as direct source, the operator as attacker. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
    public void fpvStrikeAttribution(GameTestHelper helper) {
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        zombie.setNoAi(true);
        FpvDrone drone = armed(helper, operator, new BlockPos(3, 2, 1));
        drone.setDeltaMovement(0, 0, STRIKE_MS / 20);
        helper.runAfterDelay(30, () -> {
            List<String> hits = HITS.getOrDefault(zombie.getUUID(), List.of());
            LOG.info("FPV-STRIKE zombie hits (fuze hit first, then blast): {} zombie alive={}", hits, zombie.isAlive());
            helper.assertTrue(drone.isRemoved(), "the strike must spend the drone");
            helper.assertTrue(!hits.isEmpty(), "the strike must hurt the zombie");
            helper.assertTrue(hits.stream().allMatch(h -> !h.startsWith("projectile_explosion") && !h.contains("custom")
                    || h.contains("direct=sbwdroneconfig:cubed_fpv_drone") && h.contains("attacker=operator:")),
                    "every strike damage must name the drone and the operator: " + hits);
            helper.succeed();
        });
    }

    /** Armed FPV into a wall at 12 m/s: the nose fuze fires, not SBW's destroy() blast of a broken frame. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
    public void fpvArmedWallStrike(GameTestHelper helper) {
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        wall(helper, 5);
        Zombie witness = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 4));
        witness.setNoAi(true);
        FpvDrone drone = armed(helper, operator, new BlockPos(3, 2, 1));
        drone.setDeltaMovement(0, 0, STRIKE_MS / 20);
        helper.runAfterDelay(30, () -> {
            List<String> hits = HITS.getOrDefault(witness.getUUID(), List.of());
            boolean blast = hits.stream().anyMatch(h -> h.contains("attacker=operator:"));
            LOG.info("FPV-STRIKE armed wall hit at {} m/s: destroyed={} witness hits: {}", STRIKE_MS, drone.isRemoved(), hits);
            helper.assertTrue(drone.isRemoved() && blast, "an armed wall strike must go off: " + hits);
            helper.succeed();
        });
    }

    private static void wall(GameTestHelper helper, int z) {
        for (int x = 1; x <= 5; x++) for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    private static FpvDrone spawn(GameTestHelper helper, BlockPos at, float yaw) {
        FpvDrone drone = create(helper, at, yaw);
        helper.getLevel().addFreshEntity(drone);
        return drone;
    }

    private static FpvDrone create(GameTestHelper helper, BlockPos at, float yaw) {
        FpvDrone drone = DroneWarfare.FPV.create(helper.getLevel());
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(at));
        drone.moveTo(pos.x, pos.y + 0.4, pos.z, yaw, 0);
        return drone;
    }

    /** An FPV with the Blockfield class warhead (PG-7VL), already armed for {@code operator}. */
    private static FpvDrone armed(GameTestHelper helper, ServerPlayer operator, BlockPos at) {
        FpvDrone drone = create(helper, at, 0);
        try {
            CompoundTag tag = drone.saveWithoutId(new CompoundTag());
            tag.merge(TagParser.parseTag("{Linked:1b,Controller:\"" + operator.getStringUUID() + "\",KamikazeMode:1b,"
                    + "DisplayEntity:\"superbwarfare:rpg_rocket_standard\","
                    + "DisplayData:\"1.0,1.0,1.0,0.0,-0.23,-1.0,0.0,0.0,0.0,0.1,0.35,2.0\","
                    + "Item:{id:\"superbwarfare:rpg_rocket_standard\",count:1},Ammo:1,MaxAmmo:1}"));
            drone.load(tag);
            var key = FpvDrone.class.getDeclaredMethod("armKey");
            key.setAccessible(true);
            Field armedFor = FpvDrone.class.getDeclaredField("armedFor");
            armedFor.setAccessible(true);
            armedFor.set(drone, key.invoke(drone));
            Field armed = FpvDrone.class.getDeclaredField("ARMED");
            armed.setAccessible(true);
            @SuppressWarnings("unchecked")
            var accessor = (net.minecraft.network.syncher.EntityDataAccessor<Boolean>) armed.get(null);
            drone.getEntityData().set(accessor, true);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        helper.getLevel().addFreshEntity(drone);
        return drone;
    }
}
