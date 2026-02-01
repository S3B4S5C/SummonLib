// File: SummonWormFollowSystem.java
package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import me.s3b4s5.summonlib.api.SummonRegistry;
import me.s3b4s5.summonlib.internal.animation.DefaultSummonAnimator;
import me.s3b4s5.summonlib.internal.impl.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.movement.LerpTransformMovement;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;
import me.s3b4s5.summonlib.systems.shared.SummonCombatFollowShared;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model worm segments follow the previous segment; NPC head is handled by SummonWormChargeSystem.
 * This version also cleans up orphan segments if head/prev disappears (so chains can't get stuck).
 */
public class SummonWormFollowSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LOG_PREFIX = "[SummonWormFollowSystem]";

    private static final boolean DEBUG = false;
    private static final float DEBUG_LOG_PERIOD_SEC = 0.50f;

    private static final float TARGET_CACHE_PERIOD_SEC = 0.10f;
    private static final float REBUILD_PERIOD_SEC = 0.25f;
    private static final double MOVE_EPS_SQ = 0.02;

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_MOVE = "Move";

    private static final AnimationSlot SLOT_BASE =
            SummonCombatFollowShared.resolveSlot("Idle", "Passive", "Movement");

    private static final LerpTransformMovement MOVE_LERP = new LerpTransformMovement();

    private final DefaultSummonAnimator animator = new DefaultSummonAnimator(SLOT_BASE);

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final ComponentType<EntityStore, WormTag> wormTagType;

    private final SummonTargetSelector targetSelector;

    private final ConcurrentHashMap<UUID, Set<UUID>> insideBySegment = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ownerTargetCacheCd = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Ref<EntityStore>>> cachedTargetsByOwner = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, UUID> prevBySegmentUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> chainBySegmentUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> indexBySegmentUuid = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Set<UUID>> segmentsByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ownerRebuildCooldown = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> debugCdBySummon = new ConcurrentHashMap<>();

    public SummonWormFollowSystem(
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        this.summonTagType = summonTagType;
        this.wormTagType = wormTagType;
        targetSelector = new SummonTargetSelector(summonTagType);
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                summonTagType,
                wormTagType,
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
        SummonTag sTag = chunk.getComponent(index, summonTagType);
        WormTag wTag = chunk.getComponent(index, wormTagType);
        UUIDComponent uuidComp = chunk.getComponent(index, UUIDComponent.getComponentType());
        if (sTag == null || wTag == null || uuidComp == null) return;

        UUID selfUuid = uuidComp.getUuid();
        Ref<EntityStore> selfRef = chunk.getReferenceTo(index);

        // This system is for model segments only
        if (store.getComponent(selfRef, NPCEntity.getComponentType()) != null) return;

        TransformComponent selfT = cb.getComponent(selfRef, TransformComponent.getComponentType());
        if (selfT == null) return;

        UUID ownerUuid = sTag.getOwnerUuid();
        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) {
            cleanupSegment(selfUuid, ownerUuid);
            cb.removeEntity(selfRef, RemoveReason.REMOVE);
            return;
        }

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() != store) {
            cleanupSegment(selfUuid, ownerUuid);
            cb.removeEntity(selfRef, RemoveReason.REMOVE);
            return;
        }

        if (tryRebuildOwnerChains(dt, ownerUuid)) rebuildChainsForOwner(store, ownerUuid);

        int segIndex = Math.max(0, wTag.segmentIndex);

        // If someone accidentally includes a model with segmentIndex=0, keep it idle
        if (segIndex <= 0) {
            animator.setBaseAnim(selfUuid, selfRef, ANIM_IDLE, true, store, false);
            return;
        }

        UUID prevUuid = prevBySegmentUuid.get(selfUuid);
        if (prevUuid == null) {
            rebuildChainsForOwner(store, ownerUuid);
            prevUuid = prevBySegmentUuid.get(selfUuid);
        }

        TransformComponent prevT = (prevUuid != null) ? findTransformByUuid(store, prevUuid) : null;

        // ✅ Orphan cleanup: if we still can't find prev (head removed / chain broken), remove the segment.
        if (prevT == null) {
            cleanupSegment(selfUuid, ownerUuid);
            cb.removeEntity(selfRef, RemoveReason.REMOVE);
            return;
        }

        Vector3d prevPos = prevT.getPosition();
        Vector3f prevRot = prevT.getRotation();

        double spacing = Math.max(0.05, wTag.spacing);
        Vector3d prevForward = forwardFromRotation(prevRot);

        Vector3d desired = new Vector3d(
                prevPos.x - prevForward.x * spacing,
                prevPos.y - prevForward.y * spacing,
                prevPos.z - prevForward.z * spacing
        );

        Vector3d cur = selfT.getPosition();

        double speed = 6.0;
        @Nullable SummonDefinition def = SummonRegistry.get(sTag.getSummonId());
        if (def != null && def.tuning != null) speed = Math.max(0.10, def.tuning.followSpeed);
        speed *= Math.max(0.05, wTag.followSpeedMul);

        MOVE_LERP.moveTowards(dt, cur, desired, speed, selfT);

        double maxDist = Math.max(spacing * 1.5, spacing * wTag.snapDistanceMul);
        double maxDistSq = maxDist * maxDist;

        double d2Prev = SummonCombatFollowShared.distSq(selfT.getPosition(), prevPos);
        if (d2Prev > maxDistSq) selfT.setPosition(desired);

        MOVE_LERP.faceTarget(selfT, selfT.getPosition(), prevPos);

        applyPassDamage(dt, store, cb, selfUuid, sTag, wTag, selfT, ownerRef);

        boolean moving = SummonCombatFollowShared.distSq(selfT.getPosition(), desired) > MOVE_EPS_SQ;
        animator.setBaseAnim(selfUuid, selfRef, moving ? ANIM_MOVE : ANIM_IDLE, true, store, false);

        if (DEBUG && shouldLog(dt, selfUuid)) {
            dbg(selfUuid,
                    "seg=" + segIndex
                            + " spacing=" + String.format(Locale.ROOT, "%.2f", spacing)
                            + " distToPrev=" + String.format(Locale.ROOT, "%.2f", Math.sqrt(d2Prev)));
        }
    }

    private boolean tryRebuildOwnerChains(float dt, UUID ownerUuid) {
        float cd = ownerRebuildCooldown.getOrDefault(ownerUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            ownerRebuildCooldown.put(ownerUuid, cd);
            return false;
        }
        ownerRebuildCooldown.put(ownerUuid, REBUILD_PERIOD_SEC);
        return true;
    }

    private void rebuildChainsForOwner(Store<EntityStore> store, UUID ownerUuid) {
        Map<UUID, List<Seg>> byChain = new HashMap<>();
        Set<UUID> newOwnedSet = ConcurrentHashMap.newKeySet();

        Query<EntityStore> q = Query.and(
                summonTagType,
                wormTagType,
                UUIDComponent.getComponentType(),
                TransformComponent.getComponentType(),
                NetworkId.getComponentType()
        );

        store.forEachChunk(q, (chunk, ccb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonTag sTag = chunk.getComponent(i, summonTagType);
                WormTag wTag = chunk.getComponent(i, wormTagType);
                UUIDComponent u = chunk.getComponent(i, UUIDComponent.getComponentType());
                if (sTag == null || wTag == null || u == null) continue;
                if (!ownerUuid.equals(sTag.getOwnerUuid())) continue;

                UUID segUuid = u.getUuid();
                newOwnedSet.add(segUuid);

                UUID chainId = wTag.chainId;
                if (chainId == null) continue;

                int idx = Math.max(0, wTag.segmentIndex);
                Ref<EntityStore> ref = chunk.getReferenceTo(i);

                byChain.computeIfAbsent(chainId, k -> new ArrayList<>())
                        .add(new Seg(chainId, segUuid, idx, ref));
            }
        });

        Set<UUID> old = segmentsByOwner.put(ownerUuid, newOwnedSet);
        if (old != null) {
            for (UUID oldUuid : old) {
                if (!newOwnedSet.contains(oldUuid)) {
                    prevBySegmentUuid.remove(oldUuid);
                    chainBySegmentUuid.remove(oldUuid);
                    indexBySegmentUuid.remove(oldUuid);
                    debugCdBySummon.remove(oldUuid);
                    insideBySegment.remove(oldUuid);
                }
            }
        }

        for (var e : byChain.entrySet()) {
            List<Seg> list = e.getValue();
            list.sort(Comparator.comparingInt(a -> a.index));

            for (int i = 0; i < list.size(); i++) {
                Seg seg = list.get(i);
                chainBySegmentUuid.put(seg.uuid, seg.chainId);
                indexBySegmentUuid.put(seg.uuid, seg.index);

                if (i == 0) prevBySegmentUuid.remove(seg.uuid);
                else prevBySegmentUuid.put(seg.uuid, list.get(i - 1).uuid);
            }
        }
    }

    private void cleanupSegment(UUID segUuid, UUID ownerUuid) {
        prevBySegmentUuid.remove(segUuid);
        chainBySegmentUuid.remove(segUuid);
        indexBySegmentUuid.remove(segUuid);
        debugCdBySummon.remove(segUuid);
        insideBySegment.remove(segUuid);

        Set<UUID> set = segmentsByOwner.get(ownerUuid);
        if (set != null) set.remove(segUuid);
    }

    private static final class Seg {
        final UUID chainId;
        final UUID uuid;
        final int index;
        final Ref<EntityStore> ref;

        Seg(UUID chainId, UUID uuid, int index, Ref<EntityStore> ref) {
            this.chainId = chainId;
            this.uuid = uuid;
            this.index = index;
            this.ref = ref;
        }
    }

    @Nullable
    private TransformComponent findTransformByUuid(Store<EntityStore> store, UUID uuid) {
        Query<EntityStore> q = Query.and(UUIDComponent.getComponentType(), TransformComponent.getComponentType());
        TransformComponent[] out = new TransformComponent[1];

        store.forEachChunk(q, (chunk, ccb) -> {
            if (out[0] != null) return;
            for (int i = 0; i < chunk.size(); i++) {
                UUIDComponent u = chunk.getComponent(i, UUIDComponent.getComponentType());
                if (u == null || !uuid.equals(u.getUuid())) continue;
                Ref<EntityStore> r = chunk.getReferenceTo(i);
                out[0] = store.getComponent(r, TransformComponent.getComponentType());
                return;
            }
        });

        return out[0];
    }

    private static Vector3d forwardFromRotation(Vector3f rot) {
        double yaw = rot.getYaw();
        double pitch = rot.getPitch();

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double sy = Math.sin(yaw);
        double cy = Math.cos(yaw);

        return new Vector3d(-sy * cp, sp, -cy * cp);
    }

    private void applyPassDamage(
            float dt,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            UUID segUuid,
            SummonTag sTag,
            WormTag wTag,
            TransformComponent segT,
            Ref<EntityStore> ownerRef
    ) {
        if (wTag.segmentIndex <= 0) return;

        SummonDefinition def = SummonRegistry.get(sTag.getSummonId());
        if (def == null || def.tuning == null) return;

        double hitR = Math.max(0.25, def.tuning.hitDistance);
        double hitR2 = hitR * hitR;

        UUID ownerUuid = sTag.getOwnerUuid();

        float cd = ownerTargetCacheCd.getOrDefault(ownerUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd <= 0f) {
            ownerTargetCacheCd.put(ownerUuid, TARGET_CACHE_PERIOD_SEC);
            cachedTargetsByOwner.put(
                    ownerUuid,
                    collectNearbyTargets(store, ownerRef, segT.getPosition(), def.detectRadius)
            );
        } else {
            ownerTargetCacheCd.put(ownerUuid, cd);
        }

        List<Ref<EntityStore>> targets = cachedTargetsByOwner.get(ownerUuid);
        if (targets == null || targets.isEmpty()) return;

        Set<UUID> inside = insideBySegment.computeIfAbsent(segUuid, k -> ConcurrentHashMap.newKeySet());
        Vector3d p = segT.getPosition();

        for (Ref<EntityStore> tr : targets) {
            if (tr == null || !tr.isValid()) continue;

            UUIDComponent uc = store.getComponent(tr, UUIDComponent.getComponentType());
            TransformComponent tt = store.getComponent(tr, TransformComponent.getComponentType());
            if (uc == null || tt == null) continue;

            UUID tu = uc.getUuid();
            if (tu == null) continue;

            double d2 = SummonCombatFollowShared.distSq(tt.getPosition(), p);
            boolean nowInside = d2 <= hitR2;

            if (nowInside) {
                if (!inside.contains(tu)) {
                    inside.add(tu);

                    float dmg = def.damage;
                    if (dmg > 0f) {
                        Damage damage = new Damage(new Damage.EntitySource(ownerRef), 1, dmg);
                        if (tr.equals(ownerRef)) continue;
                        cb.invoke(tr, damage);
                    }
                }
            } else {
                inside.remove(tu);
            }
        }
    }

    private List<Ref<EntityStore>> collectNearbyTargets(
            Store<EntityStore> store,
            Ref<EntityStore> ownerRef,
            Vector3d center,
            double radius
    ) {
        double r2 = radius * radius;

        Query<EntityStore> q = Query.and(
                UUIDComponent.getComponentType(),
                TransformComponent.getComponentType(),
                NetworkId.getComponentType()
        );

        ArrayList<Ref<EntityStore>> out = new ArrayList<>();

        store.forEachChunk(q, (chunk, ccb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> r = chunk.getReferenceTo(i);
                if (r == null || !r.isValid()) continue;

                if (r.equals(ownerRef)) continue;

                if (store.getComponent(r, summonTagType) != null) continue;
                if (store.getComponent(r, wormTagType) != null) continue;

                UUIDComponent u = chunk.getComponent(i, UUIDComponent.getComponentType());
                TransformComponent t = chunk.getComponent(i, TransformComponent.getComponentType());
                if (u == null || t == null) continue;

                double d2 = SummonCombatFollowShared.distSq(t.getPosition(), center);
                if (d2 > r2) continue;

                // TODO: FIX THIS
                if (!targetSelector.isTargetStillValid(
                        ownerRef,
                        store,
                        null,          // world not needed because LoS is disabled below
                        center,        // summonPos (we use center for radius check)
                        center,        // ownerEye dummy (unused because LoS disabled)
                        radius,
                        r,
                        false, false,  // requireOwnerLoS / requireSummonLoS = false (same behavior)
                        true           // requireHostileOnly
                )) continue;


                out.add(r);
            }
        });

        return out;
    }

    private boolean shouldLog(float dt, UUID uuid) {
        float cd = debugCdBySummon.getOrDefault(uuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            debugCdBySummon.put(uuid, cd);
            return false;
        }
        debugCdBySummon.put(uuid, DEBUG_LOG_PERIOD_SEC);
        return true;
    }

    private void dbg(UUID uuid, String msg) {
        ((HytaleLogger.Api) LOGGER.atInfo())
                .log("%s [%s] %s", LOG_PREFIX, SummonCombatFollowShared.shortId(uuid), msg);
    }
}
