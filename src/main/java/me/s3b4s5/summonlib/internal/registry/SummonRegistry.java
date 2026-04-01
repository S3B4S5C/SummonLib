package me.s3b4s5.summonlib.internal.registry;

import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nullable;

/**
 * Internal registry that caches compiled summon definitions from asset configs.
 * This is runtime plumbing, not a supported extension API for side mods.
 */
public final class SummonRegistry {

    private SummonRegistry() {}

    public static void register(@Nullable SummonDefinition def) {
        SummonRuntimeServices.definitions().register(def);
    }

    public static void unregister(@Nullable String id) {
        SummonRuntimeServices.definitions().unregister(id);
    }

    @Nullable
    public static SummonDefinition get(@Nullable String id) {
        return SummonRuntimeServices.definitions().get(id);
    }
}


