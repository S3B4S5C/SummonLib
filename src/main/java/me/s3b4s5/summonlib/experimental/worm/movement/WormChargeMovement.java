package me.s3b4s5.summonlib.experimental.worm.movement;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

public final class WormChargeMovement {

    public enum VerticalMode { LOCK_Y, FOLLOW_Y }

    private WormChargeMovement() {}

    public static Vector3d step(
            float dt,
            Vector3d cur,
            Vector3d desiredDirNorm,     // direction to target (normalized)
            Vector3d smoothDirNorm,      // previous smooth direction (normalized, can be null)
            double speed,                // blocks/sec
            double steer,                // how fast it can turn (bigger = snappier)
            VerticalMode verticalMode,
            TransformComponent t
    ) {
        if (dt <= 0) return smoothDirNorm != null ? smoothDirNorm : desiredDirNorm;

        Vector3d dd = desiredDirNorm;
        if (verticalMode == VerticalMode.LOCK_Y) {
            dd = new Vector3d(dd.x, 0.0, dd.z);
            dd = normalizeSafe(dd, new Vector3d(0, 0, 1));
        }

        Vector3d sd = (smoothDirNorm == null) ? dd : smoothDirNorm;

        // steering smoothing
        double a = clamp(dt * steer);
        sd = new Vector3d(
                sd.x + (dd.x - sd.x) * a,
                sd.y + (dd.y - sd.y) * a,
                sd.z + (dd.z - sd.z) * a
        );
        sd = normalizeSafe(sd, dd);

        // constant-speed step
        double step = speed * dt;
        Vector3d next = new Vector3d(
                cur.x + sd.x * step,
                cur.y + sd.y * step,
                cur.z + sd.z * step
        );
        t.setPosition(next);

        return sd;
    }

    private static Vector3d normalizeSafe(Vector3d v, Vector3d fallback) {
        double d2 = v.x*v.x + v.y*v.y + v.z*v.z;
        if (d2 < 1e-12) return fallback;
        double inv = 1.0 / Math.sqrt(d2);
        return new Vector3d(v.x*inv, v.y*inv, v.z*inv);
    }

    private static double clamp(double v) {
        return (v < 0.0) ? 0.0 : Math.min(v, 1.0);
    }
}


