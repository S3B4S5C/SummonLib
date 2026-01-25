package me.s3b4s5.summonlib.api;

import me.s3b4s5.summonlib.api.follow.ModelFollowController;

import javax.annotation.Nullable;

public final class NpcRoleSummonDefinition extends SummonDefinition {

    public final String npcRoleId;
    public final boolean applySeparation;
    public final int despawnTimerSeconds;

    public NpcRoleSummonDefinition(
            String id,
            int slotCost,
            String npcRoleId,
            boolean applySeparation,
            int despawnTimerSeconds,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            ModelFollowController followController,
            SummonSpawnFactory summonSpawnFactory
    ) {
        super(
                id,
                slotCost,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                followController,
                summonSpawnFactory
        );

        this.npcRoleId = npcRoleId;
        this.applySeparation = applySeparation;
        this.despawnTimerSeconds = despawnTimerSeconds;
    }

    public NpcRoleSummonDefinition(
            String id,
            int slotCost,
            String npcRoleId,
            boolean applySeparation,
            int despawnTimerSeconds,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            ModelFollowController followController,
            SummonSpawnFactory summonSpawnFactory,
            @Nullable SummonTuning tuning
    ) {
        super(
                id,
                slotCost,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                followController,
                summonSpawnFactory,
                tuning
        );

        this.npcRoleId = npcRoleId;
        this.applySeparation = applySeparation;
        this.despawnTimerSeconds = despawnTimerSeconds;
    }

}
