package me.s3b4s5.summonlib.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.internal.impl.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;
import me.s3b4s5.summonlib.internal.impl.spawn.NpcRoleSummonSpawnFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public final class SummonConfig implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, SummonConfig>> {

    @Nonnull
    public static final String ASSET_TYPE_ID = "SummonConfig";

    @Nonnull
    public static final AssetCodecMapCodec<String, SummonConfig> CODEC =
            new AssetCodecMapCodec<>(
                    Codec.STRING,
                    (t, k) -> t.id = (k == null ? "" : k),
                    (t) -> t.id,
                    (t, data) -> t.data = data,
                    (t) -> t.data
            );

    @Nonnull
    public static final Codec<String> CHILD_ASSET_CODEC = new ContainedAssetCodec<>(SummonConfig.class, CODEC);

    @Nonnull
    public static final Codec<String[]> CHILD_ASSET_CODEC_ARRAY =
            new ArrayCodec<>(CHILD_ASSET_CODEC, String[]::new);

    @Nonnull
    public static final ValidatorCache<String> VALIDATOR_CACHE =
            new ValidatorCache(new AssetKeyValidator(SummonConfig::getAssetStore));

    private static AssetStore<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> ASSET_STORE;

    @Nonnull
    public static AssetStore<String, SummonConfig, IndexedLookupTableAssetMap<String, SummonConfig>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(SummonConfig.class);
        }
        return ASSET_STORE;
    }

    public static IndexedLookupTableAssetMap<String, SummonConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public String id = "";
    public AssetExtraInfo.Data data;
    public boolean unknown;

    @Override
    @Nonnull
    public String getId() {
        return (id == null) ? "" : id;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public enum SummonType { MODEL, NPC_ROLE }

    private static SummonType parseSummonType(@Nullable String s) {
        if (s == null) return SummonType.MODEL;

        String n = s.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if (n.equals("NPCROLE") || n.equals("NPC_ROLE") || n.equals("NPC")) return SummonType.NPC_ROLE;
        if (n.equals("MODEL")) return SummonType.MODEL;

        for (SummonType t : SummonType.values()) {
            if (t.name().equalsIgnoreCase(n)) return t;
        }

        return SummonType.MODEL;
    }

    private static final Codec<SummonType> SUMMON_TYPE_CODEC =
            new FunctionCodec<>(Codec.STRING, SummonConfig::parseSummonType, SummonType::name);

    private static final Codec<String[]> STRING_ARRAY = new ArrayCodec<>(Codec.STRING, String[]::new);

    public SummonType summonType = SummonType.MODEL;
    public int slotCost = 1;
    public float damage = 0f;
    public double detectRadius = 0.0;
    public boolean requireOwnerLoS = true;
    public boolean requireSummonLoS = true;

    @Nullable public SummonTuning tuning = null;
    @Nullable public BackOrbitFollowController followController = null;

    public String[] modelAssets = new String[0];
    public float modelScale = 1.0f;

    public String npcRoleId = "";
    public boolean applySeparation = true;
    public int despawnTimerSeconds = 300;

    public float initialModelScaleOverride = 0f;
    public boolean debugSpawnFactory = false;

    @Nonnull
    public static final BuilderCodec<SummonConfig> ABSTRACT_CODEC =
            BuilderCodec.builder(SummonConfig.class, SummonConfig::new)

                    .appendInherited(new KeyedCodec<>("SummonType", SUMMON_TYPE_CODEC),
                            (o, v) -> o.summonType = (v == null ? SummonType.MODEL : v),
                            (o) -> o.summonType,
                            (o, p) -> o.summonType = p.summonType
                    ).add()

                    .appendInherited(new KeyedCodec<>("SlotCost", Codec.INTEGER),
                            (o, v) -> o.slotCost = (v == null ? 1 : v),
                            (o) -> o.slotCost,
                            (o, p) -> o.slotCost = p.slotCost
                    ).addValidator(Validators.greaterThanOrEqual(1)).add()

                    .appendInherited(new KeyedCodec<>("Damage", Codec.FLOAT),
                            (o, v) -> o.damage = (v == null ? 0f : v),
                            (o) -> o.damage,
                            (o, p) -> o.damage = p.damage
                    ).add()

                    .appendInherited(new KeyedCodec<>("DetectRadius", Codec.DOUBLE),
                            (o, v) -> o.detectRadius = (v == null ? 0.0 : v),
                            (o) -> o.detectRadius,
                            (o, p) -> o.detectRadius = p.detectRadius
                    ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                    .appendInherited(new KeyedCodec<>("RequireOwnerLoS", Codec.BOOLEAN),
                            (o, v) -> o.requireOwnerLoS = (v == null ? true : v),
                            (o) -> o.requireOwnerLoS,
                            (o, p) -> o.requireOwnerLoS = p.requireOwnerLoS
                    ).add()

                    .appendInherited(new KeyedCodec<>("RequireSummonLoS", Codec.BOOLEAN),
                            (o, v) -> o.requireSummonLoS = (v == null ? true : v),
                            (o) -> o.requireSummonLoS,
                            (o, p) -> o.requireSummonLoS = p.requireSummonLoS
                    ).add()

                    .appendInherited(new KeyedCodec<>("Tuning", SummonTuning.CODEC),
                            (o, v) -> o.tuning = v,
                            (o) -> o.tuning,
                            (o, p) -> o.tuning = p.tuning
                    ).add()

                    .appendInherited(new KeyedCodec<>("FollowController", BackOrbitFollowControllerCodec.CODEC),
                            (o, v) -> o.followController = v,
                            (o) -> o.followController,
                            (o, p) -> o.followController = p.followController
                    ).add()

                    .appendInherited(new KeyedCodec<>("ModelAssets", STRING_ARRAY),
                            (o, v) -> o.modelAssets = (v == null ? new String[0] : v),
                            (o) -> o.modelAssets,
                            (o, p) -> o.modelAssets = p.modelAssets
                    ).add()

                    .appendInherited(new KeyedCodec<>("ModelScale", Codec.FLOAT),
                            (o, v) -> o.modelScale = (v == null ? 1.0f : v),
                            (o) -> o.modelScale,
                            (o, p) -> o.modelScale = p.modelScale
                    ).add()

                    .appendInherited(new KeyedCodec<>("NpcRoleId", Codec.STRING),
                            (o, v) -> o.npcRoleId = (v == null ? "" : v),
                            (o) -> o.npcRoleId,
                            (o, p) -> o.npcRoleId = p.npcRoleId
                    ).add()

                    .appendInherited(new KeyedCodec<>("ApplySeparation", Codec.BOOLEAN),
                            (o, v) -> o.applySeparation = (v == null ? true : v),
                            (o) -> o.applySeparation,
                            (o, p) -> o.applySeparation = p.applySeparation
                    ).add()

                    .appendInherited(new KeyedCodec<>("DespawnTimerSeconds", Codec.INTEGER),
                            (o, v) -> o.despawnTimerSeconds = (v == null ? 300 : v),
                            (o) -> o.despawnTimerSeconds,
                            (o, p) -> o.despawnTimerSeconds = p.despawnTimerSeconds
                    ).addValidator(Validators.greaterThanOrEqual(0)).add()

                    .appendInherited(new KeyedCodec<>("InitialModelScaleOverride", Codec.FLOAT),
                            (o, v) -> o.initialModelScaleOverride = (v == null ? 0f : v),
                            (o) -> o.initialModelScaleOverride,
                            (o, p) -> o.initialModelScaleOverride = p.initialModelScaleOverride
                    ).add()

                    .appendInherited(new KeyedCodec<>("DebugSpawnFactory", Codec.BOOLEAN),
                            (o, v) -> o.debugSpawnFactory = (v != null && v),
                            (o) -> o.debugSpawnFactory,
                            (o, p) -> o.debugSpawnFactory = p.debugSpawnFactory
                    ).add()

                    .afterDecode((o, extra) -> {
                        o.tuning = SummonTuning.orDefault(o.tuning);
                        if (o.id == null) o.id = "";
                        if (o.modelAssets == null) o.modelAssets = new String[0];
                        if (o.npcRoleId == null) o.npcRoleId = "";
                    })
                    .build();

    @Nullable
    public SummonDefinition toDefinition() {
        SummonTuning t = SummonTuning.orDefault(tuning);
        ModelFollowController fc = followController;

        if (summonType == SummonType.MODEL) {
            final String[] assets = (modelAssets == null) ? new String[0] : modelAssets;

            return new ModelSummonDefinition(
                    getId(),
                    slotCost,
                    (variantIndex) -> {
                        if (assets.length == 0) return "";
                        return assets[Math.floorMod(variantIndex, assets.length)];
                    },
                    modelScale,
                    damage,
                    detectRadius,
                    requireOwnerLoS,
                    requireSummonLoS,
                    fc,
                    t
            );
        }

        if (summonType == SummonType.NPC_ROLE) {
            if (npcRoleId == null || npcRoleId.isEmpty()) return null;

            return new NpcRoleSummonDefinition(
                    getId(),
                    slotCost,
                    npcRoleId,
                    applySeparation,
                    despawnTimerSeconds,
                    damage,
                    detectRadius,
                    requireOwnerLoS,
                    requireSummonLoS,
                    fc,
                    new NpcRoleSummonSpawnFactory(npcRoleId, initialModelScaleOverride, debugSpawnFactory),
                    t
            );
        }

        return null;
    }
}
