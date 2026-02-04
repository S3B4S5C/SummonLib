// File: SummonTargetSelector.java
package me.s3b4s5.summonlib.internal.targeting;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import me.s3b4s5.summonlib.internal.tick.ContextUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One-stop targeting facade for systems:
 * - select target
 * - alive check
 * - LoS check
 * - height over ground
 *
 * Systems should not call SummonTargeting.* anymore.
 */
public final class SummonTargetSelector {

    // -----------------------
    // Hardcoded role exclusion
    // -----------------------
    private static final Set<String> ROLES_EXCLUDED_LOWER = Set.of(
            "mouse",
            "frog_blue",
            "frog_green",
            "frog_orange",
            "temple_frog_blue",
            "temple_frog_green",
            "temple_frog_orange"
            // always lowercase
    );

    // -----------------------
    // Types
    // -----------------------
    private final ComponentTypeWrapper types = new ComponentTypeWrapper();
    private final ComponentType<EntityStore, ?> summonTagType;

    // -----------------------
    // Health cache
    // -----------------------
    private static volatile int cachedHealthIndex = -1;

    public SummonTargetSelector(ComponentType<EntityStore, ?> summonTagType) {
        this.summonTagType = summonTagType;
    }

    // =========================================================
    // Public API used by systems
    // =========================================================

    public @Nullable Ref<EntityStore> select(
            Ref<EntityStore> ownerRef,
            Store<EntityStore> store,
            World world,
            Vector3d summonPos,
            Vector3d ownerEye,
            double radius,
            @Nullable Ref<EntityStore> current,
            @Nullable Ref<EntityStore> preferred,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        if (radius <= 0.0) return null;

        // 1) Keep current if still valid (same behavior you had)
        if (isTargetStillValid(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                current, requireOwnerLoS, requireSummonLoS,
                false // keep old behavior: don't re-check hostile for current
        )) {
            return current;
        }

        // 2) Try preferred with strict hostile-only
        if (isTargetStillValid(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                preferred, requireOwnerLoS, requireSummonLoS,
                true // preferred must be hostile
        )) {
            return preferred;
        }

        // 3) Fallback scan
        return findClosestAliveVisibleHostile(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                requireOwnerLoS, requireSummonLoS
        );
    }

    public @Nullable Ref<EntityStore> select(
            ContextUtil.OwnerCtx ownerCtx,
            Store<EntityStore> store,
            Vector3d summonPos,
            double radius,
            @Nullable Ref<EntityStore> current,
            @Nullable Ref<EntityStore> preferred,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        Ref<EntityStore> ownerRef = ownerCtx.ownerRef();
        World world = ownerCtx.world();
        Vector3d ownerEye = ownerCtx.ownerEye();
        if (radius <= 0.0) return null;

        // 1) Keep current if still valid (same behavior you had)
        if (isTargetStillValid(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                current, requireOwnerLoS, requireSummonLoS,
                false // keep old behavior: don't re-check hostile for current
        )) {
            return current;
        }

        // 2) Try preferred with strict hostile-only
        if (isTargetStillValid(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                preferred, requireOwnerLoS, requireSummonLoS,
                true // preferred must be hostile
        )) {
            return preferred;
        }

        // 3) Fallback scan
        return findClosestAliveVisibleHostile(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                requireOwnerLoS, requireSummonLoS
        );
    }

