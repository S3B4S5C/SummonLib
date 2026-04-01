package me.s3b4s5.summonlib.assets.config.model.follow;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.model.follow.WingFollowConfigCodec;

import javax.annotation.Nonnull;

public final class WingFollowConfig extends FollowConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Wing";

    // wing-specific fields (NO "Wing" prefix in JSON keys)
    public double stepBack = 0.55;
    public double sideSpread = 1.8;
    public double heightSpread = 0.7;
    public double heightCurvePow = 1.4;

    public double yawSpreadDeg = 28.0;
    public double rollSpreadDeg = 65.0;
    public double pitchSpreadDeg = 0.0;

    // extra required fields for controller behavior
    public String sideMode = "LEFT_ONLY"; // LEFT_ONLY / RIGHT_ONLY / SYMMETRIC
    public double orbitRadius = 0.9;
    // ---------------------------------------
    // Field schema / defaults for the editor
    // ---------------------------------------
    @Nonnull
    public static final BuilderCodec<WingFollowConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(WingFollowConfig.class, WingFollowConfig::new);
        WingFollowConfigCodec.appendWingFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, extra) -> {
            if (o.id == null) o.id = "";
            if (o.sideMode == null) o.sideMode = "LEFT_ONLY";
            if (o.heightCurvePow < 0.01) o.heightCurvePow = 0.01;
            if (o.orbitRadius < 0) o.orbitRadius = 0;
        }).build();
    }

}


