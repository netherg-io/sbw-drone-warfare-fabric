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
    void eulerMatchesRendererOrder() {
        Quaterniond q = new Quaterniond().rotateY(Math.toRadians(-30)).rotateX(Math.toRadians(20)).rotateZ(Math.toRadians(-10));
        Vector3d e = euler(q);
        assertEquals(20, e.x, 1e-6);
        assertEquals(30, e.y, 1e-6);
        assertEquals(-10, e.z, 1e-6);
        model.attitude.set(q);
        assertEquals(Math.toRadians(30), model.heading(), 1e-6);
    }
}
