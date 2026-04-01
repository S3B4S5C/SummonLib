package me.s3b4s5.summonlib.assets.config.model.follow;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.model.follow.OrbitFollowConfigCodec;

import javax.annotation.Nonnull;

public final class OrbitFollowConfig extends FollowConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Orbit";

    // orbit-specific fields
    public double radius = 1.4;
    public double spreadDeg = 120.0;
    public double orbitRadius = 0.9;

    // ---------------------------------------
    // Field schema / defaults for the editor
    // ---------------------------------------
    @Nonnull
    public static final BuilderCodec<OrbitFollowConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(OrbitFollowConfig.class, OrbitFollowConfig::new);
        OrbitFollowConfigCodec.appendOrbitFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, extra) -> {
            if (o.id == null) o.id = "";
            if (o.radius < 0) o.radius = 0;
            if (o.orbitRadius < 0) o.orbitRadius = 0;
        }).build();
    }

}


