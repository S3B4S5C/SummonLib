package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultSummonAnimator implements SummonAnimator {

    private final AnimationSlot slot;
    private final ConcurrentHashMap<UUID, String> lastBaseKeyBySummon = new ConcurrentHashMap<>();

    public DefaultSummonAnimator(AnimationSlot slot) {
        this.slot = slot;
    }

    @Override
    public void setBaseAnim(UUID summonUuid, Ref<EntityStore> summonRef, String animSetId, boolean loop, Store<EntityStore> store, boolean forceReplay) {
        String key = animSetId + "|" + (loop ? "1" : "0");
        String last = lastBaseKeyBySummon.get(summonUuid);

        if (forceReplay) {
            lastBaseKeyBySummon.remove(summonUuid);
            last = null;
        }

        if (last == null || !last.equals(key)) {
            AnimationUtils.playAnimation(summonRef, slot, animSetId, loop, store);
            lastBaseKeyBySummon.put(summonUuid, key);
        }
    }

    public void clear(UUID summonUuid) {
        lastBaseKeyBySummon.remove(summonUuid);
    }
}
