package me.s3b4s5.summonlib.assets.store;

import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class SummonConfigStore extends HytaleAssetStore<
        String,
        SummonConfig,
        IndexedLookupTableAssetMap<String, SummonConfig>> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PATH = "Entity/SummonLib/Summons";

    private SummonConfigStore(
            @Nonnull Builder<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> builder
    ) {
        super(builder);
    }

    @Nonnull
    public static SummonConfigStore create() {
        var map = new IndexedLookupTableAssetMap<String, SummonConfig>(SummonConfig[]::new);
        var b = HytaleAssetStore.builder(String.class, SummonConfig.class, map);

        // NOTE:
        // - IdProvider MUST be a concrete subtype when using a type-dispatch codec,
        //   otherwise the editor tends to create assets with missing/invalid Type.
        b.setPath(PATH)
                .setCodec(SummonConfig.CODEC)
                .setKeyFunction(SummonConfig::getId)
                .setIdProvider(ModelSummonConfig.class) // default type while only Model exists
                .setIsUnknown(SummonConfig::isUnknown);

        b.setReplaceOnRemove(SummonConfigStore::missingModel);

        return new SummonConfigStore(b);
    }

    @Override
    protected void handleRemoveOrUpdate(
            @Nullable Set<String> removedKeys,
            @Nullable Map<String, SummonConfig> loadedOrUpdated,
            @Nonnull AssetUpdateQuery query
    ) {
        super.handleRemoveOrUpdate(removedKeys, loadedOrUpdated, query);

        if (loadedOrUpdated != null) {
            for (SummonConfig cfg : loadedOrUpdated.values()) {
                if (cfg == null || cfg.isUnknown()) continue;

                // For now only Model exists
                if (cfg instanceof ModelSummonConfig model) {
                    SummonDefinition def = model.toDefinition((m) -> m.buildFollowController());
                    if (def != null) SummonRegistry.register(def);
                }
            }
        }

        LOGGER.atInfo().log("[SummonConfigStore] loaded=%d removed=%d",
                loadedOrUpdated == null ? 0 : loadedOrUpdated.size(),
                removedKeys == null ? 0 : removedKeys.size());
    }

    private static SummonConfig missingModel(String id) {
        var cfg = new ModelSummonConfig();
        cfg.id = (id == null ? "" : id);
        cfg.unknown = true;

        // Common defaults
        cfg.slotCost = 1;
        cfg.damage = 0f;
        cfg.detectRadius = 0.0;
        cfg.requireOwnerLoS = true;
        cfg.requireSummonLoS = true;

        // Follow default (single id now)
        cfg.followId = "";

        // Shared follow tuning defaults
        cfg.baseBack = 0.4;
        cfg.baseHeight = 0.8;
        cfg.attackHeight = 0.48;
        cfg.minPitchRad = -0.6;
        cfg.maxPitchRad = 0.55;

        // Model defaults
        cfg.modelAssets = new String[0];
        cfg.modelScale = 1.0f;

        return cfg;
    }
}
