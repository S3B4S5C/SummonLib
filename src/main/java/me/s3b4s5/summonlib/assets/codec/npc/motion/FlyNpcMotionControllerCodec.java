package me.s3b4s5.summonlib.assets.codec.npc.motion;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.config.npc.motion.FlyNpcMotionControllerConfig;

/**
 * Fly motion controller ASSET codec fields (fly-only).
 * Walk has no Y config; all verticality knobs live here.
 */
public final class FlyNpcMotionControllerCodec {

    private FlyNpcMotionControllerCodec() {}

    public static BuilderCodec.Builder<FlyNpcMotionControllerConfig> appendFlyFields(
            BuilderCodec.Builder<FlyNpcMotionControllerConfig> b
    ) {
        return b
                // ---- Follow height policy (Y leash) ----
                .appendInherited(new KeyedCodec<>("FollowYHigh", Codec.DOUBLE),
                        (o, v) -> o.followYHigh = (v == null ? o.followYHigh : v),
                        (o) -> o.followYHigh,
                        (o, p) -> o.followYHigh = p.followYHigh
                ).add()

                .appendInherited(new KeyedCodec<>("FollowYLow", Codec.DOUBLE),
                        (o, v) -> o.followYLow = (v == null ? o.followYLow : v),
                        (o) -> o.followYLow,
                        (o, p) -> o.followYLow = p.followYLow
                ).add()

                .appendInherited(new KeyedCodec<>("FollowYClampMin", Codec.DOUBLE),
                        (o, v) -> o.followYClampMin = (v == null ? o.followYClampMin : v),
                        (o) -> o.followYClampMin,
                        (o, p) -> o.followYClampMin = p.followYClampMin
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("FollowYClampMax", Codec.DOUBLE),
                        (o, v) -> o.followYClampMax = (v == null ? o.followYClampMax : v),
                        (o) -> o.followYClampMax,
                        (o, p) -> o.followYClampMax = p.followYClampMax
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("StuckMinDist", Codec.DOUBLE),
                        (o, v) -> o.stuckMinDist = (v == null ? o.stuckMinDist : v),
                        (o) -> o.stuckMinDist,
                        (o, p) -> o.stuckMinDist = p.stuckMinDist
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("StuckImproveEps", Codec.DOUBLE),
                        (o, v) -> o.stuckImproveEps = (v == null ? o.stuckImproveEps : v),
                        (o) -> o.stuckImproveEps,
                        (o, p) -> o.stuckImproveEps = p.stuckImproveEps
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("StuckTimeSec", Codec.FLOAT),
                        (o, v) -> o.stuckTimeSec = (v == null ? o.stuckTimeSec : v),
                        (o) -> o.stuckTimeSec,
                        (o, p) -> o.stuckTimeSec = p.stuckTimeSec
                ).addValidator(Validators.greaterThanOrEqual(0.0f)).add()

                .appendInherited(new KeyedCodec<>("LowModeHoldSec", Codec.FLOAT),
                        (o, v) -> o.lowModeHoldSec = (v == null ? o.lowModeHoldSec : v),
                        (o) -> o.lowModeHoldSec,
                        (o, p) -> o.lowModeHoldSec = p.lowModeHoldSec
                ).addValidator(Validators.greaterThanOrEqual(0.0f)).add()

                // ---- Vertical assist (optional) ----
                .appendInherited(new KeyedCodec<>("VerticalAssistEnabled", Codec.BOOLEAN),
                        (o, v) -> o.verticalAssistEnabled = (v == null ? o.verticalAssistEnabled : v),
                        (o) -> o.verticalAssistEnabled,
                        (o, p) -> o.verticalAssistEnabled = p.verticalAssistEnabled
                ).add()

                .appendInherited(new KeyedCodec<>("VerticalDeadzone", Codec.DOUBLE),
                        (o, v) -> o.verticalDeadzone = (v == null ? o.verticalDeadzone : v),
                        (o) -> o.verticalDeadzone,
                        (o, p) -> o.verticalDeadzone = p.verticalDeadzone
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("VertFollowK", Codec.DOUBLE),
                        (o, v) -> o.vertFollowK = (v == null ? o.vertFollowK : v),
                        (o) -> o.vertFollowK,
                        (o, p) -> o.vertFollowK = p.vertFollowK
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("VertFollowMaxUp", Codec.DOUBLE),
                        (o, v) -> o.vertFollowMaxUp = (v == null ? o.vertFollowMaxUp : v),
                        (o) -> o.vertFollowMaxUp,
                        (o, p) -> o.vertFollowMaxUp = p.vertFollowMaxUp
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("VertFollowMaxDown", Codec.DOUBLE),
                        (o, v) -> o.vertFollowMaxDown = (v == null ? o.vertFollowMaxDown : v),
                        (o) -> o.vertFollowMaxDown,
                        (o, p) -> o.vertFollowMaxDown = p.vertFollowMaxDown
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("VertCombatK", Codec.DOUBLE),
                        (o, v) -> o.vertCombatK = (v == null ? o.vertCombatK : v),
                        (o) -> o.vertCombatK,
                        (o, p) -> o.vertCombatK = p.vertCombatK
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("VertCombatMaxUp", Codec.DOUBLE),
                        (o, v) -> o.vertCombatMaxUp = (v == null ? o.vertCombatMaxUp : v),
                        (o) -> o.vertCombatMaxUp,
                        (o, p) -> o.vertCombatMaxUp = p.vertCombatMaxUp
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("VertCombatMaxDown", Codec.DOUBLE),
                        (o, v) -> o.vertCombatMaxDown = (v == null ? o.vertCombatMaxDown : v),
                        (o) -> o.vertCombatMaxDown,
                        (o, p) -> o.vertCombatMaxDown = p.vertCombatMaxDown
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add();
    }
}


