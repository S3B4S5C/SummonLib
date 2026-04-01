package me.s3b4s5.summonlib.systems.npc;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import me.s3b4s5.summonlib.internal.component.SummonComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

public class SummonAggroRedirectSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, SummonComponent> summonTagType;

    public SummonAggroRedirectSystem(ComponentType<EntityStore, SummonComponent> summonTagType) {
        this.summonTagType = summonTagType;
    }
    @Override
    public Query<EntityStore> getQuery() {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return Query.and(summonTagType);

        return Query.and(
                npcType,
                TransformComponent.getComponentType(),
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType()
        );
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> cb
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return;

        final Ref<EntityStore> npcRef = chunk.getReferenceTo(index);

        if (store.getComponent(npcRef, summonTagType) != null) return;

        final NPCEntity npc = chunk.getComponent(index, npcType);
        if (npc == null) return;

        final Role role = npc.getRole();
        if (role == null) return;

        final MarkedEntitySupport marked = role.getMarkedEntitySupport();

        final Ref<EntityStore> curTarget = marked.getMarkedEntityRef(MarkedEntitySupport.DEFAULT_TARGET_SLOT);
        if (curTarget == null || !curTarget.isValid()) return;

        final SummonComponent victimTag = store.getComponent(curTarget, summonTagType);
        if (victimTag == null) return;

        final Ref<EntityStore> ownerRef = resolveOwnerRef(victimTag.getOwnerUuid());
        if (ownerRef != null && ownerRef.isValid()) {
            marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, ownerRef);
        }
    }

    private Ref<EntityStore> resolveOwnerRef(UUID ownerUuid) {
        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null) return null;
        return owner.getReference();
    }
}



