package me.s3b4s5.summonlib.assets.config.model;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.assets.codec.model.ModelSummonConfigCodec;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.Follow;
import me.s3b4s5.summonlib.assets.config.model.follow.OrbitFollowConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.WingFollowConfig;
import me.s3b4s5.summonlib.assets.store.util.AssetMapUtil;
import me.s3b4s5.summonlib.internal.Logger;
import me.s3b4s5.summonlib.internal.impl.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

public final class ModelSummonConfig extends SummonConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Model";

    @Nonnull
    public static final BuilderCodec<ModelSummonConfig> ABSTRACT_CODEC = ModelSummonConfigCodec.create();

    private final Logger logger = new Logger("[ModelSummonConfig]");

    public String followId = "";

    public double baseBack = 0.4;
    public double baseHeight = 0.8;
    public double attackHeight = 0.48;
    public double minPitchRad = -0.6;
    public double maxPitchRad = 0.55;

    public String[] modelAssets = new String[0];
    public float modelScale = 1.0f;

    public double followSpeed = 16.0;
    public double travelToTargetSpeed = 10.0;

    public double hitDistance = 1.2;
    public float hitDamageDelaySec = 0.14f;
    public float attackIntervalSec = 0.45f;
    public boolean keepAttackWhileHasTarget = true;

    @Nullable
    public SummonDefinition toDefinition() {
        return toDefinition((cfg) -> null);
    }

    @Nullable
    public SummonDefinition toDefinition(@Nonnull Function<ModelSummonConfig, ModelFollowController> followResolver) {
        final String[] assets = (modelAssets == null) ? new String[0] : modelAssets;
        final float scale = (modelScale <= 0f) ? 1.0f : modelScale;

        final ModelFollowController fc = (followResolver == null) ? null : followResolver.apply(this);

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
                leashSummonToOwner,
                leashTargetToOwner,
                ownerMaintenanceCooldownSec,
                fc,
                followSpeed,
                travelToTargetSpeed,
                hitDistance,
                hitDamageDelaySec,
                attackIntervalSec,
                keepAttackWhileHasTarget
        );
    }

    public @Nullable ModelFollowController buildFollowController() {
        if (followId == null || followId.isEmpty()) return null;

        final double mn = Math.min(minPitchRad, maxPitchRad);
        final double mx = Math.max(minPitchRad, maxPitchRad);

        final var map = Follow.getAssetMap();
        final Follow follow = AssetMapUtil.getByKey(map, followId);

        if (follow == null || follow.isUnknown()) return null;

        if (follow instanceof OrbitFollowConfig orbit) {
            return orbit.build(baseBack, baseHeight, attackHeight, mn, mx);
        }

        if (follow instanceof WingFollowConfig wing) {
            final int refTotal = (modelAssets != null) ? modelAssets.length : 0;
            return wing.build(baseBack, baseHeight, attackHeight, mn, mx, refTotal);
        }

        return null;
    }
}
