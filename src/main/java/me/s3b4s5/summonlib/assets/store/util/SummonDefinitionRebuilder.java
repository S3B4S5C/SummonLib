package me.s3b4s5.summonlib.assets.store.util;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.assets.config.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;

import javax.annotation.Nullable;
import java.util.Set;

public final class SummonDefinitionRebuilder {

    private SummonDefinitionRebuilder() {}

    /**
     * Rebuild Model summon definitions that reference any Follow id in {@code changedFollowIds}.
     *
     * Works for both Follow subtypes (Orbit/Wing) because Model now stores only a single followId.
     */
    public static void rebuildModelsUsingFollow(@Nullable Set<String> changedFollowIds) {
        if (changedFollowIds == null || changedFollowIds.isEmpty()) return;

        IndexedLookupTableAssetMap<String, SummonConfig> map = getSummonMap();
        if (map == null) return;

        int n = map.getNextIndex();
        for (int i = 0; i < n; i++) {
            SummonConfig base = map.getAsset(i);
            if (!(base instanceof ModelSummonConfig cfg)) continue;
            if (cfg == null || cfg.isUnknown()) continue;

            String followId = cfg.followId;
            if (followId == null || followId.isEmpty()) continue;

            if (!changedFollowIds.contains(followId)) continue;

            SummonDefinition def = cfg.toDefinition((m) -> m.buildFollowController());
            if (def != null) SummonRegistry.register(def);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static IndexedLookupTableAssetMap<String, SummonConfig> getSummonMap() {
        try {
            return (IndexedLookupTableAssetMap<String, SummonConfig>) SummonConfig.getAssetStore().getAssetMap();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
