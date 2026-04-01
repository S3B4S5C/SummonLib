package me.s3b4s5.summonlib.internal.runtime.service;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.SummonLib;
import me.s3b4s5.summonlib.experimental.worm.WormSupport;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles summon cast/spawn mutations for an owner.
 */
public final class SummonSpawnService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;
    private static final double SPAWN_RING = 0.35;

    private final SummonDefinitionService definitions;
    private final SummonStatService stats;
    private final SummonIndexService index;
    private final SummonOwnerStateService owners;

    public SummonSpawnService(
            SummonDefinitionService definitions,
            SummonStatService stats,
            SummonIndexService index,
            SummonOwnerStateService owners
    ) {
        this.definitions = definitions;
        this.stats = stats;
        this.index = index;
        this.owners = owners;
    }

    public void cast(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            String summonId,
            int amount,
            SummonMode mode
    ) {
        if (ownerRef == null || !ownerRef.isValid()) return;

        ComponentType<EntityStore, SummonComponent> summonTagType = SummonLib.summonComponentType();
        if ((summonId == null || summonId.isBlank()) && mode != SummonMode.CLEAR) {
            if (DEBUG) LOGGER.atWarning().log("[SummonSpawnService] Missing SummonId.");
            return;
        }

        SummonDefinition definition = mode == SummonMode.CLEAR ? null : definitions.get(summonId);
        if (mode != SummonMode.CLEAR && definition == null) {
            if (DEBUG) LOGGER.atWarning().log("[SummonSpawnService] SummonDefinition not found: %s", summonId);
            return;
        }

        int capSlots = stats.getMaxSlots(store, cb, ownerRef);
        List<Ref<EntityStore>> all = collectOwnerSummons(store, summonTagType, ownerUuid);

        int usedSlots = 0;
        int usedSlotsThisType = 0;
        for (Ref<EntityStore> ref : all) {
            SummonComponent tag = store.getComponent(ref, summonTagType);
            if (tag == null) continue;

            int slotCost = Math.max(0, tag.slotCost);
            usedSlots += slotCost;
            if (mode != SummonMode.CLEAR && summonId.equals(tag.summonId)) {
                usedSlotsThisType += slotCost;
            }
        }

        int currentThisType = 0;
        if (mode != SummonMode.CLEAR && definition.slotCost > 0) {
            currentThisType = usedSlotsThisType / definition.slotCost;
        }

        switch (mode) {
            case CLEAR -> {
                clearSummons(store, cb, summonTagType, ownerUuid, ownerRef, all,
                        summonId == null || summonId.isBlank() ? null : summonId);
                index.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, collectOwnerSummons(store, summonTagType, ownerUuid));
            }
            case SET -> {
                int targetCount = Math.max(0, amount);
                if (currentThisType > targetCount) {
                    if (targetCount == 0 && definition.summonSpawnPlanFactory != null) {
                        clearSummons(store, cb, summonTagType, ownerUuid, ownerRef, all, summonId);
                        index.rebuildOwnerIndices(
                                store, cb, summonTagType, ownerUuid,
                                filterForStableIndexing(store, collectOwnerSummons(store, summonTagType, ownerUuid))
                        );
                        return;
                    }
                    removeTypeCount(store, cb, summonTagType, ownerUuid, summonId, currentThisType - targetCount, definition.slotCost);
                } else if (currentThisType < targetCount) {
                    addSummons(store, summonTagType, ownerUuid, definition,
                            targetCount - currentThisType, usedSlots, capSlots, currentThisType);
                }

                index.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, collectOwnerSummons(store, summonTagType, ownerUuid));
            }
            case ADD -> {
                addSummons(store, summonTagType, ownerUuid, definition, Math.max(1, amount), usedSlots, capSlots, currentThisType);
                index.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, collectOwnerSummons(store, summonTagType, ownerUuid));
            }
        }
    }

    public List<Ref<EntityStore>> collectOwnerSummons(
            Store<EntityStore> store,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid
    ) {
        Query<EntityStore> query = Query.and(
                summonTagType,
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType(),
                TransformComponent.getComponentType()
        );

        ArrayList<Ref<EntityStore>> out = new ArrayList<>();
        store.forEachChunk(query, (chunk, ignored) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonComponent tag = chunk.getComponent(i, summonTagType);
                if (tag == null || !ownerUuid.equals(tag.owner)) continue;
                out.add(chunk.getReferenceTo(i));
            }
        });
        return out;
    }

    private void addSummons(
            Store<EntityStore> store,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid,
            SummonDefinition definition,
            int requested,
            int usedSlots,
            int capSlots,
            int existingThisType
    ) {
        int freeSlots = Math.max(0, capSlots - usedSlots);
        int canSpawn = definition.slotCost <= 0 ? 0 : (freeSlots / definition.slotCost);
        int toSpawn = Math.min(requested, canSpawn);
        if (toSpawn <= 0) return;

        PlayerRef player = Universe.get().getPlayer(ownerUuid);
        if (player == null || player.getWorldUuid() == null) return;

        Transform ownerTransform = player.getTransform();
        Vector3d basePos = ownerTransform.getPosition().clone();
        for (int i = 0; i < toSpawn; i++) {
            long spawnSeq = owners.nextSpawnSequence(ownerUuid);
            int variantIndex = existingThisType + i;
            spawnOne(store, summonTagType, ownerTransform, basePos, definition, ownerUuid, spawnSeq, variantIndex);
        }
    }

    private void spawnOne(
            Store<EntityStore> store,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            Transform ownerTransform,
            Vector3d basePos,
            SummonDefinition definition,
            UUID ownerUuid,
            long spawnSeq,
            int variantIndex
    ) {
        Vector3d pos = basePos.clone();
        double angle = (spawnSeq % 12) * (Math.PI * 2.0 / 12.0);
        pos.x += Math.cos(angle) * SPAWN_RING;
        pos.z += Math.sin(angle) * SPAWN_RING;
        pos.y += 0.5;

        World world = store.getExternalData().getWorld();

        if (definition.summonSpawnPlanFactory != null) {
            List<Holder<EntityStore>> plan = definition.summonSpawnPlanFactory.createPlan(
                    store, ownerUuid, ownerTransform, pos, spawnSeq, variantIndex
            );
            if (plan == null || plan.isEmpty()) return;

            List<Holder<EntityStore>> holders = new ArrayList<>(plan);
            world.execute(() -> {
                for (Holder<EntityStore> holder : holders) {
                    if (holder != null) store.addEntity(holder, AddReason.SPAWN);
                }
            });
            return;
        }

        if (definition.summonSpawnFactory == null) {
            LOGGER.atWarning().log("[SummonSpawnService] Missing summonSpawnFactory for %s", definition.id);
            return;
        }

        Holder<EntityStore> built = definition.summonSpawnFactory.create(
                store, ownerUuid, ownerTransform, pos, spawnSeq, variantIndex
        );
        if (built == null) return;

        built.putComponent(summonTagType, new SummonComponent(ownerUuid, definition.id, definition.slotCost, spawnSeq, variantIndex));
        world.execute(() -> store.addEntity(built, AddReason.SPAWN));
    }

    private void clearSummons(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            List<Ref<EntityStore>> all,
            String onlyTypeOrNull
    ) {
        for (Ref<EntityStore> ref : all) {
            if (ref == null || !ref.isValid() || ref.equals(ownerRef)) continue;
            SummonComponent tag = store.getComponent(ref, summonTagType);
            if (tag == null || !ownerUuid.equals(tag.owner)) continue;
            if (onlyTypeOrNull != null && !onlyTypeOrNull.equals(tag.summonId)) continue;
            cb.removeEntity(ref, RemoveReason.REMOVE);
        }
    }

    private void removeTypeCount(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid,
            String summonId,
            int removeCount,
            int definitionSlotCost
    ) {
        if (removeCount <= 0 || definitionSlotCost <= 0) return;

        List<Ref<EntityStore>> all = collectOwnerSummons(store, summonTagType, ownerUuid);
        all.sort((a, b) -> {
            SummonComponent ta = store.getComponent(a, summonTagType);
            SummonComponent tb = store.getComponent(b, summonTagType);
            long sa = ta != null ? ta.spawnSeq : Long.MIN_VALUE;
            long sb = tb != null ? tb.spawnSeq : Long.MIN_VALUE;
            return Long.compare(sb, sa);
        });

        int removedSlots = 0;
        int removedUnits = 0;
        for (Ref<EntityStore> ref : all) {
            if (removedUnits >= removeCount) break;

            SummonComponent tag = store.getComponent(ref, summonTagType);
            if (tag == null || !ownerUuid.equals(tag.owner) || !summonId.equals(tag.summonId)) continue;

            int slotCost = Math.max(0, tag.slotCost);
            if (slotCost == 0) continue;

            cb.removeEntity(ref, RemoveReason.REMOVE);
            removedSlots += slotCost;
            removedUnits = removedSlots / definitionSlotCost;
        }
    }

    private List<Ref<EntityStore>> filterForStableIndexing(Store<EntityStore> store, List<Ref<EntityStore>> refs) {
        return WormSupport.filterStableIndexRoots(store, refs, SummonLib.wormComponentType());
    }

    public enum SummonMode {
        ADD,
        SET,
        CLEAR
    }
}



