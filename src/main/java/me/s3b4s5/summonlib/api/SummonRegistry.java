package me.s3b4s5.summonlib.api;

import me.s3b4s5.summonlib.assets.SummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;

import java.util.concurrent.ConcurrentHashMap;

public final class SummonRegistry {

    private static final ConcurrentHashMap<String, SummonDefinition> cache = new ConcurrentHashMap<>();

    private SummonRegistry() {}


    public static void register(SummonDefinition def) {
        cache.put(def.id, def);
    }

    public static SummonDefinition get(String id) {
        SummonDefinition cached = cache.get(id);
        if (cached != null) return cached;

        SummonConfig asset = SummonConfig.getAssetMap().getAsset(id);
        if (asset == null) return null;

        SummonDefinition built = asset.toDefinition();
        if (built == null) return null;

        SummonDefinition prev = cache.putIfAbsent(id, built);
        return (prev != null) ? prev : built;
    }
}
