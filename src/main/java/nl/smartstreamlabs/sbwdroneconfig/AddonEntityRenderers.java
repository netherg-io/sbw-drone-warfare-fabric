package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;

public final class AddonEntityRenderers {
    private AddonEntityRenderers() {
    }

    public static void init(IEventBus modBus) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(AddonEntityRenderers::registerEntityRenderers));
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AddonEntities.CUBED_FPV_DRONE.get(), CubedFpvDroneRenderer::new);
        event.registerEntityRenderer(AddonEntities.LUCAS_DRONE.get(), LucasDroneRenderer::new);
        event.registerEntityRenderer(AddonEntities.FIBER_OPTIC_CABLE_SEGMENT.get(), FiberOpticCableSegmentRenderer::new);
        event.registerEntityRenderer(AddonEntities.RECOVERABLE_FIBER_OPTIC_CABLE.get(), RecoverableFiberOpticCableRenderer::new);
    }
}
