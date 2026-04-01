package me.s3b4s5.summonlib.systems.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SummonAggroFromDamageSystem extends DamageEventSystem {

    private final ComponentType<EntityStore, SummonComponent> summonTagType;

    public SummonAggroFromDamageSystem(ComponentType<EntityStore, SummonComponent> summonTagType) {
        this.summonTagType = summonTagType;
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and(UUIDComponent.getComponentType());
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
        if (damage.getAmount() <= 0f) return;

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (!victimRef.isValid()) return;

        Ref<EntityStore> attackerRef = null;
        Damage.Source src = damage.getSource();
        if (src instanceof Damage.EntitySource es) {
            Ref<EntityStore> r = es.getRef();
            if (r.isValid()) attackerRef = r;
        }

        Instant now = resolveNow();

        UUID victimUuid = getUuid(chunk, index);
        boolean victimIsPlayer = cb.getComponent(victimRef, PlayerRef.getComponentType()) != null;

        UUID attackerUuid = attackerRef != null ? getUuid(store, attackerRef) : null;
        boolean attackerIsPlayer = attackerRef != null && cb.getComponent(attackerRef, PlayerRef.getComponentType()) != null;

        SummonComponent victimSummon = cb.getComponent(victimRef, summonTagType);
        SummonComponent attackerSummon = attackerRef != null ? cb.getComponent(attackerRef, summonTagType) : null;

        if (victimIsPlayer && victimUuid != null && attackerRef != null && attackerRef.isValid()) {

            if (attackerSummon != null) {
                return;
            }

            boolean friendly = isFriendlySummon(cb, victimUuid, attackerRef);

            if (!Objects.equals(victimRef, attackerRef) && !friendly) {
                SummonRuntimeServices.targets().pushAggro(victimUuid, attackerRef, now);
                SummonRuntimeServices.targets().peekValidAggro(victimUuid, now);
            }

            return;
        }

        if (attackerIsPlayer && attackerUuid != null && attackerRef.isValid() && !Objects.equals(victimRef, attackerRef)) {

            boolean friendly = isFriendlySummon(cb, attackerUuid, victimRef);

            if (victimSummon != null) {
                return;
            }

            if (!friendly) {
                SummonRuntimeServices.targets().pushAggro(attackerUuid, victimRef, now);
                SummonRuntimeServices.targets().peekValidAggro(attackerUuid, now);
            }
        }
    }

    private static Instant resolveNow() {
        return Instant.now();
    }

    @Nullable
    private static UUID getUuid(ArchetypeChunk<EntityStore> chunk, int index) {
        UUIDComponent u = chunk.getComponent(index, UUIDComponent.getComponentType());
        return u != null ? u.getUuid() : null;
    }

    @Nullable
    private static UUID getUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        UUIDComponent u = store.getComponent(ref, UUIDComponent.getComponentType());
        return u != null ? u.getUuid() : null;
    }

    private boolean isFriendlySummon(CommandBuffer<EntityStore> cb, UUID ownerUuid, Ref<EntityStore> targetRef) {
        SummonComponent t = cb.getComponent(targetRef, summonTagType);
        return t != null && ownerUuid != null && ownerUuid.equals(t.getOwnerUuid());
    }
}



