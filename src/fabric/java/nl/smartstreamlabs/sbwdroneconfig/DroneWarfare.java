package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.item.misc.AbstractDeployerItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class DroneWarfare implements ModInitializer {
    public static final EntityType<FpvDrone> FPV = drone("cubed_fpv_drone", false);
    public static final EntityType<FpvDrone> FPV_FIBRE = drone("fibre_fpv_drone", true);
    public static final Item FPV_ITEM = deployer("cubed_fpv_drone", FPV);
    public static final Item FPV_FIBRE_ITEM = deployer("fibre_fpv_drone", FPV_FIBRE);

    /** Handheld Signal Jammer (upstream mechanic): use toggles it; it jams while held in either hand. */
    public static final Item JAMMER = Registry.register(BuiltInRegistries.ITEM, id("signal_jammer"),
            new Item(new Item.Properties().stacksTo(1)) {
                @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);
                    boolean on = !isActive(stack);
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Active", on));
                    if (!level.isClientSide()) {
                        player.displayClientMessage(Component.translatable(on ? "message.sbwdroneconfig.jammer_on" : "message.sbwdroneconfig.jammer_off"), true);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                on ? SoundEvents.NOTE_BLOCK_CHIME.value() : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.25f, on ? 1.65f : 0.75f);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }

                @Override public boolean isFoil(ItemStack stack) { return isActive(stack); }
            });

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("sbwdroneconfig", path);
    }

    private static EntityType<FpvDrone> drone(String path, boolean fibre) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id(path),
                EntityType.Builder.<FpvDrone>of((type, level) -> new FpvDrone(type, level, fibre), MobCategory.MISC)
                        .sized(0.6f, 0.2f).clientTrackingRange(128).updateInterval(1)
                        .build("sbwdroneconfig:" + path));
    }

    private static Item deployer(String path, EntityType<FpvDrone> type) {
        return Registry.register(BuiltInRegistries.ITEM, id(path), new AbstractDeployerItem(new Item.Properties().stacksTo(4)) {
            @Override public Entity spawnDeployedEntity(Level level, Player player) {
                FpvDrone drone = type.create(level);
                // A picked-up fibre drone brings its paid-out fibre back (FpvDrone.interact).
                ItemStack stack = player.getMainHandItem().is(this) ? player.getMainHandItem() : player.getOffhandItem();
                CustomData data = stack.get(DataComponents.ENTITY_DATA);
                if (drone != null && data != null && stack.is(this)) EntityType.updateCustomEntityTag(level, player, drone, data);
                return drone;
            }

            @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
                CustomData data = stack.get(DataComponents.ENTITY_DATA);
                if (data == null) return;
                double left = Math.max(0, FpvLink.SPOOL_M - data.copyTag().getDouble(FpvLink.PAID_OUT_TAG));
                lines.add(Component.translatable("item.sbwdroneconfig.fibre_left", String.format("%.2f", left / 1000))
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        });
    }

    static boolean isActive(ItemStack stack) {
        return stack.is(JAMMER) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Active");
    }

    static boolean isJamming(Player player) {
        return player.isAlive() && !player.isSpectator() && (isActive(player.getMainHandItem()) || isActive(player.getOffhandItem()));
    }

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(RemoteView::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RemoteView.clear());
    }
}
