// File: SummonNpcCombatFollowSystem.java
package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import me.s3b4s5.summonlib.internal.Logger;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;
import me.s3b4s5.summonlib.internal.tick.SummonTickUtil;
import me.s3b4s5.summonlib.runtime.SummonAggroRuntime;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummonNpcTargetSystem extends EntityTickingSystem<EntityStore> {

    Logger logger = new Logger("[NpcTargetSystem]");

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final ComponentType<EntityStore, WormTag> wormTagType;

    private final SummonTargetSelector targetSelector;

    // Runtime (per summon / per owner)
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner = new ConcurrentHashMap<>();

    public SummonNpcTargetSystem(
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        this.summonTagType = summonTagType;
        this.wormTagType = wormTagType;
        this.targetSelector = new SummonTargetSelector(summonTagType);
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();

        if (npcType == null) {
            return Query.and(
                    summonTagType,
                    TransformComponent.getComponentType(),
                    UUIDComponent.getComponentType(),
                    NetworkId.getComponentType()
            );
        }

        return Query.and(
                summonTagType,
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
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> cb
    ) {
        final SummonTickUtil.NpcSummonCtx ctx = SummonTickUtil.getNpcSummonCtxOrNull(
                index, chunk, store, cb,
                summonTagType, wormTagType
        );
        if (ctx == null) return;

        final UUID ownerUuid = ctx.ownerCtx().ownerUuid();
        final Vector3d current = ctx.selfT().getPosition();

        // 1) Priority: aggro focus (owner hit / got hit)
        final Ref<EntityStore> focus = SummonAggroRuntime.pullAggroFocus(
                store, ownerUuid, summonTagType, focusTargetByOwner
        );

        // 2) Otherwise select by radius
        Ref<EntityStore> targetRef = null;
        if (focus != null && focus.isValid()) {
            targetRef = focus;
        } else if (ctx.def().detectRadius > 0.0) {
            targetRef = targetSelector.select(
                    ctx.ownerCtx(),
                    store,
                    current,
                    ctx.def().detectRadius,
                    lastTargetBySummon.get(ctx.selfUuid()),
                    null,
                    ctx.def().requireOwnerLoS,
                    ctx.def().requireSummonLoS
            );
        }

        final Ref<EntityStore> prev = lastTargetBySummon.get(ctx.selfUuid());
        final boolean changed = !Objects.equals(prev, targetRef);

        // Apply / clear runtime cache + marked target
        if (targetRef != null && targetRef.isValid()) {
            lastTargetBySummon.put(ctx.selfUuid(), targetRef);

            if (changed) {
                MarkedEntitySupport marked = ctx.role().getMarkedEntitySupport();
                marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, targetRef);
            }
        } else {
            lastTargetBySummon.remove(ctx.selfUuid());

            if (changed) {
                MarkedEntitySupport marked = ctx.role().getMarkedEntitySupport();
                clearLockedTarget(marked);
            }
        }
        logger.dbg(true, "Target: " + ctx.role().getMarkedEntitySupport().getMarkedEntityRef("LockedTarget"));
    }


    private static void clearLockedTarget(MarkedEntitySupport marked) {
        int slot = findSlotByName(marked, MarkedEntitySupport.DEFAULT_TARGET_SLOT);
        if (slot >= 0) {
            marked.setMarkedEntity(slot, null);
            return;
        }
        // Opción 2 (fallback): si no hay name map, igual puedes intentar limpiar todos
        // (solo si te conviene). Por defecto NO lo hago para evitar side-effects.
    }

    private static int findSlotByName(MarkedEntitySupport marked, String name) {
        int n = marked.getMarkedEntitySlotCount();
        for (int i = 0; i < n; i++) {
            String slotName = marked.getSlotName(i);
            if (name.equals(slotName)) return i;
        }
        return -1;
    }
}
