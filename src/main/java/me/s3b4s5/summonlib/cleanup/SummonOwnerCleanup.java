package me.s3b4s5.summonlib.cleanup;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.tags.SummonTag;

import java.util.UUID;

public final class SummonOwnerCleanup {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private SummonOwnerCleanup() {}

    /** Borra TODOS los summons del owner en ESTE world (debe ejecutarse en el world thread). */
    public static int killAllSummonsOfOwnerDirect(
            EntityStore entityStore,
            ComponentType<EntityStore, SummonTag> summonTagType,
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
                SummonTag tag = chunk.getComponent(i, summonTagType);
                if (tag == null) continue;

                UUID o = tag.getOwnerUuid(); // ajusta si tu SummonTag usa otro getter/campo
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

    /** Borra summons del owner en todos los worlds EXCEPTO keepWorld. */
    public static void removeOwnerSummonsFromOtherWorlds(
            ComponentType<EntityStore, SummonTag> summonTagType,
            UUID ownerUuid,
            World keepWorld,
            boolean debug
    ) {
        Universe.get().getWorlds().values().forEach(w -> {
            if (w == null) return;
            if (keepWorld != null && w == keepWorld) return;

            final World fw = w; // <- final para lambda
            fw.execute(() -> {
                EntityStore es = fw.getEntityStore();
                killAllSummonsOfOwnerDirect(es, summonTagType, ownerUuid, debug);
            });
        });
    }
}
