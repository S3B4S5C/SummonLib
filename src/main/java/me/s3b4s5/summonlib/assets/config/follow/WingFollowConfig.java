package me.s3b4s5.summonlib.assets.config.follow;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.api.follow.BackLineWingFollowController;
import me.s3b4s5.summonlib.assets.codec.follow.WingFollowControllerCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public final class WingFollowConfig extends Follow {

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

    private static BackLineWingFollowController.SideMode parseSideMode(@Nullable String s) {
        if (s == null) return BackLineWingFollowController.SideMode.LEFT_ONLY;

        String n = s.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if (n.isEmpty()) return BackLineWingFollowController.SideMode.LEFT_ONLY;

        try {
            return BackLineWingFollowController.SideMode.valueOf(n);
        } catch (Throwable ignored) {
            return BackLineWingFollowController.SideMode.LEFT_ONLY;
        }
    }

    /**
     * Build the follow controller using shared values provided by ModelSummonConfig.
     *
     * @param refTotal Stable reference for spacing normalization (e.g., modelAssets length)
     */
    @Nonnull
    public BackLineWingFollowController build(
            double baseBack,
            double baseHeight,
            double attackHeight,
            double minPitchRad,
            double maxPitchRad,
            int refTotal
    ) {
        BackLineWingFollowController.SideMode sm = parseSideMode(sideMode);

        return new BackLineWingFollowController(
                baseBack,
                stepBack,
                sideSpread,
                baseHeight,
                heightSpread,
                heightCurvePow,
                yawSpreadDeg,
                rollSpreadDeg,
                pitchSpreadDeg,
                sm,
                orbitRadius,
                attackHeight,
                minPitchRad,
                maxPitchRad,
                refTotal
        );
    }

    // ---------------------------------------
    // Field schema / defaults for the editor
    // ---------------------------------------
    @Nonnull
    public static final BuilderCodec<WingFollowConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(WingFollowConfig.class, WingFollowConfig::new);
        WingFollowControllerCodec.appendWingFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, extra) -> {
            if (o.id == null) o.id = "";
            if (o.sideMode == null) o.sideMode = "LEFT_ONLY";
            if (o.heightCurvePow < 0.01) o.heightCurvePow = 0.01;
            if (o.orbitRadius < 0) o.orbitRadius = 0;
        }).build();
    }

}
