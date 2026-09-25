package nl.smartstreamlabs.sbwdroneconfig;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.smartstreamlabs.sbwdroneconfig.mixin.ChannelAccessor;
import nl.smartstreamlabs.sbwdroneconfig.mixin.SoundEngineAccessor;
import nl.smartstreamlabs.sbwdroneconfig.mixin.SoundManagerAccessor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Client side of the FPV motor loop (SBW's engine sound instance, see VehicleSoundInstanceMixin).
 * Occlusion is applied once: by Sound Physics Remastered when it is installed, which by default
 * evaluates a looping sound only when it starts, so the addon has it re-evaluate FPV motor loops
 * every few ticks as they move; otherwise by the addon's own per-block muffling.
 */
public final class FpvEngineSound {
    public static final int REEVALUATE_TICKS = 5;
    /** Faster closing than this, m/s, is a jump of the camera, not relative motion (a 5" quad tops out near 50 m/s). */
    public static final double MAX_CLOSING = 150;
    /** Called reflectively, so the addon neither needs nor breaks without Sound Physics. */
    private static final @Nullable Method SOUND_PHYSICS = lookUpSoundPhysics();

    private FpvEngineSound() {}

    public static boolean soundPhysics() {
        return SOUND_PHYSICS != null;
    }

    private static @Nullable Method lookUpSoundPhysics() {
        if (!FabricLoader.getInstance().isModLoaded("sound_physics_remastered")) return null;
        try {
            return Class.forName("com.sonicether.soundphysics.SoundPhysics").getMethod("processSound",
                    int.class, double.class, double.class, double.class, SoundSource.class, ResourceLocation.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            LogUtils.getLogger().warn("Sound Physics Remastered is installed but its processSound was not found; FPV motors use the addon's occlusion", e);
            return null;
        }
    }

    /** Has Sound Physics evaluate the sound where it is now, as it does for moving sounds when that option is on. */
    public static void reevaluate(SoundInstance sound) {
        Method process = SOUND_PHYSICS;
        if (process == null) return;
        var engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).sbwdroneconfig$engine();
        ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) engine).sbwdroneconfig$channels().get(sound);
        if (handle == null) return;
        double x = sound.getX(), y = sound.getY(), z = sound.getZ();
        SoundSource category = sound.getSource();
        ResourceLocation id = sound.getLocation();
        handle.execute(channel -> {
            try {
                process.invoke(null, ((ChannelAccessor) channel).sbwdroneconfig$source(), x, y, z, category, id);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LogUtils.getLogger().debug("Sound Physics re-evaluation failed", e);
            }
        });
    }

    /** The motor tone SBW's loop would play for this drone's power. */
    public static float tone(FpvDrone drone) {
        return MotorSound.sbwPitch(drone.getPower());
    }

    public static double doppler(double closing) {
        return MotorSound.doppler(closing);
    }

    /** The addon's own muffling for the solid blocks between the drone and the listener. */
    public static float occlusionGain(Level level, Vec3 from, Vec3 to) {
        return MotorSound.occlusionGain(solidBlocks(level, from, to));
    }

    /** Solid blocks between the drone and the listener, from loaded chunks, capped. */
    static int solidBlocks(Level level, Vec3 from, Vec3 to) {
        int[] n = {0};
        BlockGetter.traverseBlocks(from, to, level, (l, pos) -> {
            if (l.getBlockState(pos).canOcclude() && ++n[0] >= MotorSound.MAX_OCCLUDING_BLOCKS) return Boolean.TRUE;
            return null;
        }, l -> Boolean.FALSE);
        return n[0];
    }
}
