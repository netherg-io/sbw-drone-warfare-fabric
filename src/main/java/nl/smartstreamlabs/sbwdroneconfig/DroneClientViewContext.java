package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class DroneClientViewContext {
    private DroneClientViewContext() {
    }

    public static ItemStack activeMonitor(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        return player == null ? ItemStack.EMPTY : SbwCompat.getActiveLinkedMonitor(player);
    }

    public static UUID activeDroneId(Minecraft minecraft) {
        ItemStack monitor = activeMonitor(minecraft);
        return monitor.isEmpty() ? null : SbwCompat.getLinkedDroneUuid(monitor);
    }

    public static Entity activeDroneEntity(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        UUID linkedDroneId = activeDroneId(minecraft);
        if (level == null || linkedDroneId == null) {
            return null;
        }

        Entity fiberEntity = DroneFiberOpticClient.findActiveDroneEntity(level);
        if (fiberEntity != null && linkedDroneId.equals(fiberEntity.getUUID())) {
            return fiberEntity;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (linkedDroneId.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    public static CubedFpvDroneEntity activeCubedFpvDrone(Minecraft minecraft) {
        Entity drone = activeDroneEntity(minecraft);
        return drone instanceof CubedFpvDroneEntity cubed ? cubed : null;
    }

    public static LucasDroneEntity activeLucasDrone(Minecraft minecraft) {
        Entity drone = activeDroneEntity(minecraft);
        return drone instanceof LucasDroneEntity lucas ? lucas : null;
    }

    public static boolean isActiveCubedFpvView() {
        return activeCubedFpvDrone(Minecraft.getInstance()) != null;
    }

    public static boolean isActiveLucasView() {
        return activeLucasDrone(Minecraft.getInstance()) != null;
    }
}
