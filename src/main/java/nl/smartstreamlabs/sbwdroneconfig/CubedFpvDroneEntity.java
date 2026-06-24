package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.Monitor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.AMMO;

public class CubedFpvDroneEntity extends DroneEntity {
    public CubedFpvDroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level, 0.0F, 0.0F, 0.0F);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (player.isCrouching() && (stack.isEmpty() || stack.is(ModTags.Items.TOOLS_CROWBAR))) {
            if (!this.level().isClientSide()) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(AddonItems.CUBED_FPV_DRONE.get()));

                for (int index = 0; index < this.entityData.get(AMMO); index++) {
                    if (!this.currentItem.isEmpty()) {
                        ItemHandlerHelper.giveItemToPlayer(player, this.currentItem.copy());
                    }
                }

                player.getInventory().items.stream()
                        .filter(itemStack -> itemStack.getItem() == ModItems.MONITOR.get())
                        .forEach(itemStack -> {
                            if (itemStack.getOrCreateTag().getString(Monitor.LINKED_DRONE).equals(this.getStringUUID())) {
                                Monitor.disLink(itemStack, player);
                            }
                        });

                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.interact(player, hand);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(AddonItems.CUBED_FPV_DRONE.get());
    }
}
