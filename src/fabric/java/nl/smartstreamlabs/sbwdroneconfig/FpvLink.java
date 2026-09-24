package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side state of one drone's link: radio (see {@link RadioLink}) or fibre. The radio is
 * re-sampled every {@link #SAMPLE_TICKS} with a bounded voxel walk; control frames then arrive
 * with the sampled link quality, and after {@link #FAILSAFE_TICKS} without any the receiver
 * declares failsafe, as ELRS does. The fibre is an ideal link that pays out along the flight path
 * and snaps once the spool is empty.
 */
final class FpvLink {
    static final int SAMPLE_TICKS = 5;
    static final int FAILSAFE_TICKS = 10;
    /** Obstruction counting stops here; the link is long dead by then. */
    static final double MAX_BLOCKS = 12;
    /** Jammers farther than this are below the noise floor and are not traced. */
    static final double JAMMER_REACH = 512;

    /** Fibre spool, m, and its mass: thin G.657 fibre about 0.1 kg/km on a 0.1 kg bobbin. */
    static final double SPOOL_M = 3000;
    static final double FIBRE_KG_PER_M = 0.0001;
    static final double BOBBIN_KG = 0.1;
    /** A cable point is laid every this many metres of flight; at most MAX_POINTS are kept for rendering. */
    static final double POINT_SPACING = 4;
    static final int MAX_POINTS = 128;

    final boolean fibre;
    double controlSnr = 99;
    double videoSnr = 99;
    private int framesMissed;
    // Fibre: path laid so far (x, y, z per point), paid-out length and whether it snapped.
    final List<Vec3> cable = new ArrayList<>();
    double paidOut;
    boolean snapped;

    FpvLink(boolean fibre) {
        this.fibre = fibre;
    }

    /** True while the receiver still has the link (not in failsafe). */
    boolean up() {
        return framesMissed < FAILSAFE_TICKS;
    }

    double controlQuality() {
        return fibre ? (snapped ? 0 : 1) : RadioLink.controlLinkQuality(controlSnr);
    }

    double videoQuality() {
        return fibre ? (snapped ? 0 : 1) : RadioLink.videoQuality(videoSnr);
    }

    /** Advances one tick; returns whether this tick's control frame arrived. */
    boolean frame(RandomSource random) {
        double q = controlQuality();
        boolean arrived = q >= 1 || random.nextDouble() < q;
        framesMissed = arrived ? 0 : framesMissed + 1;
        return arrived;
    }

    double spoolKg() {
        return fibre ? BOBBIN_KG + Math.max(0, SPOOL_M - paidOut) * FIBRE_KG_PER_M : 0;
    }

    /** Re-samples the radio, or pays out fibre. Returns true when the fibre has just snapped. */
    boolean update(ServerLevel level, Vec3 drone, @Nullable Player operator, long tick) {
        if (fibre) return payOut(drone);
        if (tick % SAMPLE_TICKS != 0) return false;
        if (operator == null || operator.level() != level) {
            controlSnr = videoSnr = -99;
            return false;
        }
        Vec3 op = operator.getEyePosition();
        double distance = op.distanceTo(drone);
        double blocks = obstruction(level, op, drone);
        double jamAtDrone = 0, jamAtOperator = 0;
        for (Player p : level.players()) {
            if (!DroneWarfare.isJamming(p)) continue;
            Vec3 j = p.getEyePosition();
            if (j.distanceTo(drone) < JAMMER_REACH) {
                jamAtDrone += RadioLink.milliwatts(RadioLink.receivedDbm(RadioLink.Band.CONTROL, RadioLink.JAMMER_DBM,
                        j.distanceTo(drone), obstruction(level, j, drone)));
            }
            if (j.distanceTo(op) < JAMMER_REACH) {
                jamAtOperator += RadioLink.milliwatts(RadioLink.receivedDbm(RadioLink.Band.VIDEO, RadioLink.JAMMER_DBM,
                        j.distanceTo(op), obstruction(level, j, op)));
            }
        }
        controlSnr = RadioLink.snrDb(RadioLink.Band.CONTROL,
                RadioLink.receivedDbm(RadioLink.Band.CONTROL, RadioLink.Band.CONTROL.txDbm, distance, blocks), jamAtDrone);
        videoSnr = RadioLink.snrDb(RadioLink.Band.VIDEO,
                RadioLink.receivedDbm(RadioLink.Band.VIDEO, RadioLink.Band.VIDEO.txDbm, distance, blocks), jamAtOperator);
        return false;
    }

    boolean payOutForTest(Vec3 drone) {
        boolean snappedNow = payOut(drone);
        if (snapped) framesMissed = FAILSAFE_TICKS;
        return snappedNow;
    }

    private boolean payOut(Vec3 drone) {
        if (snapped) return false;
        if (cable.isEmpty()) cable.add(drone);
        Vec3 last = cable.get(cable.size() - 1);
        double stretch = last.distanceTo(drone);
        if (stretch >= POINT_SPACING) {
            paidOut += stretch;
            cable.add(drone);
            // Keep the rendering budget: drop every other point of the older half.
            if (cable.size() > MAX_POINTS) {
                for (int i = cable.size() / 2 - 1; i > 0; i -= 2) cable.remove(i);
            }
            stretch = 0;
        }
        if (paidOut + stretch > SPOOL_M) {
            snapped = true;
            framesMissed = FAILSAFE_TICKS;
            return true;
        }
        return false;
    }

    /**
     * Weighted count of blocks crossed by the segment: opaque blocks and fluids 1, other colliding
     * blocks (glass, leaves, fences) 0.3. Unloaded chunks count as air rather than being loaded.
     */
    static double obstruction(Level level, Vec3 from, Vec3 to) {
        double[] blocks = {0};
        BlockGetter.traverseBlocks(from, to, level, (l, pos) -> {
            if (!l.hasChunkAt(pos)) return null;
            BlockState state = l.getBlockState(pos);
            if (state.canOcclude() || !state.getFluidState().isEmpty()) blocks[0] += 1;
            else if (!state.getCollisionShape(l, pos).isEmpty()) blocks[0] += 0.3;
            return blocks[0] >= MAX_BLOCKS ? Boolean.TRUE : null;
        }, l -> Boolean.FALSE);
        return blocks[0];
    }

    void save(CompoundTag tag) {
        if (!fibre) return;
        ListTag points = new ListTag();
        for (Vec3 p : cable) {
            points.add(FloatTag.valueOf((float) p.x));
            points.add(FloatTag.valueOf((float) p.y));
            points.add(FloatTag.valueOf((float) p.z));
        }
        tag.put("FpvFibre", points);
        tag.putDouble("FpvFibrePaidM", paidOut);
        tag.putBoolean("FpvFibreSnapped", snapped);
    }

    void load(CompoundTag tag) {
        if (!fibre) return;
        cable.clear();
        ListTag points = tag.getList("FpvFibre", Tag.TAG_FLOAT);
        for (int i = 0; i + 2 < points.size(); i += 3) {
            cable.add(new Vec3(points.getFloat(i), points.getFloat(i + 1), points.getFloat(i + 2)));
        }
        paidOut = tag.getDouble("FpvFibrePaidM");
        snapped = tag.getBoolean("FpvFibreSnapped");
        if (snapped) framesMissed = FAILSAFE_TICKS;
    }

    List<Float> packCable() {
        List<Float> out = new ArrayList<>(cable.size() * 3);
        for (Vec3 p : cable) {
            out.add((float) p.x);
            out.add((float) p.y);
            out.add((float) p.z);
        }
        return out;
    }
}
