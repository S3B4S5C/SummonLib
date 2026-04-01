package me.s3b4s5.summonlib.lifecycle;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;

import java.util.UUID;

public final class SummonOwnerCleanup {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private SummonOwnerCleanup() {}

    /** Removes all summons owned by the given player from the current world thread. */
    public static int killAllSummonsOfOwnerDirect(
            EntityStore entityStore,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            UUID ownerUuid,
            boolean debug
    ) {
        if (entityStore == null) return 0;
        Store<EntityStore> store = entityStore.getStore();
        if (store == null) return 0;

        Query<EntityStore> q = Query.and(summonTagType);

        final int[] removed = {0};

        store.forEachChunk(q, (chunk, cb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonComponent tag = chunk.getComponent(i, summonTagType);
                if (tag == null) continue;

                UUID o = tag.getOwnerUuid();
                if (o == null || !o.equals(ownerUuid)) continue;

                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                cb.removeEntity(ref, RemoveReason.REMOVE);
                removed[0]++;
            }
        });

        if (debug) {
            World w = entityStore.getWorld();
            LOGGER.atInfo().log("[SummonOwnerCleanup] removed=%d owner=%s world=%s",
                    removed[0],
                    String.valueOf(ownerUuid),
                    (w != null ? w.getName() : "null"));
        }

        return removed[0];
    }

}



