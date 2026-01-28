package me.s3b4s5.summonlib.assets;

import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class SummonConfigStore extends HytaleAssetStore<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private SummonConfigStore(@Nonnull HytaleAssetStore.Builder<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> builder) {
        super(builder);
    }

    @Nonnull
    public static SummonConfigStore create() {
        IndexedLookupTableAssetMap<String, SummonConfig> map = new IndexedLookupTableAssetMap<>(SummonConfig[]::new);

        HytaleAssetStore.Builder<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> b =
                HytaleAssetStore.builder(String.class, SummonConfig.class, map)
                        .setPath("Entity/SummonLib/Summons")
                        .setCodec(SummonConfig.CODEC)
                        .setKeyFunction(SummonConfig::getId)
                        .setIdProvider(SummonConfig.class)
                        .setIsUnknown(sc -> sc.unknown)
                        .setReplaceOnRemove((String id) -> {
                            SummonConfig sc = new SummonConfig();
                            sc.id = id;
                            sc.unknown = true;

                            sc.summonType = SummonConfig.SummonType.MODEL;
                            sc.slotCost = 1;
                            sc.damage = 0f;
                            sc.detectRadius = 0.0;
                            sc.requireOwnerLoS = true;
                            sc.requireSummonLoS = true;

                            sc.tuning = SummonTuning.DEFAULT;
                            sc.followController = null;

                            sc.modelAssets = new String[0];
                            sc.modelScale = 1.0f;

                            sc.npcRoleId = "";
                            sc.applySeparation = true;
                            sc.despawnTimerSeconds = 0;

                            sc.initialModelScaleOverride = 0f;
                            sc.debugSpawnFactory = false;
                            return sc;
                        });

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
                if (cfg == null) continue;
                SummonDefinition def = cfg.toDefinition();
                if (def != null) SummonRegistry.register(def);
            }
        }

        LOGGER.atInfo().log("[SummonConfigStore] loaded=%d removed=%d",
                loadedOrUpdated == null ? 0 : loadedOrUpdated.size(),
                removedKeys == null ? 0 : removedKeys.size());
    }
}
