package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public final class DroneControlEvents {
    private DroneControlEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!ModList.get().isLoaded(SbwDroneRangeConfig.SBW_MOD_ID) || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!SbwCompat.isLinkedMonitor(stack)) {
            PlayerDroneAnchorManager.release(player, true);
            return;
        }

        if (player.tickCount % 40 == 0) {
            DroneTrackingRangeOverride.applyConfiguredRange();
        }

        Entity drone = SbwCompat.findLinkedDrone(player, stack);
        if (drone == null) {
            PlayerDroneAnchorManager.release(player, true);
            if (SbwCompat.isUsingLinkedMonitor(stack)) {
                SbwCompat.stopUsingMonitor(stack);
                player.displayClientMessage(
                        Component.translatable("message.sbwdroneconfig.drone_missing").withStyle(ChatFormatting.RED),
                        true
                );
            }
            return;
        }

        SbwCompat.ensureDroneOwner(drone, player);

        boolean usingMonitor = SbwCompat.isUsingLinkedMonitor(stack);
        DroneLinkMode linkMode = FiberOpticLinkSystem.getLinkMode(drone);
        boolean anchorEnabled = AddonConfig.enableDronePlayerAnchor();

        if (usingMonitor && AddonConfig.enableBatterySystem()) {
            DroneBatterySystem.tickForActiveController(player, drone);
            DroneBatterySystem.syncForController(player, drone);
        }
        if (usingMonitor && AddonConfig.enableFiberOpticMode()) {
            FiberOpticLinkSystem.syncForController(player, drone);
        }

        if (usingMonitor && AddonConfig.enableDroneJammer()) {
            DroneJamStateManager.markControlled(player, drone);
            DroneJammerSystem.handleControllerFeedback(player, drone);
        }

        if (usingMonitor && anchorEnabled) {
            PlayerDroneAnchorManager.touch(player, drone);
        } else {
            PlayerDroneAnchorManager.release(player, true);
        }

        if (!anchorEnabled && linkMode == DroneLinkMode.WIRELESS) {
            double distance = SbwCompat.distanceToLinkedDrone(player, drone);
            int effectiveRange = DroneWeatherEffects.getEffectiveControlRange(player, drone, AddonConfig.droneMaxRange());
            if (distance > effectiveRange) {
                if (stack.getOrCreateTag().getBoolean(SbwCompat.TAG_USING)) {
                    SbwCompat.stopUsingMonitor(stack);
                    SbwCompat.resetDroneInput(drone);
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.sbwdroneconfig.drone_signal_lost",
                                    effectiveRange
                            ).withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return;
            }
        }

        if (AddonConfig.enableDroneChunkLoading() && usingMonitor) {
            int chunkRadius = AddonConfig.droneChunkLoadRadius();
            ChunkTicketManager.touchDrone(drone, chunkRadius);
        }
    }
}
