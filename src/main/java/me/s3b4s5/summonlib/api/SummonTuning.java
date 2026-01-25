package me.s3b4s5.summonlib.api;

import javax.annotation.Nullable;

/**
 * Tuning opcional por invocación (por SummonDefinition).
 * Defaults iguales a los hardcodes actuales del SummonCombatFollowSystem.
 */
public final class SummonTuning {

    // (1) Movement
    public final double followSpeed;            // volver a home (no-NPC)
    public final double travelToTargetSpeed;    // ir a anchor (NPC y no-NPC)
    public final double hitDistance;            // rango real para golpear

    // (2) Cadence / timings
    public final float hitDamageDelaySec;       // delay entre “en rango” y daño
    public final float attackIntervalSec;       // cooldown entre ataques
    public final boolean keepAttackWhileHasTarget;

    // Optional: stagger (si no lo quieres, lo dejas true por defecto y listo)
    public final boolean staggerAttacks;
    public final float staggerScale;            // 1.0 => igual que antes

    // (3) Leashes
    public final double leashSummonToOwner;
    public final double leashTargetToOwner;

    // (4) Hover
    public final double hoverAboveOwner;
    public final double maxAboveOwner;

    // (7) Performance
    public final float ownerMaintenanceCooldownSec;

    public static final SummonTuning DEFAULT = builder().build();

    private SummonTuning(Builder b) {
        this.followSpeed = b.followSpeed;
        this.travelToTargetSpeed = b.travelToTargetSpeed;
        this.hitDistance = b.hitDistance;

        this.hitDamageDelaySec = b.hitDamageDelaySec;
        this.attackIntervalSec = b.attackIntervalSec;
        this.keepAttackWhileHasTarget = b.keepAttackWhileHasTarget;

        this.staggerAttacks = b.staggerAttacks;
        this.staggerScale = b.staggerScale;

        this.leashSummonToOwner = b.leashSummonToOwner;
        this.leashTargetToOwner = b.leashTargetToOwner;

        this.hoverAboveOwner = b.hoverAboveOwner;
        this.maxAboveOwner = b.maxAboveOwner;

        this.ownerMaintenanceCooldownSec = b.ownerMaintenanceCooldownSec;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SummonTuning orDefault(@Nullable SummonTuning t) {
        return (t != null) ? t : DEFAULT;
    }

    public static final class Builder {
        // Defaults
        private double followSpeed = 16.0;
        private double travelToTargetSpeed = 10.0;
        private double hitDistance = 1.2;

        private float hitDamageDelaySec = 0.14f;
        private float attackIntervalSec = 0.45f;
        private boolean keepAttackWhileHasTarget = true;

        private boolean staggerAttacks = true;
        private float staggerScale = 1.0f;

        private double leashSummonToOwner = 10.0;
        private double leashTargetToOwner = 8.0;

        private double hoverAboveOwner = 6.0;
        private double maxAboveOwner = 10.0;

        private float ownerMaintenanceCooldownSec = 0.35f;

        public Builder followSpeed(double v) { this.followSpeed = v; return this; }
        public Builder travelToTargetSpeed(double v) { this.travelToTargetSpeed = v; return this; }
        public Builder hitDistance(double v) { this.hitDistance = v; return this; }

        public Builder hitDamageDelaySec(float v) { this.hitDamageDelaySec = v; return this; }
        public Builder attackIntervalSec(float v) { this.attackIntervalSec = v; return this; }
        public Builder keepAttackWhileHasTarget(boolean v) { this.keepAttackWhileHasTarget = v; return this; }

        public Builder staggerAttacks(boolean v) { this.staggerAttacks = v; return this; }
        public Builder staggerScale(float v) { this.staggerScale = v; return this; }

        public Builder leashSummonToOwner(double v) { this.leashSummonToOwner = v; return this; }
        public Builder leashTargetToOwner(double v) { this.leashTargetToOwner = v; return this; }

        public Builder hoverAboveOwner(double v) { this.hoverAboveOwner = v; return this; }
        public Builder maxAboveOwner(double v) { this.maxAboveOwner = v; return this; }

        public Builder ownerMaintenanceCooldownSec(float v) { this.ownerMaintenanceCooldownSec = v; return this; }

        public SummonTuning build() {
            // Pequeñas defensas
            if (hitDistance < 0.01) hitDistance = 0.01;
            if (attackIntervalSec < 0.01f) attackIntervalSec = 0.01f;
            if (ownerMaintenanceCooldownSec < 0.05f) ownerMaintenanceCooldownSec = 0.05f;
            if (maxAboveOwner < hoverAboveOwner) maxAboveOwner = hoverAboveOwner;
            if (staggerScale < 0f) staggerScale = 0f;
            return new SummonTuning(this);
        }
    }
}
