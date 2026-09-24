package nl.smartstreamlabs.sbwdroneconfig;

import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Server-side 5" FPV quadcopter model, SI units, fixed 20 Hz step.
 *
 * Body frame matches the SBW drone renderer/camera: +Z forward, +Y up, and the attitude equals
 * Ry(-yaw) * Rx(pitch) * Rz(roll), so positive pitch is nose-down and positive roll banks right.
 * Four motors in X layout produce thrust along body +Y; torques come only from their thrust
 * differences, so throttle, attitude authority and saturation interact as on a real quad.
 */
public final class QuadFlightModel {
    public static final double DT = 0.05;
    public static final double G = 9.81;

    /** Airframe with its battery, kg; {@link #mass} adds the payload. */
    public static final double MASS = 0.8;
    static final double MOTOR_OFFSET = 0.11 / Math.sqrt(2);
    static final double I_PITCH_ROLL = 0.003;
    static final double I_YAW = 0.005;
    /** Rated thrust per motor, N, at {@link Battery#V_REF}. */
    public static final double MOTOR_MAX = MASS * G;
    static final double YAW_TORQUE_PER_NEWTON = 0.015;
    static final double DRAG = 0.025;
    // Rotor (induced) drag is linear in speed and is what stops a slow drift at hover.
    static final double ROTOR_DRAG = 0.1;
    public static final double HOVER_THROTTLE = MASS * G / (4 * MOTOR_MAX);

    // Motor power from momentum theory: P = T^1.5 / sqrt(2 rho A) / efficiency, 5.1" props.
    static final double AIR_DENSITY = 1.225;
    static final double PROP_AREA = Math.PI * 0.065 * 0.065;
    // Figure of merit x motor x ESC; puts a bare 0.8 kg hover near 150 W, as measured on real 5" quads.
    static final double POWER_EFFICIENCY = 0.4;

    public static final double MAX_TILT = Math.toRadians(45);
    static final double ANGLE_GAIN = 8;
    static final double ANGLE_MAX_RATE = 6;
    public static final double ACRO_RATE = Math.toRadians(400);
    public static final double YAW_RATE = Math.toRadians(200);
    static final double RATE_GAIN = 0.5;
    static final double YAW_GAIN = 0.2;

    /** FPV camera mount: 20 degrees up from the frame. Not rotationX: JOML 1.10.5 (Minecraft's) builds a wrong quaternion there. */
    public static final Quaterniond CAMERA_UPTILT = new Quaterniond().rotateX(Math.toRadians(-20));

    // Motor positions (x, z) and spin direction; diagonals share a direction.
    private static final double[] MX = {MOTOR_OFFSET, -MOTOR_OFFSET, -MOTOR_OFFSET, MOTOR_OFFSET};
    private static final double[] MZ = {MOTOR_OFFSET, MOTOR_OFFSET, -MOTOR_OFFSET, -MOTOR_OFFSET};
    private static final double[] SPIN = {1, -1, 1, -1};

    public final Quaterniond attitude = new Quaterniond();
    public final Vector3d rates = new Vector3d();
    public final double[] motors = new double[4];
    /** Take-off mass, kg: airframe plus payload. */
    public double mass = MASS;
    /** Available motor thrust relative to its rating; the battery lowers it as its voltage sags. */
    public double thrustScale = 1;

    /**
     * Advances one tick.
     *
     * @param velocity world velocity in m/s, updated in place
     * @param throttle collective 0..1
     * @param pitch    stick -1..1, positive tilts the nose down
     * @param roll     stick -1..1, positive banks right
     * @param yaw      stick -1..1, positive turns right
     * @param acro     rate mode; otherwise sticks command a self-levelling angle
     * @param armed    motors spinning
     */
    public void step(Vector3d velocity, double throttle, double pitch, double roll, double yaw, boolean acro, boolean armed) {
        Vector3d want = acro
                ? new Vector3d(pitch * ACRO_RATE, -yaw * YAW_RATE, roll * ACRO_RATE)
                : angleModeRates(pitch, roll, yaw);

        // The flight controller's gains are tuned for the bare frame; a payload only adds inertia, so a loaded quad turns slower.
        double tx = I_PITCH_ROLL * RATE_GAIN * (want.x - rates.x) / DT;
        double ty = I_YAW * YAW_GAIN * (want.y - rates.y) / DT;
        double tz = I_PITCH_ROLL * RATE_GAIN * (want.z - rates.z) / DT;
        double motorMax = MOTOR_MAX * thrustScale;
        mix(armed ? Math.clamp(throttle, 0, 1) * 4 * motorMax : 0, tx, ty, tz, motorMax, armed);

        double thrust = 0;
        tx = ty = tz = 0;
        for (int i = 0; i < 4; i++) {
            thrust += motors[i];
            tx -= MZ[i] * motors[i];
            tz += MX[i] * motors[i];
            ty += YAW_TORQUE_PER_NEWTON * SPIN[i] * motors[i];
        }
        double inertia = mass / MASS;
        rates.add(tx / (I_PITCH_ROLL * inertia) * DT, ty / (I_YAW * inertia) * DT, tz / (I_PITCH_ROLL * inertia) * DT);
        double w = rates.length();
        if (w > 1e-9) {
            attitude.mul(new Quaterniond().fromAxisAngleRad(rates.x / w, rates.y / w, rates.z / w, w * DT)).normalize();
        }

        Vector3d up = attitude.transform(new Vector3d(0, 1, 0));
        double drag = DRAG * velocity.length() + ROTOR_DRAG;
        velocity.add(
                (up.x * thrust - drag * velocity.x) / mass * DT,
                ((up.y * thrust - drag * velocity.y) / mass - G) * DT,
                (up.z * thrust - drag * velocity.z) / mass * DT);
    }

