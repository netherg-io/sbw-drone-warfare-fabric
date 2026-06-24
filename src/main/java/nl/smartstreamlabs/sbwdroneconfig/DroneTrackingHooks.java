package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class DroneTrackingHooks {
    private static final int TRACKING_BUFFER_BLOCKS = 64;

    private DroneTrackingHooks() {
    }

    public static boolean shouldKeepDroneTrackedTo(ServerPlayer player, Entity trackedEntity) {
        if (player == null || trackedEntity == null || !trackedEntity.isAlive() || !SbwCompat.isDrone(trackedEntity)) {
            return false;
        }

        ItemStack stack = player.getMainHandItem();
        UUID linkedDrone = SbwCompat.getLinkedDroneUuid(stack);
        if (!SbwCompat.isUsingLinkedMonitor(stack) || linkedDrone == null || !linkedDrone.equals(trackedEntity.getUUID())) {
            return false;
        }

        return player.position().distanceTo(trackedEntity.position()) <= getDesiredTrackingRangeBlocks();
    }

    public static int getDesiredTrackingRangeBlocks() {
        return AddonConfig.droneMaxRange() + TRACKING_BUFFER_BLOCKS;
    }

    public static boolean shouldSuppressClientMonitorDisconnect(ItemStack stack, Level level, Entity holder, boolean selected) {
        if (stack.isEmpty() || level == null || holder == null || !level.isClientSide() || !selected) {
            return false;
        }

        if (!SbwCompat.isUsingLinkedMonitor(stack) || !stack.getOrCreateTag().contains(SbwCompat.TAG_LINKED_DRONE)) {
            return false;
        }

        return holder instanceof Player;
    }
}
