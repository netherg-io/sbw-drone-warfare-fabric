package nl.smartstreamlabs.sbwdroneconfig;

import org.joml.Vector3d;

import java.util.Map;

/** Payload mass and the kamikaze contact fuze: pure rules, applied by {@link FpvDrone}. */
public final class Payload {
    // Per-unit mass of SBW drone attachments, kg, after their real counterparts. Balance knobs, not
    // final values; a 5" frame cannot lift the heavy ones, which is the point.
    static final Map<String, Double> MASS_KG = Map.of(
            "superbwarfare:c4_bomb", 0.6,              // M112 block, 0.57 kg
            "superbwarfare:rpg_rocket_standard", 1.5,  // PG-7VL warhead section without the sustainer
            "superbwarfare:rpg_rocket_tbg", 2.0,       // TBG-7V warhead section
            "superbwarfare:mortar_shell", 3.1,         // 82 mm
            "superbwarfare:tm_62", 9.5,                // TM-62 anti-tank mine
            "superbwarfare:grenade_40mm", 0.25,        // VOG-25
            "superbwarfare:rgo_grenade", 0.53,
            "superbwarfare:blu_43_mine", 0.02,
            "superbwarfare:medical_kit", 0.5);
    static final double UNKNOWN_KG = 0.5;

    /** The warhead arms no closer to the operator than this, m, and never inside twice its blast radius. */
    public static final double MIN_ARM_DISTANCE = 15;
    /** Fuze zone: the nose tip, body frame (+Z forward) from the frame centre, m, and its radius. */
    static final Vector3d NOSE = new Vector3d(0, 0, 0.35);
    static final double NOSE_RADIUS = 0.15;
    /** A contact fuze needs the nose to strike: this closing speed along the nose axis, m/s. */
    static final double STRIKE_SPEED = 3;

    private Payload() {}

    public static double massKg(String itemId, int count) {
        return count * MASS_KG.getOrDefault(itemId, UNKNOWN_KG);
    }

    public static double armDistance(double blastRadius) {
        return Math.max(MIN_ARM_DISTANCE, 2 * blastRadius);
    }

    /** True when the nose leads the motion fast enough to fire the fuze on contact; a flat landing or side graze does not. */
    public static boolean noseStrikes(Vector3d noseAxis, Vector3d velocity) {
        return noseAxis.dot(velocity) >= STRIKE_SPEED;
    }
}
