package me.s3b4s5.summonlib.internal.runtime.service;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.resolve.SummonDefinitionResolver;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves and caches runtime summon definitions built from asset configs.
 */
public final class SummonDefinitionService {

    private static final HytaleLogger LOG = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    private final ConcurrentHashMap<String, SummonDefinition> cache = new ConcurrentHashMap<>();

    public void register(@Nullable SummonDefinition definition) {
        if (definition == null || definition.id == null || definition.id.isEmpty()) return;
        cache.put(definition.id, definition);
        if (DEBUG) LOG.atInfo().log("[SummonDefinitionService] register id=%s defClass=%s", definition.id, revealsClass(definition));
    }

    public void unregister(@Nullable String id) {
        if (id == null || id.isEmpty()) return;
        cache.remove(id);
        if (DEBUG) LOG.atInfo().log("[SummonDefinitionService] unregister id=%s", id);
    }

    public void clear() {
        cache.clear();
        if (DEBUG) LOG.atInfo().log("[SummonDefinitionService] clear()");
    }

    @Nullable
    public SummonDefinition get(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;

        SummonDefinition cached = cache.get(id);
        if (cached != null) return cached;

        SummonConfig cfg = getSummonConfig(id);
        if (cfg == null || cfg.isUnknown()) return null;

        SummonDefinition built = SummonDefinitionResolver.resolve(cfg);
        if (built == null) {
            if (DEBUG) LOG.atWarning().log("[SummonDefinitionService] unsupported cfgClass=%s", revealsClass(cfg));
            return null;
        }

        if (!id.equals(built.id) && DEBUG) {
            LOG.atWarning().log("[SummonDefinitionService] id mismatch requested=%s built.id=%s", id, built.id);
        }

        SummonDefinition prev = cache.putIfAbsent(id, built);
        return prev != null ? prev : built;
    }

    @Nullable
    private SummonConfig getSummonConfig(String id) {
        try {
            @SuppressWarnings("unchecked")
            IndexedLookupTableAssetMap<String, SummonConfig> map =
                    (IndexedLookupTableAssetMap<String, SummonConfig>) SummonConfig.getAssetStore().getAssetMap();

            int idx = map.getIndex(id);
            if (idx == Integer.MIN_VALUE) return null;
            return map.getAsset(idx);
        } catch (Throwable t) {
            if (DEBUG) LOG.atWarning().log("[SummonDefinitionService] getSummonConfig id=%s FAILED: %s", id, String.valueOf(t));
            return null;
        }
    }

    private static String revealsClass(Object o) {
        return o == null ? "null" : o.getClass().getSimpleName();
    }
}


