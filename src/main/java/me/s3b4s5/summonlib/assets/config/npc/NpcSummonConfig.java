package me.s3b4s5.summonlib.assets.config.npc;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import me.s3b4s5.summonlib.assets.codec.npc.NpcSummonConfigCodec;
import me.s3b4s5.summonlib.assets.config.SummonConfig;

import javax.annotation.Nonnull;

public final class NpcSummonConfig extends SummonConfig {
    /**
     * Declarative asset for an NPC-role-based summon.
     *
     * <p>This config references an NPC role and optionally an NPC motion
     * controller asset via {@link #npcMotionControllerId}.</p>
     */

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
}


