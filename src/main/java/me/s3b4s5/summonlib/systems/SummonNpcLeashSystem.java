package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderManager;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SummonNpcLeashSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, SummonTag> summonTagType;

    private final double teleportDistance = 28.0; // ajusta a gusto
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
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cb) {

        SummonTag tag = chunk.getComponent(index, summonTagType);
        if (tag == null) return;

        UUIDComponent uuid = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuid == null) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        PlayerRef owner = Universe.get().getPlayer(tag.getOwnerUuid());
        if (owner == null || owner.getWorldUuid() == null) return;

        Transform ownerT = owner.getTransform();
        Vector3d ownerPos = ownerT.getPosition();

        TransformComponent tc = cb.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) return;

        Vector3d cur = tc.getPosition();
        double dx = cur.x - ownerPos.x;
        double dy = cur.y - ownerPos.y;
        double dz = cur.z - ownerPos.z;

        if (dx*dx + dy*dy + dz*dz > teleportDistance * teleportDistance) {
            Vector3d tp = ownerPos.clone();
            tp.y += teleportHeight;

            tc.setPosition(tp);

            // opcional: “reseteo” suave del yaw (no obligatorio)
            tc.getRotation().setYaw(ownerT.getRotation().getYaw());
        }
    }
}
