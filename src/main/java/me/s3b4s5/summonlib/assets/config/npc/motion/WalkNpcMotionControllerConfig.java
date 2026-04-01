package me.s3b4s5.summonlib.assets.config.npc.motion;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.npc.motion.WalkNpcMotionControllerCodec;

import javax.annotation.Nonnull;

public final class WalkNpcMotionControllerConfig extends NpcMotionControllerConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Walk";

    @Nonnull
    public static final BuilderCodec<WalkNpcMotionControllerConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(WalkNpcMotionControllerConfig.class, WalkNpcMotionControllerConfig::new);
        WalkNpcMotionControllerCodec.appendWalkFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, _) -> {
            if (o.id == null) o.id = "";
        }).build();
    }
}


