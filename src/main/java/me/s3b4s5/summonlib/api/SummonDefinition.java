package me.s3b4s5.summonlib.api;

import me.s3b4s5.summonlib.api.follow.ModelFollowController;

import javax.annotation.Nullable;

public abstract class SummonDefinition {

    public final String id;
    public final int slotCost;

    public final float damage;
    public final double detectRadius;

    public final boolean requireOwnerLoS;
    public final boolean requireSummonLoS;

    public final ModelFollowController followController;

    /** Optional: if provided, SummonActions will use it to build the entity holder. */
    @Nullable
    public final SummonSpawnFactory summonSpawnFactory;

    /** NEW: per-summon tuning for follow/combat/leash/hover/perf. Never null. */
    public final SummonTuning tuning;

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory
    ) {
        this(id, slotCost, damage, detectRadius, requireOwnerLoS, requireSummonLoS, followController, summonSpawnFactory, null);
    }

    protected SummonDefinition(
            String id,
            int slotCost,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            ModelFollowController followController,
            @Nullable SummonSpawnFactory summonSpawnFactory,
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
        this.tuning = SummonTuning.orDefault(tuning);
    }
}
