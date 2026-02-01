package me.s3b4s5.summonlib.assets.codec.follow;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.config.follow.WingFollowConfig;

/**
 * Wing follow ASSET codec fields (wing-specific only).
 *
 * IMPORTANT:
 * - Since this is already a "Wing follow" asset, keys DO NOT have "Wing" prefix.
 * - Shared follow tuning (BaseBack/BaseHeight/AttackHeight/MinPitchRad/MaxPitchRad)
 *   must live in ModelSummonConfig (not here).
 */
public final class WingFollowControllerCodec {

    private WingFollowControllerCodec() {}

    public static BuilderCodec.Builder<WingFollowConfig> appendWingFields(
            BuilderCodec.Builder<WingFollowConfig> b
    ) {
        return b
                .appendInherited(new KeyedCodec<>("StepBack", Codec.DOUBLE),
                        (o, v) -> o.stepBack = (v == null ? o.stepBack : v),
                        (o) -> o.stepBack,
                        (o, p) -> o.stepBack = p.stepBack
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("SideSpread", Codec.DOUBLE),
                        (o, v) -> o.sideSpread = (v == null ? o.sideSpread : v),
                        (o) -> o.sideSpread,
                        (o, p) -> o.sideSpread = p.sideSpread
                ).add()

                .appendInherited(new KeyedCodec<>("HeightSpread", Codec.DOUBLE),
                        (o, v) -> o.heightSpread = (v == null ? o.heightSpread : v),
                        (o) -> o.heightSpread,
                        (o, p) -> o.heightSpread = p.heightSpread
                ).add()

                .appendInherited(new KeyedCodec<>("HeightCurvePow", Codec.DOUBLE),
                        (o, v) -> o.heightCurvePow = (v == null ? o.heightCurvePow : v),
                        (o) -> o.heightCurvePow,
                        (o, p) -> o.heightCurvePow = p.heightCurvePow
                ).addValidator(Validators.greaterThanOrEqual(0.01)).add()

                .appendInherited(new KeyedCodec<>("YawSpreadDeg", Codec.DOUBLE),
                        (o, v) -> o.yawSpreadDeg = (v == null ? o.yawSpreadDeg : v),
                        (o) -> o.yawSpreadDeg,
                        (o, p) -> o.yawSpreadDeg = p.yawSpreadDeg
                ).add()

                .appendInherited(new KeyedCodec<>("RollSpreadDeg", Codec.DOUBLE),
                        (o, v) -> o.rollSpreadDeg = (v == null ? o.rollSpreadDeg : v),
                        (o) -> o.rollSpreadDeg,
                        (o, p) -> o.rollSpreadDeg = p.rollSpreadDeg
                ).add()

                .appendInherited(new KeyedCodec<>("PitchSpreadDeg", Codec.DOUBLE),
                        (o, v) -> o.pitchSpreadDeg = (v == null ? o.pitchSpreadDeg : v),
                        (o) -> o.pitchSpreadDeg,
                        (o, p) -> o.pitchSpreadDeg = p.pitchSpreadDeg
                ).add()

                // Needed by controller.
                .appendInherited(new KeyedCodec<>("SideMode", Codec.STRING),
                        (o, v) -> o.sideMode = (v == null ? o.sideMode : v),
                        (o) -> o.sideMode,
                        (o, p) -> o.sideMode = p.sideMode
                ).add()

                .appendInherited(new KeyedCodec<>("OrbitRadius", Codec.DOUBLE),
                        (o, v) -> o.orbitRadius = (v == null ? o.orbitRadius : v),
                        (o) -> o.orbitRadius,
                        (o, p) -> o.orbitRadius = p.orbitRadius
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add();
    }
}
