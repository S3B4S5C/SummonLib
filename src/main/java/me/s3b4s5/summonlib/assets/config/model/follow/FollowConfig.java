package me.s3b4s5.summonlib.assets.config.model.follow;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;

import javax.annotation.Nonnull;

public abstract class FollowConfig implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, FollowConfig>> {

    /**
     * Type-dispatch codec for follow assets.
     *
     * <p>Register at least one follow subtype before assets load. Side-mods can
     * register additional follow types through
     * {@code me.s3b4s5.summonlib.api.SummonLibRegistration}.</p>
     */
    @Nonnull
    public static final AssetCodecMapCodec<String, FollowConfig> CODEC =
            new AssetCodecMapCodec<>(
                    Codec.STRING,
                    (t, k) -> t.id = (k == null ? "" : k),
                    (t) -> t.id,
                    (t, data) -> t.data = data,
                    (t) -> t.data,
                    true // allowDefault
            );

    private static AssetStore<String, FollowConfig, IndexedLookupTableAssetMap<String, FollowConfig>> ASSET_STORE;

    @Nonnull
    public static AssetStore<String, FollowConfig, IndexedLookupTableAssetMap<String, FollowConfig>> getAssetStore() {
        if (ASSET_STORE == null) ASSET_STORE = AssetRegistry.getAssetStore(FollowConfig.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static IndexedLookupTableAssetMap<String, FollowConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    // -------------------------
    // Asset identity (shared)
    // -------------------------
    public String id = "";
    public AssetExtraInfo.Data data;
    public boolean unknown;

    @Nonnull
    public String getId() {
        return id == null ? "" : id;
    }

    public boolean isUnknown() {
        return unknown;
    }
}


