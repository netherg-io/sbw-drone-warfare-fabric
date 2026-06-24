package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class DroneCrashExplosionSystem {
    private static final String TAG_CRASH_EXPLOSION_HANDLED = "sbwdroneconfigCrashExplosionHandled";
    private static final float CRASH_EXPLOSION_POWER = 2.8F;
    private static final float FPV_CRASH_EXPLOSION_POWER = 5.8F;
    private static final float LUCAS_CRASH_EXPLOSION_POWER = 8.5F;
    private static final float CRASH_SOUND_VOLUME = 1.8F;
    private static final float FPV_CRASH_SOUND_VOLUME = 3.8F;
    private static final float LUCAS_CRASH_SOUND_VOLUME = 4.2F;
    private static final float CRASH_SOUND_PITCH = 0.85F;
    private static final float FPV_CRASH_SOUND_PITCH = 0.72F;
    private static final float LUCAS_CRASH_SOUND_PITCH = 0.65F;
    private static final float DISTANT_EXPLOSION_SOUND_VOLUME = 3.0F;
    private static final float FPV_DISTANT_EXPLOSION_SOUND_VOLUME = 4.5F;
    private static final float LUCAS_DISTANT_EXPLOSION_SOUND_VOLUME = 4.8F;
    private static final float DISTANT_EXPLOSION_SOUND_PITCH = 1.0F;
    private static final float EXTREME_DISTANT_EXPLOSION_SOUND_VOLUME = 8.0F;
    private static final float FPV_EXTREME_DISTANT_EXPLOSION_SOUND_VOLUME = 9.0F;
    private static final float LUCAS_EXTREME_DISTANT_EXPLOSION_SOUND_VOLUME = 10.0F;
    private static final float EXTREME_DISTANT_EXPLOSION_SOUND_PITCH = 0.92F;

    private DroneCrashExplosionSystem() {
    }

    /**
     * Restores the old addon crash explosion behavior for non-kamikaze drones.
     * This keeps the SBW destroy flow intact while layering heavier particles/sounds
     * and optional real explosion damage/block destruction on top.
     */
    public static void handleDroneDestroy(ServerLevel level, Entity drone) {
        if (level == null || drone == null || !SbwCompat.isDrone(drone)) {
            return;
        }
        if (drone.getPersistentData().getBoolean(TAG_CRASH_EXPLOSION_HANDLED)) {
            return;
        }
        drone.getPersistentData().putBoolean(TAG_CRASH_EXPLOSION_HANDLED, true);

        boolean lucasDrone = drone instanceof LucasDroneEntity;
        boolean fpvDrone = drone instanceof CubedFpvDroneEntity;
        float explosionPower = lucasDrone
                ? LUCAS_CRASH_EXPLOSION_POWER
                : fpvDrone ? FPV_CRASH_EXPLOSION_POWER : CRASH_EXPLOSION_POWER;

        disconnectController(level, drone);
        FiberOpticLinkSystem.handleDroneCrashRecovery(level, drone);
        spawnCrashEffects(level, drone.position(), fpvDrone, lucasDrone);

        if (AddonConfig.enableDroneCrashExplosionDamage()) {
            level.explode(null, drone.getX(), drone.getY(), drone.getZ(), explosionPower, Level.ExplosionInteraction.TNT);
        }
    }

    private static void disconnectController(ServerLevel level, Entity drone) {
        UUID controllerUuid = SbwCompat.getDroneControllerUuid(drone);
        if (controllerUuid != null) {
            ServerPlayer controller = level.getServer().getPlayerList().getPlayer(controllerUuid);
            if (controller != null) {
                PlayerDroneAnchorManager.release(controller, true);
            }
        }

        SbwCompat.disconnectControllersForDrone(level, drone);
        SbwCompat.resetDroneInput(drone);
    }

    private static void spawnCrashEffects(ServerLevel level, Vec3 pos, boolean fpvDrone, boolean lucasDrone) {
        ParticleTool.spawnHugeExplosionParticles(level, pos);
        if (fpvDrone || lucasDrone) {
            ParticleTool.spawnHugeExplosionParticles(level, pos);
        }
        if (lucasDrone) {
            ParticleTool.spawnHugeExplosionParticles(level, pos);
        }

        float soundVolume = lucasDrone ? LUCAS_CRASH_SOUND_VOLUME : fpvDrone ? FPV_CRASH_SOUND_VOLUME : CRASH_SOUND_VOLUME;
        float soundPitch = lucasDrone ? LUCAS_CRASH_SOUND_PITCH : fpvDrone ? FPV_CRASH_SOUND_PITCH : CRASH_SOUND_PITCH;
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, soundVolume, soundPitch);
        playDistantExplosionSound(level, pos, fpvDrone, lucasDrone);

        int explosionEmitterCount = lucasDrone ? 14 : fpvDrone ? 10 : 5;
        int flashCount = lucasDrone ? 96 : fpvDrone ? 76 : 40;
        int lavaCount = lucasDrone ? 54 : fpvDrone ? 36 : 18;
        int flameCount = lucasDrone ? 90 : 0;
        int smokeCount = lucasDrone ? 65 : 0;
        double horizontalSpread = lucasDrone ? 1.45D : fpvDrone ? 0.9D : 0.35D;
        double verticalSpread = lucasDrone ? 0.65D : fpvDrone ? 0.45D : 0.25D;

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 0.35D, pos.z, explosionEmitterCount, horizontalSpread, verticalSpread, horizontalSpread, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 0.35D, pos.z, flashCount, horizontalSpread * 1.4D, verticalSpread, horizontalSpread * 1.4D, 1.0D);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.2D, pos.z, lavaCount, horizontalSpread, 0.3D, horizontalSpread, 0.02D);

        if (lucasDrone) {
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.25D, pos.z, flameCount, 1.85D, 0.85D, 1.85D, 0.08D);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 0.45D, pos.z, smokeCount, 1.35D, 0.65D, 1.35D, 0.025D);
        }
    }

    private static void playDistantExplosionSound(ServerLevel level, Vec3 pos, boolean fpvDrone, boolean lucasDrone) {
        float volume = lucasDrone ? LUCAS_DISTANT_EXPLOSION_SOUND_VOLUME : fpvDrone ? FPV_DISTANT_EXPLOSION_SOUND_VOLUME : DISTANT_EXPLOSION_SOUND_VOLUME;
        level.playSound(null, pos.x, pos.y, pos.z, AddonSounds.DRONE_DISTANT_EXPLOSION.get(), SoundSource.BLOCKS, volume, DISTANT_EXPLOSION_SOUND_PITCH);
        playExtremeDistantExplosionSound(level, pos, fpvDrone, lucasDrone);
    }

    private static void playExtremeDistantExplosionSound(ServerLevel level, Vec3 pos, boolean fpvDrone, boolean lucasDrone) {
        float volume = lucasDrone ? LUCAS_EXTREME_DISTANT_EXPLOSION_SOUND_VOLUME : fpvDrone ? FPV_EXTREME_DISTANT_EXPLOSION_SOUND_VOLUME : EXTREME_DISTANT_EXPLOSION_SOUND_VOLUME;
        level.playSound(null, pos.x, pos.y, pos.z, AddonSounds.DRONE_5000_BLOCK_EXPLOSION.get(), SoundSource.BLOCKS, volume, EXTREME_DISTANT_EXPLOSION_SOUND_PITCH);
    }
}
