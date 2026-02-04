package me.s3b4s5.summonlib.assets.config.npc;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.npc.NpcSummonConfigCodec;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionController;
import me.s3b4s5.summonlib.assets.store.util.AssetMapUtil;
import me.s3b4s5.summonlib.internal.impl.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.impl.spawn.NpcRoleSummonSpawnFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NpcSummonConfig extends SummonConfig {

    @Nonnull
    public static final String ASSET_TYPE_ID = "Npc";

    @Nonnull
    public static final BuilderCodec<NpcSummonConfig> ABSTRACT_CODEC = NpcSummonConfigCodec.create();

    public String npcRoleId = "";
    public String npcMotionControllerId = "";

    public float initialModelScaleOverride = 0f;
    public boolean debugSpawnFactory = false;

    public boolean formationEnabled = true;

    public double formationBaseRadius = 1.85;
    public double formationRingStep = 0.95;
    public int formationRingCap = 8;
    public double formationJitter = 0.10;
    public double formationMaxRadius = 6.0;

    public double formationMinMoveDist = 0.06;

    public double formationMaxTurnSpeed = 2.6;
    public double formationYawSmoothK = 10.0;

    public double formationAnchorDeadzone = 0.12;
    public double formationAnchorSmoothK = 8.0;

    public double formationOffsetSmoothK = 7.0;
    public double formationOffsetMaxSpeed = 3.0;

    public float formationRebuildIntervalSec = 0.70f;

    @Nullable
    public SummonDefinition toDefinition() {
        if (npcRoleId == null || npcRoleId.isEmpty()) return null;

        final NpcMotionController mc = resolveMotionController();


        final var formation = new NpcRoleSummonDefinition.Formation(
                formationEnabled,
                formationBaseRadius,
                formationRingStep,
                formationRingCap,
                formationJitter,
                formationMaxRadius,
                formationMinMoveDist,
                formationMaxTurnSpeed,
                formationYawSmoothK,
                formationAnchorDeadzone,
                formationAnchorSmoothK,
                formationOffsetSmoothK,
                formationOffsetMaxSpeed,
                formationRebuildIntervalSec
        );

        return new NpcRoleSummonDefinition(
                getId(),
                slotCost,
                npcRoleId,
                damage,
                detectRadius,
                requireOwnerLoS,
                requireSummonLoS,
                leashSummonToOwner,
                leashTargetToOwner,
                ownerMaintenanceCooldownSec,
                new NpcRoleSummonSpawnFactory(npcRoleId, initialModelScaleOverride, debugSpawnFactory),
                mc,
                formation
        );
    }

    public @Nullable NpcMotionController resolveMotionController() {
        if (npcMotionControllerId == null || npcMotionControllerId.isEmpty()) return null;

        var map = NpcMotionController.getAssetMap();
        NpcMotionController cfg = AssetMapUtil.getByKey(map, npcMotionControllerId);

        if (cfg == null || cfg.isUnknown()) return null;
        return cfg;
    }
}
