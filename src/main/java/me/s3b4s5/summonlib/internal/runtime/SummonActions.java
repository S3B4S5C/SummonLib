package me.s3b4s5.summonlib.internal.runtime;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;
import me.s3b4s5.summonlib.internal.runtime.service.SummonSpawnService;

import java.util.UUID;

public final class SummonActions {

    private SummonActions() {
    }

    public enum Mode {ADD, SET, CLEAR}

    public static void cast(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            String summonId,
            int amount,
            Mode mode
    ) {
        SummonSpawnService.SummonMode summonMode = switch (mode) {
            case ADD -> SummonSpawnService.SummonMode.ADD;
            case SET -> SummonSpawnService.SummonMode.SET;
            case CLEAR -> SummonSpawnService.SummonMode.CLEAR;
        };
        SummonRuntimeServices.spawns().cast(store, cb, ownerUuid, ownerRef, summonId, amount, summonMode);
    }
}


