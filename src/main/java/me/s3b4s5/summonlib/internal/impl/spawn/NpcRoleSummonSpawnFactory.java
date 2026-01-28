package me.s3b4s5.summonlib.internal.impl.spawn;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class NpcRoleSummonSpawnFactory implements SummonSpawnFactory {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final String npcRoleId;
    private final float initialModelScaleOverride; // 0 = no tocar, usa lo del role
    private final boolean debug;

    public NpcRoleSummonSpawnFactory(@Nonnull String npcRoleId) {
        this(npcRoleId, 0f, false);
    }

    public NpcRoleSummonSpawnFactory(@Nonnull String npcRoleId, float initialModelScaleOverride, boolean debug) {
        this.npcRoleId = npcRoleId;
        this.initialModelScaleOverride = initialModelScaleOverride;
        this.debug = debug;
    }

    @Override
    public Holder<EntityStore> create(
            Store<EntityStore> store,
            UUID ownerUuid,
            Transform ownerTransform,
            Vector3d spawnPos,
            long spawnSeq,
            int variantIndex
    ) {
        // 1) Resolve roleIndex igual que NPCPlugin.spawnNPC -> getIndex(npcType)
        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getIndex(npcRoleId);
        if (roleIndex < 0) {
            LOGGER.atWarning().log("[NpcRoleSummonSpawnFactory] NPCRole not loaded: %s", npcRoleId);
            return null;
        }

        String roleName = npcPlugin.getName(roleIndex);
        if (roleName == null) roleName = npcRoleId;

        // 2) Rotación: usa la del owner si existe, sino 0
        Vector3f rotation = null;
        try {
            rotation = ownerTransform != null ? ownerTransform.getRotation().clone() : null;
        } catch (Throwable ignored) {}
        if (rotation == null) rotation = new Vector3f(0f, 0f, 0f);

        // 3) Crear NPCEntity *SIN* world (esto evita "Entity already in a world!")
        NPCEntity npcComponent = new NPCEntity();

        // Spawn instant (igual que spawnEntity)
        try {
            WorldTimeResource time = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
            if (time != null) npcComponent.setSpawnInstant(time.getGameTime());
        } catch (Throwable t) {
            if (debug) LOGGER.atWarning().log("[NpcRoleSummonSpawnFactory] WorldTimeResource unavailable: %s", t.toString());
        }

        npcComponent.saveLeashInformation(spawnPos, rotation);

        npcComponent.setRoleName(roleName);
        npcComponent.setRoleIndex(roleIndex);

        if (initialModelScaleOverride > 0f) {
            npcComponent.setInitialModelScale(initialModelScaleOverride);
        }

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(NPCEntity.getComponentType(), npcComponent);
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(spawnPos, rotation));

        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));

        holder.addComponent(DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(roleName)));

        // UUID (igual que spawnEntity)
        holder.ensureComponent(UUIDComponent.getComponentType());

        if (debug) {
            LOGGER.atInfo().log(
                    "[NpcRoleSummonSpawnFactory] create OK npcRoleId=%s roleIndex=%d roleName=%s pos=%s rotYaw=%.2f scaleOverride=%.2f",
                    npcRoleId, roleIndex, roleName, Vector3d.formatShortString(spawnPos), rotation.getYaw(),
                    initialModelScaleOverride
            );
        }
        return holder;
    }
}
