package me.s3b4s5.summonlib.systems.shared;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.context.SummonReferenceResolver;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;
import me.s3b4s5.summonlib.internal.component.SummonComponent;

import javax.annotation.Nonnull;

public final class SummonDamageOverrideSystem extends DamageEventSystem {

    private static final MetaKey<Boolean> META_SUMMONLIB_DAMAGE_OVERRIDDEN =
            Damage.META_REGISTRY.registerMetaObject(d -> Boolean.FALSE);

    private final ComponentType<EntityStore, SummonComponent> summonTagType;

    public SummonDamageOverrideSystem(ComponentType<EntityStore, SummonComponent> summonTagType) {
        this.summonTagType = summonTagType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return EntityStatMap.getComponentType();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> cb,
            @Nonnull Damage damage
    ) {
        if (damage.isCancelled()) return;

        final Boolean done = damage.getIfPresentMetaObject(META_SUMMONLIB_DAMAGE_OVERRIDDEN);
        if (Boolean.TRUE.equals(done)) return;
        damage.putMetaObject(META_SUMMONLIB_DAMAGE_OVERRIDDEN, Boolean.TRUE);

        final Damage.Source src = damage.getSource();

        Ref<EntityStore> attackerRef = null;
        Ref<EntityStore> projectileRef = null;

        if (src instanceof Damage.ProjectileSource ps) {
            attackerRef = ps.getRef();
            projectileRef = ps.getProjectile();
        } else if (src instanceof Damage.EntitySource es) {
            attackerRef = es.getRef();
        } else {
            return;
        }

        if (!attackerRef.isValid()) return;

        final SummonComponent summonTag = store.getComponent(attackerRef, summonTagType);
        if (summonTag == null) return;

        final SummonDefinition def = SummonReferenceResolver.resolveDefOrNull(summonTag);
        if (def == null) return;

        final Ref<EntityStore> ownerRef = SummonReferenceResolver.resolveOwnerRef(summonTag);
        if (ownerRef == null || !ownerRef.isValid()) return;

        final PlayerRef pr = store.getComponent(ownerRef, PlayerRef.getComponentType());
        if (pr == null || !pr.isValid()) return;

        if (projectileRef != null && projectileRef.isValid()) {
            damage.setSource(new Damage.ProjectileSource(ownerRef, projectileRef));
        } else {
            damage.setSource(new Damage.EntitySource(ownerRef));
        }

        final float mult = SummonRuntimeServices.stats().getSummonDamageMultiplier(store, cb, ownerRef);
        if (mult <= 0f || !Float.isFinite(mult)) {
            damage.setAmount(0f);
            return;
        }

        damage.setAmount(def.damage * mult);
    }
}



