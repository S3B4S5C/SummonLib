package me.s3b4s5.summonlib.assets.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.config.BaseSummonConfig;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;

public final class SummonCommonCodec {

    private SummonCommonCodec() {}

    public static <T extends BaseSummonConfig> BuilderCodec.Builder<T> appendCommon(BuilderCodec.Builder<T> b) {
        return b
                .appendInherited(new KeyedCodec<>("SlotCost", Codec.INTEGER),
                        (o, v) -> o.slotCost = (v == null ? 1 : v),
                        (o) -> o.slotCost,
                        (o, p) -> o.slotCost = p.slotCost
                ).addValidator(Validators.greaterThanOrEqual(1)).add()

                .appendInherited(new KeyedCodec<>("Damage", Codec.FLOAT),
                        (o, v) -> o.damage = (v == null ? 0f : v),
                        (o) -> o.damage,
                        (o, p) -> o.damage = p.damage
                ).add()

                .appendInherited(new KeyedCodec<>("DetectRadius", Codec.DOUBLE),
                        (o, v) -> o.detectRadius = (v == null ? 0.0 : v),
                        (o) -> o.detectRadius,
                        (o, p) -> o.detectRadius = p.detectRadius
                ).addValidator(Validators.greaterThanOrEqual(0.0)).add()

                .appendInherited(new KeyedCodec<>("RequireOwnerLoS", Codec.BOOLEAN),
                        (o, v) -> o.requireOwnerLoS = (v == null ? true : v),
                        (o) -> o.requireOwnerLoS,
                        (o, p) -> o.requireOwnerLoS = p.requireOwnerLoS
                ).add()

                .appendInherited(new KeyedCodec<>("RequireSummonLoS", Codec.BOOLEAN),
                        (o, v) -> o.requireSummonLoS = (v == null ? true : v),
                        (o) -> o.requireSummonLoS,
                        (o, p) -> o.requireSummonLoS = p.requireSummonLoS
                ).add()

                .appendInherited(new KeyedCodec<>("Tuning", SummonTuning.CODEC),
                        (o, v) -> o.tuning = v,
                        (o) -> o.tuning,
                        (o, p) -> o.tuning = p.tuning
                ).add();
    }
}
