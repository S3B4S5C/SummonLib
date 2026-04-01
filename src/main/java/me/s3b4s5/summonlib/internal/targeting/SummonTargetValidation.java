package me.s3b4s5.summonlib.internal.targeting;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Target validation, line-of-sight, and alive-state helpers.
 */
public final class SummonTargetValidation {

    private static volatile int cachedHealthIndex = -1;

    private final ComponentType<EntityStore, ?> summonTagType;

    public SummonTargetValidation(ComponentType<EntityStore, ?> summonTagType) {
        this.summonTagType = summonTagType;
    }

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
            boolean requireHostileOnly,
            SummonTargetSearch search
    ) {
        if (targetRef == null || !targetRef.isValid()) return false;
        if (targetRef.equals(ownerRef)) return false;
        if (!isAlive(targetRef, store)) return false;
        if (store.getComponent(targetRef, summonTagType) != null) return false;
        if (store.getComponent(targetRef, NetworkId.getComponentType()) == null) return false;

        TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (transform == null) return false;

        Vector3d targetPos = transform.getPosition();
        if (!withinRadiusSq(summonPos, targetPos, radius)) return false;
        if (!passesLoS(world, summonPos, ownerEye, targetPos, requireOwnerLoS, requireSummonLoS)) return false;
        return !requireHostileOnly || search.isAllowedHostileOnly(store, ownerRef, targetRef, store);
    }

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

    private static boolean hasLineOfSight(World world, Vector3d from, Vector3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= 0.25) return true;

        double inv = 1.0 / dist;
        return TargetUtil.getTargetBlock(
                world,
                (blockId, _) -> blockId != 0,
                from.x, from.y, from.z,
                dx * inv, dy * inv, dz * inv,
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
            var value = statMap.get(cachedHealthIndex);
            if (value != null) return value.get();
        }

        for (int idx = 0; idx < statMap.size(); idx++) {
            var value = statMap.get(idx);
            if (value == null) continue;
            String id = value.getId();
            if (id == null) continue;
            String low = id.toLowerCase(Locale.ROOT);
            if (low.contains("health") || low.contains("hp")) {
                cachedHealthIndex = idx;
                return value.get();
            }
        }
        return Float.NaN;
    }
}


