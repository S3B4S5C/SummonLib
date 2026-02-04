//package me.s3b4s5.summonlib.internal.impl.definition;
//
//import com.hypixel.hytale.codec.Codec;
//import com.hypixel.hytale.codec.ExtraInfo;
//import com.hypixel.hytale.codec.KeyedCodec;
//import com.hypixel.hytale.codec.builder.BuilderCodec;
//
//import javax.annotation.Nullable;
//
///**
// * Tuning opcional por invocación (por SummonDefinition).
// * Defaults iguales a los hardcodes actuales del SummonCombatFollowSystem.
// *
// * Nota: esta versión es "asset friendly":
// * - campos mutables (public) para que el BuilderCodec escriba fácil
// * - herencia via Parent (appendInherited)
// * - afterDecode aplica defensas/clamps (igual a tu build() anterior)
// */
//public final class SummonTuning {
//
//    // (1) Movement
//    public double followSpeed = 16.0;            // volver a home (no-NPC)
//    public double travelToTargetSpeed = 10.0;    // ir a anchor (NPC y no-NPC)
//    public double hitDistance = 1.2;             // rango real para golpear
//
//    // (2) Cadence / timings
//    public float hitDamageDelaySec = 0.14f;      // delay entre “en rango” y daño
//    public float attackIntervalSec = 0.45f;      // cooldown entre ataques
//    public boolean keepAttackWhileHasTarget = true;
//
//    // Optional: stagger
//    public boolean staggerAttacks = true;
//    public float staggerScale = 1.0f;            // 1.0 => igual que antes
//
//    // (3) Leashes
//    public double leashSummonToOwner = 10.0;
//    public double leashTargetToOwner = 8.0;
//
//    // (4) Hover
//    public double hoverAboveOwner = 6.0;
//    public double maxAboveOwner = 10.0;
//
//    // (7) Performance
//    public float ownerMaintenanceCooldownSec = 0.35f;
//
//    public static final SummonTuning DEFAULT = new SummonTuning();
//
//    public static SummonTuning orDefault(@Nullable SummonTuning t) {
//        return (t == null) ? DEFAULT : t;
//    }
//
//    /** Aplica defensas/clamps igual que tu build(). */
//    private void sanitize() {
//        if (followSpeed < 0.0) followSpeed = 0.0;
//        if (travelToTargetSpeed < 0.0) travelToTargetSpeed = 0.0;
//
//        if (hitDistance < 0.01) hitDistance = 0.01;
//
//        if (hitDamageDelaySec < 0.0f) hitDamageDelaySec = 0.0f;
//
//        if (attackIntervalSec < 0.01f) attackIntervalSec = 0.01f;
//
//        if (ownerMaintenanceCooldownSec < 0.05f) ownerMaintenanceCooldownSec = 0.05f;
//
//        if (maxAboveOwner < hoverAboveOwner) {
//            maxAboveOwner = hoverAboveOwner;
//        }
//
//        if (staggerScale < 0f) staggerScale = 0f;
//    }
//
//    // ---------------------------------------
//    // CODEC (herencia + defaults + sanitize)
//    // ---------------------------------------
//    public static final BuilderCodec<SummonTuning> CODEC =
//            BuilderCodec.builder(SummonTuning.class, SummonTuning::new)
//
//                    // (1) Movement
//                    .appendInherited(new KeyedCodec<>("FollowSpeed", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.followSpeed = v; },
//                            (o) -> o.followSpeed,
//                            (o, p) -> o.followSpeed = p.followSpeed
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("TravelToTargetSpeed", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.travelToTargetSpeed = v; },
//                            (o) -> o.travelToTargetSpeed,
//                            (o, p) -> o.travelToTargetSpeed = p.travelToTargetSpeed
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("HitDistance", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.hitDistance = v; },
//                            (o) -> o.hitDistance,
//                            (o, p) -> o.hitDistance = p.hitDistance
//                    ).add()
//
//                    // (2) Cadence / timings
//                    .appendInherited(new KeyedCodec<>("HitDamageDelaySec", Codec.FLOAT),
//                            (o, v) -> { if (v != null) o.hitDamageDelaySec = v; },
//                            (o) -> o.hitDamageDelaySec,
//                            (o, p) -> o.hitDamageDelaySec = p.hitDamageDelaySec
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("AttackIntervalSec", Codec.FLOAT),
//                            (o, v) -> { if (v != null) o.attackIntervalSec = v; },
//                            (o) -> o.attackIntervalSec,
//                            (o, p) -> o.attackIntervalSec = p.attackIntervalSec
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("KeepAttackWhileHasTarget", Codec.BOOLEAN),
//                            (o, v) -> { if (v != null) o.keepAttackWhileHasTarget = v; },
//                            (o) -> o.keepAttackWhileHasTarget,
//                            (o, p) -> o.keepAttackWhileHasTarget = p.keepAttackWhileHasTarget
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("StaggerAttacks", Codec.BOOLEAN),
//                            (o, v) -> { if (v != null) o.staggerAttacks = v; },
//                            (o) -> o.staggerAttacks,
//                            (o, p) -> o.staggerAttacks = p.staggerAttacks
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("StaggerScale", Codec.FLOAT),
//                            (o, v) -> { if (v != null) o.staggerScale = v; },
//                            (o) -> o.staggerScale,
//                            (o, p) -> o.staggerScale = p.staggerScale
//                    ).add()
//
//                    // (3) Leashes
//                    .appendInherited(new KeyedCodec<>("LeashSummonToOwner", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.leashSummonToOwner = v; },
//                            (o) -> o.leashSummonToOwner,
//                            (o, p) -> o.leashSummonToOwner = p.leashSummonToOwner
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("LeashTargetToOwner", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.leashTargetToOwner = v; },
//                            (o) -> o.leashTargetToOwner,
//                            (o, p) -> o.leashTargetToOwner = p.leashTargetToOwner
//                    ).add()
//
//                    // (4) Hover
//                    .appendInherited(new KeyedCodec<>("HoverAboveOwner", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.hoverAboveOwner = v; },
//                            (o) -> o.hoverAboveOwner,
//                            (o, p) -> o.hoverAboveOwner = p.hoverAboveOwner
//                    ).add()
//
//                    .appendInherited(new KeyedCodec<>("MaxAboveOwner", Codec.DOUBLE),
//                            (o, v) -> { if (v != null) o.maxAboveOwner = v; },
//                            (o) -> o.maxAboveOwner,
//                            (o, p) -> o.maxAboveOwner = p.maxAboveOwner
//                    ).add()
//
//                    // (7) Performance
//                    .appendInherited(new KeyedCodec<>("OwnerMaintenanceCooldownSec", Codec.FLOAT),
//                            (o, v) -> { if (v != null) o.ownerMaintenanceCooldownSec = v; },
//                            (o) -> o.ownerMaintenanceCooldownSec,
//                            (o, p) -> o.ownerMaintenanceCooldownSec = p.ownerMaintenanceCooldownSec
//                    ).add()
//
//                    // Defensas post-decode (clamps)
//                    .afterDecode((SummonTuning o, ExtraInfo info) -> o.sanitize())
//
//                    .build();
//}
