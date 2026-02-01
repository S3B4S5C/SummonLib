package me.s3b4s5.summonlib.api;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import me.s3b4s5.summonlib.assets.config.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonRegistry {

    private static final ConcurrentHashMap<String, SummonDefinition> CACHE = new ConcurrentHashMap<>();

    private SummonRegistry() {}

    public static void register(@Nullable SummonDefinition def) {
        if (def == null) return;
        if (def.id == null || def.id.isEmpty()) return;
        CACHE.put(def.id, def);
    }

    public static void unregister(@Nullable String id) {
        if (id == null || id.isEmpty()) return;
        CACHE.remove(id);
    }

    public static void clear() {
        CACHE.clear();
    }

    @Nullable
    public static SummonDefinition get(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;

        SummonDefinition cached = CACHE.get(id);
        if (cached != null) return cached;

        SummonConfig cfg = getSummonConfig(id);
        if (cfg == null || cfg.isUnknown()) return null;

        SummonDefinition built = buildFromConfig(cfg);
        if (built == null) return null;

        SummonDefinition prev = CACHE.putIfAbsent(id, built);
        return (prev != null) ? prev : built;
    }

    // -------------------------
    // Internal helpers
    // -------------------------

    @Nullable
    private static SummonConfig getSummonConfig(String id) {
        try {
            // Single store now: SummonConfigStore (polymorphic by "Type")
            @SuppressWarnings("unchecked")
            IndexedLookupTableAssetMap<String, SummonConfig> map =
                    (IndexedLookupTableAssetMap<String, SummonConfig>) SummonConfig.getAssetStore().getAssetMap();

            int idx = map.getIndex(id);
            if (idx == Integer.MIN_VALUE) return null;

            return map.getAsset(idx);
        } catch (Throwable ignored) {
            // Store not registered yet or asset maps not ready.
            return null;
        }
    }

    @Nullable
    private static SummonDefinition buildFromConfig(SummonConfig cfg) {
        if (cfg == null) return null;

        // For now only Model exists (Npc/Worm later)
        if (cfg instanceof ModelSummonConfig model) {
            return model.toDefinition((m) -> m.buildFollowController());
        }

        return null;
    }
}
