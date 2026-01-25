package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public final class SummonTargeting {

    private static volatile int cachedHealthIndex = -1;

    private SummonTargeting() {}

    public static boolean isAlive(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return true;

        float hp = readHealthSafe(statMap);
        if (Float.isNaN(hp)) return true;
        return hp > 0.001f;
    }

    public static boolean passesLoS(World world, Vector3d summonPos, Vector3d ownerEye, Vector3d targetPos,
                                    boolean requireOwnerLoS, boolean requireSummonLoS) {
        if (requireSummonLoS && !hasLineOfSight(world, summonPos, targetPos)) return false;
        if (requireOwnerLoS && !hasLineOfSight(world, ownerEye, targetPos)) return false;
        return true;
    }

    public static Ref<EntityStore> findClosestAliveVisibleInSphere(
            Vector3d center,
            Vector3d ownerEye,
            double radius,
            Store<EntityStore> store,
            World world,
            Ref<EntityStore> ownerRef,
            ComponentTypeWrapper types,
            boolean requireOwnerLoS,
            boolean requireSummonLoS,
            com.hypixel.hytale.component.ComponentType<EntityStore, ?> summonTagType
    ) {
        List<Ref<EntityStore>> list = TargetUtil.getAllEntitiesInSphere(center, radius, store);

        Ref<EntityStore> best = null;
        double bestD2 = Double.MAX_VALUE;

        for (Ref<EntityStore> r : list) {
            if (r == null || !r.isValid()) continue;
            if (r.equals(ownerRef)) continue;

            // no target summons
            if (store.getComponent(r, summonTagType) != null) continue;
            if (store.getComponent(r, NetworkId.getComponentType()) == null) continue;

            if (!isAllowedTargetHostileOnly(store, ownerRef, r, types)) continue;

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

    public static boolean isAllowedTargetHostileOnly(Store<EntityStore> store, Ref<EntityStore> ownerRef, Ref<EntityStore> candidateRef, ComponentTypeWrapper types) {
        if (candidateRef == null || !candidateRef.isValid()) return false;

        // Never target players
        if (store.getArchetype(candidateRef).contains(types.PLAYER_TYPE)) return false;

        // Only target NPCs (strict hostile-only)
        NPCEntity npc = store.getComponent(candidateRef, types.NPC_TYPE);
        if (npc == null) return false;

        Role role = npc.getRole();
        if (role == null) return false;

        Attitude a = tryGetAttitude(role, candidateRef, ownerRef, store);
        return a == Attitude.HOSTILE;
    }

    private static Attitude tryGetAttitude(Role role, Ref<EntityStore> selfRef, Ref<EntityStore> targetRef, ComponentAccessor<EntityStore> accessor) {
        try {
            Object ws = role.getWorldSupport();

            // Try getAttitude(Ref, Ref, ComponentAccessor)
            for (Method m : ws.getClass().getMethods()) {
                if (!m.getName().equals("getAttitude")) continue;
                if (m.getParameterCount() != 3) continue;

                Class<?>[] p = m.getParameterTypes();
                if (!Ref.class.isAssignableFrom(p[0])) continue;
                if (!Ref.class.isAssignableFrom(p[1])) continue;

                Object out = m.invoke(ws, selfRef, targetRef, accessor);
                if (out instanceof Attitude a) return a;
            }
        } catch (Throwable ignored) {}

        return null;
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

    public static double heightOverGround(World world, Vector3d from, int maxDown) {
        Object hit = null;
        try {
            hit = TargetUtil.getTargetBlock(
                    world,
                    (blockId, fluidId) -> blockId != 0,
                    from.x, from.y, from.z,
                    0.0, -1.0, 0.0,
                    maxDown
            );
        } catch (Throwable ignored) {}

        if (hit == null) return Double.POSITIVE_INFINITY;

        Integer hitY = extractHitBlockY(hit);
        if (hitY == null) return Double.NaN;

        double topY = hitY + 1.0;
        return from.y - topY;
    }

    @Nullable
    private static Integer extractHitBlockY(Object hit) {
        for (String mName : new String[]{"getBlock", "getBlockPos", "block", "pos"}) {
            try {
                Method m = hit.getClass().getMethod(mName);
                Object out = m.invoke(hit);
                Integer y = extractY(out);
                if (y != null) return y;
            } catch (Throwable ignored) {}
        }

        for (String fName : new String[]{"block", "pos"}) {
            try {
                Field f = hit.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object out = f.get(hit);
                Integer y = extractY(out);
                if (y != null) return y;
            } catch (Throwable ignored) {}
        }

        return null;
    }

    @Nullable
    private static Integer extractY(Object maybeVec) {
        if (maybeVec == null) return null;

        try {
            Field fy = maybeVec.getClass().getField("y");
            Object yv = fy.get(maybeVec);
            if (yv instanceof Integer i) return i;
        } catch (Throwable ignored) {}

        try {
            Field fy = maybeVec.getClass().getDeclaredField("y");
            fy.setAccessible(true);
            Object yv = fy.get(maybeVec);
            if (yv instanceof Integer i) return i;
        } catch (Throwable ignored) {}

        try {
            Method my = maybeVec.getClass().getMethod("getY");
            Object yv = my.invoke(maybeVec);
            if (yv instanceof Integer i) return i;
        } catch (Throwable ignored) {}

        return null;
    }

    public static final class ComponentTypeWrapper {
        public final com.hypixel.hytale.component.ComponentType<EntityStore, Player> PLAYER_TYPE;
        public final com.hypixel.hytale.component.ComponentType<EntityStore, NPCEntity> NPC_TYPE;

        public ComponentTypeWrapper() {
            this.PLAYER_TYPE = Player.getComponentType();
            this.NPC_TYPE = NPCEntity.getComponentType();
        }
    }
}
