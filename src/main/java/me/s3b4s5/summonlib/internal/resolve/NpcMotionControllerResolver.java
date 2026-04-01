package me.s3b4s5.summonlib.internal.resolve;

import me.s3b4s5.summonlib.assets.config.npc.NpcSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionControllerConfig;
import me.s3b4s5.summonlib.assets.store.util.AssetMapUtil;

import javax.annotation.Nullable;

/**
 * Resolves NPC motion controller asset references for runtime use.
 */
public final class NpcMotionControllerResolver {

    private NpcMotionControllerResolver() {
    }

    public static @Nullable NpcMotionControllerConfig resolve(@Nullable NpcSummonConfig config) {
        if (config == null) {
            return null;
        }
        return resolve(config.npcMotionControllerId);
    }

    public static @Nullable NpcMotionControllerConfig resolve(@Nullable String motionControllerId) {
        if (motionControllerId == null || motionControllerId.isEmpty()) {
            return null;
        }

        NpcMotionControllerConfig config = AssetMapUtil.getByKey(NpcMotionControllerConfig.getAssetMap(), motionControllerId);
        if (config == null || config.isUnknown()) {
            return null;
        }

        return config;
    }
}


