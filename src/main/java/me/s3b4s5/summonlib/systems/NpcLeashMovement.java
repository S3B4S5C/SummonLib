package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public final class NpcLeashMovement implements SummonMovement {

    @Override
    public void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent summonT) {
        // NOOP: NO tocar Transform en NPCs.
        // El movimiento real lo hace el Role/MotionController.
    }

    @Override
    public void faceOwner(TransformComponent summonT, Object ownerRotationObj, double ownerYawRad, Object controller) {
        // NOOP: el Role se encarga de rotaciones
    }

    @Override
    public void faceTarget(TransformComponent summonT, Vector3d from, Vector3d to) {
        // NOOP
    }

    @Override
    public void setDesiredPointIfSupported(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> summonRef,
                                           Vector3d desired, double ownerYawRad, double ownerPitchRad) {

        NPCEntity npc = store.getComponent(summonRef, NPCEntity.getComponentType());
        if (npc == null) return;

        // “Leash” = punto objetivo del NPC.
        npc.setLeashPoint(desired);
        npc.setLeashHeading((float) ownerYawRad);
        npc.setLeashPitch((float) ownerPitchRad);

        // Opcional: forzar path recalculation si tu build lo soporta
        try {
            if (npc.getRole() != null && npc.getRole().getActiveMotionController() != null) {
                npc.getRole().getActiveMotionController().setForceRecomputePath(true);
            }
        } catch (Throwable ignored) {}
    }
}
