package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.WormSummonConfigCodec;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;
import me.s3b4s5.summonlib.internal.impl.definition.WormSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.spawn.ModelSummonSpawnFactory;
import me.s3b4s5.summonlib.internal.impl.spawn.NpcRoleSummonSpawnFactory;
import me.s3b4s5.summonlib.internal.impl.spawn.WormSummonSpawner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WormSummonConfig extends SummonConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Worm";

    @Nonnull
    public static final BuilderCodec<WormSummonConfig> ABSTRACT_CODEC = WormSummonConfigCodec.create();

    // Head (NPC role)
    public String headRoleId = "";
    public float headInitialModelScaleOverride = 0f;

    // Body/Tail (models)
    public String[] bodyModelAssets = new String[0];
    public float bodyModelScale = 1.0f;

    public String[] tailModelAssets = new String[0];
    public float tailModelScale = 1.0f;

    // Spacing
    public double spacingHeadToBody = 1.2;
    public double spacingBodyToBody = 1.0;
    public double spacingBodyToTail = 1.1;

    // Debug (same idea as legacy wormDebugSpawnFactory, but no prefix)
    public boolean debugSpawnFactory = false;

    @Nullable
    public SummonDefinition toDefinition() {
        if (headRoleId == null || headRoleId.isEmpty()) return null;

        final String[] bodyAssets = (bodyModelAssets == null) ? new String[0] : bodyModelAssets;
        final String[] tailAssets = (tailModelAssets == null) ? new String[0] : tailModelAssets;

        if (bodyAssets.length == 0) return null;
        if (tailAssets.length == 0) return null;

        SummonTuning t = SummonTuning.orDefault(tuning);

        float bodyScale = (bodyModelScale <= 0f) ? 1.0f : bodyModelScale;
        float tailScale = (tailModelScale <= 0f) ? 1.0f : tailModelScale;

        double h2b = Math.max(0.0, spacingHeadToBody);
        double b2b = Math.max(0.0, spacingBodyToBody);
        double b2t = Math.max(0.0, spacingBodyToTail);

        var wcfg = new WormSummonSpawner.WormSpawnConfig(
                getId(),
                new NpcRoleSummonSpawnFactory(headRoleId, headInitialModelScaleOverride, debugSpawnFactory),
                new ModelSummonSpawnFactory((variantIndex) -> bodyAssets[Math.floorMod(variantIndex, bodyAssets.length)], bodyScale),
                new ModelSummonSpawnFactory((variantIndex) -> tailAssets[Math.floorMod(variantIndex, tailAssets.length)], tailScale),
                h2b,
                b2b,
                b2t
        );

        return new WormSummonDefinition(
                getId(),
                slotCost,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                null, // follow does not apply
                wcfg,
                t
        );
    }
}
