package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
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
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.internal.animation.DefaultSummonAnimator;
import me.s3b4s5.summonlib.internal.movement.LerpTransformMovement;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;
import me.s3b4s5.summonlib.internal.tick.SummonTickUtil;
import me.s3b4s5.summonlib.runtime.SummonAggroRuntime;
import me.s3b4s5.summonlib.systems.shared.SummonCombatFollowShared;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummonCombatFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";
    private static final String ANIM_ATTACK = "Attack";

    private static final AnimationSlot SLOT_BASE =
            SummonCombatFollowShared.resolveSlot("Idle", "Passive", "Movement");

    private static final ModelFollowController DEFAULT_CONTROLLER =
            new BackOrbitFollowController(0.4, 1.4, 120.0, 0.8, 0.9, 0.8 * 0.6);

    private static final LerpTransformMovement MOVE_LERP = new LerpTransformMovement();
    private final DefaultSummonAnimator animator = new DefaultSummonAnimator(SLOT_BASE);

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final ComponentType<EntityStore, WormTag> wormTagType;

    private final SummonTargetSelector targetSelector;

    // Runtime (per summon)
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> startDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> attackModeBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> attackCooldownBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> pendingDamageDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> pendingDamageTargetBySummon = new ConcurrentHashMap<>();

    // Runtime (per owner)
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner = new ConcurrentHashMap<>();

    public SummonCombatFollowSystem(
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
        final SummonTickUtil.ModelSummonCtx ctx = SummonTickUtil.getModelSummonCtxOrNull(
                index, chunk, store, cb,
                summonTagType, wormTagType
        );
        if (ctx == null) return;

        final var def = ctx.def();
        final var t = def.tuning;

        final UUID summonUuid = ctx.selfUuid();
        final UUID ownerUuid = ctx.ownerCtx().ownerUuid();

        final float hitDamage = def.damage;
        final double detectRadius = def.detectRadius;

        final boolean requireOwnerLoS = def.requireOwnerLoS;
        final boolean requireSummonLoS = def.requireSummonLoS;

        final double followSpeed = t.followSpeed;
        final double travelToTargetSpeed = t.travelToTargetSpeed;

        final double hitDistance = t.hitDistance;
        final float hitDelay = t.hitDamageDelaySec;
        final float attackInterval = t.attackIntervalSec;
        final boolean keepAttack = t.keepAttackWhileHasTarget;

        final double leashSummonToOwner = t.leashSummonToOwner;
        final double leashTargetToOwner = t.leashTargetToOwner;

        final ModelFollowController controller = (def.followController != null) ? def.followController : DEFAULT_CONTROLLER;

        // Home position
        final Vector3d ownerPos = ctx.ownerCtx().ownerPos();
        final Vector3d homeRaw = controller.computeHome(
                ownerPos,
                ctx.ownerCtx().yawRad(),
                Math.max(0, ctx.tag().groupIndex),
                Math.max(1, ctx.tag().groupTotal)
        );
        final Vector3d home = SummonCombatFollowShared.applyOwnerHoverYOffset(
                ownerPos, homeRaw, t.hoverAboveOwner, t.maxAboveOwner
        );

        // 1) Priority: aggro focus
        final Ref<EntityStore> focus = SummonAggroRuntime.pullAggroFocus(
                store, ownerUuid, summonTagType, focusTargetByOwner
        );

        // 2) Otherwise select by radius
        Ref<EntityStore> targetRef = null;
        final Vector3d curPos = ctx.selfT().getPosition();

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

        // Target change housekeeping
        final Ref<EntityStore> prev = lastTargetBySummon.get(summonUuid);
        final boolean changed = !Objects.equals(prev, targetRef);
        if (changed) {
            onTargetChanged(
                    summonUuid,
                    ctx.selfRef(),
                    store,
                    targetRef,
                    ctx.tag(),
                    attackInterval
            );
        }

        final boolean hasTarget = (targetRef != null && targetRef.isValid());

        // Tick cooldowns + apply pending damage
        tickCooldownsAndDamage(
                dt,
                summonUuid,
                ctx,
                store,
                cb,
                hitDamage,
                requireOwnerLoS,
                requireSummonLoS
        );

        // ======================
        // TARGET MODE
        // ======================
        if (hasTarget) {
            // Validate target still alive
            if (!targetSelector.isAlive(targetRef, store)) {
                clearTargetForOwner(ownerUuid);
                clearSummonCombatState(summonUuid);
                return;
            }

            final TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetT == null) {
                clearTargetForOwner(ownerUuid);
                clearSummonCombatState(summonUuid);
                goHome(dt, store, summonUuid, ctx.selfRef(), ctx.selfT(), ctx.ownerCtx(), controller, home, followSpeed);
                return;
            }

            final Vector3d tp = targetT.getPosition();

            // Leashes
            if (leashSummonToOwner > 0.0) {
                final double d2 = SummonCombatFollowShared.distSq(ctx.selfT().getPosition(), ownerPos);
                if (d2 > leashSummonToOwner * leashSummonToOwner) {
                    clearTargetForOwner(ownerUuid);
                    clearSummonCombatState(summonUuid);
                    goHome(dt, store, summonUuid, ctx.selfRef(), ctx.selfT(), ctx.ownerCtx(), controller, home, followSpeed);
                    return;
                }
            }

            if (leashTargetToOwner > 0.0) {
                final double d2 = SummonCombatFollowShared.distSq(tp, ownerPos);
                if (d2 > leashTargetToOwner * leashTargetToOwner) {
                    clearTargetForOwner(ownerUuid);
                    clearSummonCombatState(summonUuid);
                    goHome(dt, store, summonUuid, ctx.selfRef(), ctx.selfT(), ctx.ownerCtx(), controller, home, followSpeed);
                    return;
                }
            }

            // LoS (owner/summon)
            if (!targetSelector.passesLoS(
                    ctx.ownerCtx().world(),
                    ctx.selfT().getPosition(),
                    ctx.ownerCtx().ownerEye(),
                    tp,
                    requireOwnerLoS,
                    requireSummonLoS
            )) {
                clearTargetForOwner(ownerUuid);
                clearSummonCombatState(summonUuid);
                goHome(dt, store, summonUuid, ctx.selfRef(), ctx.selfT(), ctx.ownerCtx(), controller, home, followSpeed);
                return;
            }

            // Start delay -> attackMode
            final boolean attackMode = updateAttackMode(dt, summonUuid);

            // Move to attack anchor
            final Vector3d anchor = controller.computeAttackAnchor(
                    tp,
                    Math.max(0, ctx.tag().globalIndex),
                    Math.max(1, ctx.tag().globalTotal)
            );

            MOVE_LERP.moveTowards(dt, ctx.selfT().getPosition(), anchor, travelToTargetSpeed, ctx.selfT());

            // Base anim
            final String base = (keepAttack && attackMode) ? ANIM_ATTACK : ANIM_MOVE;
            animator.setBaseAnim(summonUuid, ctx.selfRef(), base, true, store, false);

            // Schedule damage when close enough
            if (attackMode
                    && pendingDamageTargetBySummon.get(summonUuid) == null
                    && pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f) <= 0f
                    && attackCooldownBySummon.getOrDefault(summonUuid, 0f) <= 0f) {

                final double d2 = SummonCombatFollowShared.distSq(anchor, ctx.selfT().getPosition());
                if (d2 <= hitDistance * hitDistance) {
                    if (store.getComponent(targetRef, NetworkId.getComponentType()) != null) {
                        pendingDamageTargetBySummon.put(summonUuid, targetRef);
                        pendingDamageDelayBySummon.put(summonUuid, hitDelay);
                        attackCooldownBySummon.put(summonUuid, attackInterval);

                        // Kick attack anim (short-lived) even if keepAttack is false
                        animator.setBaseAnim(summonUuid, ctx.selfRef(), ANIM_ATTACK, true, store, true);
                    } else {
                        // Target not networked (or not damageable yet) -> small backoff
                        attackCooldownBySummon.put(summonUuid, 0.10f);
                    }
                }
            }

            // Face target
            MOVE_LERP.faceTarget(ctx.selfT(), ctx.selfT().getPosition(), tp);
            return;
        }

        // ======================
        // NO TARGET: go home
        // ======================
        goHome(dt, store, summonUuid, ctx.selfRef(), ctx.selfT(), ctx.ownerCtx(), controller, home, followSpeed);
    }

    private void goHome(
            float dt,
            Store<EntityStore> store,
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            TransformComponent summonT,
            SummonTickUtil.OwnerCtx ownerCtx,
            ModelFollowController controller,
            Vector3d home,
            double followSpeed
    ) {
        final Vector3d cur = summonT.getPosition();
        final boolean returning = SummonCombatFollowShared.distSq(home, cur) > 0.02;

        animator.setBaseAnim(summonUuid, summonRef, returning ? ANIM_MOVE : ANIM_IDLE, true, store, false);

        MOVE_LERP.faceOwner(summonT, ownerCtx.ownerRot(), ownerCtx.yawRad(), controller);
        MOVE_LERP.moveTowards(dt, cur, home, followSpeed, summonT);
    }

    private void onTargetChanged(
            UUID summonUuid,
            Ref<EntityStore> summonRef,
            Store<EntityStore> store,
            @Nullable Ref<EntityStore> targetRef,
            SummonTag tag,
            float attackInterval
    ) {
        if (targetRef != null) {
            lastTargetBySummon.put(summonUuid, targetRef);

            final int gi = Math.max(0, tag.globalIndex);
            final int gt = Math.max(1, tag.globalTotal);
            final float stagger = SummonCombatFollowShared.computeStartStagger(gi, gt, attackInterval);

            startDelayBySummon.put(summonUuid, Math.max(0f, stagger));
            attackModeBySummon.put(summonUuid, stagger <= 0f);
        } else {
            lastTargetBySummon.remove(summonUuid);
            startDelayBySummon.put(summonUuid, 0f);
            attackModeBySummon.put(summonUuid, false);
        }

        // Reset hit scheduling/cooldowns whenever target changes
        pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);
        attackCooldownBySummon.put(summonUuid, 0f);

        // If you want an immediate “attack pose” when target acquired, you can optionally:
        // animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
    }

    private boolean updateAttackMode(float dt, UUID summonUuid) {
        float delay = startDelayBySummon.getOrDefault(summonUuid, 0f);
        if (delay > 0f) {
            delay = Math.max(0f, delay - dt);
            startDelayBySummon.put(summonUuid, delay);
            if (delay <= 0f) attackModeBySummon.put(summonUuid, true);
        } else {
            startDelayBySummon.put(summonUuid, 0f);
            attackModeBySummon.put(summonUuid, true);
        }
        return Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));
    }

    private void tickCooldownsAndDamage(
            float dt,
            UUID summonUuid,
            SummonTickUtil.ModelSummonCtx ctx,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            float hitDamage,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        // Attack cooldown
        float atkCd = attackCooldownBySummon.getOrDefault(summonUuid, 0f);
        if (atkCd > 0f) {
            atkCd = Math.max(0f, atkCd - dt);
            attackCooldownBySummon.put(summonUuid, atkCd);
        }

        // Pending damage countdown
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
        final Vector3d summonPosNow = ctx.selfT().getPosition();

        if (!targetSelector.passesLoS(
                ctx.ownerCtx().world(),
                summonPosNow,
                ctx.ownerCtx().ownerEye(),
                tp,
                requireOwnerLoS,
                requireSummonLoS
        )) {
            // Small backoff to avoid spamming schedule/apply when LoS fails
            attackCooldownBySummon.put(summonUuid, 0.15f);
            return;
        }

        final Damage damage = new Damage(new Damage.EntitySource(ctx.ownerCtx().ownerRef()), 1, hitDamage);
        cb.invoke(targetRef, damage);
    }

    private void clearSummonCombatState(UUID summonUuid) {
        lastTargetBySummon.remove(summonUuid);
        startDelayBySummon.put(summonUuid, 0f);
        attackModeBySummon.put(summonUuid, false);

        attackCooldownBySummon.put(summonUuid, 0f);
        pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);
    }

    private void clearTargetForOwner(UUID ownerUuid) {
        focusTargetByOwner.remove(ownerUuid);
    }
}
