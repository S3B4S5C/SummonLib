package me.s3b4s5.summonlib.assets.codec.model;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.codec.SummonCommonCodec;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.FollowConfig;

public final class ModelSummonConfigCodec {

    private static final Codec<String[]> STRING_ARRAY = new ArrayCodec<>(Codec.STRING, String[]::new);

    private ModelSummonConfigCodec() {}

    public static final Codec<String> FOLLOW_REF =
            new ContainedAssetCodec<>(FollowConfig.class, FollowConfig.CODEC);

    public static BuilderCodec<ModelSummonConfig> create() {
        var b = BuilderCodec.builder(ModelSummonConfig.class, ModelSummonConfig::new);

        SummonCommonCodec.appendCommon(b);

        b.appendInherited(new KeyedCodec<>("Follow", FOLLOW_REF),
                (o, v) -> o.followId = (v == null ? "" : v),
                (o) -> o.followId,
                (o, p) -> o.followId = p.followId
        ).add();

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

        b.appendInherited(new KeyedCodec<>("FollowSpeed", Codec.DOUBLE),
                (o, v) -> o.followSpeed = (v == null ? o.followSpeed : v),
                (o) -> o.followSpeed,
                (o, p) -> o.followSpeed = p.followSpeed
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("TravelToTargetSpeed", Codec.DOUBLE),
                (o, v) -> o.travelToTargetSpeed = (v == null ? o.travelToTargetSpeed : v),
                (o) -> o.travelToTargetSpeed,
                (o, p) -> o.travelToTargetSpeed = p.travelToTargetSpeed
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("HitDistance", Codec.DOUBLE),
                (o, v) -> o.hitDistance = (v == null ? o.hitDistance : v),
                (o) -> o.hitDistance,
                (o, p) -> o.hitDistance = p.hitDistance
        ).addValidator(Validators.greaterThanOrEqual(0.01)).add();

        b.appendInherited(new KeyedCodec<>("HitDamageDelaySec", Codec.FLOAT),
                (o, v) -> o.hitDamageDelaySec = (v == null ? o.hitDamageDelaySec : v),
                (o) -> o.hitDamageDelaySec,
                (o, p) -> o.hitDamageDelaySec = p.hitDamageDelaySec
        ).addValidator(Validators.greaterThanOrEqual(0.0f)).add();

        b.appendInherited(new KeyedCodec<>("AttackIntervalSec", Codec.FLOAT),
                (o, v) -> o.attackIntervalSec = (v == null ? o.attackIntervalSec : v),
                (o) -> o.attackIntervalSec,
                (o, p) -> o.attackIntervalSec = p.attackIntervalSec
        ).addValidator(Validators.greaterThanOrEqual(0.01f)).add();

        b.appendInherited(new KeyedCodec<>("KeepAttackWhileHasTarget", Codec.BOOLEAN),
                (o, v) -> o.keepAttackWhileHasTarget = (v == null ? o.keepAttackWhileHasTarget : v),
                (o) -> o.keepAttackWhileHasTarget,
                (o, p) -> o.keepAttackWhileHasTarget = p.keepAttackWhileHasTarget
        ).add();

        b.afterDecode((o, _) -> {
            if (o.id == null) o.id = "";
            if (o.followId == null) o.followId = "";

            if (o.modelAssets == null) o.modelAssets = new String[0];
            if (o.modelScale < 0.01f) o.modelScale = 0.01f;

            double mn = Math.min(o.minPitchRad, o.maxPitchRad);
            double mx = Math.max(o.minPitchRad, o.maxPitchRad);
            o.minPitchRad = mn;
            o.maxPitchRad = mx;

            if (o.followSpeed < 0.0) o.followSpeed = 0.0;
            if (o.travelToTargetSpeed < 0.0) o.travelToTargetSpeed = 0.0;

            if (o.hitDistance < 0.01) o.hitDistance = 0.01;
            if (o.hitDamageDelaySec < 0.0f) o.hitDamageDelaySec = 0.0f;
            if (o.attackIntervalSec < 0.01f) o.attackIntervalSec = 0.01f;

            if (o.leashSummonToOwner < 0.0) o.leashSummonToOwner = 0.0;
            if (o.leashTargetToOwner < 0.0) o.leashTargetToOwner = 0.0;
            if (o.ownerMaintenanceCooldownSec < 0.0f) o.ownerMaintenanceCooldownSec = 0.0f;
        });

        return b.build();
    }
}


