package me.s3b4s5.summonlib.internal.impl.definition;

import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.internal.impl.spawn.ModelSummonSpawnFactory;

import java.util.function.IntFunction;

public final class ModelSummonDefinition extends SummonDefinition {

    public final IntFunction<String> modelAssetByVariant;
    public final float modelScale;

    public final double followSpeed;
    public final double travelToTargetSpeed;

    public final double hitDistance;
    public final float hitDamageDelaySec;
    public final float attackIntervalSec;
    public final boolean keepAttackWhileHasTarget;

    public ModelSummonDefinition(
            String id,
            int slotCost,
            IntFunction<String> modelAssetByVariant,
            float modelScale,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            double leashSummonToOwner,
            double leashTargetToOwner,
            float ownerMaintenanceCooldownSec,
            ModelFollowController followController,
            double followSpeed,
            double travelToTargetSpeed,
            double hitDistance,
            float hitDamageDelaySec,
            float attackIntervalSec,
            boolean keepAttackWhileHasTarget
    ) {
        super(
                id,
                slotCost,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                leashSummonToOwner,
                leashTargetToOwner,
                ownerMaintenanceCooldownSec,
                followController,
                new ModelSummonSpawnFactory(modelAssetByVariant, modelScale)
        );

        this.modelAssetByVariant = modelAssetByVariant;
        this.modelScale = modelScale;

        this.followSpeed = Math.max(0.0, followSpeed);
        this.travelToTargetSpeed = Math.max(0.0, travelToTargetSpeed);

        this.hitDistance = Math.max(0.01, hitDistance);
        this.hitDamageDelaySec = Math.max(0.0f, hitDamageDelaySec);
        this.attackIntervalSec = Math.max(0.01f, attackIntervalSec);
        this.keepAttackWhileHasTarget = keepAttackWhileHasTarget;
    }
}
