package me.s3b4s5.summonlib.assets.config.npc.motion;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;

import javax.annotation.Nonnull;

/**
 * Base NPC motion controller asset type.
 *
 * <p>These assets are referenced by {@code NpcSummonConfig.npcMotionControllerId}
 * and resolved into runtime NPC movement settings.</p>
 */
public abstract class NpcMotionControllerConfig
        implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NpcMotionControllerConfig>> {

    @Nonnull
    public static final AssetCodecMapCodec<String, NpcMotionControllerConfig> CODEC =
            new AssetCodecMapCodec<>(
                    Codec.STRING,
                    (t, k) -> t.id = (k == null ? "" : k),
                    (t) -> t.id,
                    (t, data) -> t.data = data,
                    (t) -> t.data,
                    true
            );

    private static AssetStore<String, NpcMotionControllerConfig, IndexedLookupTableAssetMap<String, NpcMotionControllerConfig>> ASSET_STORE;

    @Nonnull
    public static AssetStore<String, NpcMotionControllerConfig, IndexedLookupTableAssetMap<String, NpcMotionControllerConfig>> getAssetStore() {
        if (ASSET_STORE == null) ASSET_STORE = AssetRegistry.getAssetStore(NpcMotionControllerConfig.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static IndexedLookupTableAssetMap<String, NpcMotionControllerConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

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


