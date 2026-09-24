package nl.smartstreamlabs.sbwdroneconfig;

import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A watched drone pitching at a steady 120 deg/s (6 deg per server tick), drawn at 80 fps (4 frames per
 * client tick). The attitude updates reach the client tick with the jitter seen when tick phases drift:
 * each lands in its own tick or slips into the next one, so ticks get 0, 1 or 2 updates.
 * Before: the latest value is shown and lerped between ticks (what FpvDrone did). After: AttitudeLerp.
 */
class AttitudeLerpTest {
    static final double DEG_PER_TICK = 6;
    static final int FRAMES = 4, TICKS = 400;

    record Stats(double stddev, double max, int stalls) {}

    /** Frame-to-frame pitch increments, deg. */
    static List<Double> frames(boolean smoothed, long seed) {
        Random random = new Random(seed);
        List<List<Double>> arrivals = new ArrayList<>();
        for (int i = 0; i < TICKS + 3; i++) arrivals.add(new ArrayList<>());
        for (int n = 0; n < TICKS; n++) arrivals.get(n + 1 + (random.nextBoolean() ? 1 : 0)).add(n * DEG_PER_TICK);
        AttitudeLerp lerp = new AttitudeLerp();
        double shown = 0, previous = 0;
        List<Double> rendered = new ArrayList<>();
        for (int tick = 0; tick < TICKS; tick++) {
            for (double pitch : arrivals.get(tick)) {
                lerp.receive(new Quaternionf().rotateX((float) Math.toRadians(pitch)));
                if (!smoothed) shown = pitch;
            }
            previous = tick == 0 ? shown : previous;
            double now = smoothed ? previous + Math.IEEEremainder(pitchOf(lerp.tick()) - previous, 360) : shown;
            for (int f = 0; f < FRAMES; f++) rendered.add(previous + (now - previous) * f / FRAMES);
            previous = now;
        }
        List<Double> steps = new ArrayList<>();
        // Skip the start-up and the tail; measure the steady flight.
        for (int i = 20 * FRAMES; i < rendered.size() - 10 * FRAMES; i++) steps.add(rendered.get(i) - rendered.get(i - 1));
        return steps;
    }

    static double pitchOf(Quaternionf q) {
        return Math.toDegrees(2 * Math.atan2(q.x, q.w));
    }

    static Stats stats(List<Double> steps) {
        double mean = steps.stream().mapToDouble(d -> d).average().orElse(0);
        double var = steps.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0);
        return new Stats(Math.sqrt(var), steps.stream().mapToDouble(d -> d).max().orElse(0),
                (int) steps.stream().filter(d -> Math.abs(d) < 1e-6).count());
    }

    @Test
    void smoothingRemovesTheStallAndJumpOfBunchedUpdates() {
        Stats before = stats(frames(false, 7)), after = stats(frames(true, 7));
        System.out.printf("FPV-PITCH frame step (ideal %.2f deg): before sd=%.2f max=%.2f stalls=%d; after sd=%.2f max=%.2f stalls=%d%n",
                DEG_PER_TICK / FRAMES, before.stddev, before.max, before.stalls, after.stddev, after.max, after.stalls);
        assertTrue(after.stddev < before.stddev / 3, before + " -> " + after);
        assertEquals(0, after.stalls);
        assertTrue(after.max < before.max / 1.5, before + " -> " + after);
    }

    @Test
    void smoothingConvergesAndSnaps() {
        AttitudeLerp lerp = new AttitudeLerp();
        Quaternionf a = new Quaternionf().rotateX(0.3f);
        lerp.receive(a);
        assertEquals(0, lerp.tick().angle() - a.angle(), 1e-6, "first value is taken as is");
        Quaternionf b = new Quaternionf().rotateY(1.2f);
        lerp.receive(b);
        for (int i = 0; i < AttitudeLerp.STEPS; i++) lerp.tick();
        assertTrue(lerp.current.equals(b, 1e-5f), "reaches the target in STEPS ticks");
        Quaternionf c = new Quaternionf().rotateZ(-0.7f);
        assertTrue(lerp.snap(c).equals(c, 1e-6f));
        assertTrue(lerp.tick().equals(c, 1e-6f));
    }
}
