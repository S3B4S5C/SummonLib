package me.s3b4s5.summonlib.internal.runtime.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains owner summon ordering and applies model variants derived from that
 * ordering.
 */
public final class SummonIndexService {

    private final SummonDefinitionService definitions;

    public SummonIndexService(SummonDefinitionService definitions) {
        this.definitions = definitions;
    }

    public void rebuildOwnerIndices(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid,
            List<Ref<EntityStore>> refs
    ) {
        ArrayList<Ref<EntityStore>> valid = new ArrayList<>();
        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid()) continue;
            SummonComponent tag = store.getComponent(ref, summonTagType);
            if (tag != null && ownerUuid.equals(tag.owner)) valid.add(ref);
        }
        if (valid.isEmpty()) return;

        Map<String, Long> firstSeqByType = new HashMap<>();
        for (Ref<EntityStore> ref : valid) {
            SummonComponent tag = store.getComponent(ref, summonTagType);
            if (tag == null) continue;
            firstSeqByType.merge(tag.summonId, tag.spawnSeq, Math::min);
        }

        valid.sort((a, b) -> {
            SummonComponent ta = store.getComponent(a, summonTagType);
            SummonComponent tb = store.getComponent(b, summonTagType);
            if (ta == null || tb == null) return 0;

            long fa = firstSeqByType.getOrDefault(ta.summonId, Long.MAX_VALUE);
            long fb = firstSeqByType.getOrDefault(tb.summonId, Long.MAX_VALUE);
            if (fa != fb) return Long.compare(fa, fb);

            int byType = ta.summonId.compareTo(tb.summonId);
            if (byType != 0) return byType;
            return Long.compare(ta.spawnSeq, tb.spawnSeq);
        });

        int globalTotal = valid.size();
        for (int index = 0; index < valid.size(); index++) {
            SummonComponent tag = store.getComponent(valid.get(index), summonTagType);
            if (tag == null) continue;
            tag.globalIndex = index;
            tag.globalTotal = globalTotal;
        }

        Map<String, List<Ref<EntityStore>>> byType = new LinkedHashMap<>();
        for (Ref<EntityStore> ref : valid) {
            SummonComponent tag = store.getComponent(ref, summonTagType);
            if (tag == null) continue;
            byType.computeIfAbsent(tag.summonId, ignored -> new ArrayList<>()).add(ref);
        }

        for (Map.Entry<String, List<Ref<EntityStore>>> entry : byType.entrySet()) {
            String summonId = entry.getKey();
            List<Ref<EntityStore>> group = entry.getValue();
            group.sort(Comparator.comparingLong(ref -> {
                SummonComponent tag = store.getComponent(ref, summonTagType);
                return tag != null ? tag.spawnSeq : Long.MAX_VALUE;
            }));

            int groupTotal = group.size();
            for (int groupIndex = 0; groupIndex < group.size(); groupIndex++) {
                Ref<EntityStore> ref = group.get(groupIndex);
                SummonComponent tag = store.getComponent(ref, summonTagType);
                if (tag == null) continue;

                tag.groupIndex = groupIndex;
                tag.groupTotal = groupTotal;
                tag.variantIndex = groupIndex;

                SummonComponent prev = store.getComponent(ref, summonTagType);
                if (prev == null) continue;

                boolean changed =
                        prev.globalIndex != tag.globalIndex ||
                                prev.globalTotal != tag.globalTotal ||
                                prev.groupIndex != tag.groupIndex ||
                                prev.groupTotal != tag.groupTotal ||
                                prev.variantIndex != tag.variantIndex;

                if (changed) {
                    cb.putComponent(ref, summonTagType, new SummonComponent(prev));
                }
                applyModelVariantIfNeeded(store, cb, ref, summonId, tag.variantIndex, summonTagType);
            }
        }
    }

    private void applyModelVariantIfNeeded(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Ref<EntityStore> ref,
            String summonId,
            int variantIndex,
            ComponentType<EntityStore, SummonComponent> summonTagType
    ) {
        SummonComponent tag = store.getComponent(ref, summonTagType);
        if (tag != null && tag.variantIndex == variantIndex) {
            return;
        }

        SummonDefinition definition = definitions.get(summonId);
        if (!(definition instanceof ModelSummonDefinition modelDefinition)) return;

        String assetId = modelDefinition.modelAssetByVariant.apply(variantIndex);
        if (assetId == null || assetId.isBlank()) return;

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(assetId);
        if (modelAsset == null) return;

        Model model = Model.createScaledModel(modelAsset, modelDefinition.modelScale);
        cb.putComponent(ref, PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        cb.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));
    }
}



