package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;

import javax.annotation.Nonnull;

/**
 * Base summon asset type.
 *
 * <p>One asset store path hosts multiple summon subtypes selected by the JSON
 * {@code Type} field, for example {@code Model} and {@code Npc}.</p>
 *
 * <p>Flow:
 * summon JSON -> {@code SummonConfig} subtype -> summon definition resolver ->
 * internal runtime definition -> ECS systems.</p>
 */
public abstract class SummonConfig extends BaseSummonConfig
        implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, SummonConfig>> {

    @Nonnull
    public static final AssetCodecMapCodec<String, SummonConfig> CODEC =
            new AssetCodecMapCodec<>(
                    Codec.STRING,
                    (t, k) -> t.id = (k == null ? "" : k),
                    (t) -> t.id,
                    (t, data) -> t.data = data,
                    (t) -> t.data
            );

    private static AssetStore<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> ASSET_STORE;

    @Nonnull
    public static AssetStore<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> getAssetStore() {
        if (ASSET_STORE == null) ASSET_STORE = AssetRegistry.getAssetStore(SummonConfig.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static IndexedLookupTableAssetMap<String, SummonConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    @Nonnull
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public boolean isUnknown() {
        return super.isUnknown();
    }
}


