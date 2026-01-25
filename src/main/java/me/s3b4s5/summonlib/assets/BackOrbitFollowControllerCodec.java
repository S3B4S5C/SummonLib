package me.s3b4s5.summonlib.assets;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.ExtraInfo;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;

public final class BackOrbitFollowControllerCodec {

    private BackOrbitFollowControllerCodec() {}

    public static final class Params {
        public double baseBack = 0.4;
        public double radius = 1.4;
        public double spreadDeg = 120.0;
        public double baseHeight = 0.8;
        public double orbitRadius = 0.9;
        public double attackHeight = 0.48;
        public double minPitchRad = -0.6;
        public double maxPitchRad = 0.55;

        public BackOrbitFollowController build() {
            return new BackOrbitFollowController(
                    baseBack, radius, spreadDeg, baseHeight, orbitRadius, attackHeight,
                    minPitchRad, maxPitchRad
            );
        }
    }

    public static final BuilderCodec<Params> PARAMS_CODEC =
            BuilderCodec.builder(Params.class, Params::new)

                    .appendInherited(new KeyedCodec<>("BaseBack", Codec.DOUBLE),
                            (o, v) -> o.baseBack = (v == null ? o.baseBack : v),
                            (o) -> o.baseBack,
                            (o, p) -> o.baseBack = p.baseBack
                    ).add()

                    .appendInherited(new KeyedCodec<>("Radius", Codec.DOUBLE),
                            (o, v) -> o.radius = (v == null ? o.radius : v),
                            (o) -> o.radius,
                            (o, p) -> o.radius = p.radius
                    ).add()

                    .appendInherited(new KeyedCodec<>("SpreadDeg", Codec.DOUBLE),
                            (o, v) -> o.spreadDeg = (v == null ? o.spreadDeg : v),
                            (o) -> o.spreadDeg,
                            (o, p) -> o.spreadDeg = p.spreadDeg
                    ).add()

                    .appendInherited(new KeyedCodec<>("BaseHeight", Codec.DOUBLE),
                            (o, v) -> o.baseHeight = (v == null ? o.baseHeight : v),
                            (o) -> o.baseHeight,
                            (o, p) -> o.baseHeight = p.baseHeight
                    ).add()

                    .appendInherited(new KeyedCodec<>("OrbitRadius", Codec.DOUBLE),
                            (o, v) -> o.orbitRadius = (v == null ? o.orbitRadius : v),
                            (o) -> o.orbitRadius,
                            (o, p) -> o.orbitRadius = p.orbitRadius
                    ).add()

                    .appendInherited(new KeyedCodec<>("AttackHeight", Codec.DOUBLE),
                            (o, v) -> o.attackHeight = (v == null ? o.attackHeight : v),
                            (o) -> o.attackHeight,
                            (o, p) -> o.attackHeight = p.attackHeight
                    ).add()

                    .appendInherited(new KeyedCodec<>("MinPitchRad", Codec.DOUBLE),
                            (o, v) -> o.minPitchRad = (v == null ? o.minPitchRad : v),
                            (o) -> o.minPitchRad,
                            (o, p) -> o.minPitchRad = p.minPitchRad
                    ).add()

                    .appendInherited(new KeyedCodec<>("MaxPitchRad", Codec.DOUBLE),
                            (o, v) -> o.maxPitchRad = (v == null ? o.maxPitchRad : v),
                            (o) -> o.maxPitchRad,
                            (o, p) -> o.maxPitchRad = p.maxPitchRad
                    ).add()

                    .build();

    // codec que devuelve directamente el controller construido
    public static final Codec<BackOrbitFollowController> CODEC =
            new Codec<>() {
                @Override
                public BackOrbitFollowController decode(org.bson.BsonValue bsonValue, ExtraInfo extraInfo) {
                    Params p = PARAMS_CODEC.decode(bsonValue, extraInfo);
                    return p == null ? null : p.build();
                }

                @Override
                public org.bson.BsonValue encode(BackOrbitFollowController value, ExtraInfo extraInfo) {
                    // No necesitas encode para assets normalmente; puedes dejarlo vacío si no lo usas.
                    return org.bson.BsonNull.VALUE;
                }

                @Override
                public com.hypixel.hytale.codec.schema.config.Schema toSchema(com.hypixel.hytale.codec.schema.SchemaContext context) {
                    return PARAMS_CODEC.toSchema(context);
                }
            };
}
