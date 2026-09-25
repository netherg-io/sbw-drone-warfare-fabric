package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.LevelChunk;
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
    // ponytail: a hard 2 km radio cap, far past any Blockfield map. It bounds the voxel walk and SBW's
    // per-tick chunk tickets around a drone; replace it with a horizon/terrain model if maps grow.
    static final double MAX_RANGE = 2000;

    /** Fibre spool, m, and its mass: thin G.657 fibre about 0.1 kg/km on a 0.1 kg bobbin. */
    static final double SPOOL_M = 3000;
    static final double FIBRE_KG_PER_M = 0.0001;
    static final double BOBBIN_KG = 0.1;
    /** A cable point is laid every this many metres of flight; at most MAX_POINTS are kept for rendering. */
    static final double POINT_SPACING = 4;
    static final int MAX_POINTS = 128;
    /** Paid-out fibre, m; also what a picked-up fibre drone's item carries to its next deployment. */
    static final String PAID_OUT_TAG = "FpvFibrePaidM";

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
    boolean update(ServerLevel level, Vec3 drone, @Nullable Player operator, boolean session, long tick) {
        if (fibre) return payOut(drone);
        // Without a session nobody flies it; the next session is sampled within SAMPLE_TICKS.
        if (!session || tick % SAMPLE_TICKS != 0) return false;
        if (operator == null || operator.level() != level || operator.getEyePosition().distanceTo(drone) > MAX_RANGE) {
            controlSnr = videoSnr = -99;
            return false;
        }
        Vec3 op = operator.getEyePosition();
        double distance = op.distanceTo(drone);
        var control = RadioLink.Band.CONTROL;
        var video = RadioLink.Band.VIDEO;
        // No walk when free space alone already kills both links.
        boolean hopeless = RadioLink.snrDb(control, RadioLink.receivedDbm(control, control.txDbm, distance, 0), 0) < RadioLink.CONTROL_LOST_SNR
                && RadioLink.snrDb(video, RadioLink.receivedDbm(video, video.txDbm, distance, 0), 0) < RadioLink.VIDEO_LOST_SNR;
        double blocks = hopeless ? 0 : obstruction(level, op, drone);
        double controlDbm = RadioLink.receivedDbm(control, control.txDbm, distance, blocks);
        double videoDbm = RadioLink.receivedDbm(video, video.txDbm, distance, blocks);
        double jamAtDrone = 0, jamAtOperator = 0;
        for (Player p : level.players()) {
            if (!DroneWarfare.isJamming(p)) continue;
            Vec3 j = p.getEyePosition();
            jamAtDrone += jam(level, j, drone, control, controlDbm, RadioLink.CONTROL_CLEAN_SNR);
            jamAtOperator += jam(level, j, op, video, videoDbm, RadioLink.VIDEO_CLEAN_SNR);
        }
        controlSnr = RadioLink.snrDb(control, controlDbm, jamAtDrone);
        videoSnr = RadioLink.snrDb(video, videoDbm, jamAtOperator);
        return false;
    }

    /**
     * A jammer's power at a receiver, mW. The obstruction walk is skipped when even unobstructed the
     * jammer leaves the link clean; its free-space power is then an upper bound that changes nothing,
     * so the effect has no range cliff.
     */
    private static double jam(ServerLevel level, Vec3 jammer, Vec3 receiver, RadioLink.Band band, double signalDbm, double cleanSnr) {
        double d = jammer.distanceTo(receiver);
        double free = RadioLink.milliwatts(RadioLink.receivedDbm(band, RadioLink.JAMMER_DBM, d, 0));
        if (RadioLink.snrDb(band, signalDbm, free) >= cleanSnr) return free;
        return RadioLink.milliwatts(RadioLink.receivedDbm(band, RadioLink.JAMMER_DBM, d, obstruction(level, jammer, receiver)));
    }

    /**
     * Where a blade swung along {@code from}-{@code to} crosses the laid fibre (the cable points, then
     * the free end up to the drone at {@code drone}) within {@code reach} of it: {position along the
     * swing 0..1, cable index, x, y, z} of the nearest crossing along the swing, or null.
     */
    double @Nullable [] crossing(Vec3 from, Vec3 to, Vec3 drone, double reach) {
        if (!fibre || snapped || cable.isEmpty()) return null;
        double[] best = null;
        for (int i = 0; i < cable.size(); i++) {
            Vec3 a = cable.get(i), b = i + 1 < cable.size() ? cable.get(i + 1) : drone;
            double[] c = closest(from, to, a, b);
            if (c[2] <= reach && (best == null || c[0] < best[0])) {
                Vec3 p = a.add(b.subtract(a).scale(c[1]));
                best = new double[]{c[0], i, p.x, p.y, p.z};
            }
        }
        return best;
    }

    /** Cuts the fibre at a {@link #crossing}: it now ends there, and the link is gone for good, as when the spool runs out. */
    Vec3 cut(double[] crossing) {
        Vec3 at = new Vec3(crossing[2], crossing[3], crossing[4]);
        cable.subList((int) crossing[1] + 1, cable.size()).clear();
        cable.add(at);
        snapped = true;
        framesMissed = FAILSAFE_TICKS;
        return at;
    }

    /** Test shorthand: finds the crossing and cuts there. */
    @Nullable Vec3 cut(Vec3 from, Vec3 to, Vec3 drone, double reach) {
        double[] c = crossing(from, to, drone, reach);
        return c == null ? null : cut(c);
    }

    /**
     * Closest approach of segments p0-p1 and q0-q1: {s, t, distance}, with s and t the positions
     * (0..1) along each segment.
     */
    static double[] closest(Vec3 p0, Vec3 p1, Vec3 q0, Vec3 q1) {
        Vec3 d1 = p1.subtract(p0), d2 = q1.subtract(q0), r = p0.subtract(q0);
        double a = d1.dot(d1), e = d2.dot(d2), f = d2.dot(r);
        double s, t;
        if (a < 1e-12 && e < 1e-12) {
            s = t = 0;
        } else if (a < 1e-12) {
            s = 0;
            t = Math.clamp(f / e, 0, 1);
        } else {
            double c = d1.dot(r);
            if (e < 1e-12) {
                t = 0;
                s = Math.clamp(-c / a, 0, 1);
            } else {
                double b = d1.dot(d2), denom = a * e - b * b;
                s = denom > 1e-12 ? Math.clamp((b * f - c * e) / denom, 0, 1) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Math.clamp(-c / a, 0, 1);
                } else if (t > 1) {
                    t = 1;
                    s = Math.clamp((b - c) / a, 0, 1);
                }
            }
        }
        Vec3 onP = p0.add(d1.scale(s)), onQ = q0.add(d2.scale(t));
        return new double[]{s, t, onP.distanceTo(onQ)};
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
     * blocks (glass, leaves, fences) 0.3. Chunks not fully loaded count as air; getChunkNow never
     * loads or waits for one.
     */
    static double obstruction(ServerLevel level, Vec3 from, Vec3 to) {
        double[] blocks = {0};
        BlockGetter.traverseBlocks(from, to, level, (l, pos) -> {
            LevelChunk chunk = l.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) return null;
            BlockState state = chunk.getBlockState(pos);
            if (state.canOcclude() || !state.getFluidState().isEmpty()) blocks[0] += 1;
            else if (!state.getCollisionShape(chunk, pos).isEmpty()) blocks[0] += 0.3;
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
        tag.putDouble(PAID_OUT_TAG, paidOut);
        tag.putBoolean("FpvFibreSnapped", snapped);
    }

    void load(CompoundTag tag) {
        if (!fibre) return;
        cable.clear();
        ListTag points = tag.getList("FpvFibre", Tag.TAG_FLOAT);
        for (int i = 0; i + 2 < points.size(); i += 3) {
            cable.add(new Vec3(points.getFloat(i), points.getFloat(i + 1), points.getFloat(i + 2)));
        }
        paidOut = tag.getDouble(PAID_OUT_TAG);
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
