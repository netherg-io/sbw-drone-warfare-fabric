package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class CubedFpvCameraEffectsClient {
    private static int tickCounter;
    private static double previousSpeed;
    private static float vibrationStrength;

    private CubedFpvCameraEffectsClient() {
    }

    public static void init(net.minecraftforge.eventbus.api.IEventBus modBus) {
        modBus.addListener(CubedFpvCameraEffectsClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CubedFpvCameraEffectsClient.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        CubedFpvDroneEntity drone = DroneClientViewContext.activeCubedFpvDrone(Minecraft.getInstance());
        if (drone == null) {
            previousSpeed = 0.0D;
            vibrationStrength = 0.0F;
            return;
        }

        double speed = drone.getDeltaMovement().length();
        double acceleration = Math.abs(speed - previousSpeed);
        previousSpeed = speed;
        float throttleShake = (float) Math.min(1.0D, speed / 0.45D);
        float accelerationShake = (float) Math.min(1.0D, acceleration * 8.0D);
        vibrationStrength = (vibrationStrength * 0.78F) + ((throttleShake * 0.18F + accelerationShake * 0.42F) * 0.22F);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (DroneClientViewContext.activeCubedFpvDrone(Minecraft.getInstance()) == null) {
            return;
        }

        event.setFOV(AddonConfig.fpvCameraFov());
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (DroneClientViewContext.activeCubedFpvDrone(Minecraft.getInstance()) == null) {
            return;
        }

        float vibration = AddonConfig.enableFpvCameraVibration() ? vibrationStrength : 0.0F;
        double time = tickCounter + event.getPartialTick();
        event.setPitch(event.getPitch() + (float) AddonConfig.fpvCameraTiltDegrees() + (float) (Math.sin(time * 1.65D) * vibration * 0.85F));
        event.setYaw(event.getYaw() + (float) (Math.sin(time * 2.20D) * vibration * 0.45F));
        event.setRoll(event.getRoll() + (float) (Math.sin(time * 1.15D) * vibration * 0.80F));
    }
}
