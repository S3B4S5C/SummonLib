package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.internal.animation.DefaultSummonAnimator;
import me.s3b4s5.summonlib.internal.movement.LerpTransformMovement;
import me.s3b4s5.summonlib.internal.movement.NpcDirectTransformMovement;
import me.s3b4s5.summonlib.internal.movement.NpcLeashMovement;
import me.s3b4s5.summonlib.internal.movement.SummonMovement;
import me.s3b4s5.summonlib.internal.targeting.SummonTargeting;
import me.s3b4s5.summonlib.runtime.SummonIndexing;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.tags.SummonTag;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SummonNpcCombatFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LOG_PREFIX = "[SummonNpcCombatFollowSystem]";

    // Idle detection (NPC)
    private static final double NPC_MOVE_SPEED_EPS = 0.13; // blocks/sec
    private static final double NPC_IDLE_DIST_SQ = 0.25;   // (0.5 blocks)^2

    // Debug/test config
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_STACKTRACE = false;
    private static final String DEBUG_ONLY_SUMMON_ID = null;
    private static final float DEBUG_LOG_PERIOD_SEC = 0.50f;

    private static final boolean DEBUG_MOVEMENT = true;
    private static final boolean DEBUG_TARGETING = true;
    private static final boolean DEBUG_DAMAGE = true;

    private static final boolean RUN_SELF_TESTS = true;

    // Movement lock for hitDelay
    private final ConcurrentHashMap<UUID, Float> moveLockBySummon = new ConcurrentHashMap<>();

    // For speed detection
    private final ConcurrentHashMap<UUID, Vector3d> lastPosNpc = new ConcurrentHashMap<>();

    // Config
    private final ComponentType<EntityStore, SummonTag> summonTagType;

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";
    private static final String ANIM_ATTACK = "Attack";

    // NPC Action slot “neutral”
    private static final String ACTION_OFF_ANIM = ANIM_IDLE;

    // Shared slot resolution
    private static final AnimationSlot SLOT_BASE = SummonCombatFollowShared.resolveSlot("Movement", "Idle", "Passive");
    private static final AnimationSlot SLOT_ATTACK = SummonCombatFollowShared.resolveSlot("Action", "Status", "Emote");

    // NPC animators
    private final DefaultSummonAnimator animatorNpcBase = new DefaultSummonAnimator(SLOT_BASE);
    private final DefaultSummonAnimator animNpcAction = new DefaultSummonAnimator(SLOT_ATTACK);

    // Base keepalive (Hytale can stomp Movement slot)
    private final ConcurrentHashMap<UUID, Float> npcBaseAnimKeepAlive = new ConcurrentHashMap<>();
    private static final float NPC_BASE_KEEPALIVE_SEC = 0.30f;

    // Action hold so Attack isn’t cut in half
    private final ConcurrentHashMap<UUID, Float> npcActionHold = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> npcActionWasAttack = new ConcurrentHashMap<>();

    private static final ModelFollowController DEFAULT_CONTROLLER =
            new BackOrbitFollowController(0.4, 1.4, 120.0, 0.8, 0.9, 0.8 * 0.6);

    private final SummonMovement MOVE_NPC = new NpcLeashMovement();
    private static final LerpTransformMovement MOVE_LERP = new LerpTransformMovement();
    private static final NpcDirectTransformMovement MOVE_NPC_WALK_DIRECT =
            new NpcDirectTransformMovement(NpcDirectTransformMovement.VerticalMode.LOCK_Y);

    private static final NpcDirectTransformMovement MOVE_NPC_FLY_DIRECT =
            new NpcDirectTransformMovement(NpcDirectTransformMovement.VerticalMode.FOLLOW_Y);


    private final SummonTargeting.ComponentTypeWrapper types = new SummonTargeting.ComponentTypeWrapper();

    // Runtime state (per summon)
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> attackCooldownBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> pendingDamageDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> pendingDamageTargetBySummon = new ConcurrentHashMap<>();

    // Runtime state (per owner)
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ownerMaintenanceCooldown = new ConcurrentHashMap<>();

    // Debug throttles/state
    private final ConcurrentHashMap<UUID, Float> debugCdBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> introLogged = new ConcurrentHashMap<>();
    private volatile boolean selfTestsRan = false;

    public SummonNpcCombatFollowSystem(ComponentType<EntityStore, SummonTag> summonTagType) {
        this.summonTagType = summonTagType;
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        // NPC-ONLY: we filter at query level (no need for isNpc checks in tick)
        return Query.and(
                summonTagType,
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
        if (RUN_SELF_TESTS && !selfTestsRan) {
            selfTestsRan = true;
            runSelfTests();
        }

        SummonTag tag = chunk.getComponent(index, summonTagType);
        UUIDComponent uuidComp = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || uuidComp == null) return;

        final String summonId = tag.getSummonId();
        final UUID summonUuid = uuidComp.getUuid();

        // Tick hold for Action slot
        tickNpcActionHold(dt, summonUuid);

        // Move lock
        float ml = moveLockBySummon.getOrDefault(summonUuid, 0f);
        if (ml > 0f) {
            ml = Math.max(0f, ml - dt);
            moveLockBySummon.put(summonUuid, ml);
        }
        boolean movementLocked = ml > 0f;

        Ref<EntityStore> summonRef = chunk.getReferenceTo(index);
        TransformComponent summonT = cb.getComponent(summonRef, TransformComponent.getComponentType());
        if (summonT == null) {
            dbgOncePerSummon(dt, summonUuid, summonId, "WARN missing TransformComponent on summonRef");
            return;
        }

        // Owner
        UUID ownerUuid = tag.getOwnerUuid();
        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) {
            cleanupSummonState(summonUuid, ownerUuid);
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid()) {
            cleanupSummonState(summonUuid, ownerUuid);
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        Store<EntityStore> ownerStore = ownerRef.getStore();
        if (ownerStore == null || ownerStore != store) {
            cleanupSummonState(summonUuid, ownerUuid);
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        SummonDefinition def = SummonRegistry.get(summonId);
        if (def == null) return;

        final var t = def.tuning;

        final double detectRadius = def.detectRadius;
        final float hitDamage = def.damage;
        final boolean requireOwnerLoS = def.requireOwnerLoS;
        final boolean requireSummonLoS = def.requireSummonLoS;

        final double followSpeed = t.followSpeed;              // used for “go home”
        final double travelToTargetSpeed = t.travelToTargetSpeed;
        final double hitDistance = t.hitDistance;

        final float hitDelay = t.hitDamageDelaySec;
        final float attackInterval = t.attackIntervalSec;

        final double leashSummonToOwner = t.leashSummonToOwner;
        final double leashTargetToOwner = t.leashTargetToOwner;

        final float ownerMaintenanceCooldownParam = t.ownerMaintenanceCooldownSec;
        final ModelFollowController controller = (def.followController != null) ? def.followController : DEFAULT_CONTROLLER;

        // Owner transform/look
        Transform ownerTr = owner.getTransform();
        Vector3d ownerPos = ownerTr.getPosition();

        Transform ownerLook = TargetUtil.getLook(ownerRef, ownerStore);
        Vector3d ownerEye = ownerLook.getPosition();
        Vector3f ownerRot = ownerLook.getRotation();
        double ownerYawRad = ownerRot.getYaw();

        if (shouldLog(dt, summonUuid, summonId) && !Boolean.TRUE.equals(introLogged.get(summonUuid))) {
            introLogged.put(summonUuid, true);
            dbg(summonUuid, summonId, "INTRO SLOT_BASE=" + SLOT_BASE + " SLOT_ATTACK=" + SLOT_ATTACK);
        }

        // Owner maintenance (slots + indexing)
        if (tryRunOwnerMaintenance(dt, ownerUuid, ownerMaintenanceCooldownParam)) {
            enforceSlotsAndRebuild(store, cb, ownerRef, ownerUuid);
        }

        // Home point
        Vector3d homeRaw = controller.computeHome(
                ownerPos,
                ownerYawRad,
                Math.max(0, tag.groupIndex),
                Math.max(1, tag.groupTotal)
        );
        Vector3d home = SummonCombatFollowShared.applyOwnerHoverYOffset(ownerPos, homeRaw, t.hoverAboveOwner, t.maxAboveOwner);

        // Current pos + speed detection (XZ)
        Vector3d cur = summonT.getPosition();
        boolean npcMovingNow = false;

        Vector3d prev = lastPosNpc.put(summonUuid, new Vector3d(cur.x, cur.y, cur.z));
        if (prev != null && dt > 1e-6f) {
            double movedXZ = Math.sqrt(SummonCombatFollowShared.distSqXZ(cur, prev));
            double speedXZ = movedXZ / dt;
            npcMovingNow = speedXZ > NPC_MOVE_SPEED_EPS;
        }

        // Validate focus
        Ref<EntityStore> focus = focusTargetByOwner.get(ownerUuid);
        if (focus != null && (!focus.isValid() || !SummonTargeting.isAlive(focus, store))) {
            focusTargetByOwner.remove(ownerUuid);
            focus = null;
        }

        // Target select/update
        Ref<EntityStore> targetRef = null;
        if (detectRadius > 0.0) {
            targetRef = getOrUpdateSummonTarget(
                    summonUuid, ownerUuid, ownerRef,
                    store, world, cur, ownerEye, detectRadius, null,
                    requireOwnerLoS, requireSummonLoS
            );
            if (targetRef != null) lastTargetBySummon.put(summonUuid, targetRef);
            else lastTargetBySummon.remove(summonUuid);
        } else {
            dropSummonTarget(summonUuid);
        }

        if (shouldLog(dt, summonUuid, summonId) && DEBUG_MOVEMENT) {
            dbg(summonUuid, summonId,
                    String.format(Locale.ROOT,
                            "POS cur=(%.2f %.2f %.2f) home=(%.2f %.2f %.2f) movingNow=%s",
                            cur.x, cur.y, cur.z,
                            home.x, home.y, home.z,
                            String.valueOf(npcMovingNow)
                    )
            );
        }

        // Cooldowns
        float atkCd = attackCooldownBySummon.getOrDefault(summonUuid, 0f);
        atkCd = Math.max(0f, atkCd - dt);
        attackCooldownBySummon.put(summonUuid, atkCd);

        float pd = pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f);
        if (pd > 0f) {
            pd = Math.max(0f, pd - dt);
            pendingDamageDelayBySummon.put(summonUuid, pd);
            if (pd <= 0f) {
                applyPendingDamageNow(
                        summonUuid, ownerRef, store, cb,
                        world, summonT.getPosition(), ownerEye,
                        requireOwnerLoS, requireSummonLoS,
                        hitDamage
                );
            }
        }

        // ======================
        // NO TARGET: go home/idle
        // ======================
        if (targetRef == null || !targetRef.isValid()) {
            dropSummonTarget(summonUuid);

            // Movement intent: only push leash point to home if we actually want to travel
            double distHomeXZSq = SummonCombatFollowShared.distSqXZ(cur, home);
            boolean wantsToGoHome = distHomeXZSq > NPC_IDLE_DIST_SQ;

            Vector3d desired = wantsToGoHome ? home : cur;
            MOVE_NPC.setDesiredPointIfSupported(store, cb, summonRef, desired, ownerYawRad, 0);

            // anim por movimiento real
            keepNpcBase(summonUuid, summonRef, store, dt, npcMovingNow ? ANIM_MOVE : ANIM_IDLE);
            setNpcActionOff(summonUuid, summonRef, store, true);
            if (DEBUG_MOVEMENT && shouldLog(dt, summonUuid, summonId)) {
                dbg(summonUuid, summonId,
                        "NO_TARGET wantsToGoHome=" + wantsToGoHome
                                + " distHomeXZ=" + String.format(Locale.ROOT, "%.3f", Math.sqrt(distHomeXZSq)));
            }
            return;
        }

        // ======================
        // TARGET MODE
        // ======================
        // Leash constraint (summon too far)
        if (SummonCombatFollowShared.distSq(cur, ownerPos) > leashSummonToOwner * leashSummonToOwner) {
            focusTargetByOwner.remove(ownerUuid);
            dropSummonTarget(summonUuid);

            keepNpcBase(summonUuid, summonRef, store, dt, ANIM_MOVE);
            setNpcActionOff(summonUuid, summonRef, store, true);
            MOVE_NPC.setDesiredPointIfSupported(store, cb, summonRef, home, ownerYawRad, 0);
            return;
        }

        TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetT == null || !SummonTargeting.isAlive(targetRef, store)) {
            focusTargetByOwner.remove(ownerUuid);
            dropSummonTarget(summonUuid);

            setNpcActionOff(summonUuid, summonRef, store, true);
            return;
        }

        Vector3d tp = targetT.getPosition();

        // Leash constraint (target too far)
        if (SummonCombatFollowShared.distSq(tp, ownerPos) > leashTargetToOwner * leashTargetToOwner) {
            focusTargetByOwner.remove(ownerUuid);
            dropSummonTarget(summonUuid);

            setNpcActionOff(summonUuid, summonRef, store, true);
            return;
        }

        // LoS
        if (!SummonTargeting.passesLoS(world, summonT.getPosition(), ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
            focusTargetByOwner.remove(ownerUuid);
            dropSummonTarget(summonUuid);

            setNpcActionOff(summonUuid, summonRef, store, true);
            return;
        }

        // Anchor + desired point
        Vector3d anchor = controller.computeAttackAnchor(
                tp,
                Math.max(0, tag.globalIndex),
                Math.max(1, tag.globalTotal)
        );

        // 1) Decide si es volador (ideal: flag en SummonDefinition. Por ahora un heuristic simple)
        double hog = SummonTargeting.heightOverGround(world, cur, 128);
        boolean isFlying = Double.isNaN(hog) || Double.isInfinite(hog) || hog > 1.2;


        // 2) Anchor: walk mantiene Y, fly usa el Y real del anchor
        // Voladores: queremos que el punto de ataque esté cerca del target, no arriba
        double strikeYOffset = 0.20; // donde querés que quede el bat al golpear
        Vector3d flyDesired = new Vector3d(anchor.x, tp.y + strikeYOffset, anchor.z);

        Vector3d desired = isFlying
                ? flyDesired
                : new Vector3d(anchor.x, cur.y, anchor.z);

        // radio mínimo para no meterse dentro del target
        double minOrbit = Math.max(1.4, hitDistance * 0.9); // tune: 1.2–2.0 según el mob

        desired = enforceMinRadiusXZ(tp, desired, cur, minOrbit);

        float yawTo = SummonCombatFollowShared.yawRadTo(summonT.getPosition(), tp);
        float pitchTo = SummonCombatFollowShared.pitchRadTo(summonT.getPosition(), tp);

        // IMPORTANT: evita que el Role siga yéndose al home/leash viejo
        MOVE_NPC.setDesiredPointIfSupported(store, cb, summonRef, desired, yawTo, pitchTo);

        // --- DEBUG: por qué el bat pega desde arriba ---
        if (DEBUG_MOVEMENT && shouldLog(dt, summonUuid, summonId)) {
            hog = SummonTargeting.heightOverGround(world, cur, 128);

            double dyToTarget = cur.y - tp.y;
            double dyToAnchor = cur.y - anchor.y;
            double attackHeightNow = anchor.y - tp.y;

            double distXZ = Math.sqrt(SummonCombatFollowShared.distSqXZ(cur, tp));
            double distDesired3D = Math.sqrt(SummonCombatFollowShared.distSq(cur, desired));

            dbg(summonUuid, summonId, String.format(Locale.ROOT,
                    "COMBAT isFlying=%s hog=%.2f curY=%.2f tpY=%.2f anchorY=%.2f atkHeight=%.2f dyT=%.2f dyA=%.2f distXZ=%.2f distDesired3D=%.2f locked=%s",
                    String.valueOf(isFlying),
                    hog,
                    cur.y, tp.y, anchor.y,
                    attackHeightNow,
                    dyToTarget, dyToAnchor,
                    distXZ, distDesired3D,
                    String.valueOf(movementLocked)
            ));
        }

        // 3) Movimiento directo (LERP) pero controlado por modo
        if (!movementLocked) {
            Vector3d before = summonT.getPosition();
            if (isFlying) MOVE_NPC_FLY_DIRECT.moveTowards(dt, before, desired, travelToTargetSpeed, summonT);
            else MOVE_NPC_WALK_DIRECT.moveTowards(dt, before, desired, travelToTargetSpeed, summonT);
        }

        // 4) Rotación (igual)
        MOVE_NPC_WALK_DIRECT.faceTarget(summonT, summonT.getPosition(), tp);


        // Base in combat: Move (you can switch to movement-based if you prefer)
        keepNpcBase(summonUuid, summonRef, store, dt, ANIM_MOVE);

        // Attack scheduling
        boolean canAttack = pendingDamageTargetBySummon.get(summonUuid) == null
                && pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f) <= 0f
                && attackCooldownBySummon.getOrDefault(summonUuid, 0f) <= 0f;

        double dyAbsToTarget = Math.abs(summonT.getPosition().y - tp.y);
        boolean verticalOk = dyAbsToTarget <= 0.9; // tune: 0.7–1.2

        if (DEBUG_DAMAGE && shouldLog(dt, summonUuid, summonId)) {
            dbg(summonUuid, summonId, String.format(Locale.ROOT,
                    "ATTACK_CHECK canAttack=%s verticalOk=%s dyAbs=%.2f hitDist=%.2f",
                    String.valueOf(canAttack),
                    String.valueOf(verticalOk),
                    dyAbsToTarget,
                    hitDistance
            ));
        }


        if (canAttack && verticalOk) {
            double d2ForHit = isFlying
                    ? SummonCombatFollowShared.distSq(summonT.getPosition(), desired)  // 3D
                    : SummonCombatFollowShared.distSqXZ(summonT.getPosition(), tp);
            if (d2ForHit <= hitDistance * hitDistance) {
                if (store.getComponent(targetRef, NetworkId.getComponentType()) != null) {
                    pendingDamageTargetBySummon.put(summonUuid, targetRef);
                    pendingDamageDelayBySummon.put(summonUuid, hitDelay);
                    attackCooldownBySummon.put(summonUuid, attackInterval);

                    // lock movement while hitDelay so Attack reads clean
                    moveLockBySummon.put(summonUuid, hitDelay + 0.05f);

                    playNpcAttack(summonUuid, summonRef, store, attackInterval);

                    if (DEBUG_DAMAGE && shouldLog(dt, summonUuid, summonId)) {
                        dbg(summonUuid, summonId,
                                String.format(Locale.ROOT,
                                        "ATTACK fired. hitDelay=%.2f atkInterval=%.2f",
                                        hitDelay, attackInterval));
                    }
                } else {
                    // target not networked yet, retry soon
                    attackCooldownBySummon.put(summonUuid, 0.10f);
                }
            }
        } else if (canAttack && !verticalOk && DEBUG_DAMAGE && shouldLog(dt, summonUuid, summonId)) {
            dbg(summonUuid, summonId, String.format(Locale.ROOT,
                    "ATTACK_BLOCKED (too high) dyAbs=%.2f (need <= %.2f)",
                    dyAbsToTarget, 0.9
            ));
        }

        // If not in hold, keep Action OFF so Attack never “sticks”
        if (npcActionHold.getOrDefault(summonUuid, 0f) <= 0f) {
            setNpcActionOff(summonUuid, summonRef, store, false);
        }
    }

    // ----------------------------
    // NPC Action logic
    // ----------------------------

    private void tickNpcActionHold(float dt, UUID summonUuid) {
        float h = npcActionHold.getOrDefault(summonUuid, 0f);
        if (h <= 0f) return;
        h = Math.max(0f, h - dt);
        npcActionHold.put(summonUuid, h);

        if (h <= 0f) {
            npcActionWasAttack.put(summonUuid, false);
        }
    }

    private void playNpcAttack(UUID summonUuid, Ref<EntityStore> summonRef, Store<EntityStore> store, float attackInterval) {
        float hold = Math.max(0.70f, attackInterval * 0.85f);
        npcActionHold.put(summonUuid, hold);

        // Attack in Action slot (non-loop) – only called on hit
        animNpcAction.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, false, store, true);
        npcActionWasAttack.put(summonUuid, true);

        if (DEBUG && shouldLog(0f, summonUuid, DEBUG_ONLY_SUMMON_ID)) {
            dbg(summonUuid, DEBUG_ONLY_SUMMON_ID, "NPC ACTION -> Attack slot=" + SLOT_ATTACK + " hold=" + hold);
        }
    }

    private void setNpcActionOff(UUID summonUuid, Ref<EntityStore> summonRef, Store<EntityStore> store, boolean force) {
        boolean wasAtk = Boolean.TRUE.equals(npcActionWasAttack.get(summonUuid));
        boolean doForce = force || wasAtk;

        // If hold is active, don't cut the animation (unless force)
        if (npcActionHold.getOrDefault(summonUuid, 0f) > 0f && !force) return;

        animNpcAction.setBaseAnim(summonUuid, summonRef, ACTION_OFF_ANIM, true, store, doForce);
        npcActionWasAttack.put(summonUuid, false);
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

    // ----------------------------
    // Owner maintenance
    // ----------------------------

    private void enforceSlotsAndRebuild(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef, UUID ownerUuid) {
        int capSlots = SummonStats.getMaxSlots(store, ownerRef);
        ArrayList<Ref<EntityStore>> refs = new ArrayList<>();

        // IMPORTANT: this is global per-owner (NPC + non-NPC), so DON'T filter by NPCEntity here.
        Query<EntityStore> q = Query.and(
                summonTagType,
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType(),
                TransformComponent.getComponentType()
        );

        int usedSlots = 0;

        store.forEachChunk(q, (chunk, ccb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonTag t = chunk.getComponent(i, summonTagType);
                if (t == null || !ownerUuid.equals(t.owner)) continue;
                refs.add(chunk.getReferenceTo(i));
            }
        });

        for (Ref<EntityStore> r : refs) {
            SummonTag t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots += Math.max(0, t.slotCost);
        }

        refs.sort((a, b) -> {
            SummonTag ta = store.getComponent(a, summonTagType);
            SummonTag tb = store.getComponent(b, summonTagType);
            long sa = ta != null ? ta.spawnSeq : Long.MIN_VALUE;
            long sb = tb != null ? tb.spawnSeq : Long.MIN_VALUE;
            return Long.compare(sb, sa);
        });

        while (usedSlots > capSlots && !refs.isEmpty()) {
            Ref<EntityStore> r = refs.remove(0);
            SummonTag t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots -= Math.max(0, t.slotCost);
            cb.removeEntity(r, RemoveReason.REMOVE);
        }

        SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, refs);

        Ref<EntityStore> focus = focusTargetByOwner.get(ownerUuid);
        if (focus != null && (!focus.isValid() || !SummonTargeting.isAlive(focus, store))) {
            focusTargetByOwner.remove(ownerUuid);
        }
    }

    private boolean tryRunOwnerMaintenance(float dt, UUID ownerUuid, float ownerMaintenanceCdParam) {
        float cd = ownerMaintenanceCooldown.getOrDefault(ownerUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            ownerMaintenanceCooldown.put(ownerUuid, cd);
            return false;
        }
        ownerMaintenanceCooldown.put(ownerUuid, ownerMaintenanceCdParam);
        return true;
    }

    // ----------------------------
    // Targeting
    // ----------------------------

    private Ref<EntityStore> getOrUpdateSummonTarget(
            UUID summonUuid,
            UUID ownerUuid,
            Ref<EntityStore> ownerRef,
            Store<EntityStore> store,
            World world,
            Vector3d summonPos,
            Vector3d ownerEye,
            double radius,
            @Nullable Ref<EntityStore> preferred,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        Ref<EntityStore> current = lastTargetBySummon.get(summonUuid);
        if (current != null && current.isValid() && SummonTargeting.isAlive(current, store)) {
            TransformComponent t = store.getComponent(current, TransformComponent.getComponentType());
            if (t != null) {
                Vector3d tp = t.getPosition();
                if (SummonCombatFollowShared.isWithin(summonPos, tp, radius)
                        && SummonTargeting.passesLoS(world, summonPos, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
                    return current;
                }
            }
        }

        if (preferred != null && preferred.isValid() && SummonTargeting.isAlive(preferred, store)) {
            if (SummonTargeting.isAllowedTargetHostileOnly(store, ownerRef, preferred, types)
                    && !preferred.equals(ownerRef)
                    && store.getComponent(preferred, NetworkId.getComponentType()) != null) {

                TransformComponent pt = store.getComponent(preferred, TransformComponent.getComponentType());
                if (pt != null) {
                    Vector3d pp = pt.getPosition();
                    if (SummonCombatFollowShared.isWithin(summonPos, pp, radius)
                            && SummonTargeting.passesLoS(world, summonPos, ownerEye, pp, requireOwnerLoS, requireSummonLoS)) {
                        return preferred;
                    }
                }
            }
        }

        return SummonTargeting.findClosestAliveVisibleInSphere(
                summonPos, ownerEye, radius,
                store, world, ownerRef,
                types, requireOwnerLoS, requireSummonLoS,
                summonTagType
        );
    }

    // ----------------------------
    // Damage
    // ----------------------------

    private void applyPendingDamageNow(
            UUID summonUuid,
            Ref<EntityStore> ownerRef,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            World world,
            Vector3d summonPosNow,
            Vector3d ownerEye,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            float hitDamage
    ) {
        Ref<EntityStore> targetRef = pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);

        if (hitDamage <= 0f) return;
        if (targetRef == null || !targetRef.isValid()) return;
        if (!SummonTargeting.isAlive(targetRef, store)) return;
        if (store.getComponent(targetRef, NetworkId.getComponentType()) == null) return;

        TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetT == null) return;

        Vector3d tp = targetT.getPosition();
        if (!SummonTargeting.passesLoS(world, summonPosNow, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
            attackCooldownBySummon.put(summonUuid, 0.15f);
            return;
        }

        Damage damage = new Damage(new Damage.EntitySource(ownerRef), 1, hitDamage);
        cb.invoke(targetRef, damage);
    }

    private void dropSummonTarget(UUID summonUuid) {
        attackCooldownBySummon.put(summonUuid, 0f);
        pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);
        lastTargetBySummon.remove(summonUuid);

        // NPC action state reset
        moveLockBySummon.remove(summonUuid);
        npcActionHold.put(summonUuid, 0f);
        npcActionWasAttack.put(summonUuid, false);
    }

    private void cleanupSummonState(UUID summonUuid, UUID ownerUuid) {
        lastTargetBySummon.remove(summonUuid);
        attackCooldownBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.remove(summonUuid);
        pendingDamageTargetBySummon.remove(summonUuid);

        moveLockBySummon.remove(summonUuid);
        lastPosNpc.remove(summonUuid);

        focusTargetByOwner.remove(ownerUuid);
        ownerMaintenanceCooldown.remove(ownerUuid);

        debugCdBySummon.remove(summonUuid);
        introLogged.remove(summonUuid);

        npcBaseAnimKeepAlive.remove(summonUuid);
        npcActionHold.remove(summonUuid);
        npcActionWasAttack.remove(summonUuid);

        animatorNpcBase.clear(summonUuid);
        animNpcAction.clear(summonUuid);
    }

    // ----------------------------
    // Debug wrappers (shared)
    // ----------------------------

    private boolean shouldLog(float dt, UUID summonUuid, @Nullable String summonId) {
        return SummonCombatFollowShared.shouldLog(
                DEBUG, DEBUG_ONLY_SUMMON_ID, DEBUG_LOG_PERIOD_SEC,
                debugCdBySummon, dt, summonUuid, summonId
        );
    }

    private void dbg(UUID summonUuid, @Nullable String summonId, String msg) {
        SummonCombatFollowShared.dbg(
                LOGGER, DEBUG, DEBUG_ONLY_SUMMON_ID,
                LOG_PREFIX, summonUuid, summonId, msg
        );
    }

    private void dbgOncePerSummon(float dt, UUID summonUuid, @Nullable String summonId, String msg) {
        SummonCombatFollowShared.dbgOncePerSummon(
                LOGGER, DEBUG, DEBUG_ONLY_SUMMON_ID, DEBUG_LOG_PERIOD_SEC,
                debugCdBySummon, dt, LOG_PREFIX, summonUuid, summonId, msg
        );
    }

    private void runSelfTests() {
        try {
            ((HytaleLogger.Api) LOGGER.atInfo()).log(
                    "%s SELF-TEST: SLOTS BASE=%s ATTACK=%s (same=%s)",
                    LOG_PREFIX,
                    String.valueOf(SLOT_BASE),
                    String.valueOf(SLOT_ATTACK),
                    String.valueOf(SLOT_BASE == SLOT_ATTACK)
            );

            StringBuilder sb = new StringBuilder();
            for (AnimationSlot s : AnimationSlot.values()) sb.append(s.name()).append(" ");
            ((HytaleLogger.Api) LOGGER.atInfo()).log(
                    "%s AnimationSlot.values(): %s",
                    LOG_PREFIX,
                    sb.toString()
            );
        } catch (Throwable t) {
            ((HytaleLogger.Api) LOGGER.atWarning()).log(
                    "%s SELF-TEST failed: %s",
                    LOG_PREFIX,
                    String.valueOf(t)
            );
            if (DEBUG_STACKTRACE) t.printStackTrace();
        }
    }

    private static Vector3d enforceMinRadiusXZ(Vector3d targetPos, Vector3d desired, Vector3d curPos, double minR) {
        double dx = desired.x - targetPos.x;
        double dz = desired.z - targetPos.z;
        double d2 = dx * dx + dz * dz;
        double min2 = minR * minR;

        if (d2 >= min2) return desired;

        // si desired está muy cerca, usamos la dirección "desde donde estoy" para empujar hacia afuera
        double fx = curPos.x - targetPos.x;
        double fz = curPos.z - targetPos.z;
        double f2 = fx * fx + fz * fz;

        if (f2 < 1e-6) { // caso degenerado: justo en el centro
            fx = 1.0;
            fz = 0.0;
            f2 = 1.0;
        }

        double inv = 1.0 / Math.sqrt(f2);
        fx *= inv;
        fz *= inv;

        return new Vector3d(
                targetPos.x + fx * minR,
                desired.y,
                targetPos.z + fz * minR
        );
    }

}
