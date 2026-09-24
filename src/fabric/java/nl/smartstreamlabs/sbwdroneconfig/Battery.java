package nl.smartstreamlabs.sbwdroneconfig;

/**
 * 4S LiPo flight pack (1800 mAh, 1500 mAh of it usable before the cells reach 3.3 V at rest).
 * Coulomb counting over a LiPo open-circuit curve, internal-resistance sag under load, and a thrust
 * limit that follows the loaded voltage: motor rpm scales with voltage and thrust with rpm squared.
 * Past "empty" the pack still delivers its reserve, so a failsafe landing remains possible.
 */
public final class Battery {
    static final int CELLS = 4;
    public static final double CAPACITY_AH = 1.5;
    // Pack, connector and wiring; a 1500-1800 mAh 4S sags about 2.5 V at 100 A.
    static final double RESISTANCE = 0.025;
    /** Loaded voltage at which the motors reach their rated thrust. */
    static final double V_REF = 3.8 * CELLS;
    /** Loaded voltage below which the HUD warns. */
    public static final double LOW_VOLTS = 3.5 * CELLS;
    /** Flight controller, camera and video transmitter. */
    public static final double AVIONICS_W = 4;

    private static final double[] SOC = {0, 0.05, 0.1, 0.2, 0.4, 0.6, 0.8, 1};
    private static final double[] CELL_OCV = {3.3, 3.5, 3.6, 3.7, 3.8, 3.9, 4.03, 4.2};
    private static final double RESERVE_CELL_OCV_FLOOR = 3.0;

    public double usedAh;
    /** Loaded pack voltage after the last step. */
    public double volts = openCircuitVolts();

    /** Remaining usable charge, 0..1. */
    public double charge() {
        return Math.clamp(1 - usedAh / CAPACITY_AH, 0, 1);
    }

    public boolean empty() {
        return usedAh >= CAPACITY_AH;
    }

    public double openCircuitVolts() {
        double soc = 1 - usedAh / CAPACITY_AH;
        if (soc <= 0) {
            // Into the reserve: the curve falls steeply towards the cell floor over the next 10 %.
            return CELLS * Math.max(RESERVE_CELL_OCV_FLOOR, CELL_OCV[0] + soc * (CELL_OCV[0] - RESERVE_CELL_OCV_FLOOR) / 0.1);
        }
        int i = 1;
        while (i < SOC.length - 1 && SOC[i] < soc) i++;
        double t = Math.min(1, (soc - SOC[i - 1]) / (SOC[i] - SOC[i - 1]));
        return CELLS * (CELL_OCV[i - 1] + t * (CELL_OCV[i] - CELL_OCV[i - 1]));
    }

    /** Draws the given power for dt seconds; returns the current, A. */
    public double step(double watts, double dt) {
        double ocv = openCircuitVolts();
        double disc = ocv * ocv - 4 * RESISTANCE * watts;
        // Beyond the pack's maximum power (at half the open-circuit voltage) it delivers only that.
        double amps = disc > 0 ? (ocv - Math.sqrt(disc)) / (2 * RESISTANCE) : ocv / (2 * RESISTANCE);
        volts = ocv - amps * RESISTANCE;
        usedAh += amps * dt / 3600;
        return amps;
    }

    /** Motor thrust available now relative to its rating. */
    public double thrustScale() {
        return volts * volts / (V_REF * V_REF);
    }
}
