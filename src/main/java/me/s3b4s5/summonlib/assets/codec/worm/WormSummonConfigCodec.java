//package me.s3b4s5.summonlib.assets.codec.worm;
//
//import com.hypixel.hytale.codec.Codec;
//import com.hypixel.hytale.codec.KeyedCodec;
//import com.hypixel.hytale.codec.builder.BuilderCodec;
//import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
//import com.hypixel.hytale.codec.validation.Validators;
//import me.s3b4s5.summonlib.assets.codec.SummonCommonCodec;
//import me.s3b4s5.summonlib.assets.config.worm.WormSummonConfig;
//import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;
//
//public final class WormSummonConfigCodec {
//
//    private static final Codec<String[]> STRING_ARRAY = new ArrayCodec<>(Codec.STRING, String[]::new);
//
//    private WormSummonConfigCodec() {}
//
//    public static BuilderCodec<WormSummonConfig> create() {
//        var b = BuilderCodec.builder(WormSummonConfig.class, WormSummonConfig::new);
//
//        // Common fields (slotCost/damage/detect/LoS/tuning...)
//        SummonCommonCodec.appendCommon(b);
//
//        // ------------------------
//        // Head (NPC role)
//        // ------------------------
//        b.appendInherited(new KeyedCodec<>("HeadRoleId", Codec.STRING),
//                (o, v) -> o.headRoleId = (v == null ? "" : v),
//                (o) -> o.headRoleId,
//                (o, p) -> o.headRoleId = p.headRoleId
//        ).add();
//
//        b.appendInherited(new KeyedCodec<>("HeadInitialModelScaleOverride", Codec.FLOAT),
//                (o, v) -> o.headInitialModelScaleOverride = (v == null ? 0f : v),
//                (o) -> o.headInitialModelScaleOverride,
//                (o, p) -> o.headInitialModelScaleOverride = p.headInitialModelScaleOverride
//        ).add();
//
//        // ------------------------
//        // Body / Tail models
//        // ------------------------
//        b.appendInherited(new KeyedCodec<>("BodyModelAssets", STRING_ARRAY),
//                (o, v) -> o.bodyModelAssets = (v == null ? new String[0] : v),
//                (o) -> o.bodyModelAssets,
//                (o, p) -> o.bodyModelAssets = p.bodyModelAssets
//        ).add();
//
//        b.appendInherited(new KeyedCodec<>("BodyModelScale", Codec.FLOAT),
//                (o, v) -> o.bodyModelScale = (v == null ? 1.0f : v),
//                (o) -> o.bodyModelScale,
//                (o, p) -> o.bodyModelScale = p.bodyModelScale
//        ).addValidator(Validators.greaterThanOrEqual(0.01f)).add();
//
//        b.appendInherited(new KeyedCodec<>("TailModelAssets", STRING_ARRAY),
//                (o, v) -> o.tailModelAssets = (v == null ? new String[0] : v),
//                (o) -> o.tailModelAssets,
//                (o, p) -> o.tailModelAssets = p.tailModelAssets
//        ).add();
//
//        b.appendInherited(new KeyedCodec<>("TailModelScale", Codec.FLOAT),
//                (o, v) -> o.tailModelScale = (v == null ? 1.0f : v),
//                (o) -> o.tailModelScale,
//                (o, p) -> o.tailModelScale = p.tailModelScale
//        ).addValidator(Validators.greaterThanOrEqual(0.01f)).add();
//
//        // ------------------------
//        // Spacing
//        // ------------------------
//        b.appendInherited(new KeyedCodec<>("SpacingHeadToBody", Codec.DOUBLE),
//                (o, v) -> o.spacingHeadToBody = (v == null ? 1.2 : v),
//                (o) -> o.spacingHeadToBody,
//                (o, p) -> o.spacingHeadToBody = p.spacingHeadToBody
//        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();
//
//        b.appendInherited(new KeyedCodec<>("SpacingBodyToBody", Codec.DOUBLE),
//                (o, v) -> o.spacingBodyToBody = (v == null ? 1.0 : v),
//                (o) -> o.spacingBodyToBody,
//                (o, p) -> o.spacingBodyToBody = p.spacingBodyToBody
//        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();
//
//        b.appendInherited(new KeyedCodec<>("SpacingBodyToTail", Codec.DOUBLE),
//                (o, v) -> o.spacingBodyToTail = (v == null ? 1.1 : v),
//                (o) -> o.spacingBodyToTail,
//                (o, p) -> o.spacingBodyToTail = p.spacingBodyToTail
//        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();
//
//        // Debug
//        b.appendInherited(new KeyedCodec<>("DebugSpawnFactory", Codec.BOOLEAN),
//                (o, v) -> o.debugSpawnFactory = (v != null && v),
//                (o) -> o.debugSpawnFactory,
//                (o, p) -> o.debugSpawnFactory = p.debugSpawnFactory
//        ).add();
//
//        b.afterDecode((o, extra) -> {
//            o.tuning = SummonTuning.orDefault(o.tuning);
//
//            if (o.id == null) o.id = "";
//            if (o.headRoleId == null) o.headRoleId = "";
//
//            if (o.bodyModelAssets == null) o.bodyModelAssets = new String[0];
//            if (o.tailModelAssets == null) o.tailModelAssets = new String[0];
//
//            if (o.bodyModelScale < 0.01f) o.bodyModelScale = 0.01f;
//            if (o.tailModelScale < 0.01f) o.tailModelScale = 0.01f;
//
//            if (o.spacingHeadToBody < 0) o.spacingHeadToBody = 0;
//            if (o.spacingBodyToBody < 0) o.spacingBodyToBody = 0;
//            if (o.spacingBodyToTail < 0) o.spacingBodyToTail = 0;
//        });
//
//        return b.build();
//    }
//}
