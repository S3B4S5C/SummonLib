package me.s3b4s5.summonlib.systems.model;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import me.s3b4s5.summonlib.experimental.worm.WormSupport;
import me.s3b4s5.summonlib.experimental.worm.component.WormComponent;
import me.s3b4s5.summonlib.api.follow.ModelFollowBehavior;
import me.s3b4s5.summonlib.api.follow.OrbitFormationFollowBehavior;
import me.s3b4s5.summonlib.internal.animation.DefaultSummonAnimator;
import me.s3b4s5.summonlib.internal.movement.LerpTransformMovement;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;
import me.s3b4s5.summonlib.internal.context.OwnerContextResolver;
import me.s3b4s5.summonlib.internal.context.SummonContextResolver;
import me.s3b4s5.summonlib.internal.definition.ModelSummonDefinition;
import me.s3b4s5.summonlib.internal.runtime.SummonIndexing;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.systems.shared.SummonAnimationSlots;
import me.s3b4s5.summonlib.systems.shared.SummonMath;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummonCombatFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";
    private static final String ANIM_ATTACK = "Attack";

    private static final AnimationSlot SLOT_BASE =
            SummonAnimationSlots.resolveSlot("Idle", "Passive", "Movement");

    private static final ModelFollowBehavior DEFAULT_CONTROLLER =
            new OrbitFormationFollowBehavior(0.4, 1.4, 120.0, 0.8, 0.9, 0.8 * 0.6);

    private static final LerpTransformMovement MOVE_LERP = new LerpTransformMovement();

    private final DefaultSummonAnimator animator = new DefaultSummonAnimator(SLOT_BASE);
    private final SummonTargetSelector targetSelector;

    private final ComponentType<EntityStore, SummonComponent> summonTagType;
    private final ComponentType<EntityStore, WormComponent> wormTagType;

    private static final boolean DEBUG = false;

    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> startDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> attackModeBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> attackCooldownBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> pendingDamageDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> pendingDamageTargetBySummon = new ConcurrentHashMap<>();

    public SummonCombatFollowSystem(
            ComponentType<EntityStore, SummonComponent> summonTagType,
            ComponentType<EntityStore, WormComponent> wormTagType
    ) {
        this.summonTagType = summonTagType;
        this.wormTagType = wormTagType;
        this.targetSelector = new SummonTargetSelector(summonTagType);
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                summonTagType,
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
        final SummonContextResolver.ModelSummonCtx ctx = SummonContextResolver.getModelSummonCtxOrNull(
                index, chunk, store, cb,
                summonTagType, wormTagType
        );
        if (ctx == null) return;

        final Ref<EntityStore> selfRef = ctx.selfRef();

        if (NPCEntity.getComponentType() == null) return;
        if (store.getComponent(selfRef, NPCEntity.getComponentType()) != null) return;

        final WormComponent wt = store.getComponent(selfRef, wormTagType);
        if (WormSupport.isWormSegment(wt)) return;

        final ModelSummonDefinition def = ctx.def();

        final UUID summonUuid = ctx.selfUuid();
        final UUID ownerUuid = ctx.ownerCtx().ownerUuid();

        final float hitDamage = def.damage;
        final double detectRadius = def.detectRadius;

        final boolean requireOwnerLoS = def.requireOwnerLoS;
        final boolean requireSummonLoS = def.requireSummonLoS;

        final double followSpeed = def.followSpeed;
        final double travelToTargetSpeed = def.travelToTargetSpeed;

        final double hitDistance = def.hitDistance;
        final float hitDelay = def.hitDamageDelaySec;
        final float attackInterval = def.attackIntervalSec;
        final boolean keepAttack = def.keepAttackWhileHasTarget;

        final double leashSummonToOwner = def.leashSummonToOwner;
        final double leashTargetToOwner = def.leashTargetToOwner;

        final float ownerMaintenanceCooldownSec = def.ownerMaintenanceCooldownSec;

        final ModelFollowBehavior controller =
                (def.followController != null) ? def.followController : DEFAULT_CONTROLLER;

        if (SummonRuntimeServices.owners().tryRunMaintenance("model-follow", ownerUuid, dt, ownerMaintenanceCooldownSec)) {
            enforceSlotsAndRebuild(store, cb, ctx.ownerCtx().ownerRef(), ownerUuid);
        }

        final Vector3d ownerPos = ctx.ownerCtx().ownerPos();

        final Vector3d home = controller.computeHome(
                ownerPos,
                ctx.ownerCtx().yawRad(),
                Math.max(0, ctx.tag().groupIndex),
                Math.max(1, ctx.tag().groupTotal)
        );

        final Vector3d curPos = ctx.selfT().getPosition();

        final Ref<EntityStore> focus = pullAggroFocusStrict(store, ownerUuid);

        Ref<EntityStore> targetRef = null;
        if (focus != null && focus.isValid()) {
            targetRef = focus;
        } else if (detectRadius > 0.0) {
            targetRef = targetSelector.select(
                    ctx.ownerCtx(),
                    store,
                    curPos,
                    detectRadius,
                    lastTargetBySummon.get(summonUuid),
                    null,
                    requireOwnerLoS,
                    requireSummonLoS
            );
        }

        final boolean hasTarget = (targetRef != null && targetRef.isValid());

        if (hasTarget) {
            if (SummonMath.distSq(curPos, ownerPos) > (leashSummonToOwner * leashSummonToOwner)) {
                dropAndGoHome(dt, store, summonUuid, selfRef, ctx.selfT(), ctx.tag(), controller, ctx.ownerCtx(), home, followSpeed, true, true);
                return;
            }

            final TransformComponent tt = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (tt != null) {
                final Vector3d tp = tt.getPosition();
                if (SummonMath.distSq(tp, ownerPos) > (leashTargetToOwner * leashTargetToOwner)) {
                    dropAndGoHome(dt, store, summonUuid, selfRef, ctx.selfT(), ctx.tag(), controller, ctx.ownerCtx(), home, followSpeed, true, true);
                    return;
                }
            }
        }

        final Ref<EntityStore> prev = lastTargetBySummon.get(summonUuid);
        final boolean changed = !Objects.equals(prev, targetRef);
        if (changed) {
            onTargetChanged(summonUuid, selfRef, store, targetRef, ctx.tag(), keepAttack, attackInterval);
        }

        if (hasTarget) {
            updateStartDelay(dt, summonUuid, selfRef, store, keepAttack);
        } else {
            startDelayBySummon.put(summonUuid, 0f);
        }

        tickCooldownsAndDamage(
                dt,
                summonUuid,
                ctx,
                store,
                cb,
                hitDamage,
                requireOwnerLoS,
                requireSummonLoS,
                ctx.selfT().getPosition()
        );

        if (hasTarget) {
            if (!targetSelector.isAlive(targetRef, store)) {
                SummonRuntimeServices.targets().clearRuntimeTarget(ownerUuid);
                dropSummonTarget(summonUuid);
                return;
            }

            final TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetT == null) {
                dropAndGoHome(dt, store, summonUuid, selfRef, ctx.selfT(), ctx.tag(), controller, ctx.ownerCtx(), home, followSpeed, false, false);
                return;
            }

            final Vector3d tp = targetT.getPosition();

            if (!targetSelector.passesLoS(
                    ctx.ownerCtx().world(),
                    ctx.selfT().getPosition(),
                    ctx.ownerCtx().ownerEye(),
                    tp,
                    requireOwnerLoS,
                    requireSummonLoS
            )) {
                dropAndGoHome(dt, store, summonUuid, selfRef, ctx.selfT(), ctx.tag(), controller, ctx.ownerCtx(), home, followSpeed, true, true);
                return;
            }

            final int attackGi = resolveAttackGi(ctx.tag());
            final int attackGt = resolveAttackGt(ctx.tag());

            final Vector3d anchor = controller.computeAttackAnchor(tp, attackGi, attackGt);

            final boolean attackMode = Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));

            MOVE_LERP.moveTowards(dt, ctx.selfT().getPosition(), anchor, travelToTargetSpeed, ctx.selfT());

            animator.setBaseAnim(
                    summonUuid,
                    selfRef,
                    (keepAttack && attackMode) ? ANIM_ATTACK : ANIM_MOVE,
                    true,
                    store,
                    false
            );

            if (attackMode
                    && pendingDamageTargetBySummon.get(summonUuid) == null
                    && pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f) <= 0f
                    && attackCooldownBySummon.getOrDefault(summonUuid, 0f) <= 0f) {

                final double d2 = SummonMath.distSq(anchor, ctx.selfT().getPosition());
                if (d2 <= (hitDistance * hitDistance)) {
                    if (store.getComponent(targetRef, NetworkId.getComponentType()) != null) {
                        pendingDamageTargetBySummon.put(summonUuid, targetRef);
                        pendingDamageDelayBySummon.put(summonUuid, hitDelay);
                        attackCooldownBySummon.put(summonUuid, attackInterval);
                        animator.setBaseAnim(summonUuid, selfRef, ANIM_ATTACK, true, store, true);
                    } else {
                        attackCooldownBySummon.put(summonUuid, 0.10f);
                    }
                }
            }

            MOVE_LERP.faceTarget(ctx.selfT(), ctx.selfT().getPosition(), tp);
            return;
        }

        goHome(dt, store, summonUuid, selfRef, ctx.selfT(), ctx.tag(), controller, ctx.ownerCtx(), home, followSpeed, false, false);
    }

    private static int resolveAttackGi(SummonComponent tag) {
        final int gt = tag.globalTotal;
        if (gt > 1) return Math.max(0, tag.globalIndex);
        return Math.max(0, tag.groupIndex);
    }

    private static int resolveAttackGt(SummonComponent tag) {
        final int gt = tag.globalTotal;
        if (gt > 1) return gt;
        return Math.max(1, tag.groupTotal);
    }

    private @Nullable Ref<EntityStore> pullAggroFocusStrict(Store<EntityStore> store, UUID ownerUuid) {
        final Ref<EntityStore> focus = SummonRuntimeServices.targets().pullAggroOrFocus(
                store, ownerUuid, summonTagType
        );
        if (focus == null) {
            SummonRuntimeServices.targets().clearRuntimeTarget(ownerUuid);
            return null;
        }
        if (!focus.isValid() || !targetSelector.isAlive(focus, store)) {
            SummonRuntimeServices.targets().clearRuntimeTarget(ownerUuid);
            return null;
        }
        SummonRuntimeServices.targets().rememberRuntimeTarget(ownerUuid, focus);
        return focus;
    }

    private void dropAndGoHome(
            float dt,
            Store<EntityStore> store,
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            TransformComponent summonT,
            SummonComponent tag,
            ModelFollowBehavior controller,
            OwnerContextResolver.OwnerCtx ownerCtx,
            Vector3d home,
            double followSpeed,
            boolean clearOwnerFocus,
            boolean hardAnim
    ) {
        if (clearOwnerFocus) SummonRuntimeServices.targets().clearRuntimeTarget(ownerCtx.ownerUuid());
        dropSummonTarget(summonUuid);
        goHome(dt, store, summonUuid, summonRef, summonT, tag, controller, ownerCtx, home, followSpeed, true, hardAnim);
    }

    private void goHome(
            float dt,
            Store<EntityStore> store,
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            TransformComponent summonT,
            SummonComponent tag,
            ModelFollowBehavior controller,
            OwnerContextResolver.OwnerCtx ownerCtx,
            Vector3d home,
            double followSpeed,
            boolean forceMove,
            boolean hardAnim
    ) {
        final Vector3d cur = summonT.getPosition();
        final boolean returning = SummonMath.distSq(home, cur) > 0.02;

        final String anim = forceMove ? ANIM_MOVE : (returning ? ANIM_MOVE : ANIM_IDLE);
        animator.setBaseAnim(summonUuid, summonRef, anim, true, store, hardAnim);

        MOVE_LERP.faceOwner(
                summonT,
                ownerCtx.ownerRot(),
                ownerCtx.yawRad(),
                controller,
                Math.max(0, tag.groupIndex),
                Math.max(1, tag.groupTotal)
        );

        MOVE_LERP.moveTowards(dt, cur, home, followSpeed, summonT);
    }

    private void onTargetChanged(
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            Store<EntityStore> store,
            @Nullable Ref<EntityStore> targetRef,
            SummonComponent tag,
            boolean keepAttack,
            float attackInterval
    ) {
        if (targetRef == null) lastTargetBySummon.remove(summonUuid);
        else lastTargetBySummon.put(summonUuid, targetRef);

        pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);
        attackCooldownBySummon.put(summonUuid, 0f);

        if (targetRef != null) {
            final int gi = Math.max(0, tag.globalIndex);
            final int gt = Math.max(1, tag.globalTotal);

            final float stagger = SummonMath.computeStartStagger(gi, gt, attackInterval);
            startDelayBySummon.put(summonUuid, stagger);

            final boolean startNow = (stagger <= 0f);
            attackModeBySummon.put(summonUuid, startNow);

            if (startNow && keepAttack) {
                animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
            }
        } else {
            startDelayBySummon.put(summonUuid, 0f);
            attackModeBySummon.put(summonUuid, false);
        }
    }

    private void updateStartDelay(
            float dt,
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            Store<EntityStore> store,
            boolean keepAttack
    ) {
        float startDelay = startDelayBySummon.getOrDefault(summonUuid, 0f);
        final boolean attackMode = Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));

        if (startDelay > 0f) {
            startDelay = Math.max(0f, startDelay - dt);
            startDelayBySummon.put(summonUuid, startDelay);

            if (startDelay <= 0f) {
                attackModeBySummon.put(summonUuid, true);
                if (keepAttack) animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
            }
            return;
        }

        if (!attackMode) {
            attackModeBySummon.put(summonUuid, true);
            if (keepAttack) animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
        }
        startDelayBySummon.put(summonUuid, 0f);
    }

    private void tickCooldownsAndDamage(
            float dt,
            UUID summonUuid,
            SummonContextResolver.ModelSummonCtx ctx,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            float hitDamage,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            Vector3d summonPosNow
    ) {
        float atkCd = attackCooldownBySummon.getOrDefault(summonUuid, 0f);
        atkCd = Math.max(0f, atkCd - dt);
        attackCooldownBySummon.put(summonUuid, atkCd);

        float pd = pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f);
        if (pd <= 0f) return;

        pd = Math.max(0f, pd - dt);
        pendingDamageDelayBySummon.put(summonUuid, pd);

        if (pd > 0f) return;

        final Ref<EntityStore> targetRef = pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);

        if (hitDamage <= 0f) return;
        if (targetRef == null || !targetRef.isValid()) return;
        if (!targetSelector.isAlive(targetRef, store)) return;
        if (store.getComponent(targetRef, NetworkId.getComponentType()) == null) return;

        final TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetT == null) return;

        final Vector3d tp = targetT.getPosition();

        if (!targetSelector.passesLoS(
                ctx.ownerCtx().world(),
                summonPosNow,
                ctx.ownerCtx().ownerEye(),
                tp,
                requireOwnerLoS,
                requireSummonLoS
        )) {
            attackCooldownBySummon.put(summonUuid, 0.15f);
            return;
        }

        final Ref<EntityStore> summonRef = ctx.selfRef();
        if (summonRef == null || !summonRef.isValid()) return;

        final Damage damage = new Damage(new Damage.EntitySource(summonRef), 1, hitDamage);
        cb.invoke(targetRef, damage);
    }

    private void dropSummonTarget(UUID summonUuid) {
        attackModeBySummon.put(summonUuid, false);
        startDelayBySummon.put(summonUuid, 0f);
        attackCooldownBySummon.put(summonUuid, 0f);

        pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);
        lastTargetBySummon.remove(summonUuid);
    }

    private void enforceSlotsAndRebuild(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Ref<EntityStore> ownerRef,
            UUID ownerUuid
    ) {
        final int capSlots = SummonStats.getMaxSlots(store, ownerRef);

        final ArrayList<Ref<EntityStore>> refs = new ArrayList<>();
        final Query<EntityStore> q = Query.and(
                summonTagType,
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType(),
                TransformComponent.getComponentType()
        );

        store.forEachChunk(q, (chunk, ccb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                final SummonComponent t = chunk.getComponent(i, summonTagType);
                if (t == null) continue;
                if (!ownerUuid.equals(t.owner)) continue;
                refs.add(chunk.getReferenceTo(i));
            }
        });

        int usedSlots = 0;
        for (Ref<EntityStore> r : refs) {
            final SummonComponent t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots += Math.max(0, t.slotCost);
        }

        refs.sort(Comparator.comparingLong((Ref<EntityStore> r) -> {
            final SummonComponent t = store.getComponent(r, summonTagType);
            return (t != null) ? t.spawnSeq : Long.MIN_VALUE;
        }).reversed());

        while (usedSlots > capSlots && !refs.isEmpty()) {
            final Ref<EntityStore> r = refs.removeFirst();
            final SummonComponent t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots -= Math.max(0, t.slotCost);
            cb.removeEntity(r, RemoveReason.REMOVE);
        }

        SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, refs);

        final Ref<EntityStore> focus = SummonRuntimeServices.targets().pullAggroOrFocus(store, ownerUuid, summonTagType);
        if (focus != null && (!focus.isValid() || !targetSelector.isAlive(focus, store))) {
            SummonRuntimeServices.targets().clearRuntimeTarget(ownerUuid);
        }
    }
}



