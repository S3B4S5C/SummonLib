package me.s3b4s5.summonlib.stats;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class SummonStats {

    private static final int MINIONS_HARD_CAP = 100;

    // You probably want some sane caps to avoid insane scaling from stacked gear.
    private static final float DAMAGE_MULT_MIN = 0.0f;
    private static final float DAMAGE_MULT_MAX = 10.0f;

    private static volatile int MAX_MINIONS_IDX = Integer.MIN_VALUE;
    private static volatile int SUMMON_DAMAGE_IDX = Integer.MIN_VALUE;

    private SummonStats() {}

    private static int resolveIdx(String id, int cached) {
        if (cached != Integer.MIN_VALUE) return cached;
        return EntityStatType.getAssetMap().getIndex(id);
    }

    private static int maxMinionsIdx() {
        int i = MAX_MINIONS_IDX;
        if (i == Integer.MIN_VALUE) {
            int found = resolveIdx("Max Minions", Integer.MIN_VALUE);
            if (found != Integer.MIN_VALUE) {
                MAX_MINIONS_IDX = found;
                return found;
            }
            return Integer.MIN_VALUE;
        }
        return i;
    }

    private static int summonDamageIdx() {
        int i = SUMMON_DAMAGE_IDX;
        if (i == Integer.MIN_VALUE) {
            int found = resolveIdx("Summon Damage", Integer.MIN_VALUE);
            if (found != Integer.MIN_VALUE) {
                SUMMON_DAMAGE_IDX = found;
                return found;
            }
            return Integer.MIN_VALUE;
        }
        return i;
    }

    public static int getMaxSlots(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        int statIndex = maxMinionsIdx();
        if (statIndex == Integer.MIN_VALUE) return 1;

        EntityStatMap map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1;

        map.update();

        var v = map.get(statIndex);
        if (v == null) return 1;

        int cap = (int) Math.floor(v.getMax() + 1e-6f);
        if (cap < 1) cap = 1;
        if (cap > MINIONS_HARD_CAP) cap = MINIONS_HARD_CAP;
        return cap;
    }

    public static int getMaxSlots(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        int statIndex = maxMinionsIdx();
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
        if (cap > MINIONS_HARD_CAP) cap = MINIONS_HARD_CAP;
        return cap;
    }

    public static float getSummonDamageMultiplier(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        int statIndex = summonDamageIdx();
        if (statIndex == Integer.MIN_VALUE) return 1.0f;

        EntityStatMap map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1.0f;

        map.update();

        var v = map.get(statIndex);
        if (v == null) return 1.0f;

        float mult = v.getMax();
        if (mult < DAMAGE_MULT_MIN) mult = DAMAGE_MULT_MIN;
        if (mult > DAMAGE_MULT_MAX) mult = DAMAGE_MULT_MAX;
        return mult;
    }

    public static float getSummonDamageMultiplier(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        int statIndex = summonDamageIdx();
        if (statIndex == Integer.MIN_VALUE) return 1.0f;

        EntityStatMap map = (cb != null) ? cb.getComponent(ownerRef, EntityStatMap.getComponentType()) : null;
        if (map == null) map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1.0f;

        var v = map.get(statIndex);
        if (v == null) {
            map.update();
            v = map.get(statIndex);
            if (v == null) return 1.0f;
        }

        float mult = v.getMax();
        if (mult < DAMAGE_MULT_MIN) mult = DAMAGE_MULT_MIN;
        if (mult > DAMAGE_MULT_MAX) mult = DAMAGE_MULT_MAX;
        return mult;
    }
}
