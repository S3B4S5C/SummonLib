package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

public final class LerpTransformMovement implements SummonMovement {

    @Override
    public void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t) {
        double alpha = Math.min(1.0, dt * speed);
        t.setPosition(new Vector3d(
                cur.x + (target.x - cur.x) * alpha,
                cur.y + (target.y - cur.y) * alpha,
                cur.z + (target.z - cur.z) * alpha
        ));
    }

    @Override
    public void faceOwner(TransformComponent t, Object ownerRotationObj, double ownerYawRad, Object controller) {
        double minPitch = -0.6;
        double maxPitch = 0.55;

        if (ownerRotationObj instanceof com.hypixel.hytale.math.vector.Vector3f v) {
            float pitch = v.getPitch();
            float yaw = v.getYaw();
            float roll = v.getRoll();

            double clamped = clamp(pitch, minPitch, maxPitch);

            var rot = t.getRotation();
            rot.setPitch((float) clamped);
            rot.setYaw(yaw);
            rot.setRoll(roll);
            return;
        }

        var rot = t.getRotation();
        rot.setPitch(0f);
        rot.setYaw((float) ownerYawRad);
        rot.setRoll(0f);
    }

    @Override
    public void faceTarget(TransformComponent t, Vector3d from, Vector3d to) {
        double vx = to.x - from.x;
        double vz = to.z - from.z;
        float yawRad = (float) Math.atan2(-vx, -vz);
        var rot = t.getRotation();
        rot.setPitch(0f);
        rot.setYaw(yawRad);
        rot.setRoll(0f);
    }

    private static double clamp(double v, double a, double b) {
        return (v < a) ? a : (v > b) ? b : v;
    }
}
