package me.s3b4s5.summonlib.systems.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import me.s3b4s5.summonlib.assets.config.npc.motion.FlyNpcMotionControllerConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionController;
import me.s3b4s5.summonlib.internal.impl.definition.NpcRoleSummonDefinition;
import me.s3b4s5.summonlib.internal.targeting.SummonTargetSelector;
import me.s3b4s5.summonlib.internal.tick.ContextUtil;
import me.s3b4s5.summonlib.internal.tick.NpcUtil;
import me.s3b4s5.summonlib.runtime.SummonAggroRuntime;
import me.s3b4s5.summonlib.runtime.SummonIndexing;
import me.s3b4s5.summonlib.stats.SummonStats;
import me.s3b4s5.summonlib.tags.SummonTag;
import me.s3b4s5.summonlib.tags.WormTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummonNpcTargetSystem extends EntityTickingSystem<EntityStore> {

    private static final double COMBAT_EYE_SCALE = 0.60;
    private static final double COMBAT_Y_MIN = 0.25;
    private static final double COMBAT_Y_MAX = 1.05;

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final ComponentType<EntityStore, WormTag> wormTagType;

    private final SummonTargetSelector targetSelector;

    private final ConcurrentHashMap<UUID, Ref<EntityStore>> lastTargetBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Ref<EntityStore>> focusTargetByOwner = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Float> followLowTimerBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> followStuckTimerBySummon = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> followLastDistBySummon = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Float> ownerMaintenanceCooldown = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, FormationCache> formationByOwner = new ConcurrentHashMap<>();
    private final Query<EntityStore> formationQuery;

    public SummonNpcTargetSystem(
            ComponentType<EntityStore, SummonTag> summonTagType,
            ComponentType<EntityStore, WormTag> wormTagType
    ) {
        this.summonTagType = summonTagType;
        this.wormTagType = wormTagType;
        this.targetSelector = new SummonTargetSelector(summonTagType);

        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            this.formationQuery = Query.and(
                    summonTagType,
                    TransformComponent.getComponentType()
            );
        } else {
            this.formationQuery = Query.and(
                    summonTagType,
                    npcType,
                    TransformComponent.getComponentType()
            );
        }
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
        final ContextUtil.NpcSummonCtx ctx = ContextUtil.getNpcSummonCtxOrNull(
                index, chunk, store, cb,
                summonTagType, wormTagType
        );
        if (ctx == null) return;

        final UUID selfUuid = ctx.selfUuid();
        final UUID ownerUuid = ctx.ownerCtx().ownerUuid();
        if (selfUuid == null || ownerUuid == null) return;

        final Ref<EntityStore> selfRef = ctx.selfRef();
        if (selfRef == null || !selfRef.isValid()) return;

        final Role role = ctx.role();
        if (role == null) return;

        final MarkedEntitySupport marked = role.getMarkedEntitySupport();

        final NpcRoleSummonDefinition def = ctx.def();
        final NpcRoleSummonDefinition.Formation formation = def.formation;

        final NpcMotionController mc = def.motionController;
        final FlyNpcMotionControllerConfig fly = (mc instanceof FlyNpcMotionControllerConfig f) ? f : null;
        final boolean isFly = (fly != null);

        if (tryRunOwnerMaintenance(dt, ownerUuid, def.ownerMaintenanceCooldownSec)) {
            Ref<EntityStore> ownerRefMaint = ctx.ownerCtx().ownerRef();
            if (ownerRefMaint != null && ownerRefMaint.isValid()) {
                enforceNpcSlotsAndRebuild(store, cb, ownerRefMaint, ownerUuid);
            }
        }

        final Vector3d selfPos = ctx.selfT().getPosition();

        final Ref<EntityStore> ownerRef = ctx.ownerCtx().ownerRef();
        final TransformComponent ownerT = (ownerRef != null && ownerRef.isValid())
                ? store.getComponent(ownerRef, TransformComponent.getComponentType())
                : null;
        if (ownerT == null) return;

        final Vector3d ownerPos = ownerT.getPosition();
        final double distOwnerSelfSq = ownerPos.distanceSquaredTo(selfPos);

        final double ownerReturn = Math.max(0.0, def.leashSummonToOwner);
        final double ownerAcquire = ownerReturn * 0.75;
        final double targetMaxFromOwner = Math.max(0.0, def.leashTargetToOwner);

        final boolean forceReturn = distOwnerSelfSq > (ownerReturn * ownerReturn);
        if (forceReturn) {
            dropAllTargets(selfUuid, ownerUuid, marked);
        }

        Ref<EntityStore> targetRef = null;
        final boolean canAcquire = !forceReturn && distOwnerSelfSq <= (ownerAcquire * ownerAcquire);

        if (canAcquire) {
            Ref<EntityStore> focus = SummonAggroRuntime.pullAggroFocus(
                    store, ownerUuid, summonTagType, focusTargetByOwner
            );

            if (focus != null && (!focus.isValid() || !targetSelector.isAlive(focus, store))) {
                focusTargetByOwner.remove(ownerUuid);
                focus = null;
            } else if (focus != null) {
                focusTargetByOwner.put(ownerUuid, focus);
            }

            if (focus != null) {
                targetRef = focus;
            } else if (def.detectRadius > 0.0) {
                targetRef = targetSelector.select(
                        ctx.ownerCtx(),
                        store,
                        selfPos,
                        def.detectRadius,
                        lastTargetBySummon.get(selfUuid),
                        null,
                        def.requireOwnerLoS,
                        def.requireSummonLoS
                );
            }

            if (targetRef != null && targetRef.isValid()) {
                TransformComponent tt = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (tt == null) {
                    targetRef = null;
                } else {
                    double distTargetOwnerSq = ownerPos.distanceSquaredTo(tt.getPosition());
                    if (distTargetOwnerSq > (targetMaxFromOwner * targetMaxFromOwner)) {
                        focusTargetByOwner.remove(ownerUuid);
                        targetRef = null;
                    }
                }
            }
        }

        final Ref<EntityStore> prevSelected = lastTargetBySummon.get(selfUuid);
        final boolean selectedChanged = !Objects.equals(prevSelected, targetRef);

        if (targetRef != null && targetRef.isValid()) {
            lastTargetBySummon.put(selfUuid, targetRef);
            if (selectedChanged) marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, targetRef);

            followLowTimerBySummon.remove(selfUuid);
            followStuckTimerBySummon.remove(selfUuid);
            followLastDistBySummon.remove(selfUuid);
        } else {
            lastTargetBySummon.remove(selfUuid);
            if (selectedChanged) marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, null);
        }

        final double desiredLeashY = updateLeashAndReturnDesiredY(
                dt, store, cb,
                selfUuid, ownerUuid,
                selfRef, ownerRef,
                ownerPos, selfPos,
                targetRef,
                formation,
                fly
        );

        if (isFly && !Double.isNaN(desiredLeashY)) {
            final boolean hasTarget = (targetRef != null && targetRef.isValid());
            applyVerticalAssist(dt, ctx.selfT(), desiredLeashY, hasTarget, fly);
        }
    }

    private double updateLeashAndReturnDesiredY(
            float dt,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            UUID selfUuid,
            UUID ownerUuid,
            Ref<EntityStore> selfRef,
            Ref<EntityStore> ownerRef,
            Vector3d ownerPos,
            Vector3d selfPos,
            Ref<EntityStore> targetRef,
            NpcRoleSummonDefinition.Formation formation,
            FlyNpcMotionControllerConfig fly
    ) {
        if (selfRef == null || !selfRef.isValid()) return Double.NaN;

        final boolean isFly = (fly != null);

        if (targetRef != null && targetRef.isValid()) {
            TransformComponent tt = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (tt != null) {
                Vector3d leash = new Vector3d(tt.getPosition());

                if (isFly) {
                    double eye = NpcUtil.computeHeadOffset(store, targetRef);
                    double combatYOffset = clamp(eye * COMBAT_EYE_SCALE, COMBAT_Y_MIN, COMBAT_Y_MAX);
                    leash.y += combatYOffset;
                }

                NpcUtil.setLeashToPoint(selfRef, targetRef, store, cb, leash);
                return isFly ? leash.y : Double.NaN;
            }
        }

        Vector3d anchor = ownerPos;
        Vector3d formationOffset = Vector3d.ZERO;

        if (formation != null && formation.enabled) {
            FormationResult fr = getFormationFollow(ownerUuid, selfRef, store, ownerPos, dt, formation);
            anchor = fr.anchor;
            formationOffset = fr.offset;
        }

        Vector3d leash = new Vector3d(anchor);
        leash.x += formationOffset.x;
        leash.z += formationOffset.z;

        if (isFly) {
            double desiredYOffset = computeFollowYOffset(selfUuid, dt, anchor, formationOffset, selfPos, fly);
            desiredYOffset = clamp(desiredYOffset, fly.followYClampMin, fly.followYClampMax);
            leash.y += desiredYOffset;
        }

        NpcUtil.setLeashToPoint(selfRef, ownerRef, store, cb, leash);
        return isFly ? leash.y : Double.NaN;
    }

    private record FormationResult(Vector3d anchor, Vector3d offset) {
    }

    private record FormationEntry(Ref<EntityStore> ref, long spawnSeq) {
    }

    private static final class FormationCache {
        final Object lock = new Object();
        volatile long lastBuildNs = 0L;
        volatile long configHash = 0L;

        final Vector3d anchorPos = new Vector3d();
        final Vector3d lastOwnerPos = new Vector3d();
        boolean hasOwnerPos = false;

        double formationYaw = Double.NaN;

        int lastCount = 0;
        int ringCap = 8;

        final ConcurrentHashMap<Ref<EntityStore>, Integer> slotIndexByRef = new ConcurrentHashMap<>();
        final ConcurrentHashMap<Ref<EntityStore>, Vector3d> currentOffsetByRef = new ConcurrentHashMap<>();
    }

    private FormationResult getFormationFollow(
            UUID ownerUuid,
            Ref<EntityStore> selfRef,
            Store<EntityStore> store,
            Vector3d ownerPos,
            float dt,
            NpcRoleSummonDefinition.Formation formation
    ) {
        if (ownerUuid == null || selfRef == null || formation == null || !formation.enabled) {
            return new FormationResult(ownerPos, Vector3d.ZERO);
        }

        FormationCache cache = formationByOwner.computeIfAbsent(ownerUuid, k -> new FormationCache());
        long h = hashFormation(formation);

        if (cache.configHash != h) {
            synchronized (cache.lock) {
                if (cache.configHash != h) {
                    cache.configHash = h;
                    cache.lastBuildNs = 0L;
                    cache.slotIndexByRef.clear();
                    cache.currentOffsetByRef.clear();
                    cache.lastCount = 0;
                    cache.ringCap = Math.max(1, formation.ringCap);
                    cache.formationYaw = Double.NaN;
                    cache.hasOwnerPos = false;
                }
            }
        }

        updateAnchor(cache, ownerPos, dt, formation);
        updateFormationYaw(cache, dt, formation);
        maybeRebuildFormation(ownerUuid, store, cache, selfRef, formation);

        Integer slotIndex = cache.slotIndexByRef.get(selfRef);
        if (slotIndex == null) {
            return new FormationResult(cache.anchorPos, Vector3d.ZERO);
        }

        Vector3d desired = computeDesiredOffset(slotIndex, cache.lastCount, cache.ringCap, cache.formationYaw, selfRef, formation);
        Vector3d current = cache.currentOffsetByRef.computeIfAbsent(selfRef, k -> desired.clone());

        smoothOffset(current, desired, dt, formation);
        cache.currentOffsetByRef.put(selfRef, current);

        return new FormationResult(cache.anchorPos, current);
    }

    private void updateAnchor(FormationCache cache, Vector3d ownerPos, float dt, NpcRoleSummonDefinition.Formation f) {
        if (!cache.hasOwnerPos) {
            cache.anchorPos.assign(ownerPos);
            cache.lastOwnerPos.assign(ownerPos);
            cache.hasOwnerPos = true;
            return;
        }

        double dx = ownerPos.x - cache.anchorPos.x;
        double dz = ownerPos.z - cache.anchorPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        cache.lastOwnerPos.assign(ownerPos);

        if (dist < f.anchorDeadzone) return;

        double alpha = 1.0 - Math.exp(-f.anchorSmoothK * dt);
        cache.anchorPos.x += (ownerPos.x - cache.anchorPos.x) * alpha;
        cache.anchorPos.z += (ownerPos.z - cache.anchorPos.z) * alpha;
        cache.anchorPos.y = ownerPos.y;
    }

    private void updateFormationYaw(FormationCache cache, float dt, NpcRoleSummonDefinition.Formation f) {
        if (!cache.hasOwnerPos) return;

        if (Double.isNaN(cache.formationYaw)) {
            cache.formationYaw = 0.0;
            return;
        }

        double vx = cache.lastOwnerPos.x - cache.anchorPos.x;
        double vz = cache.lastOwnerPos.z - cache.anchorPos.z;

        double len = Math.sqrt(vx * vx + vz * vz);
        if (len < f.minMoveDist) return;

        double desiredYaw = Math.atan2(vx, vz);
        cache.formationYaw = smoothYaw(cache.formationYaw, desiredYaw, dt, f);
    }

    private double smoothYaw(double current, double desired, float dt, NpcRoleSummonDefinition.Formation f) {
        double alpha = 1.0 - Math.exp(-f.yawSmoothK * dt);
        double delta = turnAngle(current, desired);

        double maxStep = f.maxTurnSpeed * dt;
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;

        double stepped = wrapAngle(current + delta);
        return wrapAngle(current + turnAngle(current, stepped) * alpha);
    }

    private static double turnAngle(double from, double to) {
        double d = wrapAngle(to - from);
        if (d > Math.PI) d -= 2.0 * Math.PI;
        if (d < -Math.PI) d += 2.0 * Math.PI;
        return d;
    }

    private static double wrapAngle(double a) {
        while (a >= Math.PI) a -= 2.0 * Math.PI;
        while (a < -Math.PI) a += 2.0 * Math.PI;
        return a;
    }

    private void maybeRebuildFormation(
            UUID ownerUuid,
            Store<EntityStore> store,
            FormationCache cache,
            Ref<EntityStore> selfRef,
            NpcRoleSummonDefinition.Formation f
    ) {
        final long now = System.nanoTime();
        final long intervalNs = (long) (Math.max(0.0f, f.rebuildIntervalSec) * 1_000_000_000L);

        boolean timeExpired = intervalNs > 0L && (now - cache.lastBuildNs) > intervalNs;
        boolean missingSelf = !cache.slotIndexByRef.containsKey(selfRef);

        if (!timeExpired && !missingSelf) return;

        synchronized (cache.lock) {
            long now2 = System.nanoTime();
            boolean timeExpired2 = intervalNs > 0L && (now2 - cache.lastBuildNs) > intervalNs;
            boolean missingSelf2 = !cache.slotIndexByRef.containsKey(selfRef);

            if (!timeExpired2 && !missingSelf2) return;

            rebuildFormation(ownerUuid, store, cache, f);
            cache.lastBuildNs = now2;
        }
    }

    private void rebuildFormation(
            UUID ownerUuid,
            Store<EntityStore> store,
            FormationCache cache,
            NpcRoleSummonDefinition.Formation f
    ) {
        final ArrayList<FormationEntry> list = new ArrayList<>();

        store.forEachChunk(formationQuery, (c, ccb) -> {
            for (int i = 0; i < c.size(); i++) {
                SummonTag tag = c.getComponent(i, summonTagType);
                if (tag == null) continue;
                if (!ownerUuid.equals(tag.owner)) continue;

                Ref<EntityStore> ref = c.getReferenceTo(i);
                if (!ref.isValid()) continue;

                list.add(new FormationEntry(ref, tag.spawnSeq));
            }
        });

        if (list.isEmpty()) {
            cache.slotIndexByRef.clear();
            cache.currentOffsetByRef.clear();
            cache.lastCount = 0;
            cache.ringCap = Math.max(1, f.ringCap);
            return;
        }

        list.sort(
                Comparator
                        .comparingLong((FormationEntry e) -> e.spawnSeq)
                        .thenComparingInt(e -> System.identityHashCode(e.ref))
        );

        cache.lastCount = list.size();
        cache.ringCap = Math.max(1, f.ringCap);

        ConcurrentHashMap<Ref<EntityStore>, Vector3d> oldCurrent = new ConcurrentHashMap<>(cache.currentOffsetByRef);

        cache.slotIndexByRef.clear();
        for (int idx = 0; idx < list.size(); idx++) {
            Ref<EntityStore> ref = list.get(idx).ref;
            cache.slotIndexByRef.put(ref, idx);

            Vector3d keep = oldCurrent.get(ref);
            if (keep != null) cache.currentOffsetByRef.put(ref, keep);
        }

        cache.currentOffsetByRef.keySet().removeIf(r -> !cache.slotIndexByRef.containsKey(r));
    }

    private Vector3d computeDesiredOffset(
            int idx,
            int total,
            int ringCap,
            double yawBase,
            Ref<EntityStore> selfRef,
            NpcRoleSummonDefinition.Formation f
    ) {
        if (Double.isNaN(yawBase)) yawBase = 0.0;

        if (total <= 1) {
            double angle = yawBase + Math.PI;
            double radius = Math.min(f.baseRadius, f.maxRadius);
            return withJitter(selfRef, Math.cos(angle) * radius, Math.sin(angle) * radius, f.jitter);
        }

        int safeCap = Math.max(1, ringCap);
        int ring = idx / safeCap;
        int slot = idx % safeCap;

        double radius = f.baseRadius + ring * f.ringStep;
        radius = Math.min(radius, f.maxRadius);

        double angle = yawBase + (2.0 * Math.PI) * ((double) slot / (double) safeCap);
        angle += ring * (Math.PI / safeCap);

        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;

        return withJitter(selfRef, x, z, f.jitter);
    }

    private Vector3d withJitter(Ref<EntityStore> ref, double x, double z, double jitter) {
        if (jitter <= 0.0) return new Vector3d(x, 0.0, z);

        int h = ref.hashCode();
        double jx = hashToUnit(h * 1103515245 + 12345) * jitter;
        double jz = hashToUnit(h * 1664525 + 1013904223) * jitter;
        return new Vector3d(x + jx, 0.0, z + jz);
    }

    private static double hashToUnit(int h) {
        int v = (h ^ (h >>> 16)) & 0x7fffffff;
        double t = v / (double) 0x7fffffff;
        return (t * 2.0) - 1.0;
    }

    private void smoothOffset(Vector3d current, Vector3d desired, float dt, NpcRoleSummonDefinition.Formation f) {
        double dx = desired.x - current.x;
        double dz = desired.z - current.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1e-6) return;

        double alpha = 1.0 - Math.exp(-f.offsetSmoothK * dt);

        double nx = current.x + dx * alpha;
        double nz = current.z + dz * alpha;

        double stepX = nx - current.x;
        double stepZ = nz - current.z;
        double stepLen = Math.sqrt(stepX * stepX + stepZ * stepZ);

        double maxStep = f.offsetMaxSpeed * dt;
        if (stepLen > maxStep && stepLen > 1e-9) {
            double s = maxStep / stepLen;
            stepX *= s;
            stepZ *= s;
        }

        current.x += stepX;
        current.z += stepZ;
        current.y = 0.0;
    }

    private double computeFollowYOffset(
            UUID selfUuid,
            float dt,
            Vector3d anchorPos,
            Vector3d formationOffset,
            Vector3d selfPos,
            FlyNpcMotionControllerConfig fly
    ) {
        float lowTimer = followLowTimerBySummon.getOrDefault(selfUuid, 0f);
        if (lowTimer > 0f) {
            lowTimer = Math.max(0f, lowTimer - dt);
            followLowTimerBySummon.put(selfUuid, lowTimer);
            return fly.followYLow;
        }

        Vector3d highPoint = new Vector3d(anchorPos);
        highPoint.x += formationOffset.x;
        highPoint.z += formationOffset.z;
        highPoint.y += fly.followYHigh;

        double dist = Math.sqrt(highPoint.distanceSquaredTo(selfPos));
        Double last = followLastDistBySummon.get(selfUuid);

        float stuck = followStuckTimerBySummon.getOrDefault(selfUuid, 0f);

        if (last != null) {
            double improve = last - dist;
            if (dist > fly.stuckMinDist && improve < fly.stuckImproveEps) stuck += dt;
            else stuck = 0f;
        }

        followLastDistBySummon.put(selfUuid, dist);
        followStuckTimerBySummon.put(selfUuid, stuck);

        if (stuck >= fly.stuckTimeSec) {
            followStuckTimerBySummon.put(selfUuid, 0f);
            followLowTimerBySummon.put(selfUuid, fly.lowModeHoldSec);
            return fly.followYLow;
        }

        return fly.followYHigh;
    }

    private void applyVerticalAssist(
            float dt,
            TransformComponent selfT,
            double desiredY,
            boolean combatMode,
            FlyNpcMotionControllerConfig fly
    ) {
        if (selfT == null) return;
        if (dt <= 0f) return;

        Vector3d pos = selfT.getPosition();

        double y = pos.y;
        double dy = desiredY - y;

        if (Math.abs(dy) <= fly.verticalDeadzone) return;

        final double k = combatMode ? fly.vertCombatK : fly.vertFollowK;
        final double maxUp = combatMode ? fly.vertCombatMaxUp : fly.vertFollowMaxUp;
        final double maxDown = combatMode ? fly.vertCombatMaxDown : fly.vertFollowMaxDown;

        double alpha = 1.0 - Math.exp(-k * dt);
        double ySmooth = y + dy * alpha;

        double step = ySmooth - y;
        double maxStepUp = maxUp * dt;
        double maxStepDown = maxDown * dt;

        if (step > maxStepUp) step = maxStepUp;
        if (step < -maxStepDown) step = -maxStepDown;

        pos.y = y + step;
    }

    private void dropAllTargets(UUID selfUuid, UUID ownerUuid, MarkedEntitySupport marked) {
        focusTargetByOwner.remove(ownerUuid);
        lastTargetBySummon.remove(selfUuid);
        marked.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, null);
    }

    private boolean tryRunOwnerMaintenance(float dt, UUID ownerUuid, float cooldownSec) {
        float cd = ownerMaintenanceCooldown.getOrDefault(ownerUuid, 0f);
        cd = Math.max(0f, cd - dt);
        if (cd > 0f) {
            ownerMaintenanceCooldown.put(ownerUuid, cd);
            return false;
        }
        ownerMaintenanceCooldown.put(ownerUuid, Math.max(0f, cooldownSec));
        return true;
    }

    private void enforceNpcSlotsAndRebuild(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            Ref<EntityStore> ownerRef,
            UUID ownerUuid
    ) {
        final int capSlots = SummonStats.getMaxSlots(store, ownerRef);

        final ArrayList<Ref<EntityStore>> refs = new ArrayList<>();

        final ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) return;

        final Query<EntityStore> q = Query.and(
                summonTagType,
                npcType,
                UUIDComponent.getComponentType(),
                NetworkId.getComponentType(),
                TransformComponent.getComponentType()
        );

        store.forEachChunk(q, (c, ccb) -> {
            for (int i = 0; i < c.size(); i++) {
                final SummonTag t = c.getComponent(i, summonTagType);
                if (t == null) continue;
                if (!ownerUuid.equals(t.owner)) continue;
                refs.add(c.getReferenceTo(i));
            }
        });

        int usedSlots = 0;
        for (Ref<EntityStore> r : refs) {
            final SummonTag t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots += Math.max(0, t.slotCost);
        }

        refs.sort(Comparator.comparingLong((Ref<EntityStore> r) -> {
            final SummonTag t = store.getComponent(r, summonTagType);
            return (t != null) ? t.spawnSeq : Long.MIN_VALUE;
        }));

        while (usedSlots > capSlots && !refs.isEmpty()) {
            final int last = refs.size() - 1;
            final Ref<EntityStore> r = refs.remove(last);
            final SummonTag t = store.getComponent(r, summonTagType);
            if (t != null) usedSlots -= Math.max(0, t.slotCost);
            cb.removeEntity(r, RemoveReason.REMOVE);
        }

        SummonIndexing.rebuildOwnerIndices(store, cb, summonTagType, ownerUuid, refs);

        final Ref<EntityStore> focus = focusTargetByOwner.get(ownerUuid);
        if (focus != null && (!focus.isValid() || !targetSelector.isAlive(focus, store))) {
            focusTargetByOwner.remove(ownerUuid);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static long hashFormation(NpcRoleSummonDefinition.Formation f) {
        if (f == null) return 0L;
        long h = 1469598103934665603L;
        h = mix(h, f.enabled ? 1L : 0L);
        h = mix(h, Double.doubleToLongBits(f.baseRadius));
        h = mix(h, Double.doubleToLongBits(f.ringStep));
        h = mix(h, f.ringCap);
        h = mix(h, Double.doubleToLongBits(f.jitter));
        h = mix(h, Double.doubleToLongBits(f.maxRadius));
        h = mix(h, Double.doubleToLongBits(f.minMoveDist));
        h = mix(h, Double.doubleToLongBits(f.maxTurnSpeed));
        h = mix(h, Double.doubleToLongBits(f.yawSmoothK));
        h = mix(h, Double.doubleToLongBits(f.anchorDeadzone));
        h = mix(h, Double.doubleToLongBits(f.anchorSmoothK));
        h = mix(h, Double.doubleToLongBits(f.offsetSmoothK));
        h = mix(h, Double.doubleToLongBits(f.offsetMaxSpeed));
        h = mix(h, Float.floatToIntBits(f.rebuildIntervalSec));
        return h;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 1099511628211L;
        return h;
    }
}
