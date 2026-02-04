package me.s3b4s5.summonlib.assets.codec.npc;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import me.s3b4s5.summonlib.assets.codec.SummonCommonCodec;
import me.s3b4s5.summonlib.assets.config.npc.NpcSummonConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionController;

public final class NpcSummonConfigCodec {

    private NpcSummonConfigCodec() {}

    public static final Codec<String> NPC_MOTION_CONTROLLER_REF =
            new ContainedAssetCodec<>(NpcMotionController.class, NpcMotionController.CODEC);

    public static BuilderCodec<NpcSummonConfig> create() {
        var b = BuilderCodec.builder(NpcSummonConfig.class, NpcSummonConfig::new);

        SummonCommonCodec.appendCommon(b);

        b.appendInherited(new KeyedCodec<>("NpcRoleId", Codec.STRING),
                (o, v) -> o.npcRoleId = (v == null ? "" : v),
                (o) -> o.npcRoleId,
                (o, p) -> o.npcRoleId = p.npcRoleId
        ).add();

        b.appendInherited(new KeyedCodec<>("MotionController", NPC_MOTION_CONTROLLER_REF),
                (o, v) -> o.npcMotionControllerId = (v == null ? "" : v),
                (o) -> o.npcMotionControllerId,
                (o, p) -> o.npcMotionControllerId = p.npcMotionControllerId
        ).add();

        b.appendInherited(new KeyedCodec<>("FormationEnabled", Codec.BOOLEAN),
                (o, v) -> o.formationEnabled = (v == null ? o.formationEnabled : v),
                (o) -> o.formationEnabled,
                (o, p) -> o.formationEnabled = p.formationEnabled
        ).add();

        b.appendInherited(new KeyedCodec<>("FormationBaseRadius", Codec.DOUBLE),
                (o, v) -> o.formationBaseRadius = (v == null ? o.formationBaseRadius : v),
                (o) -> o.formationBaseRadius,
                (o, p) -> o.formationBaseRadius = p.formationBaseRadius
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationRingStep", Codec.DOUBLE),
                (o, v) -> o.formationRingStep = (v == null ? o.formationRingStep : v),
                (o) -> o.formationRingStep,
                (o, p) -> o.formationRingStep = p.formationRingStep
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationRingCapacity", Codec.INTEGER),
                (o, v) -> o.formationRingCap = (v == null ? o.formationRingCap : v),
                (o) -> o.formationRingCap,
                (o, p) -> o.formationRingCap = p.formationRingCap
        ).addValidator(Validators.greaterThanOrEqual(1)).add();

        b.appendInherited(new KeyedCodec<>("FormationJitter", Codec.DOUBLE),
                (o, v) -> o.formationJitter = (v == null ? o.formationJitter : v),
                (o) -> o.formationJitter,
                (o, p) -> o.formationJitter = p.formationJitter
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationMaxRadius", Codec.DOUBLE),
                (o, v) -> o.formationMaxRadius = (v == null ? o.formationMaxRadius : v),
                (o) -> o.formationMaxRadius,
                (o, p) -> o.formationMaxRadius = p.formationMaxRadius
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationMinMoveDistance", Codec.DOUBLE),
                (o, v) -> o.formationMinMoveDist = (v == null ? o.formationMinMoveDist : v),
                (o) -> o.formationMinMoveDist,
                (o, p) -> o.formationMinMoveDist = p.formationMinMoveDist
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationMaxTurnSpeed", Codec.DOUBLE),
                (o, v) -> o.formationMaxTurnSpeed = (v == null ? o.formationMaxTurnSpeed : v),
                (o) -> o.formationMaxTurnSpeed,
                (o, p) -> o.formationMaxTurnSpeed = p.formationMaxTurnSpeed
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationYawSmoothK", Codec.DOUBLE),
                (o, v) -> o.formationYawSmoothK = (v == null ? o.formationYawSmoothK : v),
                (o) -> o.formationYawSmoothK,
                (o, p) -> o.formationYawSmoothK = p.formationYawSmoothK
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationAnchorDeadzone", Codec.DOUBLE),
                (o, v) -> o.formationAnchorDeadzone = (v == null ? o.formationAnchorDeadzone : v),
                (o) -> o.formationAnchorDeadzone,
                (o, p) -> o.formationAnchorDeadzone = p.formationAnchorDeadzone
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationAnchorSmoothK", Codec.DOUBLE),
                (o, v) -> o.formationAnchorSmoothK = (v == null ? o.formationAnchorSmoothK : v),
                (o) -> o.formationAnchorSmoothK,
                (o, p) -> o.formationAnchorSmoothK = p.formationAnchorSmoothK
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationOffsetSmoothK", Codec.DOUBLE),
                (o, v) -> o.formationOffsetSmoothK = (v == null ? o.formationOffsetSmoothK : v),
                (o) -> o.formationOffsetSmoothK,
                (o, p) -> o.formationOffsetSmoothK = p.formationOffsetSmoothK
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationOffsetMaxSpeed", Codec.DOUBLE),
                (o, v) -> o.formationOffsetMaxSpeed = (v == null ? o.formationOffsetMaxSpeed : v),
                (o) -> o.formationOffsetMaxSpeed,
                (o, p) -> o.formationOffsetMaxSpeed = p.formationOffsetMaxSpeed
        ).addValidator(Validators.greaterThanOrEqual(0.0)).add();

        b.appendInherited(new KeyedCodec<>("FormationRebuildIntervalSeconds", Codec.FLOAT),
                (o, v) -> o.formationRebuildIntervalSec = (v == null ? o.formationRebuildIntervalSec : v),
                (o) -> o.formationRebuildIntervalSec,
                (o, p) -> o.formationRebuildIntervalSec = p.formationRebuildIntervalSec
        ).addValidator(Validators.greaterThanOrEqual(0.0f)).add();

        b.afterDecode((o, extra) -> {
            if (o.id == null) o.id = "";
            if (o.npcRoleId == null) o.npcRoleId = "";
            if (o.npcMotionControllerId == null) o.npcMotionControllerId = "";

            if (o.leashSummonToOwner < 0) o.leashSummonToOwner = 0;
            if (o.leashTargetToOwner < 0) o.leashTargetToOwner = 0;
            if (o.ownerMaintenanceCooldownSec < 0) o.ownerMaintenanceCooldownSec = 0;

            if (o.formationBaseRadius < 0) o.formationBaseRadius = 0;
            if (o.formationRingStep < 0) o.formationRingStep = 0;
            if (o.formationRingCap < 1) o.formationRingCap = 1;
            if (o.formationJitter < 0) o.formationJitter = 0;
            if (o.formationMaxRadius < 0) o.formationMaxRadius = 0;

            if (o.formationMinMoveDist < 0) o.formationMinMoveDist = 0;
            if (o.formationMaxTurnSpeed < 0) o.formationMaxTurnSpeed = 0;
            if (o.formationYawSmoothK < 0) o.formationYawSmoothK = 0;

            if (o.formationAnchorDeadzone < 0) o.formationAnchorDeadzone = 0;
            if (o.formationAnchorSmoothK < 0) o.formationAnchorSmoothK = 0;

            if (o.formationOffsetSmoothK < 0) o.formationOffsetSmoothK = 0;
            if (o.formationOffsetMaxSpeed < 0) o.formationOffsetMaxSpeed = 0;

            if (o.formationRebuildIntervalSec < 0) o.formationRebuildIntervalSec = 0;
        });

        return b.build();
    }
}
