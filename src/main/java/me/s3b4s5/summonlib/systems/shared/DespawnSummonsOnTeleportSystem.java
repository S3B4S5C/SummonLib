package me.s3b4s5.summonlib.systems.shared;

import com.hypixel.hytale.builtin.adventure.teleporter.interaction.server.UsedTeleporter;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.lifecycle.SummonOwnerLifecycle;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Despawns a player's summons as soon as the owner receives {@link UsedTeleporter}.
 */
public class DespawnSummonsOnTeleportSystem extends EntityTickingSystem<EntityStore> {

    private final SummonOwnerLifecycle ownerLifecycle;

    public DespawnSummonsOnTeleportSystem(
            SummonOwnerLifecycle ownerLifecycle
    ) {
        this.ownerLifecycle = ownerLifecycle;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Player.getComponentType(),
                UUIDComponent.getComponentType(),
                UsedTeleporter.getComponentType()
        );
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> cb
    ) {
        final Ref<EntityStore> ownerRef = chunk.getReferenceTo(index);
        if (ownerRef == null || !ownerRef.isValid()) {
            return;
        }

        final UUIDComponent ownerUuidComp = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (ownerUuidComp == null) {
            return;
        }

        final UUID ownerUuid = ownerUuidComp.getUuid();
        if (ownerUuid == null) {
            return;
        }

        ownerLifecycle.cleanupAfterTeleport(store, cb, ownerRef, ownerUuid);
    }
}


