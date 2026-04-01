package me.s3b4s5.summonlib.internal.targeting;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.context.OwnerContextResolver;

import javax.annotation.Nullable;

/**
 * Facade that composes search and validation helpers for summon targeting.
 */
public final class SummonTargetSelector {
    private final SummonTargetValidation validation;
    private final SummonTargetSearch search;

    public SummonTargetSelector(ComponentType<EntityStore, ?> summonTagType) {
        this.validation = new SummonTargetValidation(summonTagType);
        this.search = new SummonTargetSearch(summonTagType, validation);
    }

    public @Nullable Ref<EntityStore> select(
            OwnerContextResolver.OwnerCtx ownerCtx,
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

        if (validation.isTargetStillValid(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                current, requireOwnerLoS, requireSummonLoS,
                false,
                search
        )) {
            return current;
        }

        if (validation.isTargetStillValid(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                preferred, requireOwnerLoS, requireSummonLoS,
                true,
                search
        )) {
            return preferred;
        }

        return search.findClosestAliveVisibleHostile(
                ownerRef, store, world,
                summonPos, ownerEye, radius,
                requireOwnerLoS, requireSummonLoS
        );
    }

    public boolean isAlive(Ref<EntityStore> ref, Store<EntityStore> store) {
        return validation.isAlive(ref, store);
    }

    public boolean passesLoS(
            World world,
            Vector3d summonPos,
            Vector3d ownerEye,
            Vector3d targetPos,
            boolean requireOwnerLoS,
            boolean requireSummonLoS
    ) {
        return validation.passesLoS(world, summonPos, ownerEye, targetPos, requireOwnerLoS, requireSummonLoS);
    }
}


