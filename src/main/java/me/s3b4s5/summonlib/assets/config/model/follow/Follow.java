package me.s3b4s5.summonlib.assets.config.model.follow;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;

import javax.annotation.Nonnull;

public abstract class Follow implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, Follow>> {

    /**
     * Type-based codec (Interaction-style).
     * IMPORTANT: allowDefault=true so empty/new docs don't explode when Type is missing.
     * Make sure you register at least one subtype codec (Orbit/Wing) before assets load.
     */
    @Nonnull
    public static final AssetCodecMapCodec<String, Follow> CODEC =
            new AssetCodecMapCodec<>(
                    Codec.STRING,
                    (t, k) -> t.id = (k == null ? "" : k),
                    (t) -> t.id,
                    (t, data) -> t.data = data,
                    (t) -> t.data,
                    true // allowDefault
            );

    private static AssetStore<String, Follow, IndexedLookupTableAssetMap<String, Follow>> ASSET_STORE;

    @Nonnull
    public static AssetStore<String, Follow, IndexedLookupTableAssetMap<String, Follow>> getAssetStore() {
        if (ASSET_STORE == null) ASSET_STORE = AssetRegistry.getAssetStore(Follow.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static IndexedLookupTableAssetMap<String, Follow> getAssetMap() {
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
