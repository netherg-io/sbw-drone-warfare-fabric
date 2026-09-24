package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatteryTest {
    private final Battery battery = new Battery();
    private final QuadFlightModel model = new QuadFlightModel();

    /** Seconds until empty at a fixed throttle, with sag feeding back into thrust and power. */
    private double flightSeconds(double payloadKg, double throttleShareOfHover) {
        model.mass = QuadFlightModel.MASS + payloadKg;
        var velocity = new org.joml.Vector3d();
        int ticks = 0;
        while (!battery.empty() && ticks < 20 * 3600) {
            model.thrustScale = battery.thrustScale();
            model.step(velocity, Math.min(1, throttleShareOfHover * model.hoverThrottle()), 0, 0, 0, false, true);
            velocity.zero();
            battery.step(model.electricalPower() + Battery.AVIONICS_W, QuadFlightModel.DT);
            ticks++;
        }
        return ticks * QuadFlightModel.DT;
    }

    @Test
    void openCircuitCurveIsMonotonicFromFullToReserve() {
        assertEquals(16.8, battery.openCircuitVolts(), 1e-9);
        double last = Double.MAX_VALUE;
        for (double used = 0; used <= Battery.CAPACITY_AH * 1.2; used += 0.01) {
            battery.usedAh = used;
            assertTrue(battery.openCircuitVolts() <= last + 1e-9, "at " + used);
            last = battery.openCircuitVolts();
        }
        battery.usedAh = Battery.CAPACITY_AH;
        assertEquals(13.2, battery.openCircuitVolts(), 1e-9);
        assertTrue(battery.empty());
        assertEquals(0, battery.charge());
    }

    @Test
    void loadSagsTheVoltageAndThrust() {
        battery.step(0, 0.05);
        double rest = battery.volts;
        double restScale = battery.thrustScale();
        battery.step(1400, 0.05);
        assertTrue(rest - battery.volts > 2 && rest - battery.volts < 3.5, "sag " + (rest - battery.volts));
        assertTrue(battery.thrustScale() < restScale);
    }

    @Test
    void flightTimeFollowsLoad() {
        double hover = flightSeconds(0, 1);
        assertTrue(hover > 6 * 60 && hover < 11 * 60, "bare hover " + hover);
        battery.usedAh = 0;
        // Brisk flying averages about twice the hover thrust: inside the 3-6 minute target.
        double brisk = flightSeconds(0, 2);
        assertTrue(brisk > 3 * 60 && brisk < 6 * 60, "brisk " + brisk);
        battery.usedAh = 0;
        double warhead = flightSeconds(Payload.massKg("superbwarfare:rpg_rocket_standard", 1), 1);
        assertTrue(warhead > 60 && warhead < 3 * 60, "rpg hover " + warhead);
    }

    @Test
    void tiredPackNeedsMoreThrottle() {
        model.thrustScale = battery.thrustScale();
        double fresh = model.hoverThrottle();
        battery.usedAh = 0.95 * Battery.CAPACITY_AH;
        battery.step(200, 0.05);
        model.thrustScale = battery.thrustScale();
        assertTrue(model.hoverThrottle() > 1.3 * fresh, fresh + " -> " + model.hoverThrottle());
    }

    @Test
    void overloadDeliversOnlyThePackMaximum() {
        double amps = battery.step(1e6, 0.05);
        assertEquals(16.8 / (2 * Battery.RESISTANCE), amps, 1e-6);
        assertEquals(8.4, battery.volts, 1e-6);
    }
}
