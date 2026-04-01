package me.s3b4s5.summonlib.assets.store.util;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.NpcSummonConfig;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.registry.SummonRegistry;
import me.s3b4s5.summonlib.internal.resolve.SummonDefinitionResolver;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Rebuilds summon runtime definitions when summon assets or their dependencies
 * change.
 */
public final class SummonDefinitionRebuilder {

    private SummonDefinitionRebuilder() {}

    public static void rebuildSummons(@Nullable Set<String> changedSummonIds) {
        if (changedSummonIds == null || changedSummonIds.isEmpty()) return;

        IndexedLookupTableAssetMap<String, SummonConfig> summonMap = getSummonMap();
        if (summonMap == null) return;

        for (String summonId : changedSummonIds) {
            rebuildSummon(summonMap, summonId);
        }
    }

    public static void rebuildModelSummonsUsingFollow(@Nullable Set<String> changedFollowIds) {
        if (changedFollowIds == null || changedFollowIds.isEmpty()) return;

        IndexedLookupTableAssetMap<String, SummonConfig> summonMap = getSummonMap();
        if (summonMap == null) return;

        forEachSummonConfig(summonMap, cfg -> {
            if (!(cfg instanceof ModelSummonConfig model)) return;
            if (model.isUnknown()) return;

            String followId = model.followId;
            if (followId == null || followId.isEmpty()) return;
            if (!changedFollowIds.contains(followId)) return;

            rebuildSummonConfig(model);
        });
    }

    public static void rebuildNpcSummonsUsingMotionController(@Nullable Set<String> changedMotionControllerIds) {
        if (changedMotionControllerIds == null || changedMotionControllerIds.isEmpty()) return;

        IndexedLookupTableAssetMap<String, SummonConfig> summonMap = getSummonMap();
        if (summonMap == null) return;

        forEachSummonConfig(summonMap, cfg -> {
            if (!(cfg instanceof NpcSummonConfig npc)) return;
            if (npc.isUnknown()) return;

            String motionId = npc.npcMotionControllerId;
            if (motionId == null || motionId.isEmpty()) return;
            if (!changedMotionControllerIds.contains(motionId)) return;

            rebuildSummonConfig(npc);
        });
    }

    private static void rebuildSummon(IndexedLookupTableAssetMap<String, SummonConfig> summonMap, @Nullable String summonId) {
        if (summonId == null || summonId.isEmpty()) return;

        SummonConfig cfg = AssetMapUtil.getByKey(summonMap, summonId);
        if (cfg == null || cfg.isUnknown()) {
            SummonRegistry.unregister(summonId);
            return;
        }

        rebuildSummonConfig(cfg);
    }

    private static void rebuildSummonConfig(@Nullable SummonConfig cfg) {
        if (cfg == null || cfg.isUnknown()) return;

        SummonDefinition definition = SummonDefinitionResolver.resolve(cfg);
        if (definition != null) {
            SummonRegistry.register(definition);
        } else {
            SummonRegistry.unregister(cfg.getId());
        }
    }

    private static void forEachSummonConfig(
            IndexedLookupTableAssetMap<String, SummonConfig> summonMap,
            java.util.function.Consumer<SummonConfig> consumer
    ) {
        int n = summonMap.getNextIndex();
        for (int i = 0; i < n; i++) {
            SummonConfig cfg = summonMap.getAsset(i);
            if (cfg != null) consumer.accept(cfg);
        }
    }

    @Nullable
    private static IndexedLookupTableAssetMap<String, SummonConfig> getSummonMap() {
        try {
            return SummonConfig.getAssetMap();
        } catch (Throwable ignored) {
            return null;
        }
    }
}


