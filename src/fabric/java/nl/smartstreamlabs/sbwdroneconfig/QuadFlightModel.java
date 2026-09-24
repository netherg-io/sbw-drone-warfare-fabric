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

    // ponytail: one fixed airframe; payload mass and battery sag belong to the next stage.
    public static final double MASS = 0.8;
    static final double MOTOR_OFFSET = 0.11 / Math.sqrt(2);
    static final double I_PITCH_ROLL = 0.003;
    static final double I_YAW = 0.005;
    public static final double MOTOR_MAX = MASS * G;
    static final double YAW_TORQUE_PER_NEWTON = 0.015;
    static final double DRAG = 0.025;
    public static final double HOVER_THROTTLE = MASS * G / (4 * MOTOR_MAX);

    public static final double MAX_TILT = Math.toRadians(45);
    static final double ANGLE_GAIN = 8;
    static final double ANGLE_MAX_RATE = 6;
    public static final double ACRO_RATE = Math.toRadians(400);
    public static final double YAW_RATE = Math.toRadians(200);
    static final double RATE_GAIN = 0.5;
    static final double YAW_GAIN = 0.2;

    // Motor positions (x, z) and spin direction; diagonals share a direction.
    private static final double[] MX = {MOTOR_OFFSET, -MOTOR_OFFSET, -MOTOR_OFFSET, MOTOR_OFFSET};
    private static final double[] MZ = {MOTOR_OFFSET, MOTOR_OFFSET, -MOTOR_OFFSET, -MOTOR_OFFSET};
    private static final double[] SPIN = {1, -1, 1, -1};

    public final Quaterniond attitude = new Quaterniond();
    public final Vector3d rates = new Vector3d();
    public final double[] motors = new double[4];

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

        double tx = I_PITCH_ROLL * RATE_GAIN * (want.x - rates.x) / DT;
        double ty = I_YAW * YAW_GAIN * (want.y - rates.y) / DT;
        double tz = I_PITCH_ROLL * RATE_GAIN * (want.z - rates.z) / DT;
        mix(armed ? Math.clamp(throttle, 0, 1) * 4 * MOTOR_MAX : 0, tx, ty, tz, armed);

        double thrust = 0;
        tx = ty = tz = 0;
        for (int i = 0; i < 4; i++) {
            thrust += motors[i];
            tx -= MZ[i] * motors[i];
            tz += MX[i] * motors[i];
            ty += YAW_TORQUE_PER_NEWTON * SPIN[i] * motors[i];
        }
        rates.add(tx / I_PITCH_ROLL * DT, ty / I_YAW * DT, tz / I_PITCH_ROLL * DT);
        double w = rates.length();
        if (w > 1e-9) {
            attitude.mul(new Quaterniond().fromAxisAngleRad(rates.x / w, rates.y / w, rates.z / w, w * DT)).normalize();
        }

        Vector3d up = attitude.transform(new Vector3d(0, 1, 0));
        double speed = velocity.length();
        velocity.add(
                (up.x * thrust - DRAG * speed * velocity.x) / MASS * DT,
                ((up.y * thrust - DRAG * speed * velocity.y) / MASS - G) * DT,
                (up.z * thrust - DRAG * speed * velocity.z) / MASS * DT);
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
    private void mix(double collective, double tx, double ty, double tz, boolean armed) {
        double d2 = 4 * MOTOR_OFFSET * MOTOR_OFFSET;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            motors[i] = collective / 4 - MZ[i] * tx / d2 + MX[i] * tz / d2 + SPIN[i] * ty / (4 * YAW_TORQUE_PER_NEWTON);
            min = Math.min(min, motors[i]);
            max = Math.max(max, motors[i]);
        }
        double shift = min < 0 ? -min : 0;
        if (max + shift > MOTOR_MAX) shift = MOTOR_MAX - max;
        for (int i = 0; i < 4; i++) motors[i] = armed ? Math.clamp(motors[i] + shift, 0, MOTOR_MAX) : 0;
    }

    /** Minecraft yaw in radians (0 = +Z, increases turning right). */
    public double heading() {
        Vector3d f = attitude.transform(new Vector3d(0, 0, 1));
        if (f.x * f.x + f.z * f.z < 1e-6) f = attitude.transform(new Vector3d(0, 1, 0)).negate();
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

    public double thrustFraction() {
        return (motors[0] + motors[1] + motors[2] + motors[3]) / (4 * MOTOR_MAX);
    }
}
