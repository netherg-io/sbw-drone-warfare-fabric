package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public final class DroneJammerEvents {
    private static final int SCAN_INTERVAL_TICKS = 1;

    private DroneJammerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!AddonConfig.enableDroneJammer() || !ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID)) {
            return;
        }

        ItemStack jammerStack = DroneJammerItem.findActiveJammer(player);
        if (jammerStack.isEmpty()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        if (Math.floorMod(gameTime + player.getId(), SCAN_INTERVAL_TICKS) != 0L) {
            return;
        }

        double range = AddonConfig.jammerRange();
        AABB searchBox = player.getBoundingBox().inflate(range);
        for (Entity drone : level.getEntities(player, searchBox, entity -> shouldAffectDrone(level, player, entity))) {
            double distance = drone.position().distanceTo(player.position());
            if (distance <= range) {
                DroneJamStateManager.recordInfluence(level, drone, distance, range);
            }
        }
    }

    public static boolean isHoldingActiveJammer(net.minecraft.world.entity.player.Player player) {
        if (player == null || !AddonConfig.enableDroneJammer()) {
            return false;
        }

        return !DroneJammerItem.findActiveJammer(player).isEmpty();
    }

    private static boolean shouldAffectDrone(ServerLevel level, ServerPlayer player, Entity entity) {
        if (entity == null || !entity.isAlive() || !SbwCompat.isDrone(entity)) {
            return false;
        }
        if (AddonConfig.affectFriendlyDrones()) {
            return true;
        }

        SbwCompat.DroneOwnerIdentity ownerIdentity = SbwCompat.resolveDroneOwner(level, entity);
        return !ownerIdentity.hasUuid() || !player.getUUID().equals(ownerIdentity.uuid());
    }
}
