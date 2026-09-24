package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import static nl.smartstreamlabs.sbwdroneconfig.MotorSound.*;
import static org.junit.jupiter.api.Assertions.*;

class MotorSoundTest {
    @Test
    void powerReproducesThePitchThroughSbwsCurve() {
        for (double thrust = 0; thrust <= 1; thrust += 0.05) {
            float pitch = pitch(thrust);
            assertEquals(pitch, sbwPitch(power(pitch)), 1e-5);
            // SBW's DroneEntity.engineRunning() needs power above 0.05.
            assertTrue(power(pitch) > 0.05f);
        }
        assertEquals(IDLE_PITCH, pitch(0), 1e-6);
        assertEquals(FULL_PITCH, pitch(1), 1e-6);
        assertTrue(pitch(QuadFlightModel.HOVER_THROTTLE) > 0.9 && pitch(QuadFlightModel.HOVER_THROTTLE) < 1);
    }

    @Test
    void louderWithThrust() {
        assertTrue(volume(power(pitch(1))) > 2 * volume(power(pitch(0))));
        assertTrue(volume(power(pitch(1))) * 3 <= 0.55f, "SBW's fade multiplies by 3; the drone must not clip");
    }
}
