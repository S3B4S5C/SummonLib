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

    @Nullable public final ModelFollowController followController;

    @Nullable public final SummonSpawnFactory summonSpawnFactory;
    @Nullable public final SummonSpawnPlanFactory summonSpawnPlanFactory;

    public final SummonTuning tuning;

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            @Nullable ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory
    ) {
        this(id, slotCost, damage, detectRadius, requireOwnerLoS, requireSummonLoS,
                followController, summonSpawnFactory, null, null);
    }

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            @Nullable ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory,
            @Nullable SummonTuning tuning
    ) {
        this(id, slotCost, damage, detectRadius, requireOwnerLoS, requireSummonLoS,
                followController, summonSpawnFactory, null, tuning);
    }

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            @Nullable ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory,
            @Nullable SummonSpawnPlanFactory summonSpawnPlanFactory,
            @Nullable SummonTuning tuning
    ) {
        this.id = id;
        this.slotCost = slotCost;
        this.damage = damage;
        this.detectRadius = detectRadius;
        this.requireOwnerLoS = requireOwnerLoS;
        this.requireSummonLoS = requireSummonLoS;
        this.followController = followController;
        this.summonSpawnFactory = summonSpawnFactory;
        this.summonSpawnPlanFactory = summonSpawnPlanFactory;
        this.tuning = SummonTuning.orDefault(tuning);
    }
}
