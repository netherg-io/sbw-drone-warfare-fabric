package nl.smartstreamlabs.sbwdroneconfig;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static nl.smartstreamlabs.sbwdroneconfig.QuadFlightModel.*;
import static org.junit.jupiter.api.Assertions.*;

class QuadFlightModelTest {
    private final QuadFlightModel model = new QuadFlightModel();
    private final Vector3d velocity = new Vector3d();

    private void run(int ticks, double throttle, double pitch, double roll, double yaw, boolean acro) {
        for (int i = 0; i < ticks; i++) model.step(velocity, throttle, pitch, roll, yaw, acro, true);
    }

    private double pitchDeg() { return euler(model.attitude).x; }

    @Test
    void hoverThrottleHoldsAltitude() {
        run(200, HOVER_THROTTLE, 0, 0, 0, false);
        assertEquals(0, velocity.length(), 1e-6);
        assertEquals(0.25, HOVER_THROTTLE, 1e-9);
    }

    @Test
    void rotorDragStopsSlowDrift() {
        velocity.set(1, 0, 0);
        run(400, HOVER_THROTTLE, 0, 0, 0, false);
        assertTrue(velocity.x > 0 && velocity.x < 0.1, "drift " + velocity.x);
    }

    @Test
    void disarmedFallsAtGravity() {
        model.step(velocity, 1, 0, 0, 0, false, false);
        assertEquals(-G * DT, velocity.y, 1e-9);
    }

    @Test
    void angleModeLimitsTiltAndSelfLevels() {
        double peak = 0;
        for (int i = 0; i < 20; i++) {
            run(1, HOVER_THROTTLE, 1, 0, 0, false);
            peak = Math.max(peak, pitchDeg());
        }
        assertEquals(45, pitchDeg(), 1.5);
        assertTrue(peak < 50, "overshoot " + peak);
        assertTrue(velocity.z > 0, "nose-down pitch must accelerate forward (+Z)");

        run(20, HOVER_THROTTLE, 0, 0, 0, false);
        assertEquals(0, pitchDeg(), 1.0);
    }

    @Test
    void acroHoldsAttitudeWhenSticksCentre() {
        run(4, HOVER_THROTTLE, 1, 0, 0, true);
        double tilted = pitchDeg();
        assertTrue(tilted > 20, "acro pitch rate too low: " + tilted);
        run(20, HOVER_THROTTLE, 0, 0, 0, true);
        assertEquals(0, model.rates.length(), 1e-3);
        assertTrue(pitchDeg() > tilted, "acro must not self-level");
    }

    @Test
    void acroFlipIsContinuous() {
        // A full forward loop must not blow up at the Euler singularity.
        run(30, 0.5, 1, 0, 0, true);
        assertEquals(1, model.attitude.lengthSquared(), 1e-9);
        assertTrue(Double.isFinite(velocity.length()));
    }

    @Test
    void anglesStayContinuousThroughALoop() {
        Vector3d prev = euler(model.attitude, new Vector3d());
        for (int i = 0; i < 30; i++) {
            run(1, 0.5, 1, 0, 0, true);
            Vector3d e = euler(model.attitude, prev);
            assertTrue(e.distance(prev) < 30, "jump at tick " + i + ": " + prev + " -> " + e);
            Quaterniond back = new Quaterniond().rotateY(Math.toRadians(-e.y)).rotateX(Math.toRadians(e.x)).rotateZ(Math.toRadians(e.z));
            assertEquals(1, Math.abs(back.dot(model.attitude)), 1e-9);
            prev = e;
        }
        assertTrue(prev.x > 180, "a full loop should pass 180 degrees of pitch: " + prev.x);
    }

    @Test
    void headingSurvivesVerticalNose() {
        model.attitude.identity().rotateY(Math.toRadians(-30)).rotateX(Math.toRadians(90));
        assertEquals(Math.toRadians(30), model.heading(), 1e-6);
        model.attitude.identity().rotateY(Math.toRadians(-30)).rotateX(Math.toRadians(-90));
        assertEquals(Math.toRadians(30), model.heading(), 1e-6);
    }

