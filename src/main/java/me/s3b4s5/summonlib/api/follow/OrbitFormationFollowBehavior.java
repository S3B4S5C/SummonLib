package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Stable built-in follow behavior that arranges summons in an arc behind the owner.
 */
public final class OrbitFormationFollowBehavior implements ModelFollowBehavior, OwnerPitchClamp {

    private final double baseBack;
    private final double radius;
    private final double spreadDeg;
    private final double baseHeight;
    private final double orbitRadius;
    private final double attackHeight;
    private final double minPitchRad;
    private final double maxPitchRad;

    public OrbitFormationFollowBehavior(
            double baseBack,
            double radius,
            double spreadDeg,
            double baseHeight,
            double orbitRadius,
            double attackHeight
    ) {
        this(baseBack, radius, spreadDeg, baseHeight, orbitRadius, attackHeight, -0.6, 0.55);
    }

    public OrbitFormationFollowBehavior(
            double baseBack,
            double radius,
            double spreadDeg,
            double baseHeight,
            double orbitRadius,
            double attackHeight,
            double minPitchRad,
            double maxPitchRad
    ) {
        this.baseBack = baseBack;
        this.radius = radius;
        this.spreadDeg = spreadDeg;
        this.baseHeight = baseHeight;
        this.orbitRadius = orbitRadius;
        this.attackHeight = attackHeight;
        this.minPitchRad = Math.min(minPitchRad, maxPitchRad);
        this.maxPitchRad = Math.max(minPitchRad, maxPitchRad);
    }

    @Override
    public double clampOwnerPitch(double pitchRad) {
        return FollowBehaviorSupport.clampPitch(pitchRad, minPitchRad, maxPitchRad);
    }

    @Override
    public Vector3d computeHome(Vector3d ownerPos, double yawRad, int groupIndex, int groupTotal) {
        FollowBehaviorSupport.YawBasis basis = FollowBehaviorSupport.yawBasis(yawRad);

        double t01 = FollowBehaviorSupport.normalizedIndex01(groupIndex, groupTotal, 0.5);
        double angleRad = Math.toRadians(-spreadDeg / 2.0 + spreadDeg * t01);

        double side = Math.sin(angleRad) * radius;

        double halfSpreadRad = Math.toRadians(spreadDeg / 2.0);
        double minCos = Math.cos(halfSpreadRad);
        double denom = 1.0 - minCos;

        double bump = denom <= 0.0
                ? 0.0
                : Math.clamp((Math.cos(angleRad) - minCos) / denom, 0.0, 1.0);

        double back = baseBack + bump * (radius * 0.6);

        return new Vector3d(
                ownerPos.x + (-basis.fx()) * back + basis.rx() * side,
                ownerPos.y + baseHeight,
                ownerPos.z + (-basis.fz()) * back + basis.rz() * side
        );
    }

    @Override
    public Vector3d computeAttackAnchor(Vector3d targetPos, int globalIndex, int globalTotal) {
        return FollowBehaviorSupport.computeCircularAttackAnchor(
                targetPos,
                globalIndex,
                globalTotal,
                orbitRadius,
                attackHeight
        );
    }
}