package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

public final class BackOrbitFollowController implements ModelFollowController, OwnerPitchClamp {

    private final double baseBack;
    private final double radius;
    private final double spreadDeg;
    private final double baseHeight;

    private final double orbitRadius;
    private final double attackHeight;

    // Pitch clamp (radians). Positive pitch usually means looking down.
    private final double minPitchRad;
    private final double maxPitchRad;

    public BackOrbitFollowController(
            double baseBack,
            double radius,
            double spreadDeg,
            double baseHeight,
            double orbitRadius,
            double attackHeight
    ) {
        // Default clamp (tune as you like)
        this(baseBack, radius, spreadDeg, baseHeight, orbitRadius, attackHeight, -0.6, 0.55);
    }

    public BackOrbitFollowController(
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
        if (pitchRad < minPitchRad) return minPitchRad;
        if (pitchRad > maxPitchRad) return maxPitchRad;
        return pitchRad;
    }

    @Override
    public Vector3d computeHome(Vector3d ownerPos, double yawRad, int groupIndex, int groupTotal) {
        int count = Math.max(1, groupTotal);
        int i = Math.max(0, groupIndex);

        double fx = -Math.sin(yawRad);
        double fz = -Math.cos(yawRad);
        double rx =  Math.cos(yawRad);
        double rz = -Math.sin(yawRad);

        double t01 = (count <= 1) ? 0.5 : (i / (double) (count - 1));
        double ang = Math.toRadians(-spreadDeg / 2.0 + spreadDeg * t01);
        double side = Math.sin(ang) * radius;

        double half = Math.toRadians(spreadDeg / 2.0);
        double cMin = Math.cos(half);
        double bump = (Math.cos(ang) - cMin) / (1.0 - cMin);
        bump = Math.max(0.0, Math.min(1.0, bump));

        double extraBack = bump * (radius * 0.6);
        double back = baseBack + extraBack;

        double x = ownerPos.x + (-fx) * back + rx * side;
        double z = ownerPos.z + (-fz) * back + rz * side;
        double y = ownerPos.y + baseHeight;

        return new Vector3d(x, y, z);
    }

    @Override
    public Vector3d computeAttackAnchor(Vector3d targetPos, int globalIndex, int globalTotal) {
        int total = Math.max(1, globalTotal);
        int i = Math.max(0, globalIndex);

        double ang = (total <= 1) ? 0.0 : (Math.PI * 2.0) * (i / (double) total);

        double x = targetPos.x + Math.cos(ang) * orbitRadius;
        double z = targetPos.z + Math.sin(ang) * orbitRadius;
        double y = targetPos.y + attackHeight;

        return new Vector3d(x, y, z);
    }
}
