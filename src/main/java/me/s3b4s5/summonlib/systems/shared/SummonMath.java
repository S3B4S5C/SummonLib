package me.s3b4s5.summonlib.systems.shared;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Shared summon combat math helpers.
 */
public final class SummonMath {

    private SummonMath() {}

    public static double distSq(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distSqXZ(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        return dx * dx + dz * dz;
    }

    public static float computeStartStagger(int idx, int count, float attackInterval) {
        float step = attackInterval / Math.max(1, count);
        return step * Math.max(0, idx);
    }
}


