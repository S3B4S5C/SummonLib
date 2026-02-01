package me.s3b4s5.summonlib.assets.codec;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.config.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.follow.Follow;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;

public final class ModelSummonConfigCodec {

    private static final Codec<String[]> STRING_ARRAY = new ArrayCodec<>(Codec.STRING, String[]::new);

    private ModelSummonConfigCodec() {}

    // Follow reference points to unified Follow store
    public static final Codec<String> FOLLOW_REF =
            new ContainedAssetCodec<>(Follow.class, Follow.CODEC);

    public static BuilderCodec<ModelSummonConfig> create() {
        var b = BuilderCodec.builder(ModelSummonConfig.class, ModelSummonConfig::new);

        SummonCommonCodec.appendCommon(b);

        // Single Follow reference
        b.appendInherited(new KeyedCodec<>("Follow", FOLLOW_REF),
                (o, v) -> o.followId = (v == null ? "" : v),
                (o) -> o.followId,
                (o, p) -> o.followId = p.followId
        ).add();

        // Shared follow tuning (lives in Model)
        b.appendInherited(new KeyedCodec<>("BaseBack", Codec.DOUBLE),
                        (o, v) -> o.baseBack = (v == null ? o.baseBack : v),
                        (o) -> o.baseBack,
                        (o, p) -> o.baseBack = p.baseBack
                ).add()

                .appendInherited(new KeyedCodec<>("BaseHeight", Codec.DOUBLE),
                        (o, v) -> o.baseHeight = (v == null ? o.baseHeight : v),
                        (o) -> o.baseHeight,
                        (o, p) -> o.baseHeight = p.baseHeight
                ).add()

                .appendInherited(new KeyedCodec<>("AttackHeight", Codec.DOUBLE),
                        (o, v) -> o.attackHeight = (v == null ? o.attackHeight : v),
                        (o) -> o.attackHeight,
                        (o, p) -> o.attackHeight = p.attackHeight
                ).add()

                .appendInherited(new KeyedCodec<>("MinPitchRad", Codec.DOUBLE),
                        (o, v) -> o.minPitchRad = (v == null ? o.minPitchRad : v),
                        (o) -> o.minPitchRad,
                        (o, p) -> o.minPitchRad = p.minPitchRad
                ).add()

                .appendInherited(new KeyedCodec<>("MaxPitchRad", Codec.DOUBLE),
                        (o, v) -> o.maxPitchRad = (v == null ? o.maxPitchRad : v),
                        (o) -> o.maxPitchRad,
                        (o, p) -> o.maxPitchRad = p.maxPitchRad
                ).add();

        // Model-specific fields
        b.appendInherited(new KeyedCodec<>("ModelAssets", STRING_ARRAY),
                (o, v) -> o.modelAssets = (v == null ? new String[0] : v),
                (o) -> o.modelAssets,
                (o, p) -> o.modelAssets = p.modelAssets
        ).add();

        b.appendInherited(new KeyedCodec<>("ModelScale", Codec.FLOAT),
                (o, v) -> o.modelScale = (v == null ? 1.0f : v),
                (o) -> o.modelScale,
                (o, p) -> o.modelScale = p.modelScale
        ).addValidator(Validators.greaterThanOrEqual(0.01f)).add();

        b.afterDecode((o, extra) -> {
            o.tuning = SummonTuning.orDefault(o.tuning);

            if (o.id == null) o.id = "";
            if (o.followId == null) o.followId = "";

            if (o.modelAssets == null) o.modelAssets = new String[0];
            if (o.modelScale < 0.01f) o.modelScale = 0.01f;

            // Clamp ordering
            double mn = Math.min(o.minPitchRad, o.maxPitchRad);
            double mx = Math.max(o.minPitchRad, o.maxPitchRad);
            o.minPitchRad = mn;
            o.maxPitchRad = mx;
        });

        return b.build();
    }
}
