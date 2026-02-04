package me.s3b4s5.summonlib.api;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.NpcSummonConfig;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
// import me.s3b4s5.summonlib.assets.config.worm.WormSummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonRegistry {

    private static final ConcurrentHashMap<String, SummonDefinition> CACHE = new ConcurrentHashMap<>();

    private static final HytaleLogger LOG = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false; // ponlo false cuando termines

    private SummonRegistry() {}

    public static void register(@Nullable SummonDefinition def) {
        if (def == null || def.id == null || def.id.isEmpty()) return;
        CACHE.put(def.id, def);
        if (DEBUG) LOG.atInfo().log("[SummonRegistry] register id=%s defClass=%s", def.id, revealsClass(def));
    }

    public static void unregister(@Nullable String id) {
        if (id == null || id.isEmpty()) return;
        CACHE.remove(id);
        if (DEBUG) LOG.atInfo().log("[SummonRegistry] unregister id=%s", id);
    }

    public static void clear() {
        CACHE.clear();
        if (DEBUG) LOG.atInfo().log("[SummonRegistry] clear()");
    }

    @Nullable
    public static SummonDefinition get(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;

        SummonDefinition cached = CACHE.get(id);
        if (cached != null) {
            // if (DEBUG) LOG.atInfo().log("[SummonRegistry] get id=%s cacheHit=true defClass=%s", id, revealsClass(cached));
            return cached;
        }

        SummonConfig cfg = getSummonConfig(id);
        if (DEBUG) {
            LOG.atInfo().log("[SummonRegistry] get id=%s cacheHit=false cfg=%s unknown=%s",
                    id,
                    (cfg == null ? "null" : revealsClass(cfg)),
                    (cfg != null && cfg.isUnknown()));
        }

        if (cfg == null || cfg.isUnknown()) return null;

        SummonDefinition built = buildFromConfig(cfg);
        if (DEBUG) {
            LOG.atInfo().log("[SummonRegistry] build id=%s cfgClass=%s built=%s",
                    id, revealsClass(cfg), (built == null ? "null" : revealsClass(built)));
        }

        if (built == null) return null;

        // Extra safety: si por alguna razón el definition trae otro id, avisa
        if (!id.equals(built.id) && DEBUG) {
            LOG.atWarning().log("[SummonRegistry] id mismatch requested=%s built.id=%s", id, built.id);
        }

        SummonDefinition prev = CACHE.putIfAbsent(id, built);
        if (DEBUG && prev != null) {
            LOG.atInfo().log("[SummonRegistry] race id=%s (someone cached first) using=%s notice=%s",
                    id, revealsClass(prev), revealsClass(built));
        }
        return (prev != null) ? prev : built;
    }

    // -------------------------
    // Internal helpers
    // -------------------------

    @Nullable
    private static SummonConfig getSummonConfig(String id) {
        try {
            @SuppressWarnings("unchecked")
            IndexedLookupTableAssetMap<String, SummonConfig> map =
                    (IndexedLookupTableAssetMap<String, SummonConfig>) SummonConfig.getAssetStore().getAssetMap();

            int idx = map.getIndex(id);
            if (idx == Integer.MIN_VALUE) {
                if (DEBUG) LOG.atInfo().log("[SummonRegistry] config id=%s notFoundInMap", id);
                return null;
            }

            SummonConfig cfg = map.getAsset(idx);
            if (DEBUG) {
                LOG.atInfo().log("[SummonRegistry] config id=%s idx=%d class=%s",
                        id, idx, (cfg == null ? "null" : revealsClass(cfg)));
            }
            return cfg;
        } catch (Throwable t) {
            if (DEBUG) LOG.atWarning().log("[SummonRegistry] getSummonConfig id=%s FAILED: %s", id, String.valueOf(t));
            return null;
        }
    }

    @Nullable
    private static SummonDefinition buildFromConfig(SummonConfig cfg) {
        if (cfg == null) return null;

        if (cfg instanceof ModelSummonConfig model) {
            // Model usa follow
            return model.toDefinition(ModelSummonConfig::buildFollowController);
        }

        if (cfg instanceof NpcSummonConfig npc) {
            return npc.toDefinition();
        }

//        if (cfg instanceof WormSummonConfig worm) {
//            return worm.toDefinition();
//        }

        if (DEBUG) {
            LOG.atWarning().log("[SummonRegistry] buildFromConfig: unsupported cfgClass=%s", revealsClass(cfg));
        }
        return null;
    }

    private static String revealsClass(Object o) {
        return (o == null) ? "null" : o.getClass().getSimpleName();
    }
}
