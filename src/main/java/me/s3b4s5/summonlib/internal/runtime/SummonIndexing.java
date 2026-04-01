package me.s3b4s5.summonlib.internal.runtime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import java.util.List;
import java.util.UUID;

public final class SummonIndexing {

    private SummonIndexing() {}

    public static void rebuildOwnerIndices(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid,
            List<Ref<EntityStore>> refs
    ) {
        SummonRuntimeServices.index().rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, refs);
    }
}



