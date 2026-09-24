package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.client.renderer.entity.DroneRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class DroneWarfareClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(DroneWarfare.FPV, DroneRenderer::new);
    }
}
