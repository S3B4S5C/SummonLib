package me.s3b4s5.summonlib.internal.context;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import me.s3b4s5.summonlib.experimental.worm.WormSupport;
import me.s3b4s5.summonlib.experimental.worm.component.WormComponent;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Resolves summon entity context for active ECS systems.
 */
public final class SummonContextResolver {

    private SummonContextResolver() {}

    public record NpcSummonCtx(
            SummonComponent tag,
            UUID selfUuid,
            Ref<EntityStore> selfRef,
            TransformComponent selfT,
            OwnerContextResolver.OwnerCtx ownerCtx,
            World world,
            NpcRoleSummonDefinition def,
            NPCEntity npc,
            Role role
    ) {}

    public record ModelSummonCtx(
            SummonComponent tag,
            UUID selfUuid,
            Ref<EntityStore> selfRef,
            TransformComponent selfT,
            OwnerContextResolver.OwnerCtx ownerCtx,
            World world,
            ModelSummonDefinition def
    ) {}

    public static @Nullable NpcSummonCtx getNpcSummonCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            ComponentType<EntityStore, WormComponent> wormTagType
    ) {
        BaseEntityCtx base = resolveBaseEntityCtxOrNull(index, chunk, cb, summonTagType, wormTagType);
        if (base == null || base.experimentalWormSegment) return null;

        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return null;

        NPCEntity npc = chunk.getComponent(index, npcType);
        if (npc == null) return null;

        Role role = npc.getRole();
        if (role == null) return null;

        OwnerContextResolver.OwnerCtx ownerCtx = OwnerContextResolver.getOwnerCtxOrNull(store, cb, base.tag, base.selfRef);
        if (ownerCtx == null) return null;

        SummonDefinition definition = SummonRuntimeServices.definitions().get(base.tag.summonId);
        if (!(definition instanceof NpcRoleSummonDefinition npcDef)) return null;

        return new NpcSummonCtx(base.tag, base.selfUuid, base.selfRef, base.selfT, ownerCtx, ownerCtx.world(), npcDef, npc, role);
    }

    public static @Nullable ModelSummonCtx getModelSummonCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            ComponentType<EntityStore, WormComponent> wormTagType
    ) {
        BaseEntityCtx base = resolveBaseEntityCtxOrNull(index, chunk, cb, summonTagType, wormTagType);
        if (base == null || base.experimentalWormSegment) return null;

        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType != null && chunk.getComponent(index, npcType) != null) return null;

        OwnerContextResolver.OwnerCtx ownerCtx = OwnerContextResolver.getOwnerCtxOrNull(store, cb, base.tag, base.selfRef);
        if (ownerCtx == null) return null;

        SummonDefinition definition = SummonRuntimeServices.definitions().get(base.tag.summonId);
        if (!(definition instanceof ModelSummonDefinition modelDef)) return null;

        return new ModelSummonCtx(base.tag, base.selfUuid, base.selfRef, base.selfT, ownerCtx, ownerCtx.world(), modelDef);
    }

    private static @Nullable BaseEntityCtx resolveBaseEntityCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonComponent> summonTagType,
            ComponentType<EntityStore, WormComponent> wormTagType
    ) {
        SummonComponent tag = chunk.getComponent(index, summonTagType);
        UUIDComponent uuidComponent = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || uuidComponent == null) return null;

        WormComponent wormTag = chunk.getComponent(index, wormTagType);
        Ref<EntityStore> selfRef = chunk.getReferenceTo(index);
        TransformComponent selfT = cb.getComponent(selfRef, TransformComponent.getComponentType());
        if (selfT == null) return null;

        return new BaseEntityCtx(tag, WormSupport.isWormSegment(wormTag), uuidComponent.getUuid(), selfRef, selfT);
    }

    private record BaseEntityCtx(
            SummonComponent tag,
            boolean experimentalWormSegment,
            UUID selfUuid,
            Ref<EntityStore> selfRef,
            TransformComponent selfT
    ) {}
}



