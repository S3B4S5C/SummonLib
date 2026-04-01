package me.s3b4s5.summonlib.internal.definition;

import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionControllerConfig;
import me.s3b4s5.summonlib.internal.spawn.SummonSpawnFactory;

import javax.annotation.Nullable;

public final class NpcRoleSummonDefinition extends SummonDefinition {

    public final String npcRoleId;

    @Nullable public final NpcMotionControllerConfig motionController;

    public final Formation formation;

    public NpcRoleSummonDefinition(
            String id,
            int slotCost,
            String npcRoleId,
            float damage,
            double detectRadius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            double leashSummonToOwner,
            double leashTargetToOwner,
            float ownerMaintenanceCooldownSec,
            SummonSpawnFactory summonSpawnFactory,
            @Nullable NpcMotionControllerConfig motionController,
            @Nullable Formation formation
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
                null,
                summonSpawnFactory
        );

        this.npcRoleId = (npcRoleId == null) ? "" : npcRoleId;
        this.motionController = motionController;
        this.formation = (formation == null) ? Formation.DEFAULT : formation;
    }

    public static final class Formation {

        public static final Formation DEFAULT = new Formation(
                true,
                1.85, 0.95, 8,
                0.10, 6.0,
                0.06,
                2.6, 10.0,
                0.12, 8.0,
                7.0, 3.0,
                0.70f
        );

        public final boolean enabled;

        public final double baseRadius;
        public final double ringStep;
        public final int ringCap;
        public final double jitter;
        public final double maxRadius;

        public final double minMoveDist;

        public final double maxTurnSpeed;
        public final double yawSmoothK;

        public final double anchorDeadzone;
        public final double anchorSmoothK;

        public final double offsetSmoothK;
        public final double offsetMaxSpeed;

        public final float rebuildIntervalSec;

        public Formation(
                boolean enabled,
                double baseRadius,
                double ringStep,
                int ringCap,
                double jitter,
                double maxRadius,
                double minMoveDist,
                double maxTurnSpeed,
                double yawSmoothK,
                double anchorDeadzone,
                double anchorSmoothK,
                double offsetSmoothK,
                double offsetMaxSpeed,
                float rebuildIntervalSec
        ) {
            this.enabled = enabled;

            this.baseRadius = Math.max(0.0, baseRadius);
            this.ringStep = Math.max(0.0, ringStep);
            this.ringCap = Math.max(1, ringCap);
            this.jitter = Math.max(0.0, jitter);
            this.maxRadius = Math.max(0.0, maxRadius);

            this.minMoveDist = Math.max(0.0, minMoveDist);

            this.maxTurnSpeed = Math.max(0.0, maxTurnSpeed);
            this.yawSmoothK = Math.max(0.0, yawSmoothK);

            this.anchorDeadzone = Math.max(0.0, anchorDeadzone);
            this.anchorSmoothK = Math.max(0.0, anchorSmoothK);

            this.offsetSmoothK = Math.max(0.0, offsetSmoothK);
            this.offsetMaxSpeed = Math.max(0.0, offsetMaxSpeed);

            this.rebuildIntervalSec = Math.max(0.0f, rebuildIntervalSec);
        }
    }
}


