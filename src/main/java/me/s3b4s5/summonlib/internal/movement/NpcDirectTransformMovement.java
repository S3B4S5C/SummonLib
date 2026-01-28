package me.s3b4s5.summonlib.internal.movement;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class NpcDirectTransformMovement implements SummonMovement {

    public enum VerticalMode {
        LOCK_Y,     // WALK: no sube/baja, solo XZ
        FOLLOW_Y    // FLY: baja/sube hacia target.y (diagonal)
    }

    private final VerticalMode verticalMode;

    public NpcDirectTransformMovement(VerticalMode verticalMode) {
        this.verticalMode = verticalMode;
    }

    @Override
    public void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t) {
        if (dt <= 0) return;

        // Misma semántica que tu LerpTransformMovement (alpha = dt*speed)
        double alpha = Math.min(1.0, dt * speed);

        double ty = (verticalMode == VerticalMode.LOCK_Y) ? cur.y : target.y;

        t.setPosition(new Vector3d(
                cur.x + (target.x - cur.x) * alpha,
                cur.y + (ty - cur.y) * alpha,
                cur.z + (target.z - cur.z) * alpha
        ));
    }

    @Override
    public void faceOwner(TransformComponent t, Object ownerRotationObj, double ownerYawRad, Object controller) {
        double minPitch = -0.6;
        double maxPitch = 0.55;

        if (ownerRotationObj instanceof Vector3f v) {
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

    @Override
    public void setDesiredPointIfSupported(Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                                           Ref<EntityStore> summonRef, Vector3d desired,
                                           double ownerYawRad, double ownerPitchRad) {
        // NOOP: este movimiento es “directo” por Transform, no por leash/path.
    }

    private static double clamp(double v, double a, double b) {
        return (v < a) ? a : (v > b) ? b : v;
    }
}
