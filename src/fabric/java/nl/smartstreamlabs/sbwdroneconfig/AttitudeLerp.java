package nl.smartstreamlabs.sbwdroneconfig;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * Client-side smoothing of a watched drone's synced attitude. The server sends one attitude per tick,
 * but they reach a client tick in bunches (none, then two) whenever server and client tick phases
 * drift; shown as-is, the model's tilt stalls and then jumps. Like vanilla's position lerp, each new
 * value is approached over {@link #STEPS} client ticks (slerp), which absorbs the bunching at the
 * cost of about one tick of delay. The pilot's own view is not smoothed.
 */
final class AttitudeLerp {
    static final int STEPS = 3;

    final Quaternionf current = new Quaternionf();
    private final Quaternionf target = new Quaternionf();
    private int steps;
    private boolean primed;

    /** A new synced value. The first one is taken as is. */
    void receive(Quaternionfc attitude) {
        if (!primed) {
            current.set(attitude);
            primed = true;
        }
        target.set(attitude);
        steps = STEPS;
    }

    /** Once per client tick; returns the attitude to show this tick. */
    Quaternionf tick() {
        if (steps > 0) {
            current.slerp(target, 1f / steps);
            steps--;
        }
        return current;
    }

    /** Shows {@code attitude} at once (pilot's view, or a teleport). */
    Quaternionf snap(Quaternionfc attitude) {
        current.set(attitude);
        target.set(attitude);
        steps = 0;
        primed = true;
        return current;
    }
}
