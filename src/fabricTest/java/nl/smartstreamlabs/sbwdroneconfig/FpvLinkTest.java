package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FpvLinkTest {
    private final RandomSource random = RandomSource.create(1);

    @Test
    void failsafeAfterTenMissedFramesAndRecovery() {
        FpvLink link = new FpvLink(false);
        link.controlSnr = -20;
        for (int i = 0; i < FpvLink.FAILSAFE_TICKS - 1; i++) assertFalse(link.frame(random));
        assertTrue(link.up());
        link.frame(random);
        assertFalse(link.up());
        link.controlSnr = 30;
        assertTrue(link.frame(random));
        assertTrue(link.up());
    }

    @Test
    void marginalLinkDropsSomeFrames() {
        FpvLink link = new FpvLink(false);
        link.controlSnr = (RadioLink.CONTROL_LOST_SNR + RadioLink.CONTROL_CLEAN_SNR) / 2;
        int arrived = 0;
        for (int i = 0; i < 1000; i++) if (link.frame(random)) arrived++;
        assertEquals(500, arrived, 60);
    }

    @Test
    void fibrePaysOutAlongThePathAndSnapsWhenTheSpoolIsEmpty() {
        FpvLink link = new FpvLink(true);
        double kgFull = link.spoolKg();
        // Fly back and forth: the path, not the straight-line distance, pays out.
        int ticks = 0;
        boolean snapped = false;
        while (!snapped && ticks < 100_000) {
            double x = (ticks % 200 < 100 ? ticks % 100 : 100 - ticks % 100);
            snapped = link.payOutForTest(new Vec3(x, 0, 0));
            ticks++;
            assertTrue(link.cable.size() <= FpvLink.MAX_POINTS);
        }
        assertTrue(snapped);
        assertEquals(FpvLink.SPOOL_M, ticks, 10);
        assertEquals(0, link.controlQuality());
        assertEquals(0, link.videoQuality());
        assertFalse(link.up());
        assertTrue(link.spoolKg() < kgFull);
        assertEquals(FpvLink.BOBBIN_KG, link.spoolKg(), 0.01);
    }

    @Test
    void fibreIsImmuneToRadioLoss() {
        FpvLink link = new FpvLink(true);
        link.controlSnr = link.videoSnr = -99;
        assertEquals(1, link.controlQuality());
        assertEquals(1, link.videoQuality());
    }
}
