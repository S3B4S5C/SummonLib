package me.s3b4s5.summonlib.assets.config.model;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.model.ModelSummonConfigCodec;
import me.s3b4s5.summonlib.assets.config.SummonConfig;

import javax.annotation.Nonnull;

/**
 * Declarative asset for a model-based summon.
 *
 * <p>This config references a follow asset through {@link #followId}. Runtime
 * follow behavior is resolved later by the internal resolver layer.</p>
 */
public final class ModelSummonConfig extends SummonConfig {
    @Nonnull
    public static final String ASSET_TYPE_ID = "Model";

    @Nonnull
    public static final BuilderCodec<ModelSummonConfig> ABSTRACT_CODEC = ModelSummonConfigCodec.create();

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
}


