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
 * NPC Motion Controller asset (Follow-style):
 * - One store path: Entity/SummonLib/Npc/MotionController
 * - Multiple subtypes selectable via JSON "Type": Fly / Walk
 *
 * IMPORTANT:
 * - allowDefault=true so editor/new docs don't explode when Type is missing.
 * - Register subtypes (Fly/Walk) before assets load (in plugin setup).
 */
public abstract class NpcMotionController
        implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NpcMotionController>> {

    @Nonnull
    public static final AssetCodecMapCodec<String, NpcMotionController> CODEC =
            new AssetCodecMapCodec<>(
                    Codec.STRING,
                    (t, k) -> t.id = (k == null ? "" : k),
                    (t) -> t.id,
                    (t, data) -> t.data = data,
                    (t) -> t.data,
                    true // allowDefault
            );

    private static AssetStore<String, NpcMotionController, IndexedLookupTableAssetMap<String, NpcMotionController>> ASSET_STORE;

    @Nonnull
    public static AssetStore<String, NpcMotionController, IndexedLookupTableAssetMap<String, NpcMotionController>> getAssetStore() {
        if (ASSET_STORE == null) ASSET_STORE = AssetRegistry.getAssetStore(NpcMotionController.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static IndexedLookupTableAssetMap<String, NpcMotionController> getAssetMap() {
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
