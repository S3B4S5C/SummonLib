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
import me.s3b4s5.summonlib.api.SummonDefinition;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.runtime.SummonIndexing;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SummonCombatFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // =========================
    // DEBUG / TESTS (tune these)
    // =========================
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_STACKTRACE = false;

    // Set to "BatMinion" to focus; set null to log all summon IDs
    private static final String DEBUG_ONLY_SUMMON_ID = "BatMinion";

    // Log every X seconds per summon (throttled)
    private static final float DEBUG_LOG_PERIOD_SEC = 0.50f;

    // Extra verbose logs for movement/targeting
    private static final boolean DEBUG_MOVEMENT = true;
    private static final boolean DEBUG_TARGETING = true;
    private static final boolean DEBUG_DAMAGE = true;
    private static final boolean DEBUG_NPC_LEASH = true;

    // Run reflection/self-tests once per world/store thread (guarded)
    private static final boolean RUN_SELF_TESTS = true;

    // =========================
    // Config
    // =========================
    private final ComponentType<EntityStore, SummonTag> summonTagType;

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";
    private static final String ANIM_ATTACK = "Attack";
    private static final AnimationSlot SLOT_BASE = resolveSlot("Idle", "Passive", "Movement");

    private static final ModelFollowController DEFAULT_CONTROLLER =
            new BackOrbitFollowController(0.4, 1.4, 120.0, 0.8, 0.9, 0.8 * 0.6);

    // Movement strategies
    private static final SummonMovement MOVE_LERP = new LerpTransformMovement();
    private final SummonMovement MOVE_NPC = new NpcLeashMovement();

    // Animators
    private final SummonAnimator animatorDefault = new DefaultSummonAnimator(SLOT_BASE);
    private final SummonAnimator animatorNpc = new NoopSummonAnimator();

    private final SummonTargeting.ComponentTypeWrapper types = new SummonTargeting.ComponentTypeWrapper();

    // =========================
    // Runtime state
    // =========================
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> startDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> attackCooldownBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> pendingDamageDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> pendingDamageTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> attackModeBySummon = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ownerMaintenanceCooldown = new ConcurrentHashMap<>();

    // Debug throttles
    private final ConcurrentHashMap<UUID, Float> debugCdBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> introLogged = new ConcurrentHashMap<>();
    private volatile boolean selfTestsRan = false;

    public SummonCombatFollowSystem(ComponentType<EntityStore, SummonTag> summonTagType) {
        this.summonTagType = summonTagType;
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
        if (RUN_SELF_TESTS && !selfTestsRan) {
            selfTestsRan = true;
            runSelfTests();
        }

        SummonTag tag = chunk.getComponent(index, summonTagType);
        UUIDComponent uuidComp = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || uuidComp == null) return;

        final String summonId = tag.getSummonId();

        Ref<EntityStore> summonRef = chunk.getReferenceTo(index);
        TransformComponent summonT = cb.getComponent(summonRef, TransformComponent.getComponentType());
        if (summonT == null) {
            dbgOncePerSummon(dt, uuidComp.getUuid(), summonId, "WARN missing TransformComponent on summonRef");
            return;
        }

        UUID summonUuid = uuidComp.getUuid();

        // Owner
        UUID ownerUuid = tag.getOwnerUuid();
        PlayerRef owner = Universe.get().getPlayer(ownerUuid);

        if (owner == null || owner.getWorldUuid() == null) {
            dbgOncePerSummon(dt, summonUuid, summonId, "Owner missing/offline -> removing summon. owner=" + ownerUuid);
            cleanupSummonState(summonUuid, ownerUuid);
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid()) {
            dbgOncePerSummon(dt, summonUuid, summonId, "OwnerRef invalid -> removing summon. owner=" + ownerUuid);
            cleanupSummonState(summonUuid, ownerUuid);
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        Store<EntityStore> ownerStore = ownerRef.getStore();
        if (ownerStore == null || ownerStore != store) {
            dbgOncePerSummon(dt, summonUuid, summonId, "Owner store != summon store (cross-world or unloaded) -> removing summon.");
            cleanupSummonState(summonUuid, ownerUuid);
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        SummonDefinition def = SummonRegistry.get(summonId);
        if (def == null) {
            dbgOncePerSummon(dt, summonUuid, summonId, "WARN SummonDefinition missing for summonId=" + summonId);
            return;
        }

        final var t = def.tuning;

        final double detectRadius = def.detectRadius;
        final float hitDamage = def.damage;
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

        // t.hoverAboveOwner / t.maxAboveOwner

        final float ownerMaintenanceCooldownParam = t.ownerMaintenanceCooldownSec;
        final ModelFollowController controller = (def.followController != null) ? def.followController : DEFAULT_CONTROLLER;

        boolean isNpcSummon = store.getComponent(summonRef, NPCEntity.getComponentType()) != null;

        SummonMovement movementFollow = isNpcSummon ? MOVE_NPC : MOVE_LERP;
        SummonAnimator animator = isNpcSummon ? animatorNpc : animatorDefault;

        // Owner transform/look
        Transform ownerTr = owner.getTransform();
        Vector3d ownerPos = ownerTr.getPosition();

        Transform ownerLook = TargetUtil.getLook(ownerRef, ownerStore);
        Vector3d ownerEye = ownerLook.getPosition();
        Vector3f ownerRot = ownerLook.getRotation();
        double yawRad = ownerRot.getYaw();

        if (shouldLog(dt, summonUuid, summonId)) {
            if (!Boolean.TRUE.equals(introLogged.get(summonUuid))) {
                introLogged.put(summonUuid, true);
                dbg(summonUuid, summonId, "INTRO isNpcSummon=" + isNpcSummon
                        + " owner=" + ownerUuid
                        + " detectRadius=" + detectRadius
                        + " dmg=" + hitDamage
                        + " requireOwnerLoS=" + requireOwnerLoS
                        + " requireSummonLoS=" + requireSummonLoS
                        + " leashSummonToOwner=" + leashSummonToOwner
                        + " leashTargetToOwner=" + leashTargetToOwner);
            }
        }

        // Owner maintenance (slots + indexing)
        if (tryRunOwnerMaintenance(dt, ownerUuid, ownerMaintenanceCooldownParam)) {
            enforceSlotsAndRebuild(store, cb, ownerRef, ownerUuid);
        }

        Vector3d homeRaw = controller.computeHome(
                ownerPos,
                yawRad,
                Math.max(0, tag.groupIndex),
                Math.max(1, tag.groupTotal)
        );

        Vector3d home = applyOwnerHoverYOffset(ownerPos, homeRaw, t.hoverAboveOwner, t.maxAboveOwner);
        Vector3d cur = summonT.getPosition();

        // Validate focus target
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
                    store, world, cur, ownerEye, detectRadius, focus,
                    requireOwnerLoS, requireSummonLoS
            );
            if (targetRef != null) focusTargetByOwner.putIfAbsent(ownerUuid, targetRef);
        } else {
            dropSummonTarget(summonUuid);
        }

        if (shouldLog(dt, summonUuid, summonId) && DEBUG_MOVEMENT) {
            double hogCur = SummonTargeting.heightOverGround(world, cur, 128);
            double hogHome = SummonTargeting.heightOverGround(world, home, 128);
            dbg(summonUuid, summonId,
                    String.format(Locale.ROOT,
                            "POS cur=(%.2f %.2f %.2f) HoG=%.2f | home=(%.2f %.2f %.2f) HoG=%.2f | owner=(%.2f %.2f %.2f)",
                            cur.x, cur.y, cur.z, hogCur,
                            home.x, home.y, home.z, hogHome,
                            ownerPos.x, ownerPos.y, ownerPos.z
                    )
            );
        }

        // Leash constraints relative to owner
        if (targetRef != null && targetRef.isValid()) {
            if (distSq(cur, ownerPos) > leashSummonToOwner * leashSummonToOwner) {
                if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                    dbg(summonUuid, summonId, "LEASH break (summon too far from owner). Dropping target, returning home.");
                }
                focusTargetByOwner.remove(ownerUuid);
                dropSummonTarget(summonUuid);

                animator.setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, true);
                if (isNpcSummon) {
                    movementFollow.setDesiredPointIfSupported(store, cb, summonRef, home, yawRad, 0);
                } else {
                    movementFollow.faceOwner(summonT, ownerRot, yawRad, controller);
                    movementFollow.moveTowards(dt, cur, home, followSpeed, summonT);
                }
                return;
            }

            TransformComponent tt = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (tt != null) {
                Vector3d tp = tt.getPosition();
                if (distSq(tp, ownerPos) > leashTargetToOwner * leashTargetToOwner) {
                    if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                        dbg(summonUuid, summonId, "LEASH break (target too far from owner). Dropping target, returning home.");
                    }
                    focusTargetByOwner.remove(ownerUuid);
                    dropSummonTarget(summonUuid);

                    animator.setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, true);
                    if (isNpcSummon) {
                        movementFollow.setDesiredPointIfSupported(store, cb, summonRef, home, yawRad, 0);
                    } else {
                        movementFollow.faceOwner(summonT, ownerRot, yawRad, controller);
                        movementFollow.moveTowards(dt, cur, home, followSpeed, summonT);
                    }
                    return;
                }
            }
        }

        // Target change handling
        Ref<EntityStore> lastT = lastTargetBySummon.get(summonUuid);
        boolean changed = (lastT == null && targetRef != null) || (lastT != null && (targetRef == null || !lastT.equals(targetRef)));

        if (changed) {
            if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                dbg(summonUuid, summonId, "TARGET changed: " + (lastT == null ? "null" : "set") + " -> " + (targetRef == null ? "null" : "set"));
            }

            if (targetRef == null) lastTargetBySummon.remove(summonUuid);
            else lastTargetBySummon.put(summonUuid, targetRef);

            pendingDamageTargetBySummon.remove(summonUuid);
            pendingDamageDelayBySummon.put(summonUuid, 0f);
            attackCooldownBySummon.put(summonUuid, 0f);

            if (targetRef != null) {
                int gi = Math.max(0, tag.globalIndex);
                int gt = Math.max(1, tag.globalTotal);
                float stagger = computeStartStagger(gi, gt, attackInterval);
                startDelayBySummon.put(summonUuid, stagger);

                boolean startNow = (stagger <= 0f);
                attackModeBySummon.put(summonUuid, startNow);

                // Animación para no-NPC; para NPC la animación irá por separado (Noop)
                if (startNow && keepAttack && !isNpcSummon) {
                    animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                }
            } else {
                startDelayBySummon.put(summonUuid, 0f);
                attackModeBySummon.put(summonUuid, false);
            }
        }

        // Start delay / attack mode
        float startDelay = startDelayBySummon.getOrDefault(summonUuid, 0f);
        if (targetRef != null) {
            boolean attackMode = Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));

            if (startDelay > 0f) {
                float prev = startDelay;
                startDelay = Math.max(0f, startDelay - dt);
                startDelayBySummon.put(summonUuid, startDelay);

                if (prev > 0f && startDelay <= 0f) {
                    attackModeBySummon.put(summonUuid, true);
                    if (keepAttack && !isNpcSummon) {
                        animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                    }
                    if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                        dbg(summonUuid, summonId, "ATTACK_MODE enabled after stagger.");
                    }
                }
            } else {
                if (!attackMode) {
                    attackModeBySummon.put(summonUuid, true);
                    if (keepAttack && !isNpcSummon) {
                        animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                    }
                    if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                        dbg(summonUuid, summonId, "ATTACK_MODE enabled (no stagger).");
                    }
                }
                startDelayBySummon.put(summonUuid, 0f);
            }
        } else {
            startDelayBySummon.put(summonUuid, 0f);
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
        // TARGET MODE
        // ======================
        if (targetRef != null && targetRef.isValid()) {
            TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());

            if (!SummonTargeting.isAlive(targetRef, store)) {
                focusTargetByOwner.remove(ownerUuid);
                dropSummonTarget(summonUuid);
                // if (isNpcSummon) MOVE_NPC.setDesiredPointIfSupported(store, cb, summonRef, home, yawRad, 0);
                return;
            }

            if (targetT == null) {
                if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                    dbg(summonUuid, summonId, "TARGET missing TransformComponent -> drop target.");
                }
                dropSummonTarget(summonUuid);
                animator.setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, false);

                if (isNpcSummon) {
                    movementFollow.setDesiredPointIfSupported(store, cb, summonRef, home, yawRad, 0);
                } else {
                    movementFollow.faceOwner(summonT, ownerRot, yawRad, controller);
                    movementFollow.moveTowards(dt, cur, home, followSpeed, summonT);
                }
                return;
            }

            Vector3d tp = targetT.getPosition();

            if (!SummonTargeting.passesLoS(world, summonT.getPosition(), ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
                if (DEBUG_TARGETING && shouldLog(dt, summonUuid, summonId)) {
                    dbg(summonUuid, summonId, "LoS failed -> drop target.");
                }
                focusTargetByOwner.remove(ownerUuid);
                dropSummonTarget(summonUuid);
                animator.setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, true);

                if (isNpcSummon) {
                    movementFollow.setDesiredPointIfSupported(store, cb, summonRef, home, yawRad, 0);
                } else {
                    movementFollow.faceOwner(summonT, ownerRot, yawRad, controller);
                    movementFollow.moveTowards(dt, cur, home, followSpeed, summonT);
                }
                return;
            }

            Vector3d anchor = controller.computeAttackAnchor(
                    tp,
                    Math.max(0, tag.globalIndex),
                    Math.max(1, tag.globalTotal)
            );

            if (shouldLog(dt, summonUuid, summonId) && DEBUG_MOVEMENT) {
                double hogAnchor = SummonTargeting.heightOverGround(world, anchor, 128);
                dbg(summonUuid, summonId,
                        String.format(Locale.ROOT,
                                "ANCHOR (%.2f %.2f %.2f) HoG=%.2f | target=(%.2f %.2f %.2f) distToAnchor=%.2f",
                                anchor.x, anchor.y, anchor.z, hogAnchor,
                                tp.x, tp.y, tp.z,
                                Math.sqrt(distSq(anchor, summonT.getPosition()))
                        )
                );
            }

            boolean attackMode = Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));

            // ✅ AQUI ESTÁ TU IDEA: si es NPC, en combate se mueve EXACTO como no-NPC (Transform LERP a anchor)
            SummonMovement combatMove = MOVE_LERP;

            // mover hacia la formación del target (anchor)
            Vector3d before = summonT.getPosition();
            combatMove.moveTowards(dt, before, anchor, travelToTargetSpeed, summonT);

            // animación base: solo para no-NPC (para NPC la animación va aparte)
            if (!isNpcSummon) {
                if (keepAttack && attackMode) animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, false);
                else animator.setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, false);
            }

            // schedule daño igual que no-NPC (solo si realmente llegó al anchor)
            if (attackMode
                    && pendingDamageTargetBySummon.get(summonUuid) == null
                    && pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f) <= 0f
                    && attackCooldownBySummon.getOrDefault(summonUuid, 0f) <= 0f) {

                double d2 = distSq(anchor, summonT.getPosition());
                if (d2 <= (hitDistance * hitDistance)) {
                    if (store.getComponent(targetRef, NetworkId.getComponentType()) != null) {
                        pendingDamageTargetBySummon.put(summonUuid, targetRef);
                        pendingDamageDelayBySummon.put(summonUuid, hitDelay);
                        attackCooldownBySummon.put(summonUuid, attackInterval);

                        if (DEBUG_DAMAGE && shouldLog(dt, summonUuid, summonId)) {
                            dbg(summonUuid, summonId, String.format(Locale.ROOT,
                                    "DAMAGE scheduled in %.2fs (atkCd=%.2fs) hitDist=%.2f",
                                    hitDelay, attackInterval, Math.sqrt(d2)));
                        }

                        if (!isNpcSummon) animator.setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                    } else {
                        attackCooldownBySummon.put(summonUuid, 0.10f);
                    }
                } else if (DEBUG_DAMAGE && shouldLog(dt, summonUuid, summonId)) {
                    dbg(summonUuid, summonId, String.format(Locale.ROOT,
                            "DAMAGE not scheduled (too far). dist=%.2f hitDistance=%.2f",
                            Math.sqrt(d2), hitDistance));
                }
            }

            // facing: para no-NPC (y opcionalmente NPC para orientación visual)
            combatMove.faceTarget(summonT, summonT.getPosition(), tp);

            // 🔒 MUY IMPORTANTE: si es NPC, sincronizamos leash point a su posición actual para que el Role no meta drift/inercia rara
            if (isNpcSummon) {
                float yawTo = yawRadTo(summonT.getPosition(), tp);
                float pitchTo = pitchRadTo(summonT.getPosition(), tp);
                MOVE_NPC.setDesiredPointIfSupported(store, cb, summonRef, summonT.getPosition(), yawTo, pitchTo);
            }

            return;
        }

        // ======================
        // NO TARGET: go home
        // ======================
        dropSummonTarget(summonUuid);

        boolean returning = distSq(home, cur) > 0.02;

        if (!isNpcSummon) {
            animator.setBaseAnim(summonUuid, summonRef, returning ? ANIM_MOVE : ANIM_IDLE, true, store, false);
            movementFollow.faceOwner(summonT, ownerRot, yawRad, controller);
            movementFollow.moveTowards(dt, cur, home, followSpeed, summonT);
        } else {
            // NPC follow/idle (NO lo tocamos como pediste)
            movementFollow.setDesiredPointIfSupported(store, cb, summonRef, home, yawRad, 0);
        }
    }

    // =======================
    // Owner maintenance
    // =======================
    private void enforceSlotsAndRebuild(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> ownerRef, UUID ownerUuid) {
        int capSlots = SummonStats.getMaxSlots(store, ownerRef);
        ArrayList<Ref<EntityStore>> refs = new ArrayList<>();

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

    // =======================
    // Targeting
    // =======================
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
                if (isWithin(summonPos, tp, radius)
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
                    if (isWithin(summonPos, pp, radius)
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

    // =======================
    // Combat + helpers
    // =======================
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

        if (DEBUG_DAMAGE && shouldLog(0f, summonUuid, null)) {
            dbg(summonUuid, null, "DAMAGE apply now. amount=" + hitDamage);
        }

        Damage damage = new Damage(new Damage.EntitySource(ownerRef), 1, hitDamage);
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

    private static float computeStartStagger(int idx, int count, float attackInterval) {
        float step = attackInterval / Math.max(1, count);
        return step * Math.max(0, idx);
    }

    private static boolean isWithin(Vector3d a, Vector3d b, double radius) {
        return distSq(a, b) <= radius * radius;
    }

    private static double distSq(Vector3d a, Vector3d b) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static AnimationSlot resolveSlot(String... names) {
        for (String n : names) {
            try {
                return AnimationSlot.valueOf(n);
            } catch (Throwable ignored) {}
        }
        return AnimationSlot.values()[0];
    }

    private void cleanupSummonState(UUID summonUuid, UUID ownerUuid) {
        lastTargetBySummon.remove(summonUuid);
        startDelayBySummon.remove(summonUuid);
        attackCooldownBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.remove(summonUuid);
        pendingDamageTargetBySummon.remove(summonUuid);
        attackModeBySummon.remove(summonUuid);

        focusTargetByOwner.remove(ownerUuid);
        ownerMaintenanceCooldown.remove(ownerUuid);

        debugCdBySummon.remove(summonUuid);
        introLogged.remove(summonUuid);
    }

    // =======================
    // DEBUG HELPERS
    // =======================
    private boolean shouldLog(float dt, UUID summonUuid, @Nullable String summonId) {
        if (!DEBUG) return false;
        if (DEBUG_ONLY_SUMMON_ID != null && summonId != null && !DEBUG_ONLY_SUMMON_ID.equals(summonId)) return false;

        float cd = debugCdBySummon.getOrDefault(summonUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            debugCdBySummon.put(summonUuid, cd);
            return false;
        }
        debugCdBySummon.put(summonUuid, DEBUG_LOG_PERIOD_SEC);
        return true;
    }

    private void dbg(UUID summonUuid, @Nullable String summonId, String msg) {
        if (!DEBUG) return;
        if (DEBUG_ONLY_SUMMON_ID != null && summonId != null && !DEBUG_ONLY_SUMMON_ID.equals(summonId)) return;
        ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] [%s] %s", shortId(summonUuid), msg);
    }

    private void dbgOncePerSummon(float dt, UUID summonUuid, @Nullable String summonId, String msg) {
        if (!DEBUG) return;
        if (shouldLog(dt, summonUuid, summonId)) {
            dbg(summonUuid, summonId, msg);
        }
    }

    private static String shortId(UUID u) {
        String s = String.valueOf(u);
        return (s.length() <= 8) ? s : s.substring(0, 8);
    }

    private void runSelfTests() {
        try {
            ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] SELF-TEST start. SLOT_BASE=%s", String.valueOf(SLOT_BASE));
            ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] SELF-TEST NPCEntity leash methods check...");
            for (String m : new String[]{"getLeashPoint", "setLeashHeading", "setLeashPitch"}) {
                boolean found = false;
                for (Method mm : NPCEntity.class.getMethods()) {
                    if (mm.getName().equals(m)) { found = true; break; }
                }
                ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem]  NPCEntity.%s -> %s", m, found ? "FOUND" : "NOT FOUND");
            }
            ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] SELF-TEST summonTagType=%s", String.valueOf(summonTagType));
            ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] SELF-TEST TargetUtil.getTargetBlock available -> %s",
                    (TargetUtil.class.getMethods().length > 0));
            ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] SELF-TEST done.");
        } catch (Throwable t) {
            ((HytaleLogger.Api) LOGGER.atWarning()).log("[SummonCombatFollowSystem] SELF-TEST failed: %s", String.valueOf(t));
            if (DEBUG_STACKTRACE) t.printStackTrace();
        }
    }

    // =======================
    // Inner abstractions (compile-friendly)
    // =======================
    private interface SummonMovement {
        void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t);
        void faceOwner(TransformComponent t, Vector3f ownerRot, double ownerYawRad, ModelFollowController controller);
        void faceTarget(TransformComponent t, Vector3d from, Vector3d to);
        default void setDesiredPointIfSupported(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> summonRef, Vector3d desired, double yawRad, double pitchRad) {}
    }

    private static final class LerpTransformMovement implements SummonMovement {
        @Override
        public void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t) {
            double alpha = Math.min(1.0, dt * speed);
            t.setPosition(new Vector3d(
                    cur.x + (target.x - cur.x) * alpha,
                    cur.y + (target.y - cur.y) * alpha,
                    cur.z + (target.z - cur.z) * alpha
            ));
        }

        @Override
        public void faceOwner(TransformComponent t, Vector3f ownerRot, double ownerYawRad, ModelFollowController controller) {
            float pitch = ownerRot.getPitch();
            float yaw = ownerRot.getYaw();

            double minPitch = -0.6;
            double maxPitch = 0.55;
            double clamped = clamp(pitch, minPitch, maxPitch);

            Vector3f rot = t.getRotation();
            rot.setPitch((float) clamped);
            rot.setYaw(yaw);
            rot.setRoll(0f);
        }

        @Override
        public void faceTarget(TransformComponent t, Vector3d from, Vector3d to) {
            double vx = to.x - from.x;
            double vz = to.z - from.z;
            float yawRad = (float) Math.atan2(-vx, -vz);
            Vector3f rot = t.getRotation();
            rot.setPitch(0f);
            rot.setYaw(yawRad);
            rot.setRoll(0f);
        }

        private static double clamp(double v, double a, double b) {
            return (v < a) ? a : (v > b) ? b : v;
        }
    }

    private final class NpcLeashMovement implements SummonMovement {
        private volatile Method mGetLeashPoint;
        private volatile Method mSetLeashHeading;
        private volatile Method mSetLeashPitch;

        @Override public void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t) {}
        @Override public void faceOwner(TransformComponent t, Vector3f ownerRot, double ownerYawRad, ModelFollowController controller) {}
        @Override public void faceTarget(TransformComponent t, Vector3d from, Vector3d to) {}

        @Override
        public void setDesiredPointIfSupported(Store<EntityStore> store, CommandBuffer<EntityStore> cb, Ref<EntityStore> summonRef, Vector3d desired, double yawRad, double pitchRad) {
            NPCEntity npc = store.getComponent(summonRef, NPCEntity.getComponentType());
            if (npc == null) return;

            try {
                ensureMethods();

                Object leashPoint = (mGetLeashPoint != null) ? mGetLeashPoint.invoke(npc) : null;
                boolean wrote = false;

                if (leashPoint != null) {
                    Method assign = findMethod(leashPoint.getClass(), "assign", Vector3d.class);
                    if (assign != null) {
                        assign.setAccessible(true);
                        assign.invoke(leashPoint, desired);
                        wrote = true;
                    } else {
                        wrote = tryWriteXYZFields(leashPoint, desired);
                    }
                }

                if (mSetLeashHeading != null) {
                    try { mSetLeashHeading.invoke(npc, (float) yawRad); } catch (Throwable ignored) {}
                }
                if (mSetLeashPitch != null) {
                    try { mSetLeashPitch.invoke(npc, (float) pitchRad); } catch (Throwable ignored) {}
                }

                if (DEBUG && DEBUG_NPC_LEASH) {
                    UUIDComponent uc = store.getComponent(summonRef, UUIDComponent.getComponentType());
                    UUID id = (uc != null) ? uc.getUuid() : null;
                    if (id != null && shouldLog(0f, id, DEBUG_ONLY_SUMMON_ID)) {
                        ((HytaleLogger.Api) LOGGER.atInfo()).log("[SummonCombatFollowSystem] [NPC-LEASH] wrote=%s desired=(%.2f %.2f %.2f)",
                                wrote, desired.x, desired.y, desired.z);
                    }
                }

            } catch (Throwable t) {
                if (DEBUG && DEBUG_NPC_LEASH) {
                    ((HytaleLogger.Api) LOGGER.atWarning()).log("[SummonCombatFollowSystem] [NPC-LEASH] failed: %s", String.valueOf(t));
                }
                if (DEBUG_STACKTRACE) t.printStackTrace();
            }
        }

        private void ensureMethods() {
            if (mGetLeashPoint == null) {
                mGetLeashPoint = findMethod(NPCEntity.class, "getLeashPoint");
                if (mGetLeashPoint != null) mGetLeashPoint.setAccessible(true);
            }
            if (mSetLeashHeading == null) {
                mSetLeashHeading = findMethod(NPCEntity.class, "setLeashHeading", float.class);
                if (mSetLeashHeading != null) mSetLeashHeading.setAccessible(true);
            }
            if (mSetLeashPitch == null) {
                mSetLeashPitch = findMethod(NPCEntity.class, "setLeashPitch", float.class);
                if (mSetLeashPitch != null) mSetLeashPitch.setAccessible(true);
            }
        }

        @Nullable
        private Method findMethod(Class<?> c, String name, Class<?>... params) {
            try { return c.getMethod(name, params); } catch (Throwable ignored) {}
            try { return c.getDeclaredMethod(name, params); } catch (Throwable ignored) {}
            return null;
        }

        private boolean tryWriteXYZFields(Object leashPoint, Vector3d desired) {
            try {
                Field fx = findField(leashPoint.getClass(), "x");
                Field fy = findField(leashPoint.getClass(), "y");
                Field fz = findField(leashPoint.getClass(), "z");
                if (fx == null || fy == null || fz == null) return false;
                fx.setAccessible(true); fy.setAccessible(true); fz.setAccessible(true);
                fx.setDouble(leashPoint, desired.x);
                fy.setDouble(leashPoint, desired.y);
                fz.setDouble(leashPoint, desired.z);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        @Nullable
        private Field findField(Class<?> c, String name) {
            try { return c.getField(name); } catch (Throwable ignored) {}
            try { return c.getDeclaredField(name); } catch (Throwable ignored) {}
            return null;
        }
    }

    private interface SummonAnimator {
        void setBaseAnim(UUID summonUuid, Ref<EntityStore> summonRef, String animSetId, boolean loop, Store<EntityStore> store, boolean forceReplay);
    }

    private static final class DefaultSummonAnimator implements SummonAnimator {
        private final AnimationSlot slot;
        private final ConcurrentHashMap<UUID, String> lastKey = new ConcurrentHashMap<>();

        private DefaultSummonAnimator(AnimationSlot slot) {
            this.slot = slot;
        }

        @Override
        public void setBaseAnim(UUID summonUuid, Ref<EntityStore> summonRef, String animSetId, boolean loop, Store<EntityStore> store, boolean forceReplay) {
            String key = animSetId + "|" + (loop ? "1" : "0");
            String last = lastKey.get(summonUuid);

            if (forceReplay) {
                lastKey.remove(summonUuid);
                last = null;
            }

            if (last == null || !last.equals(key)) {
                try {
                    com.hypixel.hytale.server.core.entity.AnimationUtils.playAnimation(summonRef, slot, animSetId, loop, store);
                } catch (Throwable ignored) {}
                lastKey.put(summonUuid, key);
            }
        }
    }

    private static final class NoopSummonAnimator implements SummonAnimator {
        @Override
        public void setBaseAnim(UUID summonUuid, Ref<EntityStore> summonRef, String animSetId, boolean loop, Store<EntityStore> store, boolean forceReplay) { }
    }

    private static double clamp(double v, double a, double b) {
        return (v < a) ? a : (v > b) ? b : v;
    }

    private static Vector3d applyOwnerHoverYOffset(Vector3d ownerPos, Vector3d point, double hoverAboveOwner, double maxAboveOwner) {
        double desiredY = ownerPos.y + hoverAboveOwner;
        double maxY = ownerPos.y + maxAboveOwner;
        if (desiredY > maxY) desiredY = maxY;
        return new Vector3d(point.x, desiredY, point.z);
    }

    private static float yawRadTo(Vector3d from, Vector3d to) {
        double vx = to.x - from.x;
        double vz = to.z - from.z;
        return (float) Math.atan2(-vx, -vz);
    }

    private static float pitchRadTo(Vector3d from, Vector3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h < 1e-6) return 0f;
        float p = (float) Math.atan2(dy, h);
        if (p < -1.2f) p = -1.2f;
        if (p > 1.2f) p = 1.2f;
        return p;
    }
}
