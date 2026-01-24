package me.s3b4s5.summonlib.runtime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.api.ModelSummonDefinition;
import me.s3b4s5.summonlib.api.SummonDefinition;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.tags.SummonTag;

import java.util.*;

public final class SummonIndexing {

    private SummonIndexing() {}

    public static void rebuildOwnerIndices(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            UUID ownerUuid,
            List<Ref<EntityStore>> refs
    ) {
        ArrayList<Ref<EntityStore>> valid = new ArrayList<>();
        for (Ref<EntityStore> r : refs) {
            if (r != null && r.isValid()) {
                SummonTag t = store.getComponent(r, summonTagType);
                if (t != null && ownerUuid.equals(t.owner)) valid.add(r);
            }
        }
        if (valid.isEmpty()) return;

        Map<String, Long> firstSeqByType = new HashMap<>();
        for (Ref<EntityStore> r : valid) {
            SummonTag t = store.getComponent(r, summonTagType);
            if (t == null) continue;
            firstSeqByType.merge(t.summonId, t.spawnSeq, Math::min);
        }

        valid.sort((a, b) -> {
            SummonTag ta = store.getComponent(a, summonTagType);
            SummonTag tb = store.getComponent(b, summonTagType);
            if (ta == null || tb == null) return 0;

            long fa = firstSeqByType.getOrDefault(ta.summonId, Long.MAX_VALUE);
            long fb = firstSeqByType.getOrDefault(tb.summonId, Long.MAX_VALUE);
            if (fa != fb) return Long.compare(fa, fb);

            int byType = ta.summonId.compareTo(tb.summonId);
            if (byType != 0) return byType;

            return Long.compare(ta.spawnSeq, tb.spawnSeq);
        });

        int globalTotal = valid.size();
        for (int gi = 0; gi < valid.size(); gi++) {
            SummonTag t = store.getComponent(valid.get(gi), summonTagType);
            if (t == null) continue;
            t.globalIndex = gi;
            t.globalTotal = globalTotal;
        }

        Map<String, List<Ref<EntityStore>>> byType = new LinkedHashMap<>();
        for (Ref<EntityStore> r : valid) {
            SummonTag t = store.getComponent(r, summonTagType);
            if (t == null) continue;
            byType.computeIfAbsent(t.summonId, k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<String, List<Ref<EntityStore>>> e : byType.entrySet()) {
            String summonId = e.getKey();
            List<Ref<EntityStore>> group = e.getValue();
            group.sort(Comparator.comparingLong(r -> {
                SummonTag t = store.getComponent(r, summonTagType);
                return t != null ? t.spawnSeq : Long.MAX_VALUE;
            }));

            int groupTotal = group.size();

            for (int g = 0; g < group.size(); g++) {
                Ref<EntityStore> r = group.get(g);
                SummonTag t = store.getComponent(r, summonTagType);
                if (t == null) continue;

                t.groupIndex = g;
                t.groupTotal = groupTotal;

                t.variantIndex = g;

                SummonTag prev = store.getComponent(r, summonTagType);
                if (prev == null) continue;

                boolean changed =
                        prev.globalIndex != t.globalIndex ||
                                prev.globalTotal != t.globalTotal ||
                                prev.groupIndex  != t.groupIndex  ||
                                prev.groupTotal  != t.groupTotal  ||
                                prev.variantIndex!= t.variantIndex;

                if (changed) {
                    cb.putComponent(r, summonTagType, new SummonTag(prev)); // o tu updated
                }
                applyModelVariantIfNeeded(store, cb, r, summonId, t.variantIndex, summonTagType);
            }
        }
    }


    private static void applyModelVariantIfNeeded(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Ref<EntityStore> ref,
            String summonId,
            int variantIndex,
            ComponentType<EntityStore, SummonTag> summonTagType
    ) {
        SummonTag t = store.getComponent(ref, summonTagType);
        if (t != null && t.variantIndex == variantIndex) {
            return; // model already matches this variant
        }


        SummonDefinition def = SummonRegistry.get(summonId);
        if (!(def instanceof ModelSummonDefinition mdef)) return;

        String assetId = mdef.modelAssetByVariant.apply(variantIndex);
        if (assetId == null || assetId.isBlank()) return;

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(assetId);
        if (modelAsset == null) return;

        Model model = Model.createScaledModel(modelAsset, mdef.modelScale);

        cb.putComponent(ref, PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        cb.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));
    }

}
