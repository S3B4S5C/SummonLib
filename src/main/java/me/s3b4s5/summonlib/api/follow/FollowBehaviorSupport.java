package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

final class FollowBehaviorSupport {

    private FollowBehaviorSupport() {
    }

    static double clampPitch(double pitchRad, double minPitchRad, double maxPitchRad) {
        return Math.clamp(pitchRad, minPitchRad, maxPitchRad);
    }

    static int clampIndex(int index, int total) {
        int safeTotal = Math.max(1, total);
        return Math.clamp(index, 0, safeTotal - 1);
    }

    static double normalizedIndex01(int index, int total, double singleValue) {
        int safeTotal = Math.max(1, total);
        int safeIndex = clampIndex(index, safeTotal);
        return safeTotal == 1 ? singleValue : safeIndex / (double) (safeTotal - 1);
    }

    static Vector3d computeCircularAttackAnchor(
            Vector3d targetPos,
            int index,
            int total,
            double orbitRadius,
            double attackHeight
    ) {
        int safeTotal = Math.max(1, total);
        int safeIndex = clampIndex(index, safeTotal);

        double angle = safeTotal == 1
                ? 0.0
                : (Math.PI * 2.0) * (safeIndex / (double) safeTotal);

        return new Vector3d(
                targetPos.x + Math.cos(angle) * orbitRadius,
                targetPos.y + attackHeight,
                targetPos.z + Math.sin(angle) * orbitRadius
        );
    }

    static YawBasis yawBasis(double yawRad) {
        return new YawBasis(
                -Math.sin(yawRad),
                -Math.cos(yawRad),
                Math.cos(yawRad),
                -Math.sin(yawRad)
        );
    }

    record YawBasis(double fx, double fz, double rx, double rz) {
    }
}