package me.s3b4s5.summonlib.api.follow;

import com.hypixel.hytale.math.vector.Vector3d;

public final class BackLineWingFollowController implements ModelFollowController, OwnerPitchClamp, HomeRotationOffsets {

    public enum SideMode { LEFT_ONLY, RIGHT_ONLY, SYMMETRIC }

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

    /**
     * NUEVO:
     * - Si referenceTotal > 1, normalizamos los spreads como si hubiera al menos referenceTotal invocaciones.
     * - Ej: referenceTotal=6 => la espada #2 siempre usa t01=1/5 (0.2) aunque sólo haya 2 o 3 invocadas.
     * - Si referenceTotal <= 1 => comportamiento viejo (depende del groupTotal real).
     */
    private final int referenceTotal;

    public BackLineWingFollowController(
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
            double attackHeight
    ) {
        this(baseBack, stepBack, sideSpread, baseHeight, heightSpread, heightCurvePow,
                yawSpreadDeg, rollSpreadDeg, pitchSpreadDeg, sideMode,
                orbitRadius, attackHeight, -0.6, 0.55, 0);
    }

    public BackLineWingFollowController(
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
            double maxPitchRad
    ) {
        this(baseBack, stepBack, sideSpread, baseHeight, heightSpread, heightCurvePow,
                yawSpreadDeg, rollSpreadDeg, pitchSpreadDeg, sideMode,
                orbitRadius, attackHeight, minPitchRad, maxPitchRad, 0);
    }

    // NUEVO ctor con referenceTotal
    public BackLineWingFollowController(
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

        this.yawSpreadRad   = Math.toRadians(yawSpreadDeg);
        this.rollSpreadRad  = Math.toRadians(rollSpreadDeg);
        this.pitchSpreadRad = Math.toRadians(pitchSpreadDeg);
        this.sideMode = (sideMode == null) ? SideMode.LEFT_ONLY : sideMode;

        this.orbitRadius = orbitRadius;
        this.attackHeight = attackHeight;

        this.minPitchRad = Math.min(minPitchRad, maxPitchRad);
        this.maxPitchRad = Math.max(minPitchRad, maxPitchRad);

        this.referenceTotal = Math.max(0, referenceTotal);
    }

    @Override
    public double clampOwnerPitch(double pitchRad) {
        if (pitchRad < minPitchRad) return minPitchRad;
        if (pitchRad > maxPitchRad) return maxPitchRad;
        return pitchRad;
    }

    @Override
    public Vector3d computeHome(Vector3d ownerPos, double yawRad, int groupIndex, int groupTotal) {
        int realCount = Math.max(1, groupTotal);
        int i = clampI(groupIndex, realCount);

        // basis
        double fx = -Math.sin(yawRad);
        double fz = -Math.cos(yawRad);
        double rx =  Math.cos(yawRad);
        double rz = -Math.sin(yawRad);

        // >>> clave: spreads normalizados con effectiveCount (no con realCount)
        int effectiveCount = effectiveCount(realCount);
        double t01 = (effectiveCount <= 1) ? 0.0 : (i / (double)(effectiveCount - 1)); // 0..1 “como si fuera 6”
        double s = signedParam(t01);

        // fila detrás
        double back = baseBack + stepBack * i;

        // ala: side + altura (curva)
        double side = s * sideSpread;

        double curve = 1.0 - Math.abs(s);
        curve = Math.pow(Math.max(0.0, curve), heightCurvePow);
        double y = ownerPos.y + baseHeight + heightSpread * curve;

        double x = ownerPos.x + (-fx) * back + rx * side;
        double z = ownerPos.z + (-fz) * back + rz * side;

        return new Vector3d(x, y, z);
    }

    @Override
    public Vector3d computeAttackAnchor(Vector3d targetPos, int globalIndex, int globalTotal) {
        int total = Math.max(1, globalTotal);
        int i = clampI(globalIndex, total);

        double ang = (total <= 1) ? 0.0 : (Math.PI * 2.0) * (i / (double) total);
        double x = targetPos.x + Math.cos(ang) * orbitRadius;
        double z = targetPos.z + Math.sin(ang) * orbitRadius;
        double y = targetPos.y + attackHeight;
        return new Vector3d(x, y, z);
    }

    @Override
    public double homeYawOffsetRad(int groupIndex, int groupTotal) {
        int realCount = Math.max(1, groupTotal);
        int i = clampI(groupIndex, realCount);

        int effectiveCount = effectiveCount(realCount);
        double t01 = (effectiveCount <= 1) ? 0.0 : (i / (double)(effectiveCount - 1));
        double s = signedParam(t01);
        return s * yawSpreadRad;
    }

    @Override
    public double homeRollOffsetRad(int groupIndex, int groupTotal) {
        int realCount = Math.max(1, groupTotal);
        int i = clampI(groupIndex, realCount);

        int effectiveCount = effectiveCount(realCount);
        double t01 = (effectiveCount <= 1) ? 0.0 : (i / (double)(effectiveCount - 1));
        double s = signedParam(t01);
        return s * rollSpreadRad;
    }

    @Override
    public double homePitchOffsetRad(int groupIndex, int groupTotal) {
        int realCount = Math.max(1, groupTotal);
        int i = clampI(groupIndex, realCount);

        int effectiveCount = effectiveCount(realCount);
        double t01 = (effectiveCount <= 1) ? 0.0 : (i / (double)(effectiveCount - 1));
        double s = signedParam(t01);
        return s * pitchSpreadRad;
    }

    private int effectiveCount(int realCount) {
        if (referenceTotal > 1) return Math.max(realCount, referenceTotal);
        return realCount;
    }

    private int clampI(int i, int count) {
        if (i < 0) return 0;
        if (i >= count) return count - 1;
        return i;
    }

    private double signedParam(double t01) {
        return switch (sideMode) {
            case SYMMETRIC -> (t01 - 0.5) * 2.0; // -1..1
            case RIGHT_ONLY -> t01;              // 0..1
            case LEFT_ONLY -> -t01;              // 0..-1
        };
    }
}
