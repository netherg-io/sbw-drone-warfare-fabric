package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JammerBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL_TICKS = 1;
    public static final int MIN_RANGE = 4;
    public static final int MAX_RANGE = 100;

    private UUID ownerUuid;
    private int jammerRange = Math.min(MAX_RANGE, AddonConfig.jammerRange());
    private final Set<String> whitelistedGamertags = new LinkedHashSet<>();

    public JammerBlockEntity(BlockPos pos, BlockState blockState) {
        super(AddonBlockEntities.JAMMER.get(), pos, blockState);
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        setChanged();
    }

    public boolean canConfigure(ServerPlayer player) {
        return ownerUuid == null || ownerUuid.equals(player.getUUID()) || player.hasPermissions(2);
    }

    public int getJammerRange() {
        return jammerRange;
    }

    public void setJammerRange(int jammerRange) {
        int clampedRange = clampRange(jammerRange);
        if (this.jammerRange != clampedRange) {
            this.jammerRange = clampedRange;
            setChanged();
        }
    }

    public List<String> getWhitelistedGamertags() {
        return new ArrayList<>(whitelistedGamertags);
    }

    public boolean addWhitelistedGamertag(String gamertag) {
        String normalized = normalizeGamertag(gamertag);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean changed = whitelistedGamertags.add(normalized);
        if (changed) {
            setChanged();
        }
        return changed;
    }

    public boolean removeWhitelistedGamertag(String gamertag) {
        String normalized = normalizeGamertag(gamertag);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean changed = whitelistedGamertags.remove(normalized);
        if (changed) {
            setChanged();
        }
        return changed;
    }

    public boolean isWhitelistedGamertag(String gamertag) {
        String normalized = normalizeGamertag(gamertag);
        return !normalized.isEmpty() && whitelistedGamertags.contains(normalized);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JammerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !AddonConfig.enableDroneJammer()) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        if (Math.floorMod(gameTime + pos.asLong(), SCAN_INTERVAL_TICKS) != 0L) {
            return;
        }

        double range = blockEntity.jammerRange;
        AABB searchBox = new AABB(pos).inflate(range);
        Vec3 jammerCenter = Vec3.atCenterOf(pos);
        for (Entity drone : serverLevel.getEntities((Entity) null, searchBox, entity -> blockEntity.shouldAffectDrone(serverLevel, entity))) {
            double distance = drone.position().distanceTo(jammerCenter);
            if (distance <= range) {
                DroneJamStateManager.recordInfluence(serverLevel, drone, distance, range);
            }
        }
    }

    private boolean shouldAffectDrone(ServerLevel level, Entity entity) {
        if (entity == null || !entity.isAlive() || !SbwCompat.isDrone(entity)) {
            return false;
        }
        String whitelistedGamertag = findWhitelistedGamertag(level, entity);
        if (whitelistedGamertag != null) {
            if (AddonConfig.debugJammer()) {
                SbwDroneRangeConfig.LOGGER.info(
                        "Jammer at {} ignored whitelisted player {} for drone {}",
                        worldPosition,
                        whitelistedGamertag,
                        entity.getUUID()
                );
            }
            return false;
        }
        if (AddonConfig.affectFriendlyDrones() || ownerUuid == null) {
            return true;
        }

        SbwCompat.DroneOwnerIdentity ownerIdentity = SbwCompat.resolveDroneOwner(level, entity);
        return !ownerIdentity.hasUuid() || !ownerUuid.equals(ownerIdentity.uuid());
    }

    private String findWhitelistedGamertag(ServerLevel level, Entity drone) {
        if (whitelistedGamertags.isEmpty()) {
            return null;
        }

        UUID controllerUuid = SbwCompat.getDroneControllerUuid(drone);
        if (controllerUuid != null) {
            ServerPlayer controller = level.getServer().getPlayerList().getPlayer(controllerUuid);
            if (controller != null && isWhitelistedGamertag(controller.getGameProfile().getName())) {
                return controller.getGameProfile().getName();
            }
        }

        SbwCompat.DroneOwnerIdentity ownerIdentity = SbwCompat.resolveDroneOwner(level, drone);
        if (ownerIdentity.hasName() && isWhitelistedGamertag(ownerIdentity.name())) {
            return ownerIdentity.name();
        }

        if (ownerIdentity.hasUuid()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerIdentity.uuid());
            if (owner != null && isWhitelistedGamertag(owner.getGameProfile().getName())) {
                return owner.getGameProfile().getName();
            }
        }

        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putInt("JammerRange", jammerRange);
        tag.putString("WhitelistedGamertags", String.join("\n", whitelistedGamertags));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        jammerRange = clampRange(tag.getInt("JammerRange"));
        whitelistedGamertags.clear();
        String rawNames = tag.getString("WhitelistedGamertags");
        if (!rawNames.isBlank()) {
            for (String name : rawNames.split("\\R")) {
                String normalized = normalizeGamertag(name);
                if (!normalized.isEmpty()) {
                    whitelistedGamertags.add(normalized);
                }
            }
        }
    }

    private static int clampRange(int range) {
        if (range <= 0) {
            return Math.min(MAX_RANGE, Math.max(MIN_RANGE, AddonConfig.jammerRange()));
        }
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, range));
    }

    private static String normalizeGamertag(String gamertag) {
        return AddonConfig.normalizeName(gamertag, AddonConfig.blacklistMatchCaseInsensitive());
    }
}
