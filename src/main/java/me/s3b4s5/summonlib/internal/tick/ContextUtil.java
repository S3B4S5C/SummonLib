package me.s3b4s5.summonlib.internal.tick;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.internal.Logger;
import me.s3b4s5.summonlib.internal.impl.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public final class ContextUtil {

    private ContextUtil() {}

    public record OwnerCtx(
            UUID ownerUuid,
            PlayerRef owner,
            Ref<EntityStore> ownerRef,
            World world,
            Vector3d ownerPos,
            Vector3d ownerEye,
            Vector3f ownerRot,
            double yawRad
    ) {}

    public record SummonCtx(
            SummonTag tag,
            WormTag wormTag,
            UUID wormUuid,
            Ref<EntityStore> wormRef,
            TransformComponent wormT,
            UUID ownerUuid,
            PlayerRef owner,
            Ref<EntityStore> ownerRef,
            World world,
            SummonDefinition def
    ) {}

    public record NpcSummonCtx(
            SummonTag tag,
            UUID selfUuid,
            Ref<EntityStore> selfRef,
            TransformComponent selfT,
            OwnerCtx ownerCtx,
            World world,
            NpcRoleSummonDefinition def,
            NPCEntity npc,
            Role role
    ) {}

    public record ModelSummonCtx(
            SummonTag tag,
            UUID selfUuid,
            Ref<EntityStore> selfRef,
            TransformComponent selfT,
            OwnerCtx ownerCtx,
            World world,
            ModelSummonDefinition def
    ) {}

    // =========================================================
    // Worm head ctx (keeps your optional cleanup/log/remove behavior)
    // =========================================================

    public static @Nullable SummonCtx getSummonCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType,
            @Nullable Consumer<UUID> onCleanup,
            boolean removeIfOwnerInvalid,
            @Nullable Logger logger,
            boolean DEBUG
    ) {
        final SummonTag tag = chunk.getComponent(index, summonTagType);
        final WormTag wTag = chunk.getComponent(index, wormTagType);
        final UUIDComponent uuidC = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || wTag == null || uuidC == null) return null;
        if (wTag.segmentIndex != 0) return null;

        final UUID wormUuid = uuidC.getUuid();
        final Ref<EntityStore> wormRef = chunk.getReferenceTo(index);

        final TransformComponent wormT = cb.getComponent(wormRef, TransformComponent.getComponentType());
        if (wormT == null) return null;

        final UUID ownerUuid = tag.getOwnerUuid();
        final PlayerRef owner = (ownerUuid != null) ? Universe.get().getPlayer(ownerUuid) : null;

        if (ownerUuid == null || owner == null || owner.getWorldUuid() == null) {
            if (removeIfOwnerInvalid) {
                dbgRemove(logger, DEBUG, wormUuid, tag, "REMOVE owner invalid (null owner / worldUuid)");
                if (onCleanup != null) onCleanup.accept(wormUuid);
                cb.removeEntity(wormRef, RemoveReason.REMOVE);
            }
            return null;
        }

        final Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() != store) {
            if (removeIfOwnerInvalid) {
                dbgRemove(logger, DEBUG, wormUuid, tag, "REMOVE ownerRef invalid / store mismatch");
                if (onCleanup != null) onCleanup.accept(wormUuid);
                cb.removeEntity(wormRef, RemoveReason.REMOVE);
            }
            return null;
        }

        final OwnerCtx ownerCtx = buildOwnerCtxOrNull(store, ownerUuid, owner, ownerRef);

        final SummonDefinition def = resolveDefOrNull(tag);
        if (def == null) return null;

        return new SummonCtx(tag, wTag, wormUuid, wormRef, wormT, ownerUuid, owner, ownerRef, ownerCtx.world, def);
    }

    // =========================================================
    // Shared owner ctx (NPC + Model use this)
    // =========================================================

    public static @Nullable OwnerCtx getOwnerCtxOrNull(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            SummonTag tag,
            Ref<EntityStore> summonRef
    ) {
        final UUID ownerUuid = tag.getOwnerUuid();
        if (ownerUuid == null) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return null;
        }

        final PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return null;
        }

        final Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() != store) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return null;
        }

        return buildOwnerCtxOrNull(store, ownerUuid, owner, ownerRef);
    }

    // =========================================================
    // NPC ctx
    // =========================================================

    public static @Nullable NpcSummonCtx getNpcSummonCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        final BaseEntityCtx base = resolveBaseEntityCtxOrNull(index, chunk, cb, summonTagType, wormTagType);
        if (base == null) return null;

        // NPC ignores worms entirely
        if (base.wormTag != null) return null;

        final ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return null;

        final NPCEntity npc = chunk.getComponent(index, npcType);
        if (npc == null) return null;

        final Role role = npc.getRole();
        if (role == null) return null;

        final OwnerCtx ownerCtx = getOwnerCtxOrNull(store, cb, base.tag, base.selfRef);
        if (ownerCtx == null) return null;

        SummonDefinition sDef = SummonRegistry.get(base.tag.summonId);
        if (!(sDef instanceof NpcRoleSummonDefinition def)) {
            return null;
        }

        return new NpcSummonCtx(base.tag, base.selfUuid, base.selfRef, base.selfT, ownerCtx, ownerCtx.world, def, npc, role);
    }

    // =========================================================
    // Model ctx
    // =========================================================

    public static @Nullable ModelSummonCtx getModelSummonCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        final BaseEntityCtx base = resolveBaseEntityCtxOrNull(index, chunk, cb, summonTagType, wormTagType);
        if (base == null) return null;

        // Model ignores worms entirely
        if (base.wormTag != null) return null;

        // Also ignore NPC summons
        final ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType != null && chunk.getComponent(index, npcType) != null) return null;

        final OwnerCtx ownerCtx = getOwnerCtxOrNull(store, cb, base.tag, base.selfRef);
        if (ownerCtx == null) return null;

        SummonDefinition sDef = SummonRegistry.get(base.tag.summonId);
        if (!(sDef instanceof ModelSummonDefinition def)) {
            return null;
        }

        return new ModelSummonCtx(base.tag, base.selfUuid, base.selfRef, base.selfT, ownerCtx, ownerCtx.world, def);
    }

    // =========================================================
    // Shared helpers
    // =========================================================

    private static final class BaseEntityCtx {
        final SummonTag tag;
        final @Nullable WormTag wormTag;
        final UUID selfUuid;
        final Ref<EntityStore> selfRef;
        final TransformComponent selfT;

        BaseEntityCtx(SummonTag tag, @Nullable WormTag wormTag, UUID selfUuid, Ref<EntityStore> selfRef, TransformComponent selfT) {
            this.tag = tag;
            this.wormTag = wormTag;
            this.selfUuid = selfUuid;
            this.selfRef = selfRef;
            this.selfT = selfT;
        }
    }

    private static @Nullable BaseEntityCtx resolveBaseEntityCtxOrNull(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            CommandBuffer<EntityStore> cb,
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        final SummonTag tag = chunk.getComponent(index, summonTagType);
        final UUIDComponent uuidC = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || uuidC == null) return null;

        final WormTag wormTag = chunk.getComponent(index, wormTagType);

        final Ref<EntityStore> selfRef = chunk.getReferenceTo(index);
        final TransformComponent selfT = cb.getComponent(selfRef, TransformComponent.getComponentType());
        if (selfT == null) return null;

        return new BaseEntityCtx(tag, wormTag, uuidC.getUuid(), selfRef, selfT);
    }

    public static @Nullable SummonDefinition resolveDefOrNull(SummonTag tag) {
        return SummonRegistry.get(tag.getSummonId());
    }

    private static OwnerCtx buildOwnerCtxOrNull(
            Store<EntityStore> store,
            UUID ownerUuid,
            PlayerRef owner,
            Ref<EntityStore> ownerRef
    ) {
        final World world = store.getExternalData().getWorld();

        final Vector3d ownerPos = owner.getTransform().getPosition();

        final Transform ownerLook = TargetUtil.getLook(ownerRef, store);
        final Vector3d ownerEye = ownerLook.getPosition();
        final Vector3f ownerRot = ownerLook.getRotation();
        final double yawRad = ownerRot.getYaw();

        return new OwnerCtx(ownerUuid, owner, ownerRef, world, ownerPos, ownerEye, ownerRot, yawRad);
    }

    private static void dbgRemove(@Nullable Logger logger, boolean DEBUG, @Nullable UUID summonUuid, SummonTag tag, String msg) {
        if (logger == null) return;
        if (!DEBUG) return;
        if (summonUuid == null) return;
    }

    @Nullable
    public static Ref<EntityStore> resolveOwnerRef(SummonTag tag) {
        UUID ownerUuid = tag.getOwnerUuid();
        if (ownerUuid == null) return null;

        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || !owner.isValid()) return null;

        return owner.getReference();
    }
}
