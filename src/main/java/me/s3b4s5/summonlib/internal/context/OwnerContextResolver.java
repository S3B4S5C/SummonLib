package me.s3b4s5.summonlib.internal.context;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import me.s3b4s5.summonlib.internal.component.SummonComponent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Resolves and validates summon owner context.
 */
public final class OwnerContextResolver {

    private OwnerContextResolver() {}

    public record OwnerCtx(
            UUID ownerUuid,
            PlayerRef owner,
            Ref<EntityStore> ownerRef,
            World world,
            Vector3d ownerPos,
            Vector3d ownerEye,
            Vector3f ownerRot,
            double yawRad
    ) {}

    public static @Nullable OwnerCtx getOwnerCtxOrNull(
            Store<EntityStore> store,
            CommandBuffer<EntityStore> cb,
            SummonComponent tag,
            Ref<EntityStore> summonRef
    ) {
        UUID ownerUuid = tag.getOwnerUuid();
        if (ownerUuid == null) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return null;
        }

        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || owner.getWorldUuid() == null) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return null;
        }

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() != store) {
            cb.removeEntity(summonRef, RemoveReason.REMOVE);
            return null;
        }

        return buildOwnerCtx(store, ownerUuid, owner, ownerRef);
    }

    public static @Nullable Ref<EntityStore> resolveOwnerRef(SummonComponent tag) {
        UUID ownerUuid = tag.getOwnerUuid();
        if (ownerUuid == null) return null;

        PlayerRef owner = Universe.get().getPlayer(ownerUuid);
        if (owner == null || !owner.isValid()) return null;
        return owner.getReference();
    }

    private static OwnerCtx buildOwnerCtx(
            Store<EntityStore> store,
            UUID ownerUuid,
            PlayerRef owner,
            Ref<EntityStore> ownerRef
    ) {
        World world = store.getExternalData().getWorld();
        Vector3d ownerPos = owner.getTransform().getPosition();

        Transform ownerLook = TargetUtil.getLook(ownerRef, store);
        Vector3d ownerEye = ownerLook.getPosition();
        Vector3f ownerRot = ownerLook.getRotation();
        double yawRad = ownerRot.getYaw();

        return new OwnerCtx(ownerUuid, owner, ownerRef, world, ownerPos, ownerEye, ownerRot, yawRad);
    }
}



