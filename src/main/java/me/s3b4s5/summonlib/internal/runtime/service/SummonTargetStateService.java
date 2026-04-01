package me.s3b4s5.summonlib.internal.runtime.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns owner-scoped targeting state such as temporary aggro targets and
 * explicit focus UUIDs.
 */
public final class SummonTargetStateService {

    private static final float AGGRO_TTL_SEC = 6.0f;

    private final ConcurrentHashMap<UUID, AggroEntry> aggroByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> runtimeFocusByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> manualFocusByOwner = new ConcurrentHashMap<>();

    public void pushAggro(UUID ownerUuid, Ref<EntityStore> target, Instant now) {
        if (ownerUuid == null || target == null) return;
        aggroByOwner.put(ownerUuid, new AggroEntry(target, now));
    }

    @Nullable
    public Ref<EntityStore> peekValidAggro(UUID ownerUuid, Instant now) {
        AggroEntry entry = aggroByOwner.get(ownerUuid);
        if (entry == null) return null;
        if (entry.target == null || !entry.target.isValid()) {
            aggroByOwner.remove(ownerUuid);
            return null;
        }

        double ageSec = (now.toEpochMilli() - entry.time.toEpochMilli()) / 1000.0;
        if (ageSec > AGGRO_TTL_SEC) {
            aggroByOwner.remove(ownerUuid);
            return null;
        }
        return entry.target;
    }

    @Nullable
    public Ref<EntityStore> pullAggroOrFocus(
            Store<EntityStore> store,
            UUID ownerUuid,
            ComponentType<EntityStore, SummonComponent> summonTagType
    ) {
        SummonTargetSelector selector = new SummonTargetSelector(summonTagType);
        Ref<EntityStore> aggro = peekValidAggro(ownerUuid, Instant.now());
        if (aggro != null && aggro.isValid() && selector.isAlive(aggro, store)) {
            runtimeFocusByOwner.put(ownerUuid, aggro);
            return aggro;
        }

        Ref<EntityStore> focus = runtimeFocusByOwner.get(ownerUuid);
        if (focus != null && (!focus.isValid() || !selector.isAlive(focus, store))) {
            runtimeFocusByOwner.remove(ownerUuid);
            return null;
        }
        return focus;
    }

    public void rememberRuntimeTarget(UUID ownerUuid, @Nullable Ref<EntityStore> target) {
        if (ownerUuid == null) return;
        if (target == null) runtimeFocusByOwner.remove(ownerUuid);
        else runtimeFocusByOwner.put(ownerUuid, target);
    }

    public void clearRuntimeTarget(UUID ownerUuid) {
        if (ownerUuid == null) return;
        runtimeFocusByOwner.remove(ownerUuid);
    }

    public void setManualFocus(UUID ownerUuid, @Nullable UUID targetUuid) {
        if (ownerUuid == null) return;
        if (targetUuid == null) manualFocusByOwner.remove(ownerUuid);
        else manualFocusByOwner.put(ownerUuid, targetUuid);
    }

    @Nullable
    public UUID getManualFocus(UUID ownerUuid) {
        return manualFocusByOwner.get(ownerUuid);
    }

    public void clearOwner(UUID ownerUuid) {
        if (ownerUuid == null) return;
        aggroByOwner.remove(ownerUuid);
        runtimeFocusByOwner.remove(ownerUuid);
        manualFocusByOwner.remove(ownerUuid);
    }

    private record AggroEntry(Ref<EntityStore> target, Instant time) {}
}



