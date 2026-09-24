package nl.smartstreamlabs.sbwdroneconfig;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayloadTest {
    @Test
    void massCountsEveryUnitAndDefaultsUnknownItems() {
        assertEquals(1.5, Payload.massKg("superbwarfare:rpg_rocket_standard", 1), 1e-9);
        assertEquals(1.0, Payload.massKg("superbwarfare:grenade_40mm", 4), 1e-9);
        assertEquals(0.75, Payload.massKg("somemod:thing", 1) + Payload.massKg("superbwarfare:grenade_40mm", 1), 1e-9);
        assertEquals(0, Payload.massKg("superbwarfare:c4_bomb", 0), 1e-9);
    }

    @Test
    void armsOutsideTwiceTheBlastRadius() {
        assertEquals(Payload.MIN_ARM_DISTANCE, Payload.armDistance(5), 1e-9);
        assertEquals(26, Payload.armDistance(13), 1e-9);
    }

    @Test
    void onlyANoseStrikeFires() {
        Vector3d level = new Quaterniond().transform(new Vector3d(0, 0, 1));
        assertTrue(Payload.noseStrikes(level, new Vector3d(0, -1, 10)), "straight into a wall");
        assertFalse(Payload.noseStrikes(level, new Vector3d(0, -5, 0)), "flat landing");
        assertFalse(Payload.noseStrikes(level, new Vector3d(8, 0, 0)), "side graze");
        assertFalse(Payload.noseStrikes(level, new Vector3d(0, 0, 2)), "nudging along");
        Vector3d dive = new Quaterniond().rotateX(Math.toRadians(80)).transform(new Vector3d(0, 0, 1));
        assertTrue(dive.y < 0, "positive pitch is nose-down");
        assertTrue(Payload.noseStrikes(dive, new Vector3d(0, -6, 1)), "nose-down dive");
    }
}
