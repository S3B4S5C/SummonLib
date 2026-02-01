package me.s3b4s5.summonlib.runtime;

import com.hypixel.hytale.component.*;
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
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SummonActions {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    private static final double SPAWN_RING = 0.35;

    private static final ConcurrentHashMap<UUID, AtomicLong> SEQ_BY_OWNER = new ConcurrentHashMap<>();

    private SummonActions() {
    }

    public enum Mode {ADD, SET, CLEAR}

    public static void cast(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            String summonId,
            int amount,
            Mode mode
    ) {
        if (ownerRef == null || !ownerRef.isValid()) return;

        ComponentType<EntityStore, SummonTag> summonTagType = SummonLib.summonTagType();

        if ((summonId == null || summonId.isBlank()) && mode != Mode.CLEAR) {
            if (DEBUG) LOGGER.atWarning().log("[SummonActions] Missing SummonId.");
            return;
        }

        SummonDefinition def = (mode == Mode.CLEAR) ? null : SummonRegistry.get(summonId);
        if (mode != Mode.CLEAR && def == null) {
            if (DEBUG) LOGGER.atWarning().log("[SummonActions] SummonDefinition not found: %s", summonId);
            return;
        }

        int capSlots = SummonStats.getMaxSlots(store, cb, ownerRef);

        List<Ref<EntityStore>> all = collectOwnerSummons(store, summonTagType, ownerUuid);

        int usedSlots = 0;
        int usedSlotsThisType = 0;

        for (Ref<EntityStore> r : all) {
            SummonTag t = store.getComponent(r, summonTagType);
            if (t == null) continue;

            int sc = Math.max(0, t.slotCost);
            usedSlots += sc;

            if (mode != Mode.CLEAR && summonId.equals(t.summonId)) {
                usedSlotsThisType += sc;
            }
        }

        int currentThisType = 0;
        if (mode != Mode.CLEAR && def != null && def.slotCost > 0) {
            currentThisType = usedSlotsThisType / def.slotCost;
        }

        if (DEBUG) {
            LOGGER.atInfo().log("[SummonActions] mode=%s summonId=%s amount=%d usedSlots=%d capSlots=%d currentThisType=%d",
                    mode, summonId, amount, usedSlots, capSlots, currentThisType);
        }

        switch (mode) {
            case CLEAR -> {
                clearSummons(store, cb, summonTagType, ownerUuid, ownerRef, all,
                        (summonId == null || summonId.isBlank()) ? null : summonId);

                List<Ref<EntityStore>> after = collectOwnerSummons(store, summonTagType, ownerUuid);
                SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, after);
            }

            case SET -> {
                int targetCount = Math.max(0, amount);

                if (currentThisType > targetCount) {
                    if (targetCount == 0 && def.summonSpawnPlanFactory != null) {
                        // Worm: borrar cadena completa (head/tail incluidos)
                        clearSummons(store, cb, summonTagType, ownerUuid, ownerRef, all, summonId);

                        List<Ref<EntityStore>> after = collectOwnerSummons(store, summonTagType, ownerUuid);
                        SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, filterForIndexing(store, after));
                        return;
                    }
                    removeTypeCount(store, cb, summonTagType, ownerUuid, summonId, currentThisType - targetCount, def.slotCost);
                } else if (currentThisType < targetCount) {
                    int toAdd = targetCount - currentThisType;
                    addSummons(store, cb, summonTagType, ownerUuid, ownerRef, def, toAdd, usedSlots, capSlots, currentThisType);
                }

                List<Ref<EntityStore>> after = collectOwnerSummons(store, summonTagType, ownerUuid);
                SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, after);
            }

            case ADD -> {
                addSummons(store, cb, summonTagType, ownerUuid, ownerRef, def, Math.max(1, amount), usedSlots, capSlots, currentThisType);

                List<Ref<EntityStore>> after = collectOwnerSummons(store, summonTagType, ownerUuid);
                SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, after);
            }
        }
    }

    private static void addSummons(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            SummonDefinition def,
            int requested,
            int usedSlots,
            int capSlots,
            int existingThisType
    ) {
        int freeSlots = Math.max(0, capSlots - usedSlots);
        int canSpawn = def.slotCost <= 0 ? 0 : (freeSlots / def.slotCost);
        int toSpawn = Math.min(requested, canSpawn);

        if (toSpawn <= 0) {
            if (DEBUG) LOGGER.atInfo().log("[SummonActions] No free slots. used=%d cap=%d", usedSlots, capSlots);
            return;
        }

        PlayerRef pr = Universe.get().getPlayer(ownerUuid);
        if (pr == null || pr.getWorldUuid() == null) return;

        World world = Universe.get().getWorld(pr.getWorldUuid());
        if (world == null) return;

        Transform ownerT = pr.getTransform();
        Vector3d basePos = ownerT.getPosition().clone();

        for (int n = 0; n < toSpawn; n++) {
            long seq = nextSeq(ownerUuid);

            int variantIndex = existingThisType + n;

            spawnOne(store, cb, summonTagType, ownerT, basePos, def, ownerUuid, seq, variantIndex);
        }

        if (DEBUG) LOGGER.atInfo().log("[SummonActions] Spawned %d of %s", toSpawn, def.id);
    }

    private static void spawnOne(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            Transform ownerTransform,
            Vector3d basePos,
            SummonDefinition def,
            UUID ownerUuid,
            long spawnSeq,
            int variantIndex
    ) {
        Vector3d pos = basePos.clone();
        double a = (spawnSeq % 12) * (Math.PI * 2.0 / 12.0);
        pos.x += Math.cos(a) * SPAWN_RING;
        pos.z += Math.sin(a) * SPAWN_RING;
        pos.y += 0.5;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        // ✅ Multi-holder plan (Worm)
        if (def.summonSpawnPlanFactory != null) {
            List<Holder<EntityStore>> plan = def.summonSpawnPlanFactory.createPlan(
                    store, ownerUuid, ownerTransform, pos, spawnSeq, variantIndex
            );
            if (plan == null || plan.isEmpty()) return;

            final List<Holder<EntityStore>> holders = new ArrayList<>(plan);
            world.execute(() -> {
                for (Holder<EntityStore> h : holders) {
                    if (h != null) store.addEntity(h, AddReason.SPAWN);
                }
            });
            return;
        }

        // ✅ Normal single spawn
        if (def.summonSpawnFactory == null) {
            LOGGER.atWarning().log("[SummonActions] Missing summonSpawnFactory for %s", def.id);
            return;
        }

        Holder<EntityStore> built = def.summonSpawnFactory.create(store, ownerUuid, ownerTransform, pos, spawnSeq, variantIndex);
        if (built == null) return;

        built.putComponent(summonTagType, new SummonTag(ownerUuid, def.id, def.slotCost, spawnSeq, variantIndex));

        final Holder<EntityStore> h = built;
        world.execute(() -> store.addEntity(h, AddReason.SPAWN));
    }

    private static void clearSummons(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            List<Ref<EntityStore>> all,
            String onlyTypeOrNull
    ) {
        for (Ref<EntityStore> r : all) {
            if (r == null || !r.isValid() || r.equals(ownerRef)) continue;
            SummonTag t = store.getComponent(r, summonTagType);
            if (t == null) continue;
            if (!ownerUuid.equals(t.owner)) continue;

            if (onlyTypeOrNull != null && !onlyTypeOrNull.equals(t.summonId)) continue;

            cb.removeEntity(r, RemoveReason.REMOVE);
        }

        if (DEBUG) LOGGER.atInfo().log("[SummonActions] Cleared summons. onlyType=%s", onlyTypeOrNull);
    }

    private static void removeTypeCount(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            UUID ownerUuid,
            String summonId,
            int removeCount,
            int defSlotCost
    ) {
        if (removeCount <= 0 || defSlotCost <= 0) return;

        List<Ref<EntityStore>> all = collectOwnerSummons(store, summonTagType, ownerUuid);

        all.sort((a, b) -> {
            SummonTag ta = store.getComponent(a, summonTagType);
            SummonTag tb = store.getComponent(b, summonTagType);
            long sa = ta != null ? ta.spawnSeq : Long.MIN_VALUE;
            long sb = tb != null ? tb.spawnSeq : Long.MIN_VALUE;
            return Long.compare(sb, sa);
        });

        int removedSlots = 0;
        int removedUnits = 0;

        for (Ref<EntityStore> r : all) {
            if (removedUnits >= removeCount) break;

            SummonTag t = store.getComponent(r, summonTagType);
            if (t == null) continue;
            if (!ownerUuid.equals(t.owner)) continue;
            if (!summonId.equals(t.summonId)) continue;

            int sc = Math.max(0, t.slotCost);
            if (sc <= 0) continue;

            cb.removeEntity(r, RemoveReason.REMOVE);
            removedSlots += sc;
            removedUnits = removedSlots / defSlotCost;
        }
    }

    private static List<Ref<EntityStore>> collectOwnerSummons(
            Store<EntityStore> store,
            ComponentType<EntityStore, SummonTag> summonTagType,
            UUID ownerUuid
    ) {
        Query<EntityStore> q = Query.and(
                summonTagType,
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType(),
                TransformComponent.getComponentType()
        );

        ArrayList<Ref<EntityStore>> out = new ArrayList<>();
        store.forEachChunk(q, (chunk, cb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonTag tag = chunk.getComponent(i, summonTagType);
                if (tag == null) continue;
                if (!ownerUuid.equals(tag.owner)) continue;
                out.add(chunk.getReferenceTo(i));
            }
        });
        return out;
    }

    private static long nextSeq(UUID ownerUuid) {
        AtomicLong a = SEQ_BY_OWNER.computeIfAbsent(ownerUuid, k -> new AtomicLong(System.nanoTime()));
        return a.getAndIncrement();
    }

    private static List<Ref<EntityStore>> filterForIndexing(Store<EntityStore> store, List<Ref<EntityStore>> refs) {
        var wormTagType = SummonLib.wormTagType();
        ArrayList<Ref<EntityStore>> out = new ArrayList<>(refs.size());
        for (Ref<EntityStore> r : refs) {
            WormTag wt = store.getComponent(r, wormTagType);
            if (wt != null && wt.segmentIndex > 0) continue; // body/tail fuera
            out.add(r);
        }
        return out;
    }

}
