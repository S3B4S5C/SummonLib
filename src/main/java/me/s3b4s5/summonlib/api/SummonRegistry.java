package me.s3b4s5.summonlib.api;

import java.util.concurrent.ConcurrentHashMap;

public final class SummonRegistry {
    private static final ConcurrentHashMap<String, SummonDefinition> defs = new ConcurrentHashMap<>();

    private SummonRegistry() {}

    public static void register(SummonDefinition def) {
        defs.put(def.id, def);
    }

    public static SummonDefinition get(String id) {
        return defs.get(id);
    }
}
