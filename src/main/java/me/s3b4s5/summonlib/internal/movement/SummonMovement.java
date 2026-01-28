package me.s3b4s5.summonlib.internal.movement;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface SummonMovement {
    void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent summonT);
    void faceOwner(
            TransformComponent summonT,
            Object ownerRotationObj,
            double ownerYawRad,
            Object followControllerMaybePitchClamp
    );

    void faceTarget(TransformComponent summonT, Vector3d from, Vector3d to);

    /**
     * Para NPC: en vez de mover Transform, se “le pide” ir a un punto (leash).
     * Para no-NPC: puede ignorarse.
     */
    default void setDesiredPointIfSupported(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Ref<EntityStore> summonRef,
            Vector3d desired,
            double ownerYawRad,
            double ownerPitchRad
    ) {}
}
