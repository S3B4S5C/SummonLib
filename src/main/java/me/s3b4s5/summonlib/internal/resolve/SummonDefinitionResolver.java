package me.s3b4s5.summonlib.internal.resolve;

import me.s3b4s5.summonlib.api.follow.ModelFollowBehavior;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.model.ModelSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.NpcSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionControllerConfig;
import me.s3b4s5.summonlib.internal.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.spawn.NpcRoleSummonSpawnFactory;

import javax.annotation.Nullable;

/**
 * Builds runtime summon definitions from declarative asset config objects.
 */
public final class SummonDefinitionResolver {

    private SummonDefinitionResolver() {
    }

    public static @Nullable SummonDefinition resolve(@Nullable SummonConfig config) {
        return switch (config) {
            case ModelSummonConfig model -> resolve(model);
            case NpcSummonConfig npc -> resolve(npc);
            case null, default -> null;
        };
    }

    public static @Nullable SummonDefinition resolve(@Nullable ModelSummonConfig config) {
        if (config == null) {
            return null;
        }

        String[] assets = config.modelAssets == null ? new String[0] : config.modelAssets;
        float scale = config.modelScale <= 0f ? 1.0f : config.modelScale;
        ModelFollowBehavior followController = FollowControllerResolver.resolve(config);

        return new ModelSummonDefinition(
                config.getId(),
                config.slotCost,
                variantIndex -> {
                    if (assets.length == 0) {
                        return "";
                    }
                    return assets[Math.floorMod(variantIndex, assets.length)];
                },
                scale,
                config.damage,
                config.detectRadius,
                config.requireOwnerLoS,
                config.requireSummonLoS,
                config.leashSummonToOwner,
                config.leashTargetToOwner,
                config.ownerMaintenanceCooldownSec,
                followController,
                config.followSpeed,
                config.travelToTargetSpeed,
                config.hitDistance,
                config.hitDamageDelaySec,
                config.attackIntervalSec,
                config.keepAttackWhileHasTarget
        );
    }

    public static @Nullable SummonDefinition resolve(@Nullable NpcSummonConfig config) {
        if (config == null || config.npcRoleId == null || config.npcRoleId.isEmpty()) {
            return null;
        }

        NpcMotionControllerConfig motionController = NpcMotionControllerResolver.resolve(config);

        return new NpcRoleSummonDefinition(
                config.getId(),
                config.slotCost,
                config.npcRoleId,
                config.damage,
                config.detectRadius,
                config.requireOwnerLoS,
                config.requireSummonLoS,
                config.leashSummonToOwner,
                config.leashTargetToOwner,
                config.ownerMaintenanceCooldownSec,
                new NpcRoleSummonSpawnFactory(config.npcRoleId, config.initialModelScaleOverride, config.debugSpawnFactory),
                motionController,
                new NpcRoleSummonDefinition.Formation(
                        config.formationEnabled,
                        config.formationBaseRadius,
                        config.formationRingStep,
                        config.formationRingCap,
                        config.formationJitter,
                        config.formationMaxRadius,
                        config.formationMinMoveDist,
                        config.formationMaxTurnSpeed,
                        config.formationYawSmoothK,
                        config.formationAnchorDeadzone,
                        config.formationAnchorSmoothK,
                        config.formationOffsetSmoothK,
                        config.formationOffsetMaxSpeed,
                        config.formationRebuildIntervalSec
                )
        );
    }
}


