package me.s3b4s5.summonlib.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import me.s3b4s5.summonlib.tags.SummonTag;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SummonNpcLeashSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, SummonTag> summonTagType;

    private final double teleportDistance = 19.0;
    private final double teleportHeight = 1.5;

    public SummonNpcLeashSystem(ComponentType<EntityStore, SummonTag> summonTagType) {
        this.summonTagType = summonTagType;
    }

    @Override
    public @NonNullDecl Query<EntityStore> getQuery() {
        return Query.and(
                summonTagType,
                NPCEntity.getComponentType(),
                TransformComponent.getComponentType(),
                UUIDComponent.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cb) {

        SummonTag tag = chunk.getComponent(index, summonTagType);
        if (tag == null) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        PlayerRef owner = Universe.get().getPlayer(tag.getOwnerUuid());
        if (owner == null || owner.getWorldUuid() == null) return;

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() != store) return;

        Transform ownerT = owner.getTransform();
        Vector3d ownerPos = ownerT.getPosition();
        Vector3f ownerRot = ownerT.getRotation().clone();

        int idx = Math.max(0, tag.groupIndex);
        int total = Math.max(1, tag.groupTotal);
        double ang = (Math.PI * 2.0) * (idx / (double) total);
        double r = 1.6;
        double y = 0.0;

        Vector3d leashPos = new Vector3d(
                ownerPos.x + Math.cos(ang) * r,
                ownerPos.y + y,
                ownerPos.z + Math.sin(ang) * r
        );

        if (NPCEntity.getComponentType() == null) return;
        NPCEntity npc = cb.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) {
            npc.saveLeashInformation(leashPos, ownerRot);
        }

        TransformComponent tc = cb.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) return;

        Vector3d cur = tc.getPosition();
        double dx = cur.x - ownerPos.x, dy = cur.y - ownerPos.y, dz = cur.z - ownerPos.z;
        double d2 = dx*dx + dy*dy + dz*dz;

        double teleportDistance = 19.0;
        if (d2 > teleportDistance * teleportDistance) {
            Vector3d tp = new Vector3d(leashPos.x, leashPos.y + 1.5, leashPos.z);
            tc.setPosition(tp);
            tc.getRotation().setYaw(ownerRot.getYaw());

            if (npc != null) npc.saveLeashInformation(leashPos, ownerRot);
        }
    }
}