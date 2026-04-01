package me.s3b4s5.summonlib.assets.codec.model.follow;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.config.model.follow.OrbitFollowConfig;

public final class OrbitFollowConfigCodec {

    private OrbitFollowConfigCodec() {}

    public static void appendOrbitFields(
            BuilderCodec.Builder<OrbitFollowConfig> b
    ) {
        b
                .appendInherited(new KeyedCodec<>("Radius", Codec.DOUBLE),
                        (o, v) -> o.radius = (v == null ? o.radius : v),
                        (o) -> o.radius,
                        (o, p) -> o.radius = p.radius
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("SpreadDeg", Codec.DOUBLE),
                        (o, v) -> o.spreadDeg = (v == null ? o.spreadDeg : v),
                        (o) -> o.spreadDeg,
                        (o, p) -> o.spreadDeg = p.spreadDeg
                ).add()

                .appendInherited(new KeyedCodec<>("OrbitRadius", Codec.DOUBLE),
                        (o, v) -> o.orbitRadius = (v == null ? o.orbitRadius : v),
                        (o) -> o.orbitRadius,
                        (o, p) -> o.orbitRadius = p.orbitRadius
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add();
    }
}