    private Vector3d angleModeRates(double pitch, double roll, double yaw) {
        Quaterniond target = new Quaterniond().rotateY(-heading())
                .rotateX(Math.clamp(pitch, -1, 1) * MAX_TILT)
                .rotateZ(Math.clamp(roll, -1, 1) * MAX_TILT);
        Quaterniond error = new Quaterniond(attitude).conjugate().mul(target);
        if (error.w < 0) error.set(-error.x, -error.y, -error.z, -error.w);
        double half = Math.acos(Math.min(1, error.w));
        double sin = Math.sin(half);
        Vector3d want = sin < 1e-9 ? new Vector3d() : new Vector3d(error.x, error.y, error.z).mul(2 * half / sin * ANGLE_GAIN);
        if (want.length() > ANGLE_MAX_RATE) want.normalize(ANGLE_MAX_RATE);
        // Yaw about world up, whatever the current tilt.
        return want.add(attitude.transformInverse(new Vector3d(0, 1, 0)).mul(-yaw * YAW_RATE));
    }

    /** Airmode mixer: shifts the collective to keep attitude authority, then clamps each motor. */
    private void mix(double collective, double tx, double ty, double tz, double motorMax, boolean armed) {
        double d2 = 4 * MOTOR_OFFSET * MOTOR_OFFSET;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            motors[i] = collective / 4 - MZ[i] * tx / d2 + MX[i] * tz / d2 + SPIN[i] * ty / (4 * YAW_TORQUE_PER_NEWTON);
            min = Math.min(min, motors[i]);
            max = Math.max(max, motors[i]);
        }
        double shift = min < 0 ? -min : 0;
        if (max + shift > motorMax) shift = motorMax - max;
        for (int i = 0; i < 4; i++) motors[i] = armed ? Math.clamp(motors[i] + shift, 0, motorMax) : 0;
    }

    /** Minecraft yaw in radians (0 = +Z, increases turning right). */
    public double heading() {
        Vector3d f = attitude.transform(new Vector3d(0, 0, 1));
        // Nose straight down: the top faces the heading; straight up: the belly does.
        if (f.x * f.x + f.z * f.z < 1e-6) f = attitude.transform(new Vector3d(0, 1, 0)).mul(-Math.signum(f.y));
        return Math.atan2(-f.x, f.z);
    }

    /** Level attitude with the given Minecraft yaw, rates cleared. */
    public void level(double yawRad) {
        attitude.identity().rotateY(-yawRad);
        rates.zero();
    }

    /** Euler angles for the SBW renderer/camera, degrees: x = pitch, y = Minecraft yaw, z = roll. */
    public static Vector3d euler(Quaterniond q) {
        Vector3d e = q.getEulerAnglesYXZ(new Vector3d());
        return new Vector3d(Math.toDegrees(e.x), -Math.toDegrees(e.y), Math.toDegrees(e.z));
    }

    /**
     * Same angles, but of the two equivalent sets (p, y, r) and (180 - p, y + 180, r + 180) the one
     * nearest to prev, each unwrapped towards it, so render interpolation stays continuous through
     * vertical flight instead of spinning yaw and roll by 180 degrees.
     */
    public static Vector3d euler(Quaterniond q, Vector3d prev) {
        Vector3d a = nearest(euler(q), prev);
        Vector3d b = nearest(new Vector3d(180 - a.x, a.y + 180, a.z + 180), prev);
        return a.distanceSquared(prev) <= b.distanceSquared(prev) ? a : b;
    }

    /** bodyPitch that makes SBW's lerp(0.6 t, prev, bodyPitch) equal lerp(t, prev, current) over the tick. */
    public static float sbwBodyPitch(float prev, float current) {
        return prev + (current - prev) / 0.6f;
    }

    private static Vector3d nearest(Vector3d v, Vector3d ref) {
        return new Vector3d(ref.x + Math.IEEEremainder(v.x - ref.x, 360),
                ref.y + Math.IEEEremainder(v.y - ref.y, 360),
                ref.z + Math.IEEEremainder(v.z - ref.z, 360));
    }

    /** Throttle that balances the current mass with the current thrust scale; above 1 the quad cannot lift off. */
    public double hoverThrottle() {
        return mass * G / (4 * MOTOR_MAX * thrustScale);
    }

    /** Electrical power the motors draw this tick, W. */
    public double electricalPower() {
        double sum = 0;
        for (double m : motors) sum += Math.pow(m, 1.5);
        return sum / Math.sqrt(2 * AIR_DENSITY * PROP_AREA) / POWER_EFFICIENCY;
    }

    public double thrustFraction() {
        return (motors[0] + motors[1] + motors[2] + motors[3]) / (4 * MOTOR_MAX);
    }
}
