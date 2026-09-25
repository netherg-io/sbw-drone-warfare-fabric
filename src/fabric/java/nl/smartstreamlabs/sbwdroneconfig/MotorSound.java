package nl.smartstreamlabs.sbwdroneconfig;

/**
 * Motor tone through SBW's drone engine loop, which derives its pitch from the synced vehicle
 * power. Motor rpm follows the square root of thrust, so the pitch does too; power is the value
 * that makes SBW's pitch curve produce it.
 */
final class MotorSound {
    static final float IDLE_PITCH = 0.7f;
    static final float FULL_PITCH = 1.25f;
    /** Speed of sound, m/s. */
    static final double SOUND_SPEED = 343;
    /**
     * Without Sound Physics the addon muffles the motors itself, per solid block between the drone and
     * the listener, up to {@link #MAX_OCCLUDING_BLOCKS}: nobody 100 m underground hears a drone overhead.
     */
    static final double DB_PER_BLOCK = 6;
    static final int MAX_OCCLUDING_BLOCKS = 16;

    private MotorSound() {}

    static float pitch(double thrustFraction) {
        return IDLE_PITCH + (FULL_PITCH - IDLE_PITCH) * (float) Math.sqrt(Math.clamp(thrustFraction, 0, 1));
    }

    /** SBW VehicleSoundInstance.EngineSound.getPitch for a drone. */
    static float sbwPitch(float power) {
        if (power < 0.5f) return 0.6f + power * 0.4f;
        if (power <= 1) return 0.8f + (power - 0.5f) * 0.4f;
        return Math.min(power, 1.5f);
    }

    /** Power that makes {@link #sbwPitch} return the given pitch (0.6..1.5). */
    static float power(float pitch) {
        if (pitch < 0.8f) return (pitch - 0.6f) / 0.4f;
        if (pitch <= 1) return 0.5f + (pitch - 0.8f) / 0.4f;
        return pitch;
    }

    /** Before SBW's fade multiplier of 3: about 0.15 at idle to 0.55 at full throttle. */
    static float volume(float power) {
        return 0.05f + 0.13f * Math.clamp((sbwPitch(power) - IDLE_PITCH) / (FULL_PITCH - IDLE_PITCH), 0, 1);
    }

    /**
     * Doppler factor for a source closing on the listener at {@code closing} m/s (negative: receding),
     * from the change of distance, so the listener's own motion counts too. Capped below the speed of sound.
     */
    static double doppler(double closing) {
        return SOUND_SPEED / (SOUND_SPEED - Math.clamp(closing, -300, 300));
    }

    /** Gain through {@code blocks} solid blocks when Sound Physics is not installed to do it. */
    static float occlusionGain(double blocks) {
        return (float) Math.pow(10, -DB_PER_BLOCK * Math.min(blocks, MAX_OCCLUDING_BLOCKS) / 20);
    }
}
