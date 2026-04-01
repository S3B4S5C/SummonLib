package me.s3b4s5.summonlib.internal.runtime.service;

/**
 * Shared singleton access to the internal runtime services used by SummonLib.
 */
public final class SummonRuntimeServices {

    private static final SummonDefinitionService DEFINITIONS = new SummonDefinitionService();
    private static final SummonStatService STATS = new SummonStatService();
    private static final SummonTargetStateService TARGETS = new SummonTargetStateService();
    private static final SummonOwnerStateService OWNERS = new SummonOwnerStateService();
    private static final SummonIndexService INDEX = new SummonIndexService(DEFINITIONS);
    private static final SummonSpawnService SPAWNS =
            new SummonSpawnService(DEFINITIONS, STATS, INDEX, OWNERS);

    private SummonRuntimeServices() {}

    public static SummonDefinitionService definitions() {
        return DEFINITIONS;
    }

    public static SummonStatService stats() {
        return STATS;
    }

    public static SummonTargetStateService targets() {
        return TARGETS;
    }

    public static SummonOwnerStateService owners() {
        return OWNERS;
    }

    public static SummonIndexService index() {
        return INDEX;
    }

    public static SummonSpawnService spawns() {
        return SPAWNS;
    }
}


