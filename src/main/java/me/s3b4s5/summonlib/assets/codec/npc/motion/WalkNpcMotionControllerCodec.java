package me.s3b4s5.summonlib.assets.codec.npc.motion;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.config.npc.motion.WalkNpcMotionControllerConfig;

/**
 * Walk motion controller has no vertical/Y config for now.
 */
public final class WalkNpcMotionControllerCodec {

    private WalkNpcMotionControllerCodec() {}

    public static BuilderCodec.Builder<WalkNpcMotionControllerConfig> appendWalkFields(
            BuilderCodec.Builder<WalkNpcMotionControllerConfig> b
    ) {
        return b;
    }
}


