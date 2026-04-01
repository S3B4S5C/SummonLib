package me.s3b4s5.summonlib.internal.movement;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import me.s3b4s5.summonlib.api.follow.OwnerPitchClamp;
import me.s3b4s5.summonlib.api.follow.HomeRotationOffsets;

public final class LerpTransformMovement {

    public void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t) {
        double alpha = Math.min(1.0, dt * speed);
        t.setPosition(new Vector3d(
                cur.x + (target.x - cur.x) * alpha,
                cur.y + (target.y - cur.y) * alpha,
                cur.z + (target.z - cur.z) * alpha
        ));
    }

    public void faceOwner(
            TransformComponent t,
            Object ownerRotationObj,
            double ownerYawRad,
            Object controller,
            int groupIndex,
            int groupTotal
    ) {
        double minPitch = -0.6;
        double maxPitch = 0.55;

        OwnerPitchClamp clamp = (controller instanceof OwnerPitchClamp c) ? c : null;
        HomeRotationOffsets offsets = (controller instanceof HomeRotationOffsets o) ? o : null;

        double pitchRad;
        double yawRad;
        double rollRad;

        if (ownerRotationObj instanceof com.hypixel.hytale.math.vector.Vector3f v) {
            pitchRad = v.getPitch();
            yawRad   = v.getYaw();
            rollRad  = v.getRoll();
        } else {
            pitchRad = 0.0;
            yawRad   = ownerYawRad;
            rollRad  = 0.0;
        }

        double clampedPitch = (clamp != null)
                ? clamp.clampOwnerPitch(pitchRad)
                : clamp(pitchRad, minPitch, maxPitch);

        if (offsets != null) {
            int gi = Math.max(0, groupIndex);
            int gt = Math.max(1, groupTotal);

            yawRad   += offsets.homeYawOffsetRad(gi, gt);
            rollRad  += offsets.homeRollOffsetRad(gi, gt);
            clampedPitch += offsets.homePitchOffsetRad(gi, gt);
        }

        var rot = t.getRotation();
        rot.setPitch((float) clampedPitch);
        rot.setYaw((float) yawRad);
        rot.setRoll((float) rollRad);
    }

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
        return (v < a) ? a : Math.min(v, b);
    }
}


