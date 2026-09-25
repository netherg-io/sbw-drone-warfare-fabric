package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.control.DroneControlAccess;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the client of a pilot who flies an FPV drone through an active monitor centred on the drone,
 * not on his body. Vanilla sends a player only the chunks and entities within his view distance of his
 * body, so past it (~96 m on a 6-chunk server) the client lost the drone and SBW's monitor dropped
 * control while the radio link was still full.
 *
 * While the view is active the server treats the drone as the pilot's viewpoint: his chunk cache
 * centre and chunk tracking follow the drone ({@code ChunkMapMixin}), and entity tracking distances
 * are measured from it ({@code TrackedEntityMixin}). The body stays where it is and stays vulnerable;
 * other players track it as before. A ticket loads the pilot's view radius (capped) around the drone
 * and expires on its own {@link #TICKET_TICKS} after the last refresh, so nothing leaks when the
 * session ends, the drone dies or the server stops. A second, smaller ticket sits where the drone
 * will be {@link #LOOK_AHEAD_TICKS} ahead, so fast flight does not outrun chunk loading. Tickets are
 * only placed inside the world border: this is the whole world loading a piloted drone causes.
 * A drone that jumps into loaded chunks which are not ticking (a teleport, a lag correction) is
 * re-centred from here, since it no longer ticks itself; one that jumps into unloaded chunks leaves
 * the pilot's client, and SBW ends the monitor session as for any unloaded drone.
 */
public final class RemoteView {
    /** Chunk radius loaded around a piloted drone; the pilot's view distance caps it too. */
    static final int LOAD_RADIUS = 6;
    /** Concurrent remote views; further pilots keep the vanilla view (server budget). */
    static final int MAX_VIEWS = 16;
    static final int TICKET_TICKS = 20;
    /** Entity pairings around the viewpoint are re-evaluated this often and on every section change. */
    static final int REFRESH_TICKS = 10;
    /** After a view ends the body's surroundings are re-paired for this long, while its chunks come back. */
    static final int SETTLE_TICKS = 60;
    /** Fast flight: the chunk this far ahead along the velocity is loaded too (40 m/s = 5 chunks), capped. */
    static final int LOOK_AHEAD_TICKS = 40;
    static final double LOOK_AHEAD_MAX = 96;
    static final int LOOK_AHEAD_RADIUS = 2;

    static final TicketType<Integer> TICKET = TicketType.create("sbwdroneconfig_fpv_view", Integer::compare, TICKET_TICKS);

    private static final class View {
        final Entity drone;
        long lastPiloted;
        long section;
        long nextRefresh;
        View(Entity drone) { this.drone = drone; }
    }

    private static final Map<UUID, View> VIEWS = new HashMap<>();
    /** Pilots whose view just ended, with the tick until which their pairings are still refreshed. */
    private static final Map<UUID, Long> SETTLING = new HashMap<>();

    private RemoteView() {}

    /** Called from the drone's server tick while {@code player} flies it through an active monitor. */
    static void pilot(ServerPlayer player, Entity drone) {
        ServerLevel level = (ServerLevel) drone.level();
        long now = level.getServer().getTickCount();
        View view = VIEWS.get(player.getUUID());
        if (view == null || view.drone != drone) {
            if (view == null && VIEWS.size() >= MAX_VIEWS) return;
            view = new View(drone);
            view.section = Long.MIN_VALUE;
            VIEWS.put(player.getUUID(), view);
            SETTLING.remove(player.getUUID());
        }
        view.lastPiloted = now;
        int radius = loadRadius(level.getServer().getPlayerList().getViewDistance());
        ticket(level, drone.position(), radius, drone.getId());
        Vec3 step = drone.getDeltaMovement().scale(LOOK_AHEAD_TICKS);
        if (step.lengthSqr() > LOOK_AHEAD_MAX * LOOK_AHEAD_MAX) step = step.normalize().scale(LOOK_AHEAD_MAX);
        ticket(level, drone.position().add(step), LOOK_AHEAD_RADIUS, drone.getId());
    }

    private static void ticket(ServerLevel level, Vec3 at, int radius, int id) {
        if (level.getWorldBorder().isWithinBounds(at.x, at.z)) {
            level.getChunkSource().addRegionTicket(TICKET, new ChunkPos(BlockPos.containing(at)), radius, id);
        }
    }

    static int loadRadius(int serverViewDistance) {
        return Math.max(2, Math.min(LOAD_RADIUS, serverViewDistance));
    }

    /** The pilot's current viewpoint, or null for the usual view from the body. */
    public static @Nullable Entity viewpoint(ServerPlayer player) {
        View view = VIEWS.isEmpty() ? null : VIEWS.get(player.getUUID());
        if (view == null || !live(view, player)) return null;
        return view.drone;
    }

    public static ChunkPos viewCentre(ServerPlayer player) {
        Entity drone = viewpoint(player);
        return drone != null ? drone.chunkPosition() : player.chunkPosition();
    }

    private static boolean live(View view, ServerPlayer player) {
        return fresh(view.lastPiloted, player.server.getTickCount()) && view.drone.isAlive()
                && !player.isRemoved() && view.drone.level() == player.level();
    }

    /** A view is kept for one missed tick: the drone ticks after the chunk map in a server tick. */
    static boolean fresh(long lastPiloted, long now) {
        return now - lastPiloted <= 1;
    }

    /** End of server tick: drop stale views and re-pair entities around the viewpoints that moved. */
    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (Iterator<Map.Entry<UUID, View>> it = VIEWS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, View> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            View view = entry.getValue();
            if (player == null) {
                it.remove();
                continue;
            }
            // Still flown but not ticking (moved into a loaded, non-ticking chunk): bring its chunks back.
            if (!fresh(view.lastPiloted, now) && view.drone instanceof DroneEntity drone && !drone.isRemoved()
                    && DroneControlAccess.INSTANCE.canUse(player, drone, true)) {
                pilot(player, drone);
            }
            if (!live(view, player)) {
                it.remove();
                SETTLING.put(entry.getKey(), now + SETTLE_TICKS);
                repair(player);
                continue;
            }
            long section = SectionPos.asLong(view.drone.blockPosition());
            if (section != view.section || now >= view.nextRefresh) {
                view.section = section;
                view.nextRefresh = now + REFRESH_TICKS;
                repair(player);
            }
        }
        for (Iterator<Map.Entry<UUID, Long>> it = SETTLING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || now > entry.getValue()) it.remove();
            else if ((entry.getValue() - now) % REFRESH_TICKS == 0) repair(player);
        }
    }

    public static void clear() {
        VIEWS.clear();
        SETTLING.clear();
    }

    /** Vanilla re-pairs a player with every tracked entity on each of his movement packets; a pilot's body barely moves. */
    private static void repair(ServerPlayer player) {
        player.serverLevel().getChunkSource().move(player);
    }
}
