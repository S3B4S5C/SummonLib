package me.s3b4s5.summonlib.internal.targeting;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Target search and hostile filtering for summon combat systems.
 */
public final class SummonTargetSearch {

    private static final Set<String> ROLES_EXCLUDED_LOWER = Set.of(
            "mouse",
            "frog_blue",
            "frog_green",
            "frog_orange",
            "temple_frog_blue",
            "temple_frog_green",
            "temple_frog_orange"
    );

    private final ComponentType<EntityStore, ?> summonTagType;
    private final ComponentType<EntityStore, Player> playerType;
    private final ComponentType<EntityStore, NPCEntity> npcType;
    private final SummonTargetValidation validation;

    public SummonTargetSearch(
            ComponentType<EntityStore, ?> summonTagType,
            SummonTargetValidation validation
    ) {
        this.summonTagType = summonTagType;
        this.playerType = Player.getComponentType();
        this.npcType = NPCEntity.getComponentType();
        this.validation = validation;
    }

    public @Nullable Ref<EntityStore> findClosestAliveVisibleHostile(
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
        for (Ref<EntityStore> ref : list) {
            if (ref == null || !ref.isValid() || ref.equals(ownerRef)) continue;
            if (store.getComponent(ref, summonTagType) != null) continue;
            if (store.getComponent(ref, NetworkId.getComponentType()) == null) continue;

            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) continue;
            if (!validation.isAlive(ref, store)) continue;

            Vector3d targetPos = transform.getPosition();
            if (!validation.passesLoS(world, center, ownerEye, targetPos, requireOwnerLoS, requireSummonLoS)) continue;
            if (!isAllowedHostileOnly(store, ownerRef, ref, store)) continue;

            double dx = targetPos.x - center.x;
            double dy = targetPos.y - center.y;
            double dz = targetPos.z - center.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = ref;
            }
        }
        return best;
    }

    public boolean isAllowedHostileOnly(
            Store<EntityStore> store,
            Ref<EntityStore> ownerRef,
            Ref<EntityStore> candidateRef,
            ComponentAccessor<EntityStore> accessor
    ) {
        if (candidateRef == null || !candidateRef.isValid()) return false;
        if (playerType != null && store.getArchetype(candidateRef).contains(playerType)) return false;
        if (npcType == null) return false;

        NPCEntity npc = store.getComponent(candidateRef, npcType);
        if (npc == null) return false;

        Role role = npc.getRole();
        if (role == null) return false;

        String roleName = role.getRoleName();
        if (roleName != null && ROLES_EXCLUDED_LOWER.contains(roleName.toLowerCase(Locale.ROOT))) {
            return false;
        }

        Attitude attitude = getAttitudeSafe(role, candidateRef, ownerRef, accessor);
        return attitude == Attitude.HOSTILE;
    }

    private static @Nullable Attitude getAttitudeSafe(
            Role role,
            Ref<EntityStore> selfRef,
            Ref<EntityStore> targetRef,
            ComponentAccessor<EntityStore> accessor
    ) {
        try {
            WorldSupport ws = role.getWorldSupport();
            ws.requireAttitudeCache();
            return ws.getAttitude(selfRef, targetRef, accessor);
        } catch (Throwable ignored) {
            return null;
        }
    }
}


