package me.s3b4s5.summonlib.internal.context;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * NPC leash-point control helpers.
 */
public final class NpcLeashSupport {

    private NpcLeashSupport() {}

    public static void setLeashToPoint(
            Ref<EntityStore> selfRef,
            Ref<EntityStore> lookRef,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Vector3d leashPoint
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return;

        NPCEntity npc = cb.getComponent(selfRef, npcType);
        if (npc == null) npc = store.getComponent(selfRef, npcType);
        if (npc == null) return;

        npc.getLeashPoint().assign(leashPoint);

        TransformComponent lookTransform = store.getComponent(lookRef, TransformComponent.getComponentType());
        if (lookTransform != null) {
            Vector3f rot = lookTransform.getRotation();
            npc.setLeashHeading(rot.getYaw());
            npc.setLeashPitch(rot.getPitch());
        }
    }

    public static void snapToLeashPointIfTooFar(
            Ref<EntityStore> selfRef,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Vector3d ownerPos,
            Vector3f ownerRot,
            Vector3d leashPoint,
            double teleportDistance,
            double teleportHeight
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return;

        TransformComponent selfT = cb.getComponent(selfRef, TransformComponent.getComponentType());
        if (selfT == null) selfT = store.getComponent(selfRef, TransformComponent.getComponentType());
        if (selfT == null) return;

        Vector3d cur = selfT.getPosition();
        double dx = cur.x - ownerPos.x;
        double dy = cur.y - ownerPos.y;
        double dz = cur.z - ownerPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= teleportDistance * teleportDistance) return;

        Vector3d snapPos = new Vector3d(leashPoint.x, leashPoint.y + teleportHeight, leashPoint.z);
        selfT.setPosition(snapPos);
        selfT.getRotation().setYaw(ownerRot.getYaw());

        NPCEntity npc = cb.getComponent(selfRef, npcType);
        if (npc == null) npc = store.getComponent(selfRef, npcType);
        if (npc != null) {
            npc.saveLeashInformation(leashPoint, ownerRot);
        }
    }
}


