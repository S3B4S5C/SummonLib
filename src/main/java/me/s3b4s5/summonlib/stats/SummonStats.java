package me.s3b4s5.summonlib.stats;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

public final class SummonStats {

    private SummonStats() {}

    public static int getMaxSlots(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        return SummonRuntimeServices.stats().getMaxSlots(store, ownerRef);
    }

    public static int getMaxSlots(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        return SummonRuntimeServices.stats().getMaxSlots(store, cb, ownerRef);
    }

    public static float getSummonDamageMultiplier(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        return SummonRuntimeServices.stats().getSummonDamageMultiplier(store, ownerRef);
    }

    public static float getSummonDamageMultiplier(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        return SummonRuntimeServices.stats().getSummonDamageMultiplier(store, cb, ownerRef);
    }
}


