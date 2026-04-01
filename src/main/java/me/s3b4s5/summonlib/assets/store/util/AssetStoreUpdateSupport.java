package me.s3b4s5.summonlib.assets.store.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AssetStoreUpdateSupport {

    private AssetStoreUpdateSupport() {
    }

    @Nonnull
    public static <T> Set<String> collectTouchedKeys(
            @Nullable Set<String> removedKeys,
            @Nullable Map<String, T> loadedOrUpdated
    ) {
        HashSet<String> touched = new HashSet<>();

        if (loadedOrUpdated != null) {
            touched.addAll(loadedOrUpdated.keySet());
        }

        if (removedKeys != null) {
            touched.addAll(removedKeys);
        }

        return touched;
    }
}