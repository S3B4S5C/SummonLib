package me.s3b4s5.summonlib.internal.resolve;

import me.s3b4s5.summonlib.api.follow.ModelFollowBehavior;
import me.s3b4s5.summonlib.api.follow.OrbitFormationFollowBehavior;
import me.s3b4s5.summonlib.api.follow.WingFormationFollowBehavior;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.FollowConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.OrbitFollowConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.WingFollowConfig;
import me.s3b4s5.summonlib.assets.store.util.AssetMapUtil;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Resolves asset-backed follow config assets into runtime follow behavior instances.
 */
public final class FollowControllerResolver {

    private FollowControllerResolver() {
    }

    public static @Nullable ModelFollowBehavior resolve(@Nullable ModelSummonConfig config) {
        if (config == null || config.followId == null || config.followId.isEmpty()) {
            return null;
        }

        FollowConfig follow = AssetMapUtil.getByKey(FollowConfig.getAssetMap(), config.followId);
        return resolve(follow, config);
    }

    public static @Nullable ModelFollowBehavior resolve(@Nullable FollowConfig follow, @Nullable ModelSummonConfig ownerConfig) {
        if (follow == null || follow.isUnknown() || ownerConfig == null) {
            return null;
        }

        double minPitchRad = Math.min(ownerConfig.minPitchRad, ownerConfig.maxPitchRad);
        double maxPitchRad = Math.max(ownerConfig.minPitchRad, ownerConfig.maxPitchRad);

        if (follow instanceof OrbitFollowConfig orbit) {
            return new OrbitFormationFollowBehavior(
                    ownerConfig.baseBack,
                    orbit.radius,
                    orbit.spreadDeg,
                    ownerConfig.baseHeight,
                    orbit.orbitRadius,
                    ownerConfig.attackHeight,
                    minPitchRad,
                    maxPitchRad
            );
        }

        if (follow instanceof WingFollowConfig wing) {
            int refTotal = ownerConfig.modelAssets != null ? ownerConfig.modelAssets.length : 0;
            return new WingFormationFollowBehavior(
                    ownerConfig.baseBack,
                    wing.stepBack,
                    wing.sideSpread,
                    ownerConfig.baseHeight,
                    wing.heightSpread,
                    Math.max(0.01, wing.heightCurvePow),
                    wing.yawSpreadDeg,
                    wing.rollSpreadDeg,
                    wing.pitchSpreadDeg,
                    parseSideMode(wing.sideMode),
                    wing.orbitRadius,
                    ownerConfig.attackHeight,
                    minPitchRad,
                    maxPitchRad,
                    refTotal
            );
        }

        return null;
    }

    private static WingFormationFollowBehavior.SideMode parseSideMode(@Nullable String sideMode) {
        if (sideMode == null) {
            return WingFormationFollowBehavior.SideMode.LEFT_ONLY;
        }

        String normalized = sideMode.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return WingFormationFollowBehavior.SideMode.LEFT_ONLY;
        }

        try {
            return WingFormationFollowBehavior.SideMode.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return WingFormationFollowBehavior.SideMode.LEFT_ONLY;
        }
    }
}


