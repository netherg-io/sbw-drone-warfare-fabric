package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AddonCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SbwDroneRangeConfig.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sbwdroneconfig.main"))
                    .icon(() -> new ItemStack(AddonItems.DRONE_JAMMER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(AddonItems.DRONE_DETECTION_SIREN.get());
                        output.accept(AddonItems.ANTI_DRONE_NET.get());
                        output.accept(AddonItems.ANTI_DRONE_NET_CARPET.get());
                        output.accept(AddonItems.ANTI_DRONE_NET_PANEL.get());
                        output.accept(AddonItems.EXTRACTION_CRATE.get());
                        output.accept(AddonItems.JAMMER.get());
                        output.accept(AddonItems.FUEL_MIXER.get());
                        output.accept(AddonItems.DRONE_JAMMER.get());
                        output.accept(AddonItems.SPOTLIGHT_MODULE.get());
                        output.accept(AddonItems.FIBER_OPTIC_SPOOL_UPGRADE.get());
                        output.accept(AddonItems.EMPTY_JERRYCAN.get());
                        output.accept(AddonItems.GASOLINE_CANISTER.get());
                        output.accept(AddonItems.GASOLINE_JERRYCAN.get());
                        output.accept(AddonItems.CUBED_FPV_DRONE.get());
                        output.accept(AddonItems.LUCAS_DRONE.get());
                    })
                    .build()
    );

    private AddonCreativeTabs() {
    }
}
