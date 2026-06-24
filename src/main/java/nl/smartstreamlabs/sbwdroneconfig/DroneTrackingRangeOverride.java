package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.ToIntFunction;

public final class DroneTrackingRangeOverride {
    private static volatile int lastAppliedRangeChunks = Integer.MIN_VALUE;

    private DroneTrackingRangeOverride() {
    }

    public static void applyConfiguredRange() {
        if (!ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID)) {
            return;
        }

        EntityType<?> droneType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(SbwDroneRangeConfig.DRONE_ENTITY_ID));
        if (droneType == null) {
            SbwDroneRangeConfig.LOGGER.warn("Could not find Superb Warfare drone entity type {}; skipping tracking override.", SbwDroneRangeConfig.DRONE_ENTITY_ID);
            return;
        }

        int currentRangeChunks = droneType.clientTrackingRange();
        int desiredRangeChunks = Math.max(currentRangeChunks, blocksToTrackingChunks(AddonConfig.droneMaxRange() + 64));
        if (lastAppliedRangeChunks == desiredRangeChunks && currentRangeChunks >= desiredRangeChunks) {
            return;
        }
        if (desiredRangeChunks <= currentRangeChunks) {
            lastAppliedRangeChunks = desiredRangeChunks;
            return;
        }

        try {
            boolean updatedInt = tryUpdateTrackingRangeValue(droneType, currentRangeChunks, desiredRangeChunks);
            boolean updatedSupplier = tryUpdateTrackingRangeSupplier(droneType, currentRangeChunks, desiredRangeChunks);

            if (updatedInt || updatedSupplier) {
                lastAppliedRangeChunks = droneType.clientTrackingRange();
                SbwDroneRangeConfig.LOGGER.info(
                        "Raised Superb Warfare drone client tracking range from {} chunks ({} blocks) to {} chunks ({} blocks).",
                        currentRangeChunks,
                        trackingChunksToBlocks(currentRangeChunks),
                        droneType.clientTrackingRange(),
                        trackingChunksToBlocks(droneType.clientTrackingRange())
                );
            } else {
                SbwDroneRangeConfig.LOGGER.warn(
                        "Could not locate the Superb Warfare drone tracking fields. Drone range remains at {} chunks ({} blocks).",
                        currentRangeChunks,
                        trackingChunksToBlocks(currentRangeChunks)
                );
            }
        } catch (Exception exception) {
            SbwDroneRangeConfig.LOGGER.error("Failed to override Superb Warfare drone tracking range.", exception);
        }
    }

    private static boolean tryUpdateTrackingRangeValue(EntityType<?> entityType, int currentRange, int desiredRange) throws IllegalAccessException {
        for (Field field : EntityType.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != int.class) {
                continue;
            }

            field.setAccessible(true);
            if (field.getInt(entityType) != currentRange) {
                continue;
            }

            field.setInt(entityType, desiredRange);
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean tryUpdateTrackingRangeSupplier(EntityType<?> entityType, int currentRange, int desiredRange) throws IllegalAccessException {
        for (Field field : EntityType.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != ToIntFunction.class) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(entityType);
            if (!(value instanceof ToIntFunction<?> rawFunction)) {
                continue;
            }

            ToIntFunction<EntityType<?>> function = (ToIntFunction<EntityType<?>>) rawFunction;
            if (function.applyAsInt(entityType) != currentRange) {
                continue;
            }

            field.set(entityType, (ToIntFunction<EntityType<?>>) ignored -> desiredRange);
            return true;
        }

        return false;
    }

    private static int blocksToTrackingChunks(int blocks) {
        return Math.max(1, (blocks + 15) / 16);
    }

    private static int trackingChunksToBlocks(int trackingChunks) {
        return trackingChunks * 16;
    }
}
