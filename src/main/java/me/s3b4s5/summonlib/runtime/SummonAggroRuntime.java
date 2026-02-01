package me.s3b4s5.summonlib.runtime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;
import me.s3b4s5.summonlib.tags.SummonTag;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonAggroRuntime {

    private SummonAggroRuntime() {
    }

    private static final float AGGRO_TTL_SEC = 6.0f;

    private static final class Entry {
        final Ref<EntityStore> target;
        final Instant time;

        Entry(Ref<EntityStore> target, Instant time) {
            this.target = target;
            this.time = time;
        }
    }

    private static final ConcurrentHashMap<UUID, Entry> byOwner = new ConcurrentHashMap<>();

    public static void push(UUID ownerUuid, Ref<EntityStore> target, Instant now) {
        if (ownerUuid == null || target == null) return;
        byOwner.put(ownerUuid, new Entry(target, now));
    }

    @Nullable
    public static Ref<EntityStore> peekValid(UUID ownerUuid, Instant now) {
        Entry e = byOwner.get(ownerUuid);
        if (e == null) return null;
        if (e.target == null || !e.target.isValid()) {
            byOwner.remove(ownerUuid);
            return null;
        }
        double ageSec = (now.toEpochMilli() - e.time.toEpochMilli()) / 1000.0;
        if (ageSec > AGGRO_TTL_SEC) {
            byOwner.remove(ownerUuid);
            return null;
        }
        return e.target;
    }

    public static void clear(UUID ownerUuid) {
        byOwner.remove(ownerUuid);
    }

    public static @Nullable Ref<EntityStore> pullAggroFocus(
            Store<EntityStore> store,
            UUID ownerUuid,
            ComponentType<EntityStore, SummonTag> summonTagType,
            ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner
    ) {

        SummonTargetSelector targetSelector = new SummonTargetSelector(summonTagType);
        Instant nowUsed = Instant.now();
        Ref<EntityStore> aggro = peekValid(ownerUuid, nowUsed);

        if (aggro == null) {
            return validateFocus(ownerUuid, store, focusTargetByOwner, summonTagType);
        }

        boolean valid = aggro.isValid();
        boolean alive = false;
            alive = valid && targetSelector.isAlive(aggro, store);


        if (valid && alive) {
            focusTargetByOwner.put(ownerUuid, aggro);
            return aggro;
        }

        return validateFocus(ownerUuid, store, focusTargetByOwner, summonTagType);
    }

    private static @Nullable Ref<EntityStore> validateFocus(
            UUID ownerUuid,
            Store<EntityStore> store,
            ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner,
            ComponentType<EntityStore, SummonTag> summonTagType
    ) {
        SummonTargetSelector targetSelector = new SummonTargetSelector(summonTagType);
        Ref<EntityStore> focus = focusTargetByOwner.get(ownerUuid);
        if (focus != null && (!focus.isValid() || !targetSelector.isAlive(focus, store))) {
            focusTargetByOwner.remove(ownerUuid);
            return null;
        }
        return focus;
    }
}
