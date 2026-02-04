package me.s3b4s5.summonlib.internal.impl.definition;

import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.internal.impl.spawn.SummonSpawnFactory;
import me.s3b4s5.summonlib.internal.impl.spawn.SummonSpawnPlanFactory;

import javax.annotation.Nullable;

public abstract class SummonDefinition {

    public final String id;
    public final int slotCost;

    public final float damage;
    public final double detectRadius;

    public final boolean requireOwnerLoS;
    public final boolean requireSummonLoS;

    public final double leashSummonToOwner;
    public final double leashTargetToOwner;
    public final float ownerMaintenanceCooldownSec;

    @Nullable public final ModelFollowController followController;

    @Nullable public final SummonSpawnFactory summonSpawnFactory;
    @Nullable public final SummonSpawnPlanFactory summonSpawnPlanFactory;

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            double leashSummonToOwner,
            double leashTargetToOwner,
            float ownerMaintenanceCooldownSec,
            @Nullable ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory
    ) {
        this(id, slotCost, damage, detectRadius, requireOwnerLoS, requireSummonLoS,
                leashSummonToOwner, leashTargetToOwner, ownerMaintenanceCooldownSec,
                followController, summonSpawnFactory, null);
    }

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            double leashSummonToOwner,
            double leashTargetToOwner,
            float ownerMaintenanceCooldownSec,
            @Nullable ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory,
            @Nullable SummonSpawnPlanFactory summonSpawnPlanFactory
    ) {
        this.id = id;
        this.slotCost = slotCost;
        this.damage = damage;
        this.detectRadius = detectRadius;
        this.requireOwnerLoS = requireOwnerLoS;
        this.requireSummonLoS = requireSummonLoS;

        this.leashSummonToOwner = Math.max(0.0, leashSummonToOwner);
        this.leashTargetToOwner = Math.max(0.0, leashTargetToOwner);
        this.ownerMaintenanceCooldownSec = Math.max(0.0f, ownerMaintenanceCooldownSec);

        this.followController = followController;
        this.summonSpawnFactory = summonSpawnFactory;
        this.summonSpawnPlanFactory = summonSpawnPlanFactory;
    }
}
