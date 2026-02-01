package me.s3b4s5.summonlib.internal.movement;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import me.s3b4s5.summonlib.api.follow.OwnerPitchClamp;

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
        // Default clamp (radians) — matches your previous hardcode
        double minPitch = -0.6;
        double maxPitch = 0.55;

        // If the active formation/controller provides a clamp, use it
        OwnerPitchClamp clamp = (controller instanceof OwnerPitchClamp c) ? c : null;

        if (ownerRotationObj instanceof com.hypixel.hytale.math.vector.Vector3f v) {
            // IMPORTANT: in your codebase this pitch/yaw/roll are already treated as radians
            double pitchRad = v.getPitch();
            double yawRad   = v.getYaw();
            double rollRad  = v.getRoll();


            double clampedPitch = (clamp != null)
                    ? clamp.clampOwnerPitch(pitchRad)
                    : clamp(pitchRad, minPitch, maxPitch);

            var rot = t.getRotation();
            rot.setPitch((float) clampedPitch);
            rot.setYaw((float) yawRad);
            rot.setRoll((float) rollRad);
            return;
        }

        // Fallback: we don't have full rotation, keep yaw and neutral pitch/roll.
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
