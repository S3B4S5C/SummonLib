package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Stable built-in follow behavior for wing or back-line formations.
 */
public final class WingFormationFollowBehavior implements ModelFollowBehavior, OwnerPitchClamp, HomeRotationOffsets {

    public enum SideMode {
        LEFT_ONLY,
        RIGHT_ONLY,
        SYMMETRIC
    }

    private final double baseBack;
    private final double stepBack;
    private final double sideSpread;
    private final double baseHeight;
    private final double heightSpread;
    private final double heightCurvePow;

    private final double yawSpreadRad;
    private final double rollSpreadRad;
    private final double pitchSpreadRad;
    private final SideMode sideMode;

    private final double orbitRadius;
    private final double attackHeight;

    private final double minPitchRad;
    private final double maxPitchRad;

    private final int referenceTotal;

    public WingFormationFollowBehavior(
            double baseBack,
            double stepBack,
            double sideSpread,
            double baseHeight,
            double heightSpread,
            double heightCurvePow,
            double yawSpreadDeg,
            double rollSpreadDeg,
            double pitchSpreadDeg,
            SideMode sideMode,
            double orbitRadius,
            double attackHeight,
            double minPitchRad,
            double maxPitchRad,
            int referenceTotal
    ) {
        this.baseBack = baseBack;
        this.stepBack = stepBack;
        this.sideSpread = sideSpread;
        this.baseHeight = baseHeight;
        this.heightSpread = heightSpread;
        this.heightCurvePow = Math.max(0.01, heightCurvePow);

        this.yawSpreadRad = Math.toRadians(yawSpreadDeg);
        this.rollSpreadRad = Math.toRadians(rollSpreadDeg);
        this.pitchSpreadRad = Math.toRadians(pitchSpreadDeg);
        this.sideMode = sideMode != null ? sideMode : SideMode.LEFT_ONLY;

        this.orbitRadius = orbitRadius;
        this.attackHeight = attackHeight;

        this.minPitchRad = Math.min(minPitchRad, maxPitchRad);
        this.maxPitchRad = Math.max(minPitchRad, maxPitchRad);

        this.referenceTotal = Math.max(0, referenceTotal);
    }

    @Override
    public double clampOwnerPitch(double pitchRad) {
        return FollowBehaviorSupport.clampPitch(pitchRad, minPitchRad, maxPitchRad);
    }

    @Override
    public Vector3d computeHome(Vector3d ownerPos, double yawRad, int groupIndex, int groupTotal) {
        int effectiveCount = effectiveCount(groupTotal);
        int index = FollowBehaviorSupport.clampIndex(groupIndex, effectiveCount);
        double spreadParam = signedParam(index, effectiveCount);

        FollowBehaviorSupport.YawBasis basis = FollowBehaviorSupport.yawBasis(yawRad);

        double back = baseBack + stepBack * index;
        double side = spreadParam * sideSpread;

        double curve = 1.0 - Math.abs(spreadParam);
        curve = Math.pow(Math.max(0.0, curve), heightCurvePow);

        double x = ownerPos.x + (-basis.fx()) * back + basis.rx() * side;
        double y = ownerPos.y + baseHeight + heightSpread * curve;
        double z = ownerPos.z + (-basis.fz()) * back + basis.rz() * side;

        return new Vector3d(x, y, z);
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

    @Override
    public double homeYawOffsetRad(int groupIndex, int groupTotal) {
        return signedParam(groupIndex, effectiveCount(groupTotal)) * yawSpreadRad;
    }

    @Override
    public double homeRollOffsetRad(int groupIndex, int groupTotal) {
        return signedParam(groupIndex, effectiveCount(groupTotal)) * rollSpreadRad;
    }

    @Override
    public double homePitchOffsetRad(int groupIndex, int groupTotal) {
        return signedParam(groupIndex, effectiveCount(groupTotal)) * pitchSpreadRad;
    }

    private int effectiveCount(int realCount) {
        int safeRealCount = Math.max(1, realCount);
        return referenceTotal > 1
                ? Math.max(safeRealCount, referenceTotal)
                : safeRealCount;
    }

    private double signedParam(int index, int total) {
        double t01 = FollowBehaviorSupport.normalizedIndex01(index, total, 0.0);

        return switch (sideMode) {
            case SYMMETRIC -> (t01 - 0.5) * 2.0;
            case RIGHT_ONLY -> t01;
            case LEFT_ONLY -> -t01;
        };
    }
}