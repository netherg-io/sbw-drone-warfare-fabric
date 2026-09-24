package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import static nl.smartstreamlabs.sbwdroneconfig.RadioLink.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioLinkTest {
    private static double controlSnr(double operatorM, double blocks, double jammerM) {
        double jam = jammerM > 0 ? milliwatts(receivedDbm(Band.CONTROL, JAMMER_DBM, jammerM, 0)) : 0;
        return snrDb(Band.CONTROL, receivedDbm(Band.CONTROL, Band.CONTROL.txDbm, operatorM, blocks), jam);
    }

    private static double videoSnr(double operatorM, double blocks) {
        return snrDb(Band.VIDEO, receivedDbm(Band.VIDEO, Band.VIDEO.txDbm, operatorM, blocks), 0);
    }

    @Test
    void freeSpacePathLossMatchesTheTextbook() {
        assertEquals(100.2, fsplDb(1000, 2440), 0.1);
        assertEquals(6.02, fsplDb(200, 5800) - fsplDb(100, 5800), 0.01);
    }

    @Test
    void openSkyReachesPastAnyMapButVideoFadesFirst() {
        assertEquals(1, controlLinkQuality(controlSnr(2000, 0, 0)));
        assertEquals(1, videoQuality(videoSnr(150, 0)));
        assertTrue(videoQuality(videoSnr(2000, 0)) < 1, "video starts to break up within a few hundred metres");
    }

    @Test
    void blocksInTheWayCutVideoBeforeControl() {
        assertTrue(videoQuality(videoSnr(150, 1)) > 0.5);
        assertEquals(0, videoQuality(videoSnr(150, 3)), "three blocks of wall kill 5.8 GHz video at 150 m");
        assertEquals(1, controlLinkQuality(controlSnr(150, 3, 0)), "2.4 GHz control still gets through");
        assertEquals(0, controlLinkQuality(controlSnr(150, 8, 0)), "a hill does not");
    }

    @Test
    void jammerBurnThrough() {
        assertEquals(0, controlLinkQuality(controlSnr(150, 0, 30)), "jammer 30 m from a drone flown from 150 m");
        assertEquals(1, controlLinkQuality(controlSnr(10, 0, 30)), "an operator 10 m away burns through");
        assertEquals(1, controlLinkQuality(controlSnr(150, 0, 300)), "jammer twice as far from the drone as the operator");
    }

    @Test
    void qualityRampsBetweenThresholds() {
        assertEquals(0.5, controlLinkQuality((CONTROL_LOST_SNR + CONTROL_CLEAN_SNR) / 2), 1e-9);
        assertEquals(0, videoQuality(VIDEO_LOST_SNR));
        assertEquals(1, videoQuality(VIDEO_CLEAN_SNR));
    }
}