    /** Systems can use this too to avoid repeating checks. */
    public boolean isTargetStillValid(
            Ref<EntityStore> ownerRef,
            Store<EntityStore> store,
            World world,
            Vector3d summonPos,
            Vector3d ownerEye,
            double radius,
            @Nullable Ref<EntityStore> targetRef,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            boolean requireHostileOnly
    ) {
        if (targetRef == null || !targetRef.isValid()) return false;
        if (targetRef.equals(ownerRef)) return false;

        if (!isAlive(targetRef, store)) return false;

        // Never target summons
        if (store.getComponent(targetRef, summonTagType) != null) return false;

        // Must be networked (practical filter)
        if (store.getComponent(targetRef, NetworkId.getComponentType()) == null) return false;

        TransformComponent t = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (t == null) return false;

        Vector3d tp = t.getPosition();
        if (!withinRadiusSq(summonPos, tp, radius)) return false;

        if (!passesLoS(world, summonPos, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) return false;

        if (requireHostileOnly) {
            if (!isAllowedHostileOnly(store, ownerRef, targetRef, store)) return false;
        }

        return true;
    }

    // -----------------------
    // Utilities systems used
    // -----------------------

    public boolean isAlive(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return true;

        float hp = readHealthSafe(statMap);
        if (Float.isNaN(hp)) return true;
        return hp > 0.001f;
    }

    public boolean passesLoS(
            World world,
            Vector3d summonPos,
            Vector3d ownerEye,
            Vector3d targetPos,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        if (!requireOwnerLoS && !requireSummonLoS) return true;

        Vector3d a0 = new Vector3d(targetPos.x, targetPos.y + 0.20, targetPos.z);
        Vector3d a1 = new Vector3d(targetPos.x, targetPos.y + 0.90, targetPos.z);
        Vector3d a2 = new Vector3d(targetPos.x, targetPos.y + 1.60, targetPos.z);

        boolean ownerOk = !requireOwnerLoS;
        boolean summonOk = !requireSummonLoS;

        if (!ownerOk) {
            ownerOk = hasLineOfSight(world, ownerEye, a0)
                    || hasLineOfSight(world, ownerEye, a1)
                    || hasLineOfSight(world, ownerEye, a2);
        }

        if (!summonOk) {
            summonOk = hasLineOfSight(world, summonPos, a0)
                    || hasLineOfSight(world, summonPos, a1)
                    || hasLineOfSight(world, summonPos, a2);
        }

        return ownerOk && summonOk;
    }

    public double heightOverGround(World world, Vector3d from, int maxDown) {
        Vector3i hit = TargetUtil.getTargetBlock(
                world,
                (blockId, fluidId) -> blockId != 0,
                from.x, from.y, from.z,
                0.0, -1.0, 0.0,
                maxDown
        );

        if (hit == null) return Double.POSITIVE_INFINITY;

        double topY = hit.y + 1.0;
        return from.y - topY;
    }

    // =========================================================
    // Internal targeting implementation
    // =========================================================

    private @Nullable Ref<EntityStore> findClosestAliveVisibleHostile(
            Ref<EntityStore> ownerRef,
            Store<EntityStore> store,
            World world,
            Vector3d center,
            Vector3d ownerEye,
            double radius,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        List<Ref<EntityStore>> list = TargetUtil.getAllEntitiesInSphere(center, radius, store);

        Ref<EntityStore> best = null;
        double bestD2 = Double.MAX_VALUE;

        for (Ref<EntityStore> r : list) {
            if (r == null || !r.isValid()) continue;
            if (r.equals(ownerRef)) continue;

            // Never target summons
            if (store.getComponent(r, summonTagType) != null) continue;

            // Must be networked
            if (store.getComponent(r, NetworkId.getComponentType()) == null) continue;

            TransformComponent t = store.getComponent(r, TransformComponent.getComponentType());
            if (t == null) continue;

            if (!isAlive(r, store)) continue;

            Vector3d tp = t.getPosition();

            if (!passesLoS(world, center, ownerEye, tp, requireOwnerLoS, requireSummonLoS)) continue;

            if (!isAllowedHostileOnly(store, ownerRef, r, store)) continue;

            double dx = tp.x - center.x, dy = tp.y - center.y, dz = tp.z - center.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = r;
            }
        }

        return best;
    }

    /**
     * Strict hostile-only filter (with hardcoded roles_excluded):
     * - Never targets players
     * - Only targets NPCs
     * - Excludes certain role names
     * - Uses WorldSupport attitude cache safely
     */
    private boolean isAllowedHostileOnly(
            Store<EntityStore> store,
            Ref<EntityStore> ownerRef,
            Ref<EntityStore> candidateRef,
            ComponentAccessor<EntityStore> accessor
    ) {
        if (candidateRef == null || !candidateRef.isValid()) return false;

        // Never target players
        if (store.getArchetype(candidateRef).contains(types.PLAYER_TYPE)) return false;

        // Only target NPCs
        NPCEntity npc = store.getComponent(candidateRef, types.NPC_TYPE);
        if (npc == null) return false;

        Role role = npc.getRole();
        if (role == null) return false;

        // Hardcoded roles_excluded
        String roleName = role.getRoleName();
        if (roleName != null) {
            // Avoid allocation unless needed
            String low = roleName.toLowerCase(Locale.ROOT);
            if (ROLES_EXCLUDED_LOWER.contains(low)) return false;
        }

        Attitude a = getAttitudeSafe(role, candidateRef, ownerRef, accessor);
        return a == Attitude.HOSTILE;
    }

    private static @Nullable Attitude getAttitudeSafe(
            Role role,
            Ref<EntityStore> selfRef,
            Ref<EntityStore> targetRef,
            ComponentAccessor<EntityStore> accessor
    ) {
        try {
            WorldSupport ws = role.getWorldSupport();
            if (ws == null) return null;

            ws.requireAttitudeCache();
            return ws.getAttitude(selfRef, targetRef, accessor);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasLineOfSight(World world, Vector3d from, Vector3d to) {
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

    private static boolean withinRadiusSq(Vector3d a, Vector3d b, double r) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        return (dx * dx + dy * dy + dz * dz) <= (r * r);
    }

    private static float readHealthSafe(EntityStatMap statMap) {
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

    public static final class ComponentTypeWrapper {
        public final ComponentType<EntityStore, Player> PLAYER_TYPE;
        public final ComponentType<EntityStore, NPCEntity> NPC_TYPE;

        public ComponentTypeWrapper() {
            this.PLAYER_TYPE = Player.getComponentType();
            this.NPC_TYPE = NPCEntity.getComponentType();
        }
    }
}
