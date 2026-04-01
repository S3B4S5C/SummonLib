package me.s3b4s5.summonlib.internal.runtime.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Resolves owner-facing summon stats and caches the backing stat indices.
 */
public final class SummonStatService {

    private static final int MINIONS_HARD_CAP = 100;
    private static final float DAMAGE_MULT_MIN = 0.0f;
    private static final float DAMAGE_MULT_MAX = 10.0f;

    private volatile int maxMinionsIdx = Integer.MIN_VALUE;
    private volatile int summonDamageIdx = Integer.MIN_VALUE;

    public int getMaxSlots(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        int statIndex = resolveMaxMinionsIdx();
        if (statIndex == Integer.MIN_VALUE) return 1;

        EntityStatMap map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1;

        map.update();
        var value = map.get(statIndex);
        if (value == null) return 1;
        return clampSlots(value.getMax());
    }

    public int getMaxSlots(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        int statIndex = resolveMaxMinionsIdx();
        if (statIndex == Integer.MIN_VALUE) return 1;

        EntityStatMap map = cb != null ? cb.getComponent(ownerRef, EntityStatMap.getComponentType()) : null;
        if (map == null) map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1;

        var value = map.get(statIndex);
        if (value == null) {
            map.update();
            value = map.get(statIndex);
            if (value == null) return 1;
        }
        return clampSlots(value.getMax());
    }

    public float getSummonDamageMultiplier(Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        int statIndex = resolveSummonDamageIdx();
        if (statIndex == Integer.MIN_VALUE) return 1.0f;

        EntityStatMap map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1.0f;

        map.update();
        var value = map.get(statIndex);
        if (value == null) return 1.0f;
        return clampDamage(value.getMax());
    }

    public float getSummonDamageMultiplier(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef) {
        int statIndex = resolveSummonDamageIdx();
        if (statIndex == Integer.MIN_VALUE) return 1.0f;

        EntityStatMap map = cb != null ? cb.getComponent(ownerRef, EntityStatMap.getComponentType()) : null;
        if (map == null) map = store.getComponent(ownerRef, EntityStatMap.getComponentType());
        if (map == null) return 1.0f;

        var value = map.get(statIndex);
        if (value == null) {
            map.update();
            value = map.get(statIndex);
            if (value == null) return 1.0f;
        }
        return clampDamage(value.getMax());
    }

    private int resolveMaxMinionsIdx() {
        int idx = maxMinionsIdx;
        if (idx != Integer.MIN_VALUE) return idx;

        int found = EntityStatType.getAssetMap().getIndex("Max Minions");
        if (found != Integer.MIN_VALUE) {
            maxMinionsIdx = found;
            return found;
        }
        return Integer.MIN_VALUE;
    }

    private int resolveSummonDamageIdx() {
        int idx = summonDamageIdx;
        if (idx != Integer.MIN_VALUE) return idx;

        int found = EntityStatType.getAssetMap().getIndex("Summon Damage");
        if (found != Integer.MIN_VALUE) {
            summonDamageIdx = found;
            return found;
        }
        return Integer.MIN_VALUE;
    }

    private static int clampSlots(float maxValue) {
        int cap = (int) Math.floor(maxValue + 1e-6f);
        if (cap < 1) cap = 1;
        if (cap > MINIONS_HARD_CAP) cap = MINIONS_HARD_CAP;
        return cap;
    }

    private static float clampDamage(float maxValue) {
        float mult = maxValue;
        if (mult < DAMAGE_MULT_MIN) mult = DAMAGE_MULT_MIN;
        if (mult > DAMAGE_MULT_MAX) mult = DAMAGE_MULT_MAX;
        return mult;
    }
}


