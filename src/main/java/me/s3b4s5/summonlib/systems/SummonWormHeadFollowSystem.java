package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.internal.animation.DefaultSummonAnimator;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.movement.NpcLeashMovement;
import me.s3b4s5.summonlib.internal.movement.SummonMovement;
import me.s3b4s5.summonlib.systems.shared.SummonCombatFollowShared;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummonWormHeadFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final double NPC_MOVE_SPEED_EPS = 0.13;
    private static final double NPC_IDLE_DIST_SQ = 0.25;

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";

    private static final AnimationSlot SLOT_BASE =
            SummonCombatFollowShared.resolveSlot("Movement", "Idle", "Passive");

    private final DefaultSummonAnimator animatorNpcBase = new DefaultSummonAnimator(SLOT_BASE);
    private final ConcurrentHashMap<UUID, Float> npcBaseAnimKeepAlive = new ConcurrentHashMap<>();
    private static final float NPC_BASE_KEEPALIVE_SEC = 0.30f;

    private final ConcurrentHashMap<UUID, Vector3d> lastPosNpc = new ConcurrentHashMap<>();

    private static final ModelFollowController DEFAULT_CONTROLLER =
            new BackOrbitFollowController(0.4, 1.4, 120.0, 0.8, 0.9, 0.8 * 0.6);

    private final SummonMovement MOVE_NPC = new NpcLeashMovement();

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final ComponentType<EntityStore, WormTag> wormTagType;

    public SummonWormHeadFollowSystem(
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        this.summonTagType = summonTagType;
        this.wormTagType = wormTagType;
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        // SOLO NPC worms (la cabeza es NPC)
        return Query.and(
                summonTagType,
                wormTagType,
                NPCEntity.getComponentType(),
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
        SummonTag tag = chunk.getComponent(index, summonTagType);
        WormTag wTag = chunk.getComponent(index, wormTagType);
        UUIDComponent uuidComp = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || wTag == null || uuidComp == null) return;

        // SOLO HEAD
        if (wTag.segmentIndex != 0) return;

        UUID summonUuid = uuidComp.getUuid();
        Ref<EntityStore> summonRef = chunk.getReferenceTo(index);

        TransformComponent summonT = cb.getComponent(summonRef, TransformComponent.getComponentType());
        if (summonT == null) return;

        // Owner valid
        UUID ownerUuid = tag.getOwnerUuid();
        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() != store) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        SummonDefinition def = SummonRegistry.get(tag.getSummonId());
        if (def == null || def.tuning == null) return;

        var t = def.tuning;
        ModelFollowController controller = (def.followController != null) ? def.followController : DEFAULT_CONTROLLER;

        // Home like NPC system
        Transform ownerTr = owner.getTransform();
        Vector3d ownerPos = ownerTr.getPosition();
        double ownerYawRad = ownerTr.getRotation().getYaw();

        Vector3d homeRaw = controller.computeHome(
                ownerPos,
                ownerYawRad,
                Math.max(0, tag.groupIndex),
                Math.max(1, tag.groupTotal)
        );

        Vector3d home = SummonCombatFollowShared.applyOwnerHoverYOffset(
                ownerPos, homeRaw, t.hoverAboveOwner, t.maxAboveOwner
        );

        Vector3d cur = summonT.getPosition();

        // Movement detection like NPC system (XZ speed)
        boolean movingNow = false;
        Vector3d prev = lastPosNpc.put(summonUuid, new Vector3d(cur.x, cur.y, cur.z));
        if (prev != null && dt > 1e-6f) {
            double movedXZ = Math.sqrt(SummonCombatFollowShared.distSqXZ(cur, prev));
            double speedXZ = movedXZ / dt;
            movingNow = speedXZ > NPC_MOVE_SPEED_EPS;
        }

        double distHomeXZSq = SummonCombatFollowShared.distSqXZ(cur, home);
        boolean wantsToGoHome = distHomeXZSq > NPC_IDLE_DIST_SQ;

        Vector3d desired = wantsToGoHome ? home : cur;

        // THIS is the leashpoint update (lo que te faltaba antes)
        MOVE_NPC.setDesiredPointIfSupported(store, cb, summonRef, desired, ownerYawRad, 0);

        keepNpcBase(summonUuid, summonRef, store, dt, movingNow ? ANIM_MOVE : ANIM_IDLE);
    }

    private void keepNpcBase(UUID summonUuid, Ref<EntityStore> summonRef, Store<EntityStore> store, float dt, String animId) {
        float cd = npcBaseAnimKeepAlive.getOrDefault(summonUuid, 0f);
        cd = Math.max(0f, cd - dt);

        boolean force = false;
        if (cd <= 0f) {
            force = true;
            cd = NPC_BASE_KEEPALIVE_SEC;
        }
        npcBaseAnimKeepAlive.put(summonUuid, cd);

        animatorNpcBase.setBaseAnim(summonUuid, summonRef, animId, true, store, force);
    }
}
