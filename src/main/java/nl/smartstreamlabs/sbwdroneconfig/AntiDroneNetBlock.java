package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.UUID;

public class AntiDroneNetBlock extends IronBarsBlock {
    private static final String TAG_NET_COOLDOWN_UNTIL = "sbwdroneconfigAntiDroneNetCooldownUntil";
    private static final int NET_COOLDOWN_TICKS = 12;
    private static final double JAM_RANGE = 1.5D;

    public AntiDroneNetBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Players should still be able to walk through the anti-drone net.
        return Shapes.empty();
    }

    public static void onDroneBaseTick(Entity drone) {
        if (drone == null || drone.level().isClientSide() || !drone.isAlive() || !SbwCompat.isDrone(drone)) {
            return;
        }

        if (!(drone.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        if (drone.getPersistentData().getLong(TAG_NET_COOLDOWN_UNTIL) > gameTime) {
            return;
        }

        AABB droneBox = drone.getBoundingBox();
        AABB jammerRangeBox = droneBox.inflate(JAM_RANGE);
        BlockPos min = BlockPos.containing(jammerRangeBox.minX, jammerRangeBox.minY, jammerRangeBox.minZ);
        BlockPos max = BlockPos.containing(jammerRangeBox.maxX, jammerRangeBox.maxY, jammerRangeBox.maxZ);

        for (BlockPos scanPos : BlockPos.betweenClosed(min, max)) {
            BlockState scanState = serverLevel.getBlockState(scanPos);
            if (!isAntiDroneNetTrapBlock(scanState)) {
                continue;
            }

            AABB trapBox = getTrapTriggerBox(scanState, scanPos);
            AABB detectionBox = isThinAntiDroneNetTrap(scanState)
                    ? droneBox.inflate(0.2D)
                    : jammerRangeBox;
            if (!detectionBox.intersects(trapBox)) {
                continue;
            }

            tryJam(serverLevel, drone, scanPos.immutable());
            return;
        }
    }

    public static boolean isAntiDroneNetTrapBlock(BlockState state) {
        return state.getBlock() instanceof AntiDroneNetBlock
                || state.getBlock() instanceof AntiDroneNetCarpetBlock
                || state.getBlock() instanceof AntiDroneNetPanelBlock;
    }

    private static boolean isThinAntiDroneNetTrap(BlockState state) {
        return state.getBlock() instanceof AntiDroneNetCarpetBlock || state.getBlock() instanceof AntiDroneNetPanelBlock;
    }

    private static AABB getTrapTriggerBox(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof AntiDroneNetCarpetBlock) {
            return AntiDroneNetCarpetBlock.getDroneTriggerBox(pos);
        }
        if (state.getBlock() instanceof AntiDroneNetPanelBlock) {
            return AntiDroneNetPanelBlock.getDroneTriggerBox(state, pos);
        }
        return new AABB(pos);
    }

    private static void tryJam(ServerLevel serverLevel, Entity entity, BlockPos netPos) {
        long gameTime = serverLevel.getGameTime();
        if (entity.getPersistentData().getLong(TAG_NET_COOLDOWN_UNTIL) > gameTime) {
            return;
        }

        String notificationKey = FiberOpticLinkSystem.handleAntiDroneNetCut(serverLevel, entity);
        boolean disconnected = notificationKey != null || DroneWaterDisconnectSystem.forceControlDisconnect(serverLevel, entity);
        SbwCompat.resetDroneInput(entity);
        entity.getPersistentData().putLong(TAG_NET_COOLDOWN_UNTIL, gameTime + NET_COOLDOWN_TICKS);
        SbwDroneRangeConfig.LOGGER.info("Anti-Drone Net jammed drone at {}", netPos);
        if (!entity.onGround()) {
            entity.getPersistentData().putBoolean("down", true);
        }
        entity.setDeltaMovement(0.0D, Math.min(0.0D, entity.getDeltaMovement().y), 0.0D);
        serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                entity.getX(),
                entity.getY() + 0.35D,
                entity.getZ(),
                8,
                0.18D,
                0.18D,
                0.18D,
                0.02D
        );
        serverLevel.playSound(
                null,
                netPos,
                SoundEvents.REDSTONE_TORCH_BURNOUT,
                SoundSource.BLOCKS,
                1.0F,
                0.8F
        );

        if (disconnected) {
            notifyController(
                    serverLevel,
                    entity,
                    notificationKey != null ? notificationKey : "message.sbwdroneconfig.drone_net_connection_lost"
            );
        }
    }

    private static void notifyController(ServerLevel serverLevel, Entity drone, String messageKey) {
        UUID controllerUuid = SbwCompat.getDroneControllerUuid(drone);
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if ((controllerUuid != null && controllerUuid.equals(player.getUUID()))
                    || SbwCompat.isUsingLinkedMonitorForDrone(player, drone)) {
                player.displayClientMessage(
                        Component.translatable(messageKey).withStyle(net.minecraft.ChatFormatting.RED),
                        true
                );
            }
        }
    }
}
