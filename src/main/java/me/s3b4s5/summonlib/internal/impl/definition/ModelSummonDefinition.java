package me.s3b4s5.summonlib.internal.impl.definition;

import me.s3b4s5.summonlib.internal.impl.spawn.ModelSummonSpawnFactory;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;

import javax.annotation.Nullable;
import java.util.function.IntFunction;

public final class ModelSummonDefinition extends SummonDefinition {

    public final IntFunction<String> modelAssetByVariant;
    public final float modelScale;

    public ModelSummonDefinition(
            String id,
            int slotCost,
            IntFunction<String> modelAssetByVariant,
            float modelScale,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            ModelFollowController followController
    ) {
        // default spawner for model summons
        super(
                id,
                slotCost,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                followController,
                new ModelSummonSpawnFactory(modelAssetByVariant, modelScale)
        );

        this.modelAssetByVariant = modelAssetByVariant;
        this.modelScale = modelScale;
    }

    public ModelSummonDefinition(
            String id,
            int slotCost,
            IntFunction<String> modelAssetByVariant,
            float modelScale,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            ModelFollowController followController,
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
                new ModelSummonSpawnFactory(modelAssetByVariant, modelScale),
                tuning
        );
        this.modelAssetByVariant = modelAssetByVariant;
        this.modelScale = modelScale;
    }

}
