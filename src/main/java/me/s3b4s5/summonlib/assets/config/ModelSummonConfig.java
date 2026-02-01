package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.assets.codec.ModelSummonConfigCodec;
import me.s3b4s5.summonlib.assets.config.follow.Follow;
import me.s3b4s5.summonlib.assets.config.follow.OrbitFollowConfig;
import me.s3b4s5.summonlib.assets.config.follow.WingFollowConfig;
import me.s3b4s5.summonlib.assets.store.util.AssetMapUtil;
import me.s3b4s5.summonlib.internal.impl.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;


public final class ModelSummonConfig extends SummonConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Model";

    @Nonnull
    public static final BuilderCodec<ModelSummonConfig> ABSTRACT_CODEC = ModelSummonConfigCodec.create();

    // Single Follow asset id
    public String followId = "";

    // Shared follow tuning (lives in Model)
    public double baseBack = 0.4;
    public double baseHeight = 0.8;
    public double attackHeight = 0.48;
    public double minPitchRad = -0.6;
    public double maxPitchRad = 0.55;

    public String[] modelAssets = new String[0];
    public float modelScale = 1.0f;

    @Nullable
    public SummonDefinition toDefinition() {
        return toDefinition((cfg) -> null);
    }

    @Nullable
    public SummonDefinition toDefinition(@Nonnull Function<ModelSummonConfig, ModelFollowController> followResolver) {
        SummonTuning t = SummonTuning.orDefault(tuning);

        final String[] assets = (modelAssets == null) ? new String[0] : modelAssets;
        final float scale = (modelScale <= 0f) ? 1.0f : modelScale;

        ModelFollowController fc = (followResolver == null) ? null : followResolver.apply(this);

        return new ModelSummonDefinition(
                getId(),
                slotCost,
                (variantIndex) -> {
                    if (assets.length == 0) return "";
                    return assets[Math.floorMod(variantIndex, assets.length)];
                },
                scale,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                fc,
                t
        );
    }

    public @Nullable ModelFollowController buildFollowController() {
        if (followId == null || followId.isEmpty()) return null;

        // clamp seguridad
        double mn = Math.min(minPitchRad, maxPitchRad);
        double mx = Math.max(minPitchRad, maxPitchRad);

        Follow follow = AssetMapUtil.getByKey(Follow.getAssetMap(), followId);
        if (follow == null || follow.isUnknown()) return null;

        // Detect subtype at runtime
        if (follow instanceof OrbitFollowConfig orbit) {
            return orbit.build(baseBack, baseHeight, attackHeight, mn, mx);
        }

        if (follow instanceof WingFollowConfig wing) {
            int refTotal = (modelAssets != null) ? modelAssets.length : 0;
            return wing.build(baseBack, baseHeight, attackHeight, mn, mx, refTotal);
        }

        return null;
    }
}
