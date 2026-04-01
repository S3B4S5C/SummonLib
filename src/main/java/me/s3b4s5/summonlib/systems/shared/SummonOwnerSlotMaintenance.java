package me.s3b4s5.summonlib.systems.shared;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.runtime.SummonIndexing;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public final class SummonOwnerSlotMaintenance {

    private SummonOwnerSlotMaintenance() {
    }

    public static void enforceGlobalSlotsAndRebuild(
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> cb,
            @Nonnull Ref<EntityStore> ownerRef,
            @Nonnull UUID ownerUuid,
            @Nonnull ComponentType<EntityStore, SummonComponent> summonComponentType,
            @Nonnull SummonTargetSelector targetSelector
    ) {
        final int capSlots = SummonRuntimeServices.stats().getMaxSlots(store, ownerRef);
        final ArrayList<Ref<EntityStore>> refs = collectOwnerSummons(store, ownerUuid, summonComponentType);

        int usedSlots = 0;
        for (Ref<EntityStore> ref : refs) {
            final SummonComponent summon = store.getComponent(ref, summonComponentType);
            if (summon != null) {
                usedSlots += Math.max(0, summon.slotCost);
            }
        }

        refs.sort(Comparator.comparingLong((Ref<EntityStore> ref) -> {
            final SummonComponent summon = store.getComponent(ref, summonComponentType);
            return summon != null ? summon.spawnSeq : Long.MIN_VALUE;
        }).reversed());

        while (usedSlots > capSlots && !refs.isEmpty()) {
            final Ref<EntityStore> ref = refs.removeFirst();
            final SummonComponent summon = store.getComponent(ref, summonComponentType);
            if (summon != null) {
                usedSlots -= Math.max(0, summon.slotCost);
            }
            cb.removeEntity(ref, RemoveReason.REMOVE);
        }

        SummonIndexing.rebuildOwnerIndices(store, cb, summonComponentType, ownerUuid, refs);
        clearInvalidOwnerFocus(store, ownerUuid, summonComponentType, targetSelector);
    }

    @Nonnull
    private static ArrayList<Ref<EntityStore>> collectOwnerSummons(
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID ownerUuid,
            @Nonnull ComponentType<EntityStore, SummonComponent> summonComponentType
    ) {
        final ArrayList<Ref<EntityStore>> refs = new ArrayList<>();

        final Query<EntityStore> query = Query.and(
                summonComponentType,
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType(),
                TransformComponent.getComponentType()
        );

        store.forEachChunk(query, (chunk, _) -> {
            for (int i = 0; i < chunk.size(); i++) {
                final SummonComponent summon = chunk.getComponent(i, summonComponentType);
                if (summon == null) continue;
                if (!ownerUuid.equals(summon.owner)) continue;
                refs.add(chunk.getReferenceTo(i));
            }
        });

        return refs;
    }

    private static void clearInvalidOwnerFocus(
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID ownerUuid,
            @Nonnull ComponentType<EntityStore, SummonComponent> summonComponentType,
            @Nonnull SummonTargetSelector targetSelector
    ) {
        final Ref<EntityStore> focus =
                SummonRuntimeServices.targets().pullAggroOrFocus(store, ownerUuid, summonComponentType);

        if (focus != null && (!focus.isValid() || !targetSelector.isAlive(focus, store))) {
            SummonRuntimeServices.targets().clearRuntimeTarget(ownerUuid);
        }
    }
}