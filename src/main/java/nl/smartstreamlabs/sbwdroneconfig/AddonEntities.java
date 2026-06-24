package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SbwDroneRangeConfig.MOD_ID);

    public static final RegistryObject<EntityType<CubedFpvDroneEntity>> CUBED_FPV_DRONE = ENTITY_TYPES.register(
            CubedFpvDroneDefinition.ID,
            () -> EntityType.Builder.<CubedFpvDroneEntity>of(CubedFpvDroneEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .sized(0.6F, 0.2F)
                    .build(CubedFpvDroneDefinition.ID)
    );

    public static final RegistryObject<EntityType<LucasDroneEntity>> LUCAS_DRONE = ENTITY_TYPES.register(
            LucasDroneDefinition.ID,
            () -> EntityType.Builder.<LucasDroneEntity>of(LucasDroneEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .sized(3.2F, 0.35F)
                    .build(LucasDroneDefinition.ID)
    );

    public static final RegistryObject<EntityType<FiberOpticCableSegmentEntity>> FIBER_OPTIC_CABLE_SEGMENT = ENTITY_TYPES.register(
            "fiber_optic_cable_segment",
            () -> EntityType.Builder.<FiberOpticCableSegmentEntity>of(FiberOpticCableSegmentEntity::new, MobCategory.MISC)
                    .setTrackingRange(256)
                    .setUpdateInterval(1)
                    .sized(0.35F, 0.35F)
                    .build("fiber_optic_cable_segment")
    );

    public static final RegistryObject<EntityType<RecoverableFiberOpticCableEntity>> RECOVERABLE_FIBER_OPTIC_CABLE = ENTITY_TYPES.register(
            "recoverable_fiber_optic_cable",
            () -> EntityType.Builder.<RecoverableFiberOpticCableEntity>of(RecoverableFiberOpticCableEntity::new, MobCategory.MISC)
                    .setTrackingRange(128)
                    .setUpdateInterval(10)
                    .sized(0.45F, 0.2F)
                    .build("recoverable_fiber_optic_cable")
    );

    private AddonEntities() {
    }
}
