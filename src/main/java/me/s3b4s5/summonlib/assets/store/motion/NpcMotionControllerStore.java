package me.s3b4s5.summonlib.assets.store.motion;

import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionController;
import me.s3b4s5.summonlib.assets.config.npc.motion.WalkNpcMotionControllerConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class NpcMotionControllerStore extends HytaleAssetStore<
        String,
        NpcMotionController,
        IndexedLookupTableAssetMap<String, NpcMotionController>> {

    private static final String PATH = "Entity/SummonLib/Npc/MotionController";

    private NpcMotionControllerStore(@Nonnull Builder<String, NpcMotionController, IndexedLookupTableAssetMap<String, NpcMotionController>> b) {
        super(b);
    }

    @Nonnull
    public static NpcMotionControllerStore create() {
        var map = new IndexedLookupTableAssetMap<String, NpcMotionController>(NpcMotionController[]::new);
        var b = HytaleAssetStore.builder(String.class, NpcMotionController.class, map);

        b.setPath(PATH)
                .setCodec(NpcMotionController.CODEC) // ✅ AssetCodecMapCodec (Follow-style)
                .setKeyFunction(NpcMotionController::getId)
                // default type for editor creation (concrete subtype)
                .setIdProvider(WalkNpcMotionControllerConfig.class)
                .setIsUnknown(NpcMotionController::isUnknown);

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
            @Nullable Map<String, NpcMotionController> loadedOrUpdated,
            @Nonnull AssetUpdateQuery query
    ) {
        super.handleRemoveOrUpdate(removedKeys, loadedOrUpdated, query);

        java.util.HashSet<String> touched = new java.util.HashSet<>();
        if (loadedOrUpdated != null) touched.addAll(loadedOrUpdated.keySet());
        if (removedKeys != null) touched.addAll(removedKeys);

        me.s3b4s5.summonlib.assets.store.util.SummonDefinitionRebuilder.rebuildModelsUsingFollow(touched);
    }
}
