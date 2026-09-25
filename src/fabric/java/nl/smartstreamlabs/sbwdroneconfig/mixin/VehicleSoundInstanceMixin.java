package nl.smartstreamlabs.sbwdroneconfig.mixin;

import com.atsuishio.superbwarfare.client.sound.VehicleSoundInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.FpvDrone;
import nl.smartstreamlabs.sbwdroneconfig.FpvEngineSound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FPV motor loop, after SBW has set it up for the tick: the pitch is the motor tone times a real
 * Doppler factor (SBW adds a fixed-scale approximation), the pilot hears the tone of his own motors
 * (SBW pins it to 1 in the drone's camera), and obstruction is applied once (see {@link FpvEngineSound}).
 */
@Mixin(VehicleSoundInstance.class)
abstract class VehicleSoundInstanceMixin extends AbstractTickableSoundInstance {
    @Shadow(remap = false) @Final private VehicleEntity mobileVehicle;
    @Unique private double sbwdroneconfig$lastDistance = Double.NaN;
    @Unique private float sbwdroneconfig$occlusion = 1;
    @Unique private int sbwdroneconfig$age;

    private VehicleSoundInstanceMixin(SoundEvent sound, SoundSource source, RandomSource random) {
        super(sound, source, random);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sbwdroneconfig$fpvMotor(CallbackInfo ci) {
        if (!(mobileVehicle instanceof FpvDrone drone) || !((Object) this instanceof VehicleSoundInstance.EngineSound) || isStopped()) return;
        float tone = FpvEngineSound.tone(drone);
        if (drone.viewedHere()) {
            // The listener rides in the drone: its own motors, no Doppler, no obstruction.
            pitch = tone;
            sbwdroneconfig$lastDistance = Double.NaN;
            return;
        }
        Vec3 ear = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 at = new Vec3(x, y, z);
        double distance = at.distanceTo(ear);
        double closing = Double.isNaN(sbwdroneconfig$lastDistance) ? 0 : (sbwdroneconfig$lastDistance - distance) * 20;
        sbwdroneconfig$lastDistance = distance;
        pitch = (float) (tone * FpvEngineSound.doppler(closing));
        if (sbwdroneconfig$age++ % FpvEngineSound.REEVALUATE_TICKS == 0) {
            if (FpvEngineSound.soundPhysics()) FpvEngineSound.reevaluate(this);
            else sbwdroneconfig$occlusion = FpvEngineSound.occlusionGain(drone.level(), at, ear);
        }
        if (!FpvEngineSound.soundPhysics()) volume *= sbwdroneconfig$occlusion;
    }
}
