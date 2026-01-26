package me.s3b4s5.summonlib.stats;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class SummonStats {
    private static final int HARD_CAP = 10;
    private static volatile int IDX = Integer.MIN_VALUE;

    private SummonStats() {}

    private static int idx() {
        int i = IDX;
        if (i == Integer.MIN_VALUE) {
            int found = EntityStatType.getAssetMap().getIndex("MaxMinions");
            if (found != Integer.MIN_VALUE) {
                IDX = found;
                return found;
            }
            return Integer.MIN_VALUE;
        }
        return i;
    }

    public static int getMaxSlots(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        int statIndex = idx();
        if (statIndex == Integer.MIN_VALUE) return 1;

        EntityStatMap map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1;

        map.update();

        var v = map.get(statIndex);
        if (v == null) return 1;

        int cap = (int) Math.floor(v.getMax() + 1e-6f);
        if (cap < 1) cap = 1;
        if (cap > HARD_CAP) cap = HARD_CAP;
        return cap;
    }

    public static int getMaxSlots(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        int statIndex = idx();
        if (statIndex == Integer.MIN_VALUE) return 1;

        EntityStatMap map = (cb != null) ? cb.getComponent(ownerRef, EntityStatMap.getComponentType()) : null;
        if (map == null) map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1;

        var v = map.get(statIndex);
        if (v == null) {
            map.update();
            v = map.get(statIndex);
            if (v == null) return 1;
        }

        int cap = (int) Math.floor(v.getMax() + 1e-6f);
        if (cap < 1) cap = 1;
        if (cap > HARD_CAP) cap = HARD_CAP;
        return cap;
    }

}
