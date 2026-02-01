package me.s3b4s5.summonlib.assets.config;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.NpcSummonConfigCodec;
import me.s3b4s5.summonlib.internal.impl.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonTuning;
import me.s3b4s5.summonlib.internal.impl.spawn.NpcRoleSummonSpawnFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NpcSummonConfig extends SummonConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Npc";

    @Nonnull
    public static final BuilderCodec<NpcSummonConfig> ABSTRACT_CODEC = NpcSummonConfigCodec.create();

    // Required
    public String npcRoleId = "";

    // Defaults (NOT exposed in JSON; keep behavior stable)
    public boolean applySeparation = true;
    public int despawnTimerSeconds = 300;

    public float initialModelScaleOverride = 0f;
    public boolean debugSpawnFactory = false;

    @Nullable
    public SummonDefinition toDefinition() {
        if (npcRoleId == null || npcRoleId.isEmpty()) return null;

        SummonTuning t = SummonTuning.orDefault(tuning);

        return new NpcRoleSummonDefinition(
                getId(),
                slotCost,
                npcRoleId,
                applySeparation,
                despawnTimerSeconds,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                null, // follow does not apply
                new NpcRoleSummonSpawnFactory(npcRoleId, initialModelScaleOverride, debugSpawnFactory),
                t
        );
    }
}
