package me.s3b4s5.summonlib.api;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.tags.SummonTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SummonApi {

    private static volatile ComponentType<EntityStore, SummonTag> TAG_TYPE;

    private static final ConcurrentHashMap<UUID, AtomicLong> SEQ_BY_OWNER = new ConcurrentHashMap<>();
    private static final double FIND_RADIUS = 96.0;

    private SummonApi() {}

    public static void init(ComponentType<EntityStore, SummonTag> summonTagType) {
        TAG_TYPE = summonTagType;
    }

    private static long nextSeq(UUID owner) {
        return SEQ_BY_OWNER.computeIfAbsent(owner, k -> new AtomicLong(System.nanoTime())).getAndIncrement();
    }

    public static int addInWorld(World world, UUID ownerUuid, String summonId, int amount) {
        if (amount <= 0) return 0;
        if (TAG_TYPE == null) throw new IllegalStateException("SummonApi.init(TAG_TYPE) was not called.");

        SummonDefinition def = SummonRegistry.get(summonId);
        if (def == null || def.summonSpawnFactory == null) return 0;

        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) return 0;

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid()) return 0;

        Store<EntityStore> store = world.getEntityStore().getStore();

        Transform ownerTr = owner.getTransform();
        Vector3d ownerPos = ownerTr.getPosition();

        List<Ref<EntityStore>> existing = findOwnerSummonsNear(store, ownerRef, ownerUuid, ownerPos);

        int usedSlots = 0;
        int sameTypeCount = 0;

        for (Ref<EntityStore> r : existing) {
            SummonTag t = store.getComponent(r, TAG_TYPE);
            if (t == null) continue;
            usedSlots += Math.max(0, t.slotCost);
            if (summonId.equals(t.summonId)) sameTypeCount++;
        }

        int capSlots = SummonStats.getMaxSlots(store, ownerRef);
        int free = capSlots - usedSlots;
        if (free <= 0) return 0;

        int cost = Math.max(1, def.slotCost);
        int allowed = Math.min(amount, free / cost);
        if (allowed <= 0) return 0;

        List<Ref<EntityStore>> spawned = new ArrayList<>(allowed);

        for (int j = 0; j < allowed; j++) {
            int variantIndex = sameTypeCount + j;

            Vector3d pos = ownerPos.clone();
            pos.y += 0.8;

            long seq = nextSeq(ownerUuid);

            // NEW: factory returns the holder fully prepared
            Holder<EntityStore> holder = def.summonSpawnFactory.create(
                    store, ownerUuid, ownerTr, pos, seq, variantIndex
            );
            if (holder == null) continue;

            holder.putComponent(TAG_TYPE, new SummonTag(ownerUuid, summonId, cost, seq, variantIndex));

            Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
            if (ref != null && ref.isValid()) spawned.add(ref);
        }

        if (spawned.isEmpty()) return 0;

        ArrayList<Ref<EntityStore>> all = new ArrayList<>(existing.size() + spawned.size());
        all.addAll(existing);
        all.addAll(spawned);

        rebuildIndicesGrouped(store, ownerUuid, all);

        return spawned.size();
    }

    public static int clearAllInWorld(World world, UUID ownerUuid) {
        if (TAG_TYPE == null) throw new IllegalStateException("SummonApi.init(TAG_TYPE) was not called.");

        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) return 0;

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid()) return 0;

        Store<EntityStore> store = world.getEntityStore().getStore();
        Vector3d ownerPos = owner.getTransform().getPosition();

        List<Ref<EntityStore>> list = findOwnerSummonsNear(store, ownerRef, ownerUuid, ownerPos);

        int removed = 0;
        for (Ref<EntityStore> r : list) {
            if (r == null || !r.isValid()) continue;
            store.removeEntity(r, RemoveReason.REMOVE);
            removed++;
        }
        return removed;
    }

    private static List<Ref<EntityStore>> findOwnerSummonsNear(
            Store<EntityStore> store,
            Ref<EntityStore> ownerRef,
            UUID ownerUuid,
            Vector3d center
    ) {
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(center, FIND_RADIUS, store);
        ArrayList<Ref<EntityStore>> out = new ArrayList<>();

        for (Ref<EntityStore> r : nearby) {
            if (r == null || !r.isValid()) continue;
            if (r.equals(ownerRef)) continue;

            SummonTag t = store.getComponent(r, TAG_TYPE);
            if (t == null) continue;
            if (!ownerUuid.equals(t.owner)) continue;

            out.add(r);
        }
        return out;
    }

    private static final class Entry {
        final Ref<EntityStore> ref;
        final SummonTag tag;
        Entry(Ref<EntityStore> ref, SummonTag tag) { this.ref = ref; this.tag = tag; }
    }

    private static void rebuildIndicesGrouped(Store<EntityStore> store, UUID ownerUuid, ArrayList<Ref<EntityStore>> refs) {
        ArrayList<Entry> entries = new ArrayList<>(refs.size());

        for (Ref<EntityStore> r : refs) {
            if (r == null || !r.isValid()) continue;
            SummonTag t = store.getComponent(r, TAG_TYPE);
            if (t == null) continue;
            if (!ownerUuid.equals(t.owner)) continue;
            entries.add(new Entry(r, t));
        }
        if (entries.isEmpty()) return;

        HashMap<String, Long> groupMin = new HashMap<>();
        HashMap<String, Integer> groupCount = new HashMap<>();

        for (Entry e : entries) {
            groupMin.merge(e.tag.summonId, e.tag.spawnSeq, Math::min);
            groupCount.merge(e.tag.summonId, 1, Integer::sum);
        }

        entries.sort((a, b) -> {
            long ga = groupMin.getOrDefault(a.tag.summonId, Long.MAX_VALUE);
            long gb = groupMin.getOrDefault(b.tag.summonId, Long.MAX_VALUE);
            if (ga != gb) return Long.compare(ga, gb);

            int byType = a.tag.summonId.compareTo(b.tag.summonId);
            if (byType != 0) return byType;

            return Long.compare(a.tag.spawnSeq, b.tag.spawnSeq);
        });

        int globalTotal = entries.size();
        HashMap<String, Integer> groupCursor = new HashMap<>();

        for (int globalIndex = 0; globalIndex < globalTotal; globalIndex++) {
            Entry e = entries.get(globalIndex);
            SummonTag old = e.tag;

            int gi = groupCursor.getOrDefault(old.summonId, 0);
            int gt = groupCount.getOrDefault(old.summonId, 1);
            groupCursor.put(old.summonId, gi + 1);

            SummonTag updated = new SummonTag(ownerUuid, old.summonId, old.slotCost, old.spawnSeq, gi);
            updated.setGroupIndex(gi);
            updated.setGroupTotal(gt);
            updated.setGlobalIndex(globalIndex);
            updated.setGlobalTotal(globalTotal);
            updated.setVariantIndex(gi); // keep A,B,C... stable inside the group

            store.putComponent(e.ref, TAG_TYPE, updated);
        }
    }
}
