package me.s3b4s5.summonlib.experimental.worm;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.experimental.worm.component.WormComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal bridge used by the stable core to recognize experimental worm entities.
 */
public final class WormSupport {

    private WormSupport() {
    }

    public static boolean isWormSegment(@Nullable WormComponent tag) {
        return tag != null;
    }

    public static boolean isRootSegment(@Nullable WormComponent tag) {
        return tag != null && tag.segmentIndex <= 0;
    }

    public static boolean shouldExcludeFromStableIndexing(@Nullable WormComponent tag) {
        return tag != null && tag.segmentIndex > 0;
    }

    public static List<Ref<EntityStore>> filterStableIndexRoots(
            Store<EntityStore> store,
            List<Ref<EntityStore>> refs,
            ComponentType<EntityStore, WormComponent> wormTagType
    ) {
        ArrayList<Ref<EntityStore>> out = new ArrayList<>(refs.size());
        for (Ref<EntityStore> ref : refs) {
            WormComponent tag = store.getComponent(ref, wormTagType);
            if (shouldExcludeFromStableIndexing(tag)) {
                continue;
            }
            out.add(ref);
        }
        return out;
    }
}



