package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class SbwCompat {
    public static final String TAG_LINKED = "Linked";
    public static final String TAG_USING = "Using";
    public static final String TAG_LINKED_DRONE = "LinkedDrone";
    public static final String TAG_DRONE_OWNER_UUID = "sbwdroneconfigDroneOwnerUuid";
    public static final String TAG_DRONE_OWNER_NAME = "sbwdroneconfigDroneOwnerName";

    private SbwCompat() {
    }

    public static boolean isMonitor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && SbwDroneRangeConfig.MONITOR_ITEM_ID.equals(key.toString());
    }

    public static boolean isLinkedMonitor(ItemStack stack) {
        if (!isMonitor(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getBoolean(TAG_LINKED) && tag.contains(TAG_LINKED_DRONE);
    }

    public static boolean isUsingLinkedMonitor(ItemStack stack) {
        return isLinkedMonitor(stack) && stack.getOrCreateTag().getBoolean(TAG_USING);
    }

    public static UUID getLinkedDroneUuid(ItemStack stack) {
        if (!isLinkedMonitor(stack)) {
            return null;
        }
        String uuidString = stack.getOrCreateTag().getString(TAG_LINKED_DRONE);
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Entity findLinkedDrone(ServerPlayer player, ItemStack stack) {
        UUID uuid = getLinkedDroneUuid(stack);
        if (uuid == null) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(uuid);
        if (entity != null && isDrone(entity)) {
            return entity;
        }
        return null;
    }

    public static boolean isDrone(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && (SbwDroneRangeConfig.DRONE_ENTITY_ID.equals(key.toString())
                || SbwDroneRangeConfig.CUBED_FPV_DRONE_ENTITY_ID.equals(key.toString())
                || SbwDroneRangeConfig.LUCAS_DRONE_ENTITY_ID.equals(key.toString()));
    }

    public static boolean isUsingLinkedMonitorForDrone(Player player, Entity drone) {
        if (player == null || drone == null) {
            return false;
        }
        return isUsingLinkedMonitorForDrone(player.getMainHandItem(), drone)
                || isUsingLinkedMonitorForDrone(player.getOffhandItem(), drone);
    }

    public static ItemStack getActiveLinkedMonitor(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (isUsingLinkedMonitor(mainHand)) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();
        if (isUsingLinkedMonitor(offhand)) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    public static Entity findActiveLinkedDrone(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        ItemStack activeMonitor = getActiveLinkedMonitor(player);
        if (activeMonitor.isEmpty()) {
            return null;
        }

        return findLinkedDrone(player, activeMonitor);
    }

    public static boolean isUsingLinkedMonitorForDrone(ItemStack stack, Entity drone) {
        if (stack.isEmpty() || drone == null) {
            return false;
        }

        UUID linkedDrone = getLinkedDroneUuid(stack);
        return isUsingLinkedMonitor(stack) && linkedDrone != null && linkedDrone.equals(drone.getUUID());
    }

    public static ServerPlayer findLinkedMonitorController(ServerLevel level, Entity drone) {
        if (level == null || drone == null || !isDrone(drone)) {
            return null;
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (isLinkedMonitorForDrone(player.getMainHandItem(), drone)
                    || isLinkedMonitorForDrone(player.getOffhandItem(), drone)) {
                return player;
            }

            for (ItemStack stack : player.getInventory().items) {
                if (isLinkedMonitorForDrone(stack, drone)) {
                    return player;
                }
            }
        }

        return null;
    }

    private static boolean isLinkedMonitorForDrone(ItemStack stack, Entity drone) {
        if (stack.isEmpty() || drone == null) {
            return false;
        }

        UUID linkedDrone = getLinkedDroneUuid(stack);
        return linkedDrone != null && linkedDrone.equals(drone.getUUID());
    }

    public static boolean isSbwBattery(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !SbwDroneRangeConfig.SBW_MOD_ID.equals(key.getNamespace())) {
            return false;
        }
        String path = key.getPath();
        return "cell".equals(path)
                || "battery".equals(path)
                || "small_battery_pack".equals(path)
                || "medium_battery_pack".equals(path)
                || "large_battery_pack".equals(path);
    }

    public static double distanceToLinkedDrone(ServerPlayer player, Entity drone) {
        return player.position().distanceTo(drone.position());
    }

    public static void stopUsingMonitor(ItemStack stack) {
        if (!isMonitor(stack)) {
            return;
        }
        stack.getOrCreateTag().putBoolean(TAG_USING, false);
    }

    public static boolean disconnectControllersForDrone(ServerLevel level, Entity drone) {
        if (level == null || drone == null || !isDrone(drone)) {
            return false;
        }

        boolean disconnected = false;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            boolean playerDisconnected = disconnectPlayerFromDrone(player, drone);
            disconnected = disconnected || playerDisconnected;
        }
        return disconnected;
    }

    public static boolean disconnectPlayerFromDrone(ServerPlayer player, Entity drone) {
        if (player == null || drone == null || !isDrone(drone)) {
            return false;
        }

        boolean disconnected = false;
        disconnected = disconnectMonitorStack(player.getMainHandItem(), drone) || disconnected;
        disconnected = disconnectMonitorStack(player.getOffhandItem(), drone) || disconnected;

        for (ItemStack stack : player.getInventory().items) {
            disconnected = disconnectMonitorStack(stack, drone) || disconnected;
        }

        if (disconnected) {
            resetDroneInput(drone);
        }
        return disconnected;
    }

    private static boolean disconnectMonitorStack(ItemStack stack, Entity drone) {
        if (!isUsingLinkedMonitor(stack)) {
            return false;
        }

        UUID linkedDrone = getLinkedDroneUuid(stack);
        if (linkedDrone == null || !linkedDrone.equals(drone.getUUID())) {
            return false;
        }

        stopUsingMonitor(stack);
        return true;
    }

    public static void resetDroneInput(Entity drone) {
        CompoundTag data = drone.getPersistentData();
        data.putBoolean("left", false);
        data.putBoolean("right", false);
        data.putBoolean("forward", false);
        data.putBoolean("backward", false);
        data.putBoolean("up", false);
        data.putBoolean("down", false);
    }

    public static UUID getDroneControllerUuid(Entity drone) {
        if (drone == null || !isDrone(drone)) {
            return null;
        }

        try {
            Method method = drone.getClass().getMethod("getController");
            Object result = method.invoke(drone);
            if (result instanceof Player player) {
                return player.getUUID();
            }
            if (result instanceof Entity entity) {
                return entity.getUUID();
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    public static String getDroneCustomName(Entity drone) {
        if (drone == null || !isDrone(drone) || !drone.hasCustomName()) {
            return null;
        }

        Component customName = drone.getCustomName();
        if (customName == null) {
            return null;
        }

        String rawName = customName.getString();
        if (rawName == null) {
            return null;
        }

        String sanitized = rawName.trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    public static void assignDroneOwner(Entity drone, Player player) {
        if (player == null) {
            return;
        }
        assignDroneOwner(drone, player.getUUID(), player.getGameProfile().getName());
    }

    public static void assignDroneOwner(Entity drone, UUID ownerUuid, String ownerName) {
        if (drone == null || !isDrone(drone) || ownerUuid == null) {
            return;
        }

        CompoundTag data = drone.getPersistentData();
        data.putUUID(TAG_DRONE_OWNER_UUID, ownerUuid);

        String normalizedName = sanitizeOwnerName(ownerName);
        if (normalizedName != null) {
            data.putString(TAG_DRONE_OWNER_NAME, normalizedName);
        } else {
            data.remove(TAG_DRONE_OWNER_NAME);
        }
    }

    public static void ensureDroneOwner(Entity drone, Player player) {
        if (drone == null || player == null || !isDrone(drone)) {
            return;
        }

        DroneOwnerIdentity ownerIdentity = resolveDroneOwner(player.level() instanceof ServerLevel serverLevel ? serverLevel : null, drone);
        if (!ownerIdentity.hasUuid()) {
            assignDroneOwner(drone, player);
        }
    }

    public static DroneOwnerIdentity resolveDroneOwner(ServerLevel level, Entity drone) {
        if (drone == null || !isDrone(drone)) {
            return DroneOwnerIdentity.UNKNOWN;
        }

        UUID ownerUuid = getStoredDroneOwnerUuid(drone);
        String ownerName = getStoredDroneOwnerName(drone);

        if (ownerUuid == null) {
            UUID controllerUuid = getDroneControllerUuid(drone);
            if (controllerUuid != null) {
                String controllerName = resolvePlayerName(level, controllerUuid);
                String resolvedName = controllerName != null ? controllerName : ownerName;
                assignDroneOwner(drone, controllerUuid, resolvedName);
                return new DroneOwnerIdentity(controllerUuid, sanitizeOwnerName(resolvedName));
            }

            ServerPlayer linkedController = findLinkedMonitorController(level, drone);
            if (linkedController != null) {
                assignDroneOwner(drone, linkedController);
                return new DroneOwnerIdentity(linkedController.getUUID(), linkedController.getGameProfile().getName());
            }

            if (ownerName != null) {
                UUID resolvedUuid = resolvePlayerUuid(level, ownerName);
                if (resolvedUuid != null) {
                    assignDroneOwner(drone, resolvedUuid, ownerName);
                    return new DroneOwnerIdentity(resolvedUuid, ownerName);
                }
            }

            return new DroneOwnerIdentity(null, ownerName);
        }

        if (ownerName == null) {
            String resolvedName = resolvePlayerName(level, ownerUuid);
            if (resolvedName != null) {
                assignDroneOwner(drone, ownerUuid, resolvedName);
                ownerName = resolvedName;
            }
        }

        return new DroneOwnerIdentity(ownerUuid, ownerName);
    }

    public static UUID getStoredDroneOwnerUuid(Entity drone) {
        if (drone == null || !isDrone(drone)) {
            return null;
        }

        CompoundTag data = drone.getPersistentData();
        return data.hasUUID(TAG_DRONE_OWNER_UUID) ? data.getUUID(TAG_DRONE_OWNER_UUID) : null;
    }

    public static String getStoredDroneOwnerName(Entity drone) {
        if (drone == null || !isDrone(drone)) {
            return null;
        }

        return sanitizeOwnerName(drone.getPersistentData().getString(TAG_DRONE_OWNER_NAME));
    }

    private static UUID resolvePlayerUuid(ServerLevel level, String ownerName) {
        String sanitized = sanitizeOwnerName(ownerName);
        if (level == null || sanitized == null) {
            return null;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(sanitized);
        return player == null ? null : player.getUUID();
    }

    private static String resolvePlayerName(ServerLevel level, UUID ownerUuid) {
        if (level == null || ownerUuid == null) {
            return null;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerUuid);
        return player == null ? null : sanitizeOwnerName(player.getGameProfile().getName());
    }

    private static String sanitizeOwnerName(String ownerName) {
        if (ownerName == null) {
            return null;
        }
        String sanitized = ownerName.trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    public record DroneOwnerIdentity(UUID uuid, String name) {
        public static final DroneOwnerIdentity UNKNOWN = new DroneOwnerIdentity(null, null);

        public DroneOwnerIdentity {
            name = sanitizeOwnerName(name);
        }

        public boolean isKnown() {
            return hasUuid() || hasName();
        }

        public boolean hasUuid() {
            return uuid != null;
        }

        public boolean hasName() {
            return name != null && !name.isBlank();
        }

        public String normalizedName() {
            return hasName() ? name.toLowerCase(Locale.ROOT) : "";
        }
    }
}
