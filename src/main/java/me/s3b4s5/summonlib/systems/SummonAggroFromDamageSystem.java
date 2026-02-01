package me.s3b4s5.summonlib.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import me.s3b4s5.summonlib.internal.Logger;
import me.s3b4s5.summonlib.runtime.SummonAggroRuntime;
import me.s3b4s5.summonlib.tags.SummonTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public class SummonAggroFromDamageSystem extends DamageEventSystem {

    private final ComponentType<EntityStore, SummonTag> summonTagType;
    private final Logger logger;

    public SummonAggroFromDamageSystem(ComponentType<EntityStore, SummonTag> summonTagType) {
        this.summonTagType = summonTagType;
        this.logger = new Logger("[SummonAggro]");
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
        if (victimRef == null || !victimRef.isValid()) return;

        Ref<EntityStore> attackerRef = null;
        Damage.Source src = damage.getSource();
        if (src instanceof Damage.EntitySource es) {
            Ref<EntityStore> r = es.getRef();
            if (r != null && r.isValid()) attackerRef = r;
        }

        Instant now = resolveNow(cb);

        TimeResource time = (TimeResource) cb.getResource(TimeResource.getResourceType());
        Instant nowTR = (time != null ? time.getNow() : null);
        Instant nowReal = Instant.now();

        logger.dbg(true,
                "DMG amt=" + damage.getAmount() +
                        " causeIdx=" + damage.getDamageCauseIndex() +
                        " src=" + (src != null ? src.getClass().getSimpleName() : "null") +
                        " | timeResource=" + (time != null) +
                        " nowTR=" + (nowTR != null ? nowTR : "null") +
                        " nowReal=" + nowReal +
                        " nowUsed=" + now
        );

        UUID victimUuid = getUuid(chunk, index);
        boolean victimIsPlayer = cb.getComponent(victimRef, PlayerRef.getComponentType()) != null;

        UUID attackerUuid = attackerRef != null ? getUuid(store, attackerRef) : null;
        boolean attackerIsPlayer = attackerRef != null && cb.getComponent(attackerRef, PlayerRef.getComponentType()) != null;

        SummonTag victimSummon = cb.getComponent(victimRef, summonTagType);
        SummonTag attackerSummon = attackerRef != null ? cb.getComponent(attackerRef, summonTagType) : null;

        logger.dbg(true, "VICTIM ref=" + victimRef + " uuid=" + victimUuid + " isPlayer=" + victimIsPlayer
                + " isSummon=" + (victimSummon != null) + " summonOwner=" + (victimSummon != null ? victimSummon.getOwnerUuid() : null));
        logger.dbg(true, "ATTACKER ref=" + attackerRef + " uuid=" + attackerUuid + " isPlayer=" + attackerIsPlayer
                + " isSummon=" + (attackerSummon != null) + " summonOwner=" + (attackerSummon != null ? attackerSummon.getOwnerUuid() : null));

        logger.dbg(true, "ATTACKER :" + attackerRef + " VICTIM: " + victimRef);
        logger.dbg(true, "ATTACKERuuid :" + attackerUuid + " VICTIMuuid: " + victimUuid);

        // 1) If player gets hurt -> focus = attacker
        if (victimIsPlayer && victimUuid != null && attackerRef != null && attackerRef.isValid()) {
            logger.dbg(true, "BRANCH victimIsPlayer=true");

            if (attackerSummon != null) {
                logger.dbg(true, "NO PUSH playerDamaged (attacker is summon)");
                return;
            }

            boolean friendly = isFriendlySummon(cb, victimUuid, attackerRef);
            logger.dbg(true, "CHECK friendly(attacker)=" + friendly);

            if (!victimRef.equals(attackerRef) && !friendly) {
                SummonAggroRuntime.push(victimUuid, attackerRef, now);

                // Verificación inmediata
                Ref<EntityStore> peekNow = SummonAggroRuntime.peekValid(victimUuid, now);
                logger.dbg(true, "PUSH playerDamaged owner=" + victimUuid + " target(attacker)=" + attackerRef
                        + " nowUsed=" + now + " | peek(nowUsed)=" + peekNow);
            } else {
                logger.dbg(true, "NO PUSH playerDamaged (self-hit o friendly)");
            }

            return;
        }

        // 2) If player attack something -> focus = victim
        if (attackerIsPlayer && attackerUuid != null && attackerRef != null && attackerRef.isValid() && !victimRef.equals(attackerRef)) {
            logger.dbg(true, "BRANCH attackerIsPlayer=true");

            boolean friendly = isFriendlySummon(cb, attackerUuid, victimRef);
            logger.dbg(true, "CHECK friendly(victim)=" + friendly);

            if (victimSummon != null) {
                logger.dbg(true, "NO PUSH playerAttacks (victim is summon)");
                return;
            }

            if (!friendly) {
                SummonAggroRuntime.push(attackerUuid, victimRef, now);

                Ref<EntityStore> peekNow = SummonAggroRuntime.peekValid(attackerUuid, now);
                logger.dbg(true, "PUSH playerAttacks owner=" + attackerUuid + " target(victim)=" + victimRef
                        + " nowUsed=" + now + " | peek(nowUsed)=" + peekNow);
            } else {
                logger.dbg(true, "NO PUSH playerAttacks (victim friendly)");
            }

            logger.dbg(true, "AttackerIsPlayer");
        }
    }

    private static Instant resolveNow(CommandBuffer<EntityStore> cb) {
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
        SummonTag t = cb.getComponent(targetRef, summonTagType);
        return t != null && ownerUuid != null && ownerUuid.equals(t.getOwnerUuid());
    }
}
