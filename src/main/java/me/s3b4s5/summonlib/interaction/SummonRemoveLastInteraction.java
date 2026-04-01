package me.s3b4s5.summonlib.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;

public final class SummonRemoveLastInteraction extends Interaction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    public static final BuilderCodec<SummonRemoveLastInteraction> CODEC =
            BuilderCodec.builder(SummonRemoveLastInteraction.class, SummonRemoveLastInteraction::new, Interaction.ABSTRACT_CODEC)
                    .appendInherited(
                            new KeyedCodec<>("SummonId", Codec.STRING),
                            (o, v) -> o.summonId = (v == null ? "" : v),
                            o -> o.summonId,
                            (o, p) -> o.summonId = p.summonId
                    )
                    .addValidator(Validators.nonNull())
                    .add()
                    .appendInherited(
                            new KeyedCodec<>("Amount", Codec.INTEGER),
                            (o, v) -> o.amount = (v == null ? 1 : v),
                            o -> o.amount,
                            (o, p) -> o.amount = p.amount
                    )
                    .addValidator(Validators.greaterThanOrEqual(1))
                    .add()
                    .build();

    private String summonId = "";
    private int amount = 1;

    public SummonRemoveLastInteraction() {
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.None;
    }

    @Override
    protected void tick0(
            boolean firstRun,
            float time,
            @NonNullDecl InteractionType type,
            @Nonnull InteractionContext context,
            @NonNullDecl CooldownHandler cooldownHandler
    ) {
        if (!firstRun) return;

        SummonInteractionSupport.OwnerInteractionContext ownerContext =
                SummonInteractionSupport.resolveOwnerContext(context);
        if (ownerContext == null) return;

        ArrayList<Entry> list = SummonInteractionSupport.collectOwnerSummons(
                ownerContext.store(),
                ownerContext.ownerUuid(),
                summonId,
                (ref, summon) -> new Entry(ref, summon.spawnSeq)
        );

        if (list.isEmpty()) {
            if (DEBUG) {
                LOGGER.atInfo().log(
                        "[SummonRemoveLast] none. owner=%s summonId=%s",
                        ownerContext.ownerUuid(),
                        summonId
                );
            }
            return;
        }

        list.sort(Comparator.comparingLong((Entry e) -> e.spawnSeq).reversed());

        int removed = 0;
        int toRemove = Math.max(1, amount);

        for (int i = 0; i < list.size() && removed < toRemove; i++) {
            Ref<EntityStore> ref = list.get(i).ref;
            if (ref == null || !ref.isValid()) continue;
            ownerContext.commandBuffer().removeEntity(ref, RemoveReason.REMOVE);
            removed++;
        }

        if (DEBUG) {
            LOGGER.atInfo().log(
                    "[SummonRemoveLast] type=%s removed=%d/%d owner=%s summonId=%s",
                    type,
                    removed,
                    toRemove,
                    ownerContext.ownerUuid(),
                    summonId
            );
        }
    }

    private record Entry(Ref<EntityStore> ref, long spawnSeq) {
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @NonNullDecl InteractionType type,
            @Nonnull InteractionContext context,
            @NonNullDecl CooldownHandler cooldownHandler
    ) {
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext context) {
        return false;
    }

    @NonNullDecl
    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.SimpleInteraction();
    }

    @Override
    public boolean needsRemoteSync() {
        return false;
    }
}