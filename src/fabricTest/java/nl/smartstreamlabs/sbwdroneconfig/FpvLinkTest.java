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

    @Test
    void segmentClosestApproach() {
        double[] c = FpvLink.closest(new Vec3(0, 0, -1), new Vec3(0, 0, 1), new Vec3(-1, 0.3, 0), new Vec3(1, 0.3, 0));
        assertEquals(0.5, c[0], 1e-9);
        assertEquals(0.5, c[1], 1e-9);
        assertEquals(0.3, c[2], 1e-9);
        // Parallel segments, and a degenerate one.
        assertEquals(1.0, FpvLink.closest(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(1, 1, 0))[2], 1e-9);
        assertEquals(2.0, FpvLink.closest(Vec3.ZERO, Vec3.ZERO, new Vec3(-1, 2, 0), new Vec3(1, 2, 0))[2], 1e-9);
    }

    @Test
    void aSwingAcrossTheFibreCutsItThereForGood() {
        FpvLink link = new FpvLink(true);
        for (int x = 0; x <= 40; x++) link.payOutForTest(new Vec3(x, 1, 0));
        Vec3 drone = new Vec3(40, 1, 0);
        assertNull(link.cut(new Vec3(20, 3, -1), new Vec3(20, 3, 1), drone, 0.4), "a swing 2 m above the fibre misses");
        Vec3 at = link.cut(new Vec3(21, 1.2, -1), new Vec3(21, 1.2, 1), drone, 0.4);
        assertNotNull(at);
        assertEquals(21, at.x, 1e-6);
        assertTrue(link.snapped);
        assertEquals(0, link.controlQuality());
        assertEquals(at, link.cable.get(link.cable.size() - 1), "the fibre now ends at the cut");
        assertTrue(link.cable.stream().allMatch(p -> p.x <= 21 + 1e-6));
        assertNull(link.cut(new Vec3(10, 1, -1), new Vec3(10, 1, 1), drone, 0.4), "a cut fibre cannot be cut again");
    }

    @Test
    void theFreeEndUpToTheDroneCanBeCutToo() {
        FpvLink link = new FpvLink(true);
        link.payOutForTest(Vec3.ZERO);
        link.payOutForTest(new Vec3(3, 0, 0));
        assertNotNull(link.cut(new Vec3(2, 0, -1), new Vec3(2, 0, 1), new Vec3(3, 0, 0), 0.4));
    }

    @Test
    void theCableStaysWithinItsElementBudgetOverTheWholeSpool() {
        FpvLink link = new FpvLink(true);
        for (int i = 0; i <= 3200 && !link.snapped; i++) {
            link.payOutForTest(new Vec3(i, 10 + Math.sin(i / 50.0) * 20, Math.cos(i / 80.0) * 30));
            assertTrue(link.cable.size() <= FpvLink.MAX_POINTS);
        }
        assertTrue(link.snapped, "the 3 km spool runs out");
        assertTrue(link.packCable().size() <= FpvLink.MAX_POINTS * 3);
    }
}
