package me.s3b4s5.summonlib.internal.runtime;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public final class SummonAggroRuntime {

    private SummonAggroRuntime() {
    }

    public static void push(UUID ownerUuid, Ref<EntityStore> target, Instant now) {
        SummonRuntimeServices.targets().pushAggro(ownerUuid, target, now);
    }

    @Nullable
    public static Ref<EntityStore> peekValid(UUID ownerUuid, Instant now) {
        return SummonRuntimeServices.targets().peekValidAggro(ownerUuid, now);
    }

    public static void clear(UUID ownerUuid) {
        SummonRuntimeServices.targets().clearOwner(ownerUuid);
    }

    public static @Nullable Ref<EntityStore> pullAggroFocus(
            Store<EntityStore> store,
            UUID ownerUuid,
            ComponentType<EntityStore, SummonComponent> summonTagType
    ) {
        return SummonRuntimeServices.targets().pullAggroOrFocus(store, ownerUuid, summonTagType);
    }
}



