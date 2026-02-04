package me.s3b4s5.summonlib.assets.config.npc.motion;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.npc.motion.WalkNpcMotionControllerCodec;

import javax.annotation.Nonnull;

public final class WalkNpcMotionControllerConfig extends NpcMotionController {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Walk";

    // Walk has no Y/vertical config for now.

    @Nonnull
    public static final BuilderCodec<WalkNpcMotionControllerConfig> ABSTRACT_CODEC;

    static {
        var b = BuilderCodec.builder(WalkNpcMotionControllerConfig.class, WalkNpcMotionControllerConfig::new);
        WalkNpcMotionControllerCodec.appendWalkFields(b);

        ABSTRACT_CODEC = b.afterDecode((o, extra) -> {
            if (o.id == null) o.id = "";
        }).build();
    }
}
