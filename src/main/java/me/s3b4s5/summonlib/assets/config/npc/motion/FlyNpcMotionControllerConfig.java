package me.s3b4s5.summonlib.assets.config.npc.motion;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.npc.motion.FlyNpcMotionControllerCodec;

import javax.annotation.Nonnull;

public final class FlyNpcMotionControllerConfig extends NpcMotionControllerConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Fly";

    public double followYHigh = 4.5;
    public double followYLow = 1.5;

    public double followYClampMin = 0.8;
    public double followYClampMax = 6.5;

    public double stuckMinDist = 3.0;
    public double stuckImproveEps = 0.05;
    public float stuckTimeSec = 1.25f;
    public float lowModeHoldSec = 2.0f;

    public boolean verticalAssistEnabled = false;
    public double verticalDeadzone = 0.20;

    public double vertFollowK = 7.0;
    public double vertFollowMaxUp = 7.0;
    public double vertFollowMaxDown = 9.0;

    public double vertCombatK = 10.0;
    public double vertCombatMaxUp = 10.0;
    public double vertCombatMaxDown = 14.0;

    @Nonnull
    public static final BuilderCodec<FlyNpcMotionControllerConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(FlyNpcMotionControllerConfig.class, FlyNpcMotionControllerConfig::new);
        FlyNpcMotionControllerCodec.appendFlyFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, _) -> {
            if (o.id == null) o.id = "";

            if (o.followYHigh < 0) o.followYHigh = 0;
            if (o.followYLow < 0) o.followYLow = 0;

            if (o.followYClampMin < 0) o.followYClampMin = 0;
            if (o.followYClampMax < 0) o.followYClampMax = 0;

            double mn = Math.min(o.followYClampMin, o.followYClampMax);
            double mx = Math.max(o.followYClampMin, o.followYClampMax);
            o.followYClampMin = mn;
            o.followYClampMax = mx;

            if (o.stuckMinDist < 0) o.stuckMinDist = 0;
            if (o.stuckImproveEps < 0) o.stuckImproveEps = 0;
            if (o.stuckTimeSec < 0) o.stuckTimeSec = 0;
            if (o.lowModeHoldSec < 0) o.lowModeHoldSec = 0;

            if (o.verticalDeadzone < 0) o.verticalDeadzone = 0;

            if (o.vertFollowK < 0) o.vertFollowK = 0;
            if (o.vertFollowMaxUp < 0) o.vertFollowMaxUp = 0;
            if (o.vertFollowMaxDown < 0) o.vertFollowMaxDown = 0;

            if (o.vertCombatK < 0) o.vertCombatK = 0;
            if (o.vertCombatMaxUp < 0) o.vertCombatMaxUp = 0;
            if (o.vertCombatMaxDown < 0) o.vertCombatMaxDown = 0;
        }).build();
    }
}


