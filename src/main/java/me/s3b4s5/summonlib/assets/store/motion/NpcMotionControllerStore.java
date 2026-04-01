package me.s3b4s5.summonlib.assets.store.motion;

import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionControllerConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.WalkNpcMotionControllerConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class NpcMotionControllerStore extends HytaleAssetStore<
        String,
        NpcMotionControllerConfig,
        IndexedLookupTableAssetMap<String, NpcMotionControllerConfig>> {

    private static final String PATH = "Entity/SummonLib/Npc/MotionController";

    private NpcMotionControllerStore(@Nonnull Builder<String, NpcMotionControllerConfig, IndexedLookupTableAssetMap<String, NpcMotionControllerConfig>> b) {
        super(b);
    }

    @Nonnull
    public static NpcMotionControllerStore create() {
        var map = new IndexedLookupTableAssetMap<String, NpcMotionControllerConfig>(NpcMotionControllerConfig[]::new);
        var b = HytaleAssetStore.builder(String.class, NpcMotionControllerConfig.class, map);

        b.setPath(PATH)
                .setCodec(NpcMotionControllerConfig.CODEC)
                .setKeyFunction(NpcMotionControllerConfig::getId)
                // default type for editor creation (concrete subtype)
                .setIdProvider(WalkNpcMotionControllerConfig.class)
                .setIsUnknown(NpcMotionControllerConfig::isUnknown);

        // If something references a missing id, return unknown placeholder (Walk default).
        b.setReplaceOnRemove((String id) -> {
            var cfg = new WalkNpcMotionControllerConfig();
            cfg.id = (id == null ? "" : id);
            cfg.unknown = true;
            return cfg;
        });

        return new NpcMotionControllerStore(b);
    }

    @Override
    protected void handleRemoveOrUpdate(
            @Nullable Set<String> removedKeys,
            @Nullable Map<String, NpcMotionControllerConfig> loadedOrUpdated,
            @Nonnull AssetUpdateQuery query
    ) {
        super.handleRemoveOrUpdate(removedKeys, loadedOrUpdated, query);

        java.util.HashSet<String> touched = new java.util.HashSet<>();
        if (loadedOrUpdated != null) touched.addAll(loadedOrUpdated.keySet());
        if (removedKeys != null) touched.addAll(removedKeys);

        me.s3b4s5.summonlib.assets.store.util.SummonDefinitionRebuilder.rebuildNpcSummonsUsingMotionController(touched);
    }
}


