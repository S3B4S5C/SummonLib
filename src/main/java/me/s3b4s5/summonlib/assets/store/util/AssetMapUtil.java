package me.s3b4s5.summonlib.assets.store.util;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;

import javax.annotation.Nullable;

public final class AssetMapUtil {

    private AssetMapUtil() {}

    @Nullable
    public static <T extends JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, T>>> T getByKey(IndexedLookupTableAssetMap<String, T> map, @Nullable String key) {
        if (map == null || key == null || key.isEmpty()) return null;
        int idx = map.getIndex(key);
        return (idx == Integer.MIN_VALUE) ? null : map.getAsset(idx);
    }
}
