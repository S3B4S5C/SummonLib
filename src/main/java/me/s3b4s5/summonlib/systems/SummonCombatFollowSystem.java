package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SpawnUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import me.s3b4s5.summonlib.api.SummonDefinition;
import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.api.follow.BackOrbitFollowController;
import me.s3b4s5.summonlib.api.follow.ModelFollowController;
import me.s3b4s5.summonlib.runtime.SummonIndexing;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SummonCombatFollowSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, SummonTag> summonTagType;

    private final double followSpeed = 16.0;
    private final double travelToTargetSpeed = 10.0;

    private final double hitDistance = 1.2;

    private static final float HIT_DAMAGE_DELAY_SEC = 0.14f;
    private static final float ATTACK_INTERVAL_SEC = 0.45f;
    private static final boolean KEEP_ATTACK_WHILE_HAS_TARGET = true;

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";
    private static final String ANIM_ATTACK = "Attack";
    private static final AnimationSlot SLOT_BASE = resolveSlot("Idle", "Passive", "Movement");

    private final double leashSummonToOwner = 18.0;
    private final double leashTargetToOwner = 14.0;

    private final ConcurrentHashMap<UUID, String> lastBaseKeyBySummon = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> startDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> attackCooldownBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> pendingDamageDelayBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> pendingDamageTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> attackModeBySummon = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ownerMaintenanceCooldown = new ConcurrentHashMap<>();

    private volatile int cachedHealthIndex = -1;

    private static final ModelFollowController DEFAULT_CONTROLLER =
            new BackOrbitFollowController(0.4, 1.4, 120.0, 0.8, 0.9, 0.8 * 0.6);

    private static final ComponentType<EntityStore, Player> PLAYER_TYPE = Player.getComponentType();
    private static final ComponentType<EntityStore, NPCEntity> NPC_TYPE = NPCEntity.getComponentType();


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
        SummonTag tag = chunk.getComponent(index, summonTagType);
        Ref<EntityStore> summonRef = chunk.getReferenceTo(index);
        UUIDComponent uuidComp = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (tag == null || uuidComp == null) return;

        UUID ownerUuid = tag.getOwnerUuid();
        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) return;

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid()) return;

        Store<EntityStore> ownerAccessor = ownerRef.getStore();

        TransformComponent summonT = cb.getComponent(summonRef, TransformComponent.getComponentType());
        if (summonT == null) return;

        UUID summonUuid = uuidComp.getUuid();
        World world = store.getExternalData().getWorld();

        SummonDefinition def = SummonRegistry.get(tag.getSummonId());
        if (def == null) return;

        double detectRadius = def.detectRadius;
        float hitDamage = def.damage;
        boolean requireOwnerLoS = def.requireOwnerLoS;
        boolean requireSummonLoS = def.requireSummonLoS;

        ModelFollowController controller = (def.followController != null) ? def.followController : DEFAULT_CONTROLLER;

        Transform ownerTr = owner.getTransform();
        Vector3d ownerPos = ownerTr.getPosition();

        Transform ownerLook = TargetUtil.getLook(ownerRef, ownerAccessor);
        Vector3d ownerEye = ownerLook.getPosition();
        Object ownerRotObj = ownerLook.getRotation();
        double yawRad = ownerLook.getRotation().getYaw();

        if (tryRunOwnerMaintenance(dt, ownerUuid)) {
            enforceSlotsAndRebuild(store, cb, ownerRef, ownerUuid);
        }

        Vector3d home = controller.computeHome(
                ownerPos,
                yawRad,
                Math.max(0, tag.groupIndex),
                Math.max(1, tag.groupTotal)
        );

        Vector3d cur = summonT.getPosition();

        Ref<EntityStore> focus = focusTargetByOwner.get(ownerUuid);
        if (focus != null && (!focus.isValid() || !isAlive(focus, store))) {
            focusTargetByOwner.remove(ownerUuid);
            focus = null;
        }

        Ref<EntityStore> targetRef = null;
        if (detectRadius > 0.0) {
            targetRef = getOrUpdateSummonTarget(
                    summonUuid, ownerUuid, ownerRef,
                    store, world, cur, ownerEye, detectRadius, focus,
                    requireOwnerLoS, requireSummonLoS
            );
            if (targetRef != null) {
                focusTargetByOwner.putIfAbsent(ownerUuid, targetRef);
            }
        } else {
            dropSummonTarget(summonUuid);
        }

        if (targetRef != null && targetRef.isValid()) {
            double sx = cur.x - ownerPos.x, sy = cur.y - ownerPos.y, sz = cur.z - ownerPos.z;
            if (sx * sx + sy * sy + sz * sz > leashSummonToOwner * leashSummonToOwner) {
                focusTargetByOwner.remove(ownerUuid);
                dropSummonTarget(summonUuid);
                setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, true);
                faceOwner(summonT, ownerRotObj, yawRad, controller);
                moveTowards(dt, cur, home, followSpeed, summonT);
                return;
            }

            TransformComponent tt = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (tt != null) {
                Vector3d tp = tt.getPosition();
                double tx = tp.x - ownerPos.x, ty = tp.y - ownerPos.y, tz = tp.z - ownerPos.z;
                if (tx * tx + ty * ty + tz * tz > leashTargetToOwner * leashTargetToOwner) {
                    focusTargetByOwner.remove(ownerUuid);
                    dropSummonTarget(summonUuid);
                    setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, true);
                    faceOwner(summonT, ownerRotObj, yawRad, controller);
                    moveTowards(dt, cur, home, followSpeed, summonT);
                    return;
                }
            }
        }

        Ref<EntityStore> lastT = lastTargetBySummon.get(summonUuid);
        boolean changed = (lastT == null && targetRef != null) || (lastT != null && (targetRef == null || !lastT.equals(targetRef)));

        if (changed) {
            if (targetRef == null) lastTargetBySummon.remove(summonUuid);
            else lastTargetBySummon.put(summonUuid, targetRef);

            pendingDamageTargetBySummon.remove(summonUuid);
            pendingDamageDelayBySummon.put(summonUuid, 0f);
            attackCooldownBySummon.put(summonUuid, 0f);

            if (targetRef != null) {
                int gi = Math.max(0, tag.globalIndex);
                int gt = Math.max(1, tag.globalTotal);
                float stagger = computeStartStagger(gi, gt);
                startDelayBySummon.put(summonUuid, stagger);

                boolean startNow = (stagger <= 0f);
                attackModeBySummon.put(summonUuid, startNow);

                if (startNow && KEEP_ATTACK_WHILE_HAS_TARGET) {
                    setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                }
            } else {
                startDelayBySummon.put(summonUuid, 0f);
                attackModeBySummon.put(summonUuid, false);
            }
        }

        float startDelay = startDelayBySummon.getOrDefault(summonUuid, 0f);
        if (targetRef != null) {
            boolean attackMode = Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));

            if (startDelay > 0f) {
                float prev = startDelay;
                startDelay = Math.max(0f, startDelay - dt);
                startDelayBySummon.put(summonUuid, startDelay);

                if (prev > 0f && startDelay <= 0f) {
                    attackModeBySummon.put(summonUuid, true);
                    if (KEEP_ATTACK_WHILE_HAS_TARGET) {
                        setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                    }
                }
            } else {
                if (!attackMode) {
                    attackModeBySummon.put(summonUuid, true);
                    if (KEEP_ATTACK_WHILE_HAS_TARGET) {
                        setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                    }
                }
                startDelayBySummon.put(summonUuid, 0f);
            }
        } else {
            startDelayBySummon.put(summonUuid, 0f);
        }

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
                        world, cur, ownerEye,
                        requireOwnerLoS, requireSummonLoS,
                        hitDamage
                );
            }
        }

        if (targetRef != null && targetRef.isValid()) {
            TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetT == null) {
                dropSummonTarget(summonUuid);
                setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, false);
                faceOwner(summonT, ownerRotObj, yawRad, controller);
                moveTowards(dt, cur, home, followSpeed, summonT);
                return;
            }

            Vector3d tp = targetT.getPosition();

            if (!passesLoS(world, cur, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
                focusTargetByOwner.remove(ownerUuid);
                dropSummonTarget(summonUuid);
                setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, true);
                faceOwner(summonT, ownerRotObj, yawRad, controller);
                moveTowards(dt, cur, home, followSpeed, summonT);
                return;
            }

            Vector3d anchor = controller.computeAttackAnchor(
                    tp,
                    Math.max(0, tag.globalIndex),
                    Math.max(1, tag.globalTotal)
            );

            moveTowards(dt, cur, anchor, travelToTargetSpeed, summonT);

            boolean attackMode = Boolean.TRUE.equals(attackModeBySummon.get(summonUuid));
            if (KEEP_ATTACK_WHILE_HAS_TARGET && attackMode)
                setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, false);
            else setBaseAnim(summonUuid, summonRef, ANIM_MOVE, true, store, false);

            if (attackMode
                    && pendingDamageTargetBySummon.get(summonUuid) == null
                    && pendingDamageDelayBySummon.getOrDefault(summonUuid, 0f) <= 0f
                    && attackCooldownBySummon.getOrDefault(summonUuid, 0f) <= 0f) {

                double dx = anchor.x - summonT.getPosition().x;
                double dy = anchor.y - summonT.getPosition().y;
                double dz = anchor.z - summonT.getPosition().z;
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq <= (hitDistance * hitDistance)) {
                    if (store.getComponent(targetRef, NetworkId.getComponentType()) != null) {
                        pendingDamageTargetBySummon.put(summonUuid, targetRef);
                        pendingDamageDelayBySummon.put(summonUuid, HIT_DAMAGE_DELAY_SEC);
                        attackCooldownBySummon.put(summonUuid, ATTACK_INTERVAL_SEC);
                        setBaseAnim(summonUuid, summonRef, ANIM_ATTACK, true, store, true);
                    } else {
                        attackCooldownBySummon.put(summonUuid, 0.10f);
                    }
                }
            }

            faceTargetLook(summonT, summonT.getPosition(), tp);
            return;
        }

        dropSummonTarget(summonUuid);

        Vector3d dHome = new Vector3d(home.x - cur.x, home.y - cur.y, home.z - cur.z);
        boolean returning = (dHome.x * dHome.x + dHome.y * dHome.y + dHome.z * dHome.z) > 0.02;

        setBaseAnim(summonUuid, summonRef, returning ? ANIM_MOVE : ANIM_IDLE, true, store, false);
        faceOwner(summonT, ownerRotObj, yawRad, controller);
        moveTowards(dt, cur, home, followSpeed, summonT);
    }

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
                Ref<EntityStore> r = chunk.getReferenceTo(i);
                refs.add(r);
            }
        });

        for (Ref<EntityStore> r : refs) {
            SummonTag t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots += Math.max(0, t.slotCost);
        }

        // Remove newest last until under cap
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
        if (focus != null && (!focus.isValid() || !isAlive(focus, store))) {
            focusTargetByOwner.remove(ownerUuid);
        }
    }

    private boolean tryRunOwnerMaintenance(float dt, UUID ownerUuid) {
        float cd = ownerMaintenanceCooldown.getOrDefault(ownerUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            ownerMaintenanceCooldown.put(ownerUuid, cd);
            return false;
        }
        ownerMaintenanceCooldown.put(ownerUuid, 0.35f);
        return true;
    }

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
        if (current != null && current.isValid() && isAlive(current, store)) {
            TransformComponent t = store.getComponent(current, TransformComponent.getComponentType());
            if (t != null) {
                Vector3d tp = t.getPosition();
                if (isWithin(summonPos, tp, radius) && passesLoS(world, summonPos, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
                    return current;
                }
            }
        }

        if (preferred != null && preferred.isValid() && isAlive(preferred, store)) {

            // Only accept hostile targets (strict)
            if (isAllowedTargetHostileOnly(store, ownerRef, preferred)
                    && !preferred.equals(ownerRef)
                    && store.getComponent(preferred, summonTagType) == null
                    && store.getComponent(preferred, NetworkId.getComponentType()) != null) {

                TransformComponent pt = store.getComponent(preferred, TransformComponent.getComponentType());
                if (pt != null) {
                    Vector3d pp = pt.getPosition();

                    if (isWithin(summonPos, pp, radius)
                            && passesLoS(world, summonPos, ownerEye, pp, requireOwnerLoS, requireSummonLoS)) {
                        return preferred;
                    }
                }
            }
        }


        Ref<EntityStore> next = findClosestAliveVisibleInSphere(
                summonPos, ownerEye, radius, store, world, ownerRef, requireOwnerLoS, requireSummonLoS
        );

        if (next != null) {
            Ref<EntityStore> f = focusTargetByOwner.get(ownerUuid);
            if (f == null || !f.isValid() || !isAlive(f, store)) {
                focusTargetByOwner.put(ownerUuid, next);
            }
        }

        return next;
    }

    @Nullable
    private Ref<EntityStore> findClosestAliveVisibleInSphere(
            Vector3d center,
            Vector3d ownerEye,
            double radius,
            Store<EntityStore> store,
            World world,
            Ref<EntityStore> ownerRef,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        List<Ref<EntityStore>> list = TargetUtil.getAllEntitiesInSphere(center, radius, store);

        Ref<EntityStore> best = null;
        double bestD2 = Double.MAX_VALUE;

        for (Ref<EntityStore> r : list) {
            if (r == null || !r.isValid()) continue;
            if (r.equals(ownerRef)) continue;
            if (store.getComponent(r, summonTagType) != null) continue;
            if (store.getComponent(r, NetworkId.getComponentType()) == null) continue;

            if (!isAllowedTargetHostileOnly(store, ownerRef, r)) continue;

            TransformComponent t = store.getComponent(r, TransformComponent.getComponentType());
            if (t == null) continue;
            if (!isAlive(r, store)) continue;

            Vector3d p = t.getPosition();
            if (!passesLoS(world, center, ownerEye, p, requireOwnerLoS, requireSummonLoS)) continue;

            double dx = p.x - center.x, dy = p.y - center.y, dz = p.z - center.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = r;
            }
        }

        return best;
    }

    private boolean passesLoS(World world, Vector3d summonPos, Vector3d ownerEye, Vector3d targetPos, boolean requireOwnerLoS, boolean requireSummonLoS) {
        if (requireSummonLoS && !hasLineOfSight(world, summonPos, targetPos)) return false;
        if (requireOwnerLoS && !hasLineOfSight(world, ownerEye, targetPos)) return false;
        return true;
    }

    private boolean hasLineOfSight(World world, Vector3d from, Vector3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= 0.25) return true;

        double inv = 1.0 / dist;
        double dirX = dx * inv;
        double dirY = dy * inv;
        double dirZ = dz * inv;

        return TargetUtil.getTargetBlock(
                world,
                (blockId, fluidId) -> blockId != 0,
                from.x, from.y, from.z,
                dirX, dirY, dirZ,
                Math.max(0.0, dist - 0.10)
        ) == null;
    }

    private void dropSummonTarget(UUID summonUuid) {
        attackModeBySummon.put(summonUuid, false);
        startDelayBySummon.put(summonUuid, 0f);
        attackCooldownBySummon.put(summonUuid, 0f);
        pendingDamageTargetBySummon.remove(summonUuid);
        pendingDamageDelayBySummon.put(summonUuid, 0f);
        lastTargetBySummon.remove(summonUuid);
    }

    private boolean isWithin(Vector3d a, Vector3d b, double radius) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

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
        if (!isAlive(targetRef, store)) return;
        if (store.getComponent(targetRef, NetworkId.getComponentType()) == null) return;

        TransformComponent targetT = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetT == null) return;

        Vector3d tp = targetT.getPosition();
        if (!passesLoS(world, summonPosNow, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) {
            attackCooldownBySummon.put(summonUuid, 0.15f);
            return;
        }

        Damage damage = new Damage(new Damage.EntitySource(ownerRef), 1, hitDamage);
        cb.invoke(targetRef, damage);
    }

    private float computeStartStagger(int idx, int count) {
        float step = ATTACK_INTERVAL_SEC / Math.max(1, count);
        return step * Math.max(0, idx);
    }

    private void moveTowards(float dt, Vector3d cur, Vector3d target, double speed, TransformComponent t) {
        double alpha = Math.min(1.0, dt * speed);
        if (alpha > 1.0) alpha = 1.0;
        t.setPosition(new Vector3d(
                cur.x + (target.x - cur.x) * alpha,
                cur.y + (target.y - cur.y) * alpha,
                cur.z + (target.z - cur.z) * alpha
        ));
    }

    private boolean isAlive(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return true;

        float hp = readHealthSafe(statMap);
        if (Float.isNaN(hp)) return true;
        return hp > 0.001f;
    }

    private float readHealthSafe(EntityStatMap statMap) {
        var v8 = statMap.get(8);
        if (v8 != null && "Health".equals(v8.getId())) {
            cachedHealthIndex = 8;
            return v8.get();
        }

        if (cachedHealthIndex < 0) {
            int idx = EntityStatType.getAssetMap().getIndex("Health");
            if (idx >= 0) cachedHealthIndex = idx;
        }

        if (cachedHealthIndex >= 0) {
            var v = statMap.get(cachedHealthIndex);
            if (v != null) return v.get();
        }

        for (int idx = 0; idx < statMap.size(); idx++) {
            var v = statMap.get(idx);
            if (v == null) continue;
            String sid = v.getId();
            if (sid == null) continue;
            String low = sid.toLowerCase(Locale.ROOT);
            if (low.contains("health") || low.contains("hp")) {
                cachedHealthIndex = idx;
                return v.get();
            }
        }

        return Float.NaN;
    }

    private void setBaseAnim(UUID summonUuid, Ref<EntityStore> summonRef, String animSetId, boolean loop, Store<EntityStore> store, boolean forceReplay) {
        String key = animSetId + "|" + (loop ? "1" : "0");
        String last = lastBaseKeyBySummon.get(summonUuid);

        if (forceReplay) {
            lastBaseKeyBySummon.remove(summonUuid);
            last = null;
        }

        if (last == null || !last.equals(key)) {
            AnimationUtils.playAnimation(summonRef, SLOT_BASE, animSetId, loop, store);
            lastBaseKeyBySummon.put(summonUuid, key);
        }
    }

    private static AnimationSlot resolveSlot(String... names) {
        for (String n : names) {
            try {
                return AnimationSlot.valueOf(n);
            } catch (Throwable ignored) {
            }
        }
        return AnimationSlot.values()[0];
    }

    private void faceOwner(
            TransformComponent t,
            Object ownerRotation,
            double ownerYawRad,
            ModelFollowController controller
    ) {
        double minPitch = -0.6;
        double maxPitch = 0.55;
        if (controller instanceof me.s3b4s5.summonlib.api.follow.OwnerPitchClamp pc) {
            // We'll use pc.clampOwnerPitch(...) below, keep defaults only as fallback.
        }

        // Try to preserve owner's yaw/roll but clamp pitch.
        if (ownerRotation instanceof com.hypixel.hytale.math.vector.Vector3f v) {
            float pitch = v.getPitch();
            float yaw = v.getYaw();
            float roll = v.getRoll();

            double clamped = (controller instanceof me.s3b4s5.summonlib.api.follow.OwnerPitchClamp pc)
                    ? pc.clampOwnerPitch(pitch)
                    : clamp(pitch, minPitch, maxPitch);

            var rot = t.getRotation();
            rot.setPitch((float) clamped);
            rot.setYaw(yaw);
            rot.setRoll(roll);
            return;
        }

        // Fallback: yaw only, no pitch.
        var rot = t.getRotation();
        rot.setPitch(0f);
        rot.setYaw((float) ownerYawRad);
        rot.setRoll(0f);
    }

    private static double clamp(double v, double a, double b) {
        return (v < a) ? a : (v > b) ? b : v;
    }


    private void faceTargetLook(TransformComponent t, Vector3d from, Vector3d to) {
        double vx = to.x - from.x;
        double vz = to.z - from.z;
        float yawRad = (float) Math.atan2(-vx, -vz);
        var rot = t.getRotation();
        rot.setPitch(0f);
        rot.setYaw(yawRad);
        rot.setRoll(0f);
    }

    private boolean isAllowedTargetHostileOnly(
            Store<EntityStore> store,
            Ref<EntityStore> ownerRef,
            Ref<EntityStore> candidateRef
    ) {
        if (candidateRef == null || !candidateRef.isValid()) return false;

        // Never target players
        if (store.getArchetype(candidateRef).contains(PLAYER_TYPE)) return false;

        // Only target NPCs (strict hostile-only)
        NPCEntity npc = store.getComponent(candidateRef, NPC_TYPE);
        if (npc == null) return false;

        Role role = tryGetRoleFromNpc(npc);
        if (role == null) return false;

        Attitude a = tryGetAttitude(role, candidateRef, ownerRef, store);
        return a == Attitude.HOSTILE;
    }

    private Role tryGetRoleFromNpc(NPCEntity npc) {
        try {
            // Any public no-arg method returning Role
            for (Method m : npc.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Role.class.isAssignableFrom(m.getReturnType())) continue;
                Object r = m.invoke(npc);
                if (r instanceof Role role) return role;
            }
        } catch (Throwable ignored) {
        }

        try {
            // Any field of type Role (in case there is no getter)
            for (Field f : npc.getClass().getDeclaredFields()) {
                if (!Role.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object r = f.get(npc);
                if (r instanceof Role role) return role;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Attitude tryGetAttitude(Role role, Ref<EntityStore> selfRef, Ref<EntityStore> targetRef, ComponentAccessor<EntityStore> accessor) {
        try {
            Object ws = role.getWorldSupport();

            // Try getAttitude(Ref, Ref, ComponentAccessor)
            for (Method m : ws.getClass().getMethods()) {
                if (!m.getName().equals("getAttitude")) continue;
                if (m.getParameterCount() != 3) continue;

                Class<?>[] p = m.getParameterTypes();
                if (!Ref.class.isAssignableFrom(p[0])) continue;
                if (!Ref.class.isAssignableFrom(p[1])) continue;
                if (!p[2].isAssignableFrom(accessor.getClass()) && !ComponentAccessor.class.isAssignableFrom(p[2]))
                    continue;

                Object out = m.invoke(ws, selfRef, targetRef, accessor);
                if (out instanceof Attitude a) return a;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

}
