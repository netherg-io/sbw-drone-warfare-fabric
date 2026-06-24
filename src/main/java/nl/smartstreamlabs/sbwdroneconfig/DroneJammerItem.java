package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class DroneJammerItem extends Item {
    public static final String TAG_ACTIVE = "sbwdroneconfigJammerActive";

    public DroneJammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.sbwdroneconfig.drone_jammer.1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.sbwdroneconfig.drone_jammer.2").withStyle(ChatFormatting.DARK_GREEN));
        tooltipComponents.add(Component.translatable("tooltip.sbwdroneconfig.drone_jammer.3").withStyle(ChatFormatting.DARK_GREEN));
        tooltipComponents.add(
                Component.translatable(
                        isActive(stack)
                                ? "tooltip.sbwdroneconfig.drone_jammer.state_on"
                                : "tooltip.sbwdroneconfig.drone_jammer.state_off"
                ).withStyle(isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.RED)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        boolean active = !isActive(stack);
        setActive(stack, active);

        if (!level.isClientSide()) {
            player.displayClientMessage(
                    Component.translatable(
                            active
                                    ? "message.sbwdroneconfig.drone_jammer_enabled"
                                    : "message.sbwdroneconfig.drone_jammer_disabled"
                    ).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean isActive(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AddonItems.DRONE_JAMMER.get()) && stack.getOrCreateTag().getBoolean(TAG_ACTIVE);
    }

    public static ItemStack findActiveJammer(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (isActive(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (isActive(offHand)) {
            return offHand;
        }

        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (isActive(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean isActiveJammerHeld(Player player) {
        if (player == null) {
            return false;
        }
        return isActive(player.getMainHandItem()) || isActive(player.getOffhandItem());
    }

    public static void setActive(ItemStack stack, boolean active) {
        if (stack.isEmpty() || !stack.is(AddonItems.DRONE_JAMMER.get())) {
            return;
        }
        stack.getOrCreateTag().putBoolean(TAG_ACTIVE, active);
    }
}
