package me.s3b4s5.summonlib.assets.store.follow;

import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import me.s3b4s5.summonlib.assets.config.model.follow.FollowConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.OrbitFollowConfig;
import me.s3b4s5.summonlib.assets.store.util.AssetStoreUpdateSupport;
import me.s3b4s5.summonlib.assets.store.util.SummonDefinitionRebuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class FollowConfigStore extends HytaleAssetStore<
        String,
        FollowConfig,
        IndexedLookupTableAssetMap<String, FollowConfig>> {

    private static final String PATH = "Entity/SummonLib/Follow";

    private FollowConfigStore(@Nonnull Builder<String, FollowConfig, IndexedLookupTableAssetMap<String, FollowConfig>> builder) {
        super(builder);
    }

    @Nonnull
    public static FollowConfigStore create() {
        var map = new IndexedLookupTableAssetMap<>(FollowConfig[]::new);
        var b = HytaleAssetStore.builder(String.class, FollowConfig.class, map);

        b.setPath(PATH)
                .setCodec(FollowConfig.CODEC)
                .setKeyFunction(FollowConfig::getId)
                .setIdProvider(FollowConfig.class)
                .setIsUnknown(FollowConfig::isUnknown);

        b.setReplaceOnRemove((String id) -> {
            var cfg = new OrbitFollowConfig();
            cfg.id = id;
            cfg.unknown = true;
            return cfg;
        });

        return new FollowConfigStore(b);
    }

    @Override
    protected void handleRemoveOrUpdate(
            @Nullable Set<String> removedKeys,
            @Nullable Map<String, FollowConfig> loadedOrUpdated,
            @Nonnull AssetUpdateQuery query
    ) {
        super.handleRemoveOrUpdate(removedKeys, loadedOrUpdated, query);

        Set<String> touched = AssetStoreUpdateSupport.collectTouchedKeys(removedKeys, loadedOrUpdated);
        SummonDefinitionRebuilder.rebuildModelSummonsUsingFollow(touched);
    }
}