package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.item.misc.AbstractDeployerItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class DroneWarfare implements ModInitializer {
    public static final EntityType<FpvDrone> FPV = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("sbwdroneconfig", "cubed_fpv_drone"),
            EntityType.Builder.<FpvDrone>of(FpvDrone::new, MobCategory.MISC)
                    .sized(0.6f, 0.2f).clientTrackingRange(128).updateInterval(1)
                    .build("sbwdroneconfig:cubed_fpv_drone"));
    public static final Item FPV_ITEM = Registry.register(BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath("sbwdroneconfig", "cubed_fpv_drone"),
            new AbstractDeployerItem(new Item.Properties().stacksTo(4)) {
                @Override public Entity spawnDeployedEntity(Level level, Player player) {
                    return new FpvDrone(FPV, level);
                }
            });

    @Override public void onInitialize() {}
}
