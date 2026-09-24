package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;

/**
 * FPV quad on top of the SBW drone lifecycle (monitor link, control session, HUD, camera).
 * Only the flight is replaced: the server runs {@link QuadFlightModel} from the session-checked
 * SBW inputs, and clients just display the synced attitude and position.
 *
 * Controls: W/S pitch, A/D roll, mouse X yaw, Space/Shift move a throttle that stays where it is
 * left, Ctrl toggles Angle/Acro. Without an active session the drone levels and descends.
 */
public final class FpvDrone extends DroneEntity {
    static final EntityDataAccessor<Quaternionf> ATTITUDE =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.QUATERNION);
    static final EntityDataAccessor<Float> THROTTLE =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Boolean> ACRO =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.BOOLEAN);

    private static final float CAMERA_UPTILT = 20;
    private static final double STICK_SLEW = 0.25;
    private static final double THROTTLE_STEP = 0.025;
    private static final double FAILSAFE_THROTTLE = 0.9 * QuadFlightModel.HOVER_THROTTLE;

    private final QuadFlightModel model = new QuadFlightModel();
    private boolean attitudeReady;
    private double pitchStick;
    private double rollStick;
    private boolean modeKeyWasDown;

    public FpvDrone(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
    }

    @Override public Item droneItem() { return DroneWarfare.FPV_ITEM; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTITUDE, new Quaternionf());
        builder.define(THROTTLE, 0f);
        builder.define(ACRO, false);
    }

    public boolean isAcro() { return entityData.get(ACRO); }

    public float throttle() { return entityData.get(THROTTLE); }

    @Override
    public void travel() {
        // VehicleEntity.baseTick adds its data-driven gravity after travel(); the model owns gravity.
        double sbwGravity = computed().getGravity();
        if (level().isClientSide()) {
            setDeltaMovement(getDeltaMovement().add(0, sbwGravity, 0));
            applyAngles(entityData.get(ATTITUDE));
            return;
        }
        if (!attitudeReady) {
            model.level(Math.toRadians(getYRot()));
            attitudeReady = true;
        }

        boolean controlled = !"none".equals(entityData.get(SESSION));
        boolean modeKey = controlled && sprintInputDown();
        if (modeKey && !modeKeyWasDown) {
            entityData.set(ACRO, !isAcro());
            Player controller = getController();
            if (controller != null) {
                controller.displayClientMessage(Component.literal(isAcro() ? "FPV: ACRO" : "FPV: ANGLE"), true);
            }
        }
        modeKeyWasDown = modeKey;

        pitchStick = approach(pitchStick, controlled ? axis(forwardInputDown(), backInputDown()) : 0, STICK_SLEW);
        rollStick = approach(rollStick, controlled ? axis(rightInputDown(), leftInputDown()) : 0, STICK_SLEW);
        // Same mouse scale as the SBW drone (0.5 deg/tick per unit), expressed as a stick.
        double yawStick = controlled ? Mth.clamp(getMouseMoveSpeedX() * 10 / Math.toDegrees(QuadFlightModel.YAW_RATE), -1, 1) : 0;

        double throttle = throttle();
        throttle = controlled
                ? throttle + axis(upInputDown(), downInputDown()) * THROTTLE_STEP
                : approach(throttle, onGround() ? 0 : FAILSAFE_THROTTLE, THROTTLE_STEP);
        throttle = Mth.clamp(throttle, 0, 1);
        entityData.set(THROTTLE, (float) throttle);

        boolean armed = controlled || !onGround();
        if (onGround() && throttle < QuadFlightModel.HOVER_THROTTLE / 2) model.level(model.heading());

        Vec3 motion = getDeltaMovement();
        Vector3d velocity = new Vector3d(motion.x, motion.y, motion.z).mul(20);
        if (onGround()) velocity.mul(0.5, 1, 0.5);
        model.step(velocity, throttle, pitchStick, rollStick, yawStick, controlled && isAcro(), armed);
        setDeltaMovement(velocity.x / 20, velocity.y / 20 + sbwGravity, velocity.z / 20);
        setPower(armed ? (float) (0.06 + 0.14 * model.thrustFraction()) : 0);

        Quaternionf attitude = new Quaternionf(model.attitude);
        entityData.set(ATTITUDE, attitude);
        applyAngles(attitude);
        crashIntoEntities();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        // DroneEntity.baseTick decays roll after travel(); keep the model's attitude instead.
        applyAngles(entityData.get(ATTITUDE));
    }

    private void applyAngles(Quaternionf attitude) {
        Vector3d euler = QuadFlightModel.euler(new Quaterniond(attitude));
        // Unwrap against the previous tick so render interpolation never spins through 360 degrees.
        setYRot(yRotO + Mth.wrapDegrees((float) euler.y - yRotO));
        setBodyXRot((float) euler.x);
        setXRot((float) euler.x - CAMERA_UPTILT);
        setZRot(getPrevRoll() + Mth.wrapDegrees((float) euler.z - getPrevRoll()));
    }

    /** SBW DroneEntity.travel's contact check, kept so payload/kamikaze behaviour is unchanged. */
    private void crashIntoEntities() {
        Player controller = getController();
        AABB box = AABB.ofSize(getEyePosition(), 0.7, 0.3, 0.7);
        for (var target : level().getEntitiesOfClass(Entity.class, box, e -> e != this
                && !(e instanceof ItemEntity || e instanceof Projectile || e instanceof AreaEffectCloud || e instanceof C4Entity
                || e.getType().is(ModTags.EntityTypes.DECOY)))) {
            hitEntityCrash(controller, target);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FpvAcro", isAcro());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ACRO, tag.getBoolean("FpvAcro"));
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }

    private static double approach(double value, double target, double step) {
        return value + Mth.clamp(target - value, -step, step);
    }
}
