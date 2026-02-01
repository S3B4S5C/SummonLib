package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;

import javax.annotation.Nonnull;

/**
 * Base "SummonConfig" asset type.
 *
 * Goal:
 * - One AssetStore path (e.g. Entity/SummonLib/Summons)
 * - Multiple subtypes selectable via JSON "Type" (Model / Npc / Worm / ...)
 *
 *   SummonConfig.CODEC.register("Model", ModelSummonConfig.class, ModelSummonConfig.ABSTRACT_CODEC);
 *   SummonConfig.CODEC.register("Npc",   NpcSummonConfig.class,   NpcSummonConfig.ABSTRACT_CODEC);
 *   SummonConfig.CODEC.register("Worm",  WormSummonConfig.class,  WormSummonConfig.ABSTRACT_CODEC);
 */
public abstract class SummonConfig extends BaseSummonConfig
        implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, SummonConfig>> {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Summon";

    /**
     * IMPORTANT: This is a *map codec* driven by JSON "Type".
     * Subtypes must be registered on this codec-map before assets are created/loaded.
     */
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

    // asset identity lives in BaseSummonConfig:
    // public String id = "";
    // public AssetExtraInfo.Data data;
    // public boolean unknown;

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
