package me.s3b4s5.summonlib.assets.store.follow;

import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import me.s3b4s5.summonlib.assets.config.follow.Follow;
import me.s3b4s5.summonlib.assets.config.follow.OrbitFollowConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class FollowConfigStore extends HytaleAssetStore<
        String,
        Follow,
        IndexedLookupTableAssetMap<String, Follow>> {

    // 👇 Esta es la carpeta ÚNICA que vas a ver/usar para Follow
    private static final String PATH = "Entity/SummonLib/Follow";

    private FollowConfigStore(@Nonnull Builder<String, Follow, IndexedLookupTableAssetMap<String, Follow>> builder) {
        super(builder);
    }

    @Nonnull
    public static FollowConfigStore create() {
        var map = new IndexedLookupTableAssetMap<String, Follow>(Follow[]::new);
        var b = HytaleAssetStore.builder(String.class, Follow.class, map);

        b.setPath(PATH)
                .setCodec(Follow.CODEC)
                .setKeyFunction(Follow::getId)
                .setIdProvider(Follow.class)
                .setIsUnknown(Follow::isUnknown);

        // When something references a missing follow id, return an "unknown" placeholder.
        b.setReplaceOnRemove((String id) -> {
            var cfg = new OrbitFollowConfig(); // default placeholder type
            cfg.id = id;
            cfg.unknown = true;
            return cfg;
        });

        return new FollowConfigStore(b);
    }

    @Override
    protected void handleRemoveOrUpdate(
            @Nullable Set<String> removedKeys,
            @Nullable Map<String, Follow> loadedOrUpdated,
            @Nonnull AssetUpdateQuery query
    ) {
        super.handleRemoveOrUpdate(removedKeys, loadedOrUpdated, query);

        java.util.HashSet<String> touched = new java.util.HashSet<>();
        if (loadedOrUpdated != null) touched.addAll(loadedOrUpdated.keySet());
        if (removedKeys != null) touched.addAll(removedKeys);

        me.s3b4s5.summonlib.assets.store.util.SummonDefinitionRebuilder.rebuildModelsUsingFollow(touched);
    }
}
