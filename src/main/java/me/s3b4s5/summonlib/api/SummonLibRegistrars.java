package me.s3b4s5.summonlib.api;

import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.FollowConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.OrbitFollowConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.WingFollowConfig;
import me.s3b4s5.summonlib.assets.config.npc.NpcSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.FlyNpcMotionControllerConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionControllerConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.WalkNpcMotionControllerConfig;
import me.s3b4s5.summonlib.interaction.SummonCastInteraction;
import me.s3b4s5.summonlib.interaction.SummonClearSummonsInteraction;
import me.s3b4s5.summonlib.interaction.SummonRemoveLastInteraction;

import javax.annotation.Nonnull;

/**
 * Public grouped helpers for registering SummonLib's built-in extension types.
 *
 * <p>These methods are useful both for SummonLib itself and for side-mods that
 * want to mirror the library's startup order in tests or custom bootstrap
 * flows.</p>
 */
public final class SummonLibRegistrars {

    private SummonLibRegistrars() {
    }

    /**
     * Registers all built-in asset subtypes supported by the library.
     */
    public static void registerBuiltinTypes(@Nonnull SummonLibRegistration registration) {
        registerBuiltinFollowTypes(registration);
        registerBuiltinNpcMotionControllerTypes(registration);
        registerBuiltinSummonConfigTypes(registration);
    }

    /**
     * Registers built-in follow asset types.
     */
    public static void registerBuiltinFollowTypes(@Nonnull SummonLibRegistration registration) {
        registration.registerFollowConfigType(
                OrbitFollowConfig.ASSET_TYPE_ID,
                OrbitFollowConfig.class,
                OrbitFollowConfig.ABSTRACT_CODEC
        );
        registration.registerFollowConfigType(
                WingFollowConfig.ASSET_TYPE_ID,
                WingFollowConfig.class,
                WingFollowConfig.ABSTRACT_CODEC
        );
    }

    /**
     * Registers built-in NPC motion controller asset types.
     */
    public static void registerBuiltinNpcMotionControllerTypes(@Nonnull SummonLibRegistration registration) {
        registration.registerNpcMotionControllerType(
                FlyNpcMotionControllerConfig.ASSET_TYPE_ID,
                FlyNpcMotionControllerConfig.class,
                FlyNpcMotionControllerConfig.ABSTRACT_CODEC
        );
        registration.registerNpcMotionControllerType(
                WalkNpcMotionControllerConfig.ASSET_TYPE_ID,
                WalkNpcMotionControllerConfig.class,
                WalkNpcMotionControllerConfig.ABSTRACT_CODEC
        );
    }

    /**
     * Registers built-in summon config asset types.
     */
    public static void registerBuiltinSummonConfigTypes(@Nonnull SummonLibRegistration registration) {
        registration.registerSummonConfigType(
                ModelSummonConfig.ASSET_TYPE_ID,
                ModelSummonConfig.class,
                ModelSummonConfig.ABSTRACT_CODEC
        );
        registration.registerSummonConfigType(
                NpcSummonConfig.ASSET_TYPE_ID,
                NpcSummonConfig.class,
                NpcSummonConfig.ABSTRACT_CODEC
        );
    }

    /**
     * Registers built-in interactions shipped by SummonLib.
     */
    public static void registerBuiltinInteractions(@Nonnull SummonLibRegistration registration) {
        registration.registerInteraction("SummonCast", SummonCastInteraction.class, SummonCastInteraction.CODEC);
        registration.registerInteraction("SummonRemoveLast", SummonRemoveLastInteraction.class, SummonRemoveLastInteraction.CODEC);
        registration.registerInteraction("SummonClearSummons", SummonClearSummonsInteraction.class, SummonClearSummonsInteraction.CODEC);
    }
}


