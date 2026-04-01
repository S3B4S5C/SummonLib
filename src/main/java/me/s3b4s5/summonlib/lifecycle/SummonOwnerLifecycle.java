package me.s3b4s5.summonlib.lifecycle;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.runtime.SummonIndexing;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central lifecycle policy for owner-scoped summon cleanup.
 */
public final class SummonOwnerLifecycle {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, SummonComponent> summonTagType;
    private final boolean debug;
    private final boolean cleanupGhostsOnWorldJoin;

    public SummonOwnerLifecycle(
            ComponentType<EntityStore, SummonComponent> summonTagType,
            boolean debug,
            boolean cleanupGhostsOnWorldJoin
    ) {
        this.summonTagType = summonTagType;
        this.debug = debug;
        this.cleanupGhostsOnWorldJoin = cleanupGhostsOnWorldJoin;
    }

    public void register(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, this::onPlayerLeaveWorld);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        if (cleanupGhostsOnWorldJoin) {
            plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerAddedToWorld);
        }
    }

    public void onPlayerLeaveWorld(DrainPlayerFromWorldEvent event) {
        if (event == null) return;

        UUID ownerUuid = resolveOwnerUuid(event.getHolder() != null
                ? event.getHolder().getComponent(PlayerRef.getComponentType())
                : null);
        if (ownerUuid == null) return;

        World world = resolveEventWorld(
                event.getWorld(),
                event.getHolder() != null ? event.getHolder().getComponent(PlayerRef.getComponentType()) : null
        );
        scheduleCleanup(world, ownerUuid, CleanupReason.WORLD_EXIT, true);
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null) return;

        PlayerRef player = event.getPlayerRef();
        UUID ownerUuid = resolveOwnerUuid(player);
        if (ownerUuid == null) return;

        World world = resolveWorld(player);
        scheduleCleanup(world, ownerUuid, CleanupReason.DISCONNECT, true);
    }

    public void onPlayerAddedToWorld(AddPlayerToWorldEvent event) {
        if (!cleanupGhostsOnWorldJoin || event == null) return;

        PlayerRef player = event.getHolder() != null
                ? event.getHolder().getComponent(PlayerRef.getComponentType())
                : null;
        UUID ownerUuid = resolveOwnerUuid(player);
        if (ownerUuid == null) return;

        World keepWorld = resolveEventWorld(event.getWorld(), player);
        if (debug) {
            LOGGER.atInfo().log("[SummonLifecycle] cross-world cleanup owner=%s keepWorld=%s",
                    String.valueOf(ownerUuid),
                    keepWorld != null ? keepWorld.getName() : "null");
        }

        Universe.get().getWorlds().values().forEach(world -> {
            if (world == null) return;
            if (keepWorld != null && world == keepWorld) return;
            scheduleCleanup(world, ownerUuid, CleanupReason.CROSS_WORLD_GHOST, false);
        });
    }

    public void cleanupAfterTeleport(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Ref<EntityStore> ownerRef,
            UUID ownerUuid
    ) {
        if (ownerUuid == null) return;

        removeOwnerSummonsInStore(store, cb, ownerUuid);
        clearOwnerRuntimeState(ownerUuid);
        if (debug) {
            LOGGER.atInfo().log("[SummonLifecycle] teleport cleanup owner=%s", String.valueOf(ownerUuid));
        }
    }

    private void scheduleCleanup(@Nullable World world, UUID ownerUuid, CleanupReason reason, boolean clearRuntimeState) {
        if (ownerUuid == null) return;
        if (world == null) {
            if (clearRuntimeState) {
                clearOwnerRuntimeState(ownerUuid);
            }
            if (debug) {
                LOGGER.atInfo().log("[SummonLifecycle] skipped cleanup owner=%s reason=%s world=null",
                        String.valueOf(ownerUuid), reason.name());
            }
            return;
        }

        if (debug) {
            LOGGER.atInfo().log("[SummonLifecycle] schedule owner=%s world=%s reason=%s",
                    String.valueOf(ownerUuid), world.getName(), reason.name());
        }

        world.execute(() -> {
            int removed = SummonOwnerCleanup.killAllSummonsOfOwnerDirect(
                    world.getEntityStore(),
                    summonTagType,
                    ownerUuid,
                    debug
            );
            if (clearRuntimeState) {
                clearOwnerRuntimeState(ownerUuid);
            }
            if (debug) {
                LOGGER.atInfo().log("[SummonLifecycle] completed owner=%s world=%s reason=%s removed=%d",
                        String.valueOf(ownerUuid), world.getName(), reason.name(), removed);
            }
        });
    }

    private void removeOwnerSummonsInStore(Store<EntityStore> store, CommandBuffer<EntityStore> cb, UUID ownerUuid) {
        if (store == null || cb == null || ownerUuid == null) return;

        List<Ref<EntityStore>> refs = SummonRuntimeServices.spawns().collectOwnerSummons(store, summonTagType, ownerUuid);
        for (Ref<EntityStore> ref : refs) {
            cb.removeEntity(ref, com.hypixel.hytale.component.RemoveReason.REMOVE);
        }
        SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, new ArrayList<>());
    }

    private void clearOwnerRuntimeState(UUID ownerUuid) {
        SummonRuntimeServices.targets().clearOwner(ownerUuid);
        SummonRuntimeServices.owners().clearOwner(ownerUuid);
    }

    @Nullable
    private static UUID resolveOwnerUuid(@Nullable PlayerRef player) {
        return player != null ? player.getUuid() : null;
    }

    @Nullable
    private static World resolveWorld(@Nullable PlayerRef player) {
        if (player == null || player.getWorldUuid() == null) return null;
        return Universe.get().getWorld(player.getWorldUuid());
    }

    @Nullable
    private static World resolveEventWorld(@Nullable World fallback, @Nullable PlayerRef player) {
        World world = resolveWorld(player);
        return world != null ? world : fallback;
    }

    private enum CleanupReason {
        WORLD_EXIT,
        DISCONNECT,
        TELEPORT,
        CROSS_WORLD_GHOST
    }
}



