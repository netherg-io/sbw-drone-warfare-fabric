package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.control.DroneControlAccess;
import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSerializers;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.misc.MonitorItem;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.List;

/**
 * FPV quad on top of the SBW drone lifecycle (monitor link, control session, HUD, camera).
 * Only the flight is replaced: the server runs {@link QuadFlightModel} from the session-checked
 * SBW inputs, and clients just display the synced attitude and position.
 *
 * Controls: W/S pitch, A/D roll, mouse X yaw, Space/Shift move a throttle that stays where it is
 * left, Ctrl toggles Angle/Acro. Without an active session, or with an empty {@link Battery}, the
 * drone levels and descends. The SBW attachment adds its {@link Payload} mass; a kamikaze warhead
 * arms only away from the operator and then fires on a nose strike. The link is a radio
 * ({@link RadioLink}: range, obstruction, jamming) or, for the fibre variant, a cable; losing it
 * puts the drone into the same failsafe.
 */
public final class FpvDrone extends DroneEntity {
    static final EntityDataAccessor<Quaternionf> ATTITUDE =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.QUATERNION);
    static final EntityDataAccessor<Float> THROTTLE =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Boolean> ACRO =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Float> VOLTS =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> CHARGE =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> PAYLOAD_KG =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Boolean> ARMED =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Float> LINK_QUALITY =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> VIDEO =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> FIBRE_M =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<List<Float>> CABLE =
            SynchedEntityData.defineId(FpvDrone.class, ModSerializers.FLOAT_LIST_SERIALIZER.get());

    private static final double STICK_SLEW = 0.25;
    private static final double THROTTLE_STEP = 0.025;
    // Failsafe descent at this share of the hover throttle; the flight controller estimates hover like INAV does.
    private static final double FAILSAFE_HOVER_SHARE = 0.9;
    /** How close to the fibre a swing must pass to cut it, m. */
    static final double FIBRE_CUT_REACH = 0.4;

    private final QuadFlightModel model = new QuadFlightModel();
    private final Battery battery = new Battery();
    final boolean fibre;
    private final FpvLink link;
    private boolean linkWasUp = true;
    private double yawStick;
    private boolean attitudeReady;
    private double pitchStick;
    private double rollStick;
    private boolean modeKeyWasDown;
    // Camera yaw/roll differ from the frame's once it banks; pitch goes to xRot, which only the camera reads.
    private float camYaw, camYawO, camRoll, camRollO;
    // Model pitch; SBW's bodyPitch/pitchO hold render values derived from it.
    private float pitch, pitchPrev;
    private String armedFor = "";
    private boolean detonated;

    // Client: watched drones show the synced attitude smoothed over bunched updates; the pilot's own view is not.
    private final AttitudeLerp shownAttitude = new AttitudeLerp();
    private @Nullable Quaternionf clientAttitude;

    /** Entity id of the FPV drone the local player views through a monitor; set by the client each tick. */
    static volatile int viewedId = -1;

    public FpvDrone(EntityType<? extends DroneEntity> type, Level level, boolean fibre) {
        super(type, level);
        this.fibre = fibre;
        this.link = new FpvLink(fibre);
    }

    @Override public Item droneItem() { return fibre ? DroneWarfare.FPV_FIBRE_ITEM : DroneWarfare.FPV_ITEM; }

    /** SBW's fixed range ends in a signal-loss explosion; here the link model decides, and a lost link is a failsafe. */
    @Override public double getMaxControlDistance() { return 1e6; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTITUDE, new Quaternionf());
        builder.define(THROTTLE, 0f);
        builder.define(ACRO, false);
        builder.define(VOLTS, (float) new Battery().volts);
        builder.define(CHARGE, 1f);
        builder.define(PAYLOAD_KG, 0f);
        builder.define(ARMED, false);
        builder.define(LINK_QUALITY, 1f);
        builder.define(VIDEO, 1f);
        builder.define(FIBRE_M, 0f);
        builder.define(CABLE, List.of());
    }

    public float linkQuality() { return entityData.get(LINK_QUALITY); }

    public float videoQuality() { return entityData.get(VIDEO); }

    public float fibrePaidOut() { return entityData.get(FIBRE_M); }

    public List<Float> cable() { return entityData.get(CABLE); }

    public boolean isAcro() { return entityData.get(ACRO); }

    public float throttle() { return entityData.get(THROTTLE); }

    public float volts() { return entityData.get(VOLTS); }

    public float charge() { return entityData.get(CHARGE); }

    public float payloadKg() { return entityData.get(PAYLOAD_KG); }

    public boolean isArmed() { return entityData.get(ARMED); }

    private boolean kamikaze() { return entityData.get(IS_KAMIKAZE) && getAmmo() > 0; }

    /** Operator and warhead the fuze armed for; another of either is safe until it arms itself. */
    private String armKey() { return entityData.get(CONTROLLER) + "|" + getItemId(getCurrentItem()); }

    private boolean live() { return isArmed() && armedFor.equals(armKey()); }

    @Override
    public void travel() {
        // VehicleEntity.baseTick adds its data-driven gravity after travel(); the model owns gravity.
        double sbwGravity = computed().getGravity();
        if (level().isClientSide()) {
            setDeltaMovement(getDeltaMovement().add(0, sbwGravity, 0));
            applyAngles(attitude());
            return;
        }
        if (!attitudeReady) {
            model.level(Math.toRadians(getYRot()));
            attitudeReady = true;
        }

        Player controller = getController();
        // The pilot's client follows the drone, not his body, or it loses the drone past his view distance.
        if (controller instanceof ServerPlayer pilot && DroneControlAccess.INSTANCE.canUse(pilot, this, true)) {
            RemoteView.pilot(pilot, this);
        }
        boolean session = !"none".equals(entityData.get(SESSION));
        int cablePoints = link.cable.size();
        if (link.update((ServerLevel) level(), position().add(0, getBbHeight() / 2, 0), controller, session, tickCount) && controller != null) {
            controller.displayClientMessage(Component.literal("FPV: FIBRE SNAPPED").withStyle(ChatFormatting.RED), true);
        }
        boolean frame = link.frame(random);
        boolean linkUp = link.up();
        if (session && linkUp != linkWasUp && controller != null) {
            controller.displayClientMessage(linkUp ? Component.literal("FPV: LINK OK").withStyle(ChatFormatting.GREEN)
                    : Component.literal("FPV: LINK LOST, FAILSAFE").withStyle(ChatFormatting.RED), true);
        }
        linkWasUp = linkUp;
        entityData.set(LINK_QUALITY, (float) link.controlQuality());
        entityData.set(VIDEO, (float) link.videoQuality());
        if (fibre) {
            entityData.set(FIBRE_M, (float) link.paidOut);
            if (link.cable.size() != cablePoints) entityData.set(CABLE, link.packCable());
        }

        boolean wasEmpty = battery.empty();
        boolean controlled = session && !wasEmpty && linkUp;
        // A lost control frame holds the last sticks, as a real receiver does until failsafe.
        boolean fresh = controlled && frame;
        updateArming(controller, controlled);
        boolean modeKey = fresh && sprintInputDown();
        if (modeKey && !modeKeyWasDown) {
            entityData.set(ACRO, !isAcro());
            if (controller != null) {
                controller.displayClientMessage(Component.literal(isAcro() ? "FPV: ACRO" : "FPV: ANGLE"), true);
            }
        }
        if (frame || !controlled) modeKeyWasDown = modeKey;

        if (fresh || !controlled) {
            pitchStick = approach(pitchStick, fresh ? axis(forwardInputDown(), backInputDown()) : 0, STICK_SLEW);
            rollStick = approach(rollStick, fresh ? axis(rightInputDown(), leftInputDown()) : 0, STICK_SLEW);
            // Same mouse scale as the SBW drone (0.5 deg/tick per unit), expressed as a stick.
            yawStick = fresh ? Mth.clamp(getMouseMoveSpeedX() * 10 / Math.toDegrees(QuadFlightModel.YAW_RATE), -1, 1) : 0;
        }

        double payloadKg = getCurrentItem().isEmpty() ? 0 : Payload.massKg(getItemId(getCurrentItem()), getAmmo());
        model.mass = QuadFlightModel.MASS + payloadKg + link.spoolKg();
        model.thrustScale = battery.thrustScale();
        double hover = model.hoverThrottle();
        double failsafe = Math.min(1, FAILSAFE_HOVER_SHARE * hover);

        double throttle = throttle();
        throttle = controlled
                ? throttle + (fresh ? axis(upInputDown(), downInputDown()) * THROTTLE_STEP : 0)
                : approach(Math.min(throttle, failsafe), onGround() ? 0 : failsafe, THROTTLE_STEP);
        throttle = Mth.clamp(throttle, 0, 1);
        entityData.set(THROTTLE, (float) throttle);

        boolean armed = controlled || !onGround();
        if (onGround() && throttle < hover / 2) model.level(model.heading());

        Vec3 motion = getDeltaMovement();
        Vector3d velocity = new Vector3d(motion.x, motion.y, motion.z).mul(20);
        if (onGround()) velocity.mul(0.5, 1, 0.5);
        model.step(velocity, throttle, pitchStick, rollStick, yawStick, controlled && isAcro(), armed);
        setDeltaMovement(velocity.x / 20, velocity.y / 20 + sbwGravity, velocity.z / 20);
        setPower(armed ? MotorSound.power(MotorSound.pitch(model.thrustFraction())) : 0);

        battery.step(model.electricalPower() + Battery.AVIONICS_W, QuadFlightModel.DT);
        if (!wasEmpty && battery.empty() && controller != null) {
            controller.displayClientMessage(Component.literal("FPV: BATTERY EMPTY, LANDING").withStyle(ChatFormatting.RED), true);
        }
        entityData.set(VOLTS, (float) battery.volts);
        entityData.set(CHARGE, (float) battery.charge());
        entityData.set(PAYLOAD_KG, (float) payloadKg);

        Quaternionf attitude = new Quaternionf(model.attitude);
        entityData.set(ATTITUDE, attitude);
        applyAngles(attitude);
        crashIntoEntities();
    }

    @Override
    public void baseTick() {
        if (level().isClientSide()) {
            clientAttitude = getId() == viewedId ? shownAttitude.snap(entityData.get(ATTITUDE)) : shownAttitude.tick();
        }
        camYawO = camYaw;
        camRollO = camRoll;
        pitchPrev = pitch;
        // Without a link the drop/detonate command never reaches the drone.
        if (!level().isClientSide() && !link.up()) setFire(false);
        if (!level().isClientSide() && getFire() && kamikaze() && !live()) {
            // SBW's manual detonation would destroy the drone with a dud warhead.
            setFire(false);
            Player controller = getController();
            if (controller != null) controller.displayClientMessage(Component.literal(String.format(
                    "FPV: SAFE, arms %.0f m from you", armDistance())).withStyle(ChatFormatting.YELLOW), true);
        }
        super.baseTick();
        // DroneEntity.baseTick decays roll after travel(); keep the model's attitude instead.
        applyAngles(attitude());
        // SBW's renderer draws getBodyPitch(t) = lerp(0.6 t, pitchO, bodyPitch), which jumps at every tick.
        setPitchO(pitchPrev);
        setBodyXRot(QuadFlightModel.sbwBodyPitch(pitchPrev, pitch));
    }

    /**
     * SBW's client keeps the input sequence on its drone entity object. When the drone leaves the
     * pilot's client and comes back (a teleport, or a jump past his loaded chunks under lag), the
     * new object starts again at 0 and the server would reject every input until the monitor is
     * toggled. A fresh session restarts the sequence on both ends; the held sticks stay.
     */
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!"none".equals(entityData.get(SESSION)) && player.getStringUUID().equals(entityData.get(CONTROLLER))) {
            beginControlSession();
        }
    }

    /** Loaded fibre drones, server side (kept by DroneWarfare from the entity load events): what a swing may cut. */
    static final java.util.Set<FpvDrone> LOADED_FIBRE = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * A player's arm swing (attacking, mining, placing: every swing a client reports) cuts the laid fibre
     * of a fibre drone within his reach, as a blade or a boot through the line would, operator and
     * teammates included; nothing solid may be in between. The nearest crossing along the swing is cut.
     */
    public static void swing(ServerPlayer player) {
        if (LOADED_FIBRE.isEmpty() || player.isSpectator() || !player.isAlive()) return;
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.entityInteractionRange()));
        FpvDrone hit = null;
        double[] best = null;
        for (FpvDrone drone : LOADED_FIBRE) {
            if (drone.level() != level || !drone.isAlive()) continue;
            double[] c = drone.link.crossing(eye, end, drone.position().add(0, drone.getBbHeight() / 2, 0), FIBRE_CUT_REACH);
            if (c != null && (best == null || c[0] < best[0])) {
                best = c;
                hit = drone;
            }
        }
        if (hit == null) return;
        Vec3 at = new Vec3(best[2], best[3], best[4]);
        // Measured to the fibre itself, so a pane or a door just in front of it still protects it.
        if (level.clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) return;
        hit.link.cut(best);
        hit.entityData.set(CABLE, hit.link.packCable());
        hit.entityData.set(LINK_QUALITY, 0f);
        level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
        Player operator = hit.getController();
        if (operator != null) operator.displayClientMessage(Component.literal("FPV: FIBRE CUT").withStyle(ChatFormatting.RED), true);
    }

    /** Deployment from a picked-up fibre drone's item: the fibre it had already paid out, and nothing else from the item. */
    void restoreSpool(double paidOut) {
        if (!fibre || !Double.isFinite(paidOut)) return;
        link.paidOut = Math.clamp(paidOut, 0, FpvLink.SPOOL_M);
        entityData.set(FIBRE_M, (float) link.paidOut);
    }

    /**
     * SBW's pickup (sneak + empty hand or crowbar) returns a new drone item. A fibre drone that has laid
     * fibre gives back its item with the fibre already paid out ({@link DataComponents#ENTITY_DATA}),
     * which the next deployment puts back: a used spool is not a fresh 3 km one.
     */
    @Override
    public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getMainHandItem();
        // The synced paid-out length, so the client predicts the same branch as the server.
        if (!fibre || !player.isShiftKeyDown() || fibrePaidOut() <= 0
                || !(held.isEmpty() || held.is(ModTags.Items.TOOLS_CROWBAR))) {
            return super.interact(player, hand);
        }
        if (level().isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
        CompoundTag spool = new CompoundTag();
        spool.putString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString());
        spool.putDouble(FpvLink.PAID_OUT_TAG, link.paidOut);
        ItemStack item = new ItemStack(droneItem());
        item.set(DataComponents.ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(spool));
        give(player, item);
        for (int i = 0; i < getAmmo(); i++) give(player, getCurrentItem().copy());
        for (ItemStack stack : player.getInventory().items) {
            if (!(stack.getItem() instanceof MonitorItem)) continue;
            CompoundTag tag = NBTTool.getTag(stack);
            if (tag.getString(MonitorItem.LINKED_DRONE).equals(getStringUUID())) {
                MonitorItem.Companion.disLink(tag, player);
                NBTTool.saveTag(stack, tag);
            }
        }
        discard();
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** The attitude to show: the model's on the server, the smoothed synced one on a client. */
    private Quaternionf attitude() {
        return level().isClientSide() && clientAttitude != null ? clientAttitude : entityData.get(ATTITUDE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // Also called while super() constructs the entity, before this class's fields exist.
        if (shownAttitude != null && ATTITUDE.equals(key) && level() != null && level().isClientSide()) {
            shownAttitude.receive(entityData.get(ATTITUDE));
        }
    }

    private void applyAngles(Quaternionf attitude) {
        Quaterniond q = new Quaterniond(attitude);
        Vector3d body = QuadFlightModel.euler(q, new Vector3d(pitchPrev, yRotO, getPrevRoll()));
        setYRot((float) body.y);
        pitch = (float) body.x;
        setBodyXRot(pitch);
        setZRot((float) body.z);
        Vector3d camera = QuadFlightModel.euler(q.mul(QuadFlightModel.CAMERA_UPTILT), new Vector3d(xRotO, camYawO, camRollO));
        setXRot((float) camera.x);
        camYaw = (float) camera.y;
        camRoll = (float) camera.z;
    }

    /** SBW's monitor camera and the drone renderer share this; the operator's own model is hidden in that view. */
    @Override
    public float getYaw(float tickDelta) {
        return level().isClientSide() && getId() == viewedId ? Mth.lerp(tickDelta, camYawO, camYaw) : super.getYaw(tickDelta);
    }

    /** Client: the local player is flying this drone through his monitor, so he listens from inside it. */
    public boolean viewedHere() {
        return level().isClientSide() && getId() == viewedId;
    }

    public float cameraRoll(float tickDelta) {
        return Mth.lerp(tickDelta, camRollO, camRoll);
    }

    /**
     * SBW DroneEntity.travel's hull contact check for plain drones and drop payloads. A kamikaze
     * warhead fires only through the nose fuze in {@link #move}, never by a hull bump.
     */
    private void crashIntoEntities() {
        if (kamikaze()) return;
        Player controller = getController();
        AABB box = AABB.ofSize(getEyePosition(), 0.7, 0.3, 0.7);
        for (var target : level().getEntitiesOfClass(Entity.class, box, e -> e != this
                && !(e instanceof ItemEntity || e instanceof Projectile || e instanceof AreaEffectCloud || e instanceof C4Entity
                || e.getType().is(ModTags.EntityTypes.DECOY)))) {
            hitEntityCrash(controller, target);
        }
    }

    private double armDistance() {
        var data = CustomData.DRONE_ATTACHMENT.get(getItemId(getCurrentItem()));
        return Payload.armDistance(data == null ? 0 : data.explosionRadius);
    }

    /** Arms once, latched, when the flying drone first gets far enough from its operator; disarms when the warhead is gone. */
    private void updateArming(@Nullable Player controller, boolean controlled) {
        if (!kamikaze() || !armedFor.equals(armKey())) {
            entityData.set(ARMED, false);
        }
        if (kamikaze() && !isArmed() && controlled && controller != null && controller.level() == level()
                && distanceTo(controller) >= armDistance()) {
            entityData.set(ARMED, true);
            armedFor = armKey();
            controller.displayClientMessage(Component.literal("FPV: ARMED").withStyle(ChatFormatting.RED), true);
        }
    }

    private Vec3 nosePosition() {
        Vector3d nose = model.attitude.transform(new Vector3d(Payload.NOSE));
        return new Vec3(getX() + nose.x, getY() + getBbHeight() / 2 + nose.y, getZ() + nose.z);
    }

    /**
     * Contact fuze. The nose zone is swept along this tick's intended motion, so neither a thin block
     * nor a player is skipped at speed; firing needs the nose to lead (see {@link Payload#noseStrikes}).
     * The sweep runs before the move: SBW's collision damage in super.move would otherwise break the
     * 1 HP frame first, and the warhead would go off through SBW's destroy() at the frame centre.
     */
    @Override
    public void move(MoverType type, Vec3 movement) {
        if (type == MoverType.SELF && !level().isClientSide() && live() && kamikaze() && fuze(movement)) return;
        super.move(type, movement);
    }

    /** Sweeps the nose zone along {@code movement}; detonates and returns true on a strike. */
    private boolean fuze(Vec3 movement) {
        if (detonated || !isAlive() || movement.lengthSqr() < 1e-8) return false;
        Vec3 nose = nosePosition();
        Vector3d axis = model.attitude.transform(new Vector3d(0, 0, 1));
        if (!Payload.noseStrikes(axis, new Vector3d(movement.x, movement.y, movement.z).mul(20))) return false;

        Vec3 dir = movement.normalize();
        Vec3 reach = movement.add(dir.scale(Payload.NOSE_RADIUS));
        // Blocks: the centre and four rim rays of the nose zone, nearest contact first. A ray that
        // starts inside a block hits at once: the nose is already in it.
        Vec3 u = dir.cross(Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize().scale(Payload.NOSE_RADIUS);
        Vec3 v = dir.cross(u);
        Vec3 blockHit = null;
        double reachLeft = Double.MAX_VALUE;
        for (Vec3 offset : new Vec3[]{Vec3.ZERO, u, u.reverse(), v, v.reverse()}) {
            Vec3 from = nose.add(offset);
            HitResult hit = level().clip(new ClipContext(from, from.add(reach), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(from) < reachLeft) {
                reachLeft = hit.getLocation().distanceToSqr(from);
                blockHit = hit.getLocation();
            }
        }
        Vec3 end = blockHit != null ? nose.add(dir.scale(Math.sqrt(reachLeft))) : nose.add(reach);
        // Entities: each box grown by the zone radius; a nose already inside one counts as a hit there.
        Entity target = null;
        Vec3 targetHit = null;
        for (Entity e : level().getEntities(this, new AABB(nose, end).inflate(1), this::fuzeTarget)) {
            AABB box = e.getBoundingBox().inflate(Payload.NOSE_RADIUS);
            Vec3 at = box.contains(nose) ? nose : box.clip(nose, end).orElse(null);
            if (at != null && (targetHit == null || at.distanceToSqr(nose) < targetHit.distanceToSqr(nose))) {
                target = e;
                targetHit = at;
            }
        }
        if (target != null) detonate(target, targetHit);
        else if (blockHit != null) detonate(null, blockHit);
        else return false;
        return true;
    }

    private boolean fuzeTarget(Entity e) {
        return e != this && !e.isSpectator() && e.isPickable()
                && !(e instanceof ItemEntity || e instanceof Projectile || e instanceof AreaEffectCloud || e instanceof C4Entity
                || e.getType().is(ModTags.EntityTypes.DECOY));
    }

    /**
     * SBW's kamikaze hit and explosion (attachment data), at the strike point, then the drone is destroyed.
     * The drone itself is the direct source and the operator the attacker, so servers can tell a drone
     * strike from a thrown or fired warhead of the same type.
     */
    private void detonate(@Nullable Entity target, Vec3 at) {
        var data = CustomData.DRONE_ATTACHMENT.get(getItemId(getCurrentItem()));
        Player controller = getController();
        if (data != null) {
            if (target != null) {
                DamageHandler.doDamage(target, ModDamageTypes.causeCustomExplosionDamage(level().registryAccess(), this, controller), data.hitDamage);
                target.invulnerableTime = 0;
            }
            createCustomExplosion().source(this).attacker(controller)
                    .damage(data.explosionDamage).radius(data.explosionRadius).position(at).explode();
        }
        // Spent: neither SBW's destroy() nor its signal-loss blast may explode again.
        detonated = true;
        clearPayload();
        hurt(ModDamageTypes.causeCustomExplosionDamage(level().registryAccess(), this, controller), 10000);
    }

    /**
     * The 1 HP frame breaks at 0. SBW's vehicles fire a second, "wreck" explosion (5 m, 1 damage) in the
     * tick they are destroyed with health at or below minus their maximum, which any 2-point hit, and
     * the fuze's own kill, would reach; an FPV drone goes off once, through its warhead, or not at all.
     */
    @Override
    public void setHealth(float health) {
        super.setHealth(Math.max(health, 0));
    }

    /** An unarmed warhead is a dud; this is the check SBW's destroy() uses to detonate it. */
    @Override
    public void destroy() {
        var data = CustomData.DRONE_ATTACHMENT.get(getItemId(getCurrentItem()));
        if (data != null && data.isKamikaze && !live()) clearPayload();
        super.destroy();
    }


    private void clearPayload() {
        setCurrentItem(ItemStack.EMPTY);
        setAmmo(0);
        entityData.set(DISPLAY_ENTITY, "");
        entityData.set(IS_KAMIKAZE, false);
        entityData.set(MAX_AMMO, 1);
        entityData.set(ARMED, false);
    }

    @Override
    public float getEngineSoundVolume() {
        return engineRunning() ? MotorSound.volume(getPower()) : 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FpvAcro", isAcro());
        tag.putBoolean("FpvArmed", isArmed());
        tag.putString("FpvArmedFor", armedFor);
        tag.putDouble("FpvBatteryUsedAh", battery.usedAh);
        tag.putFloat("FpvThrottle", throttle());
        tag.putFloat("FpvVolts", volts());
        // Telemetry for /data get; recomputed, never loaded.
        tag.putFloat("FpvLinkQuality", linkQuality());
        tag.putFloat("FpvVideo", videoQuality());
        tag.putDouble("FpvControlSnr", link.controlSnr);
        link.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ACRO, tag.getBoolean("FpvAcro"));
        entityData.set(ARMED, tag.getBoolean("FpvArmed"));
        armedFor = tag.getString("FpvArmedFor");
        entityData.set(THROTTLE, tag.getFloat("FpvThrottle"));
        battery.usedAh = tag.getDouble("FpvBatteryUsedAh");
        battery.volts = battery.openCircuitVolts();
        entityData.set(VOLTS, (float) battery.volts);
        entityData.set(CHARGE, (float) battery.charge());
        link.load(tag);
        entityData.set(FIBRE_M, (float) link.paidOut);
        entityData.set(CABLE, link.packCable());
        // Sync the loaded heading with the spawn packet, not one tick later.
        model.level(Math.toRadians(getYRot()));
        attitudeReady = true;
        entityData.set(ATTITUDE, new Quaternionf(model.attitude));
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }

    private static double approach(double value, double target, double step) {
        return value + Mth.clamp(target - value, -step, step);
    }
}