    @Test
    void rollRightAndYawRightFollowMinecraftAxes() {
        run(20, HOVER_THROTTLE, 0, 1, 0, false);
        assertTrue(velocity.x < 0, "facing +Z, right is -X");

        model.level(0);
        run(20, HOVER_THROTTLE, 0, 0, 1, false);
        assertTrue(model.heading() > 0, "yaw right increases Minecraft yaw");
    }

    @Test
    void motorsSaturateAndDragBoundsSpeed() {
        run(400, 1, 1, 0, 0, false);
        for (double m : model.motors) assertTrue(m >= 0 && m <= MOTOR_MAX + 1e-9);
        assertTrue(velocity.length() < 45, "speed " + velocity.length());
    }

    @Test
    void cameraUptiltTiltsTheViewUp() {
        Vector3d level = euler(new Quaterniond().mul(CAMERA_UPTILT));
        assertEquals(-20, level.x, 1e-6);
        assertEquals(0, level.y, 1e-6);
        assertEquals(0, level.z, 1e-6);
        // Banked right, the uptilted camera looks up and to the right of the nose.
        Vector3d banked = euler(new Quaterniond().rotateZ(Math.toRadians(45)).mul(CAMERA_UPTILT));
        assertEquals(14.4, banked.y, 0.1);
    }

    @Test
    void eulerMatchesRendererOrder() {
        Quaterniond q = new Quaterniond().rotateY(Math.toRadians(-30)).rotateX(Math.toRadians(20)).rotateZ(Math.toRadians(-10));
        Vector3d e = euler(q);
        assertEquals(20, e.x, 1e-6);
        assertEquals(30, e.y, 1e-6);
        assertEquals(-10, e.z, 1e-6);
        model.attitude.set(q);
        assertEquals(Math.toRadians(30), model.heading(), 1e-6);
    }

    @Test
    void payloadRaisesHoverThrottleAndSlowsTurns() {
        model.mass = MASS + Payload.massKg("superbwarfare:rpg_rocket_standard", 1);
        run(40, HOVER_THROTTLE, 0, 0, 0, false);
        assertTrue(velocity.y < -3, "bare-frame hover throttle must sink with a warhead: " + velocity.y);

        velocity.zero();
        assertEquals(HOVER_THROTTLE * 2.3 / 0.8, model.hoverThrottle(), 1e-9);
        run(200, model.hoverThrottle(), 0, 0, 0, false);
        assertEquals(0, velocity.length(), 1e-6);

        run(3, model.hoverThrottle(), 1, 0, 0, false);
        double loaded = pitchDeg();
        model.level(0);
        model.mass = MASS;
        run(3, HOVER_THROTTLE, 1, 0, 0, false);
        assertTrue(loaded < 0.6 * pitchDeg(), "loaded " + loaded + " vs bare " + pitchDeg());
    }

    @Test
    void overweightCannotLiftOff() {
        model.mass = MASS + Payload.massKg("superbwarfare:tm_62", 1);
        assertTrue(model.hoverThrottle() > 1);
        run(20, 1, 0, 0, 0, false);
        assertTrue(velocity.y < 0);
    }

    @Test
    void sagLowersAvailableThrust() {
        model.thrustScale = 0.7;
        assertEquals(HOVER_THROTTLE / 0.7, model.hoverThrottle(), 1e-9);
        run(1, 1, 0, 0, 0, false);
        for (double m : model.motors) assertEquals(0.7 * MOTOR_MAX, m, 1e-9);
    }

    @Test
    void hoverPowerMatchesA5InchQuad() {
        run(1, HOVER_THROTTLE, 0, 0, 0, false);
        assertEquals(150, model.electricalPower(), 20);
        run(1, 1, 0, 0, 0, false);
        assertTrue(model.electricalPower() > 1000 && model.electricalPower() < 1600, "full " + model.electricalPower());
    }

    @Test
    void sbwBodyPitchInterpolatesLinearly() {
        float prev = 10, current = 25, body = sbwBodyPitch(prev, current);
        for (float t = 0; t <= 1; t += 0.25f) {
            assertEquals(prev + t * (current - prev), prev + 0.6f * t * (body - prev), 1e-4);
        }
    }
}
