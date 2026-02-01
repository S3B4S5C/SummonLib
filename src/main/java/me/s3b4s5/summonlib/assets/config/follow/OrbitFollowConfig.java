package me.s3b4s5.summonlib.assets.config.follow;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.assets.codec.follow.BackOrbitFollowControllerCodec;

import javax.annotation.Nonnull;

public final class OrbitFollowConfig extends Follow {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Orbit";

    // orbit-specific fields
    public double radius = 1.4;
    public double spreadDeg = 120.0;
    public double orbitRadius = 0.9;

    /**
     * Build the follow controller using shared values provided by ModelSummonConfig.
     */
    @Nonnull
    public BackOrbitFollowController build(
            double baseBack,
            double baseHeight,
            double attackHeight,
            double minPitchRad,
            double maxPitchRad
    ) {
        return new BackOrbitFollowController(
                baseBack,
                radius,
                spreadDeg,
                baseHeight,
                orbitRadius,
                attackHeight,
                minPitchRad,
                maxPitchRad
        );
    }

    // ---------------------------------------
    // Field schema / defaults for the editor
    // ---------------------------------------
    @Nonnull
    public static final BuilderCodec<OrbitFollowConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(OrbitFollowConfig.class, OrbitFollowConfig::new);
        BackOrbitFollowControllerCodec.appendOrbitFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, extra) -> {
            if (o.id == null) o.id = "";
            if (o.radius < 0) o.radius = 0;
            if (o.orbitRadius < 0) o.orbitRadius = 0;
        }).build();
    }

}
