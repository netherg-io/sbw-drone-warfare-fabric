package nl.smartstreamlabs.sbwdroneconfig;

/**
 * Link budget for the wireless FPV, one block = 1 m. Two links, as on a real FPV:
 * a 2.4 GHz long-range control uplink (operator to drone, LoRa-class, decodes below the noise floor)
 * and a 5.8 GHz analog video downlink (drone to operator, needs a clean SNR).
 * Received power = TX power + antenna gains - free-space path loss - loss of the blocks crossed;
 * a jammer adds its received power to the receiver noise, so it fights the wanted signal
 * (burn-through: the closer the operator, the harder the drone is to jam).
 * Values are balance knobs grounded in typical hardware, not measurements.
 */
public final class RadioLink {
    public enum Band {
        // TX dBm, noise floor dBm (thermal over the channel bandwidth + 6 dB NF), loss per block crossed, dB
        CONTROL(2440, 20, -111, 8),
        VIDEO(5800, 26, -95, 12);

        final double mhz, txDbm, noiseDbm, blockLossDb;

        Band(double mhz, double txDbm, double noiseDbm, double blockLossDb) {
            this.mhz = mhz;
            this.txDbm = txDbm;
            this.noiseDbm = noiseDbm;
            this.blockLossDb = blockLossDb;
        }
    }

    static final double ANTENNA_GAIN_DBI = 2;
    /** Handheld jammer, per band. */
    public static final double JAMMER_DBM = 20;
    /** Control uplink: packets decode down to this SNR; below it the link is lost. */
    public static final double CONTROL_LOST_SNR = -5;
    /** Between the lost SNR and this one packets drop at a rising rate. */
    public static final double CONTROL_CLEAN_SNR = 0;
    /** Video: fully clean from this SNR, unwatchable below the lost one. */
    public static final double VIDEO_CLEAN_SNR = 20;
    public static final double VIDEO_LOST_SNR = 3;

    private RadioLink() {}

    public static double fsplDb(double meters, double mhz) {
        return 20 * Math.log10(Math.max(1, meters)) + 20 * Math.log10(mhz) - 27.55;
    }

    /** Power at the receiver, dBm, from a transmitter of txDbm through the given obstruction (in full blocks). */
    public static double receivedDbm(Band band, double txDbm, double meters, double blocks) {
        return txDbm + 2 * ANTENNA_GAIN_DBI - fsplDb(meters, band.mhz) - blocks * band.blockLossDb;
    }

    /** SNR of a wanted signal against the band's noise plus any interference, dB. */
    public static double snrDb(Band band, double signalDbm, double interferenceMw) {
        return signalDbm - 10 * Math.log10(Math.pow(10, band.noiseDbm / 10) + interferenceMw);
    }

    public static double milliwatts(double dbm) {
        return Math.pow(10, dbm / 10);
    }

    /** Share of control packets that arrive, 0..1. */
    public static double controlLinkQuality(double snr) {
        return Math.clamp((snr - CONTROL_LOST_SNR) / (CONTROL_CLEAN_SNR - CONTROL_LOST_SNR), 0, 1);
    }

    /** Video picture quality, 0 (noise only) .. 1 (clean). */
    public static double videoQuality(double snr) {
        return Math.clamp((snr - VIDEO_LOST_SNR) / (VIDEO_CLEAN_SNR - VIDEO_LOST_SNR), 0, 1);
    }
}
