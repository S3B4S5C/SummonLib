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

public final class SummonClearSummonsInteraction extends Interaction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    public static final BuilderCodec<SummonClearSummonsInteraction> CODEC =
            BuilderCodec.builder(SummonClearSummonsInteraction.class, SummonClearSummonsInteraction::new, Interaction.ABSTRACT_CODEC)
                    .appendInherited(
                            new KeyedCodec<>("SummonId", Codec.STRING),
                            (o, v) -> o.summonId = (v == null ? "" : v),
                            o -> o.summonId,
                            (o, p) -> o.summonId = p.summonId
                    )
                    .addValidator(Validators.nonNull())
                    .add()
                    .build();

    private String summonId = "";

    public SummonClearSummonsInteraction() {
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

        ArrayList<Ref<EntityStore>> list = SummonInteractionSupport.collectOwnerSummons(
                ownerContext.store(),
                ownerContext.ownerUuid(),
                summonId,
                (ref, _) -> ref
        );

        int removed = 0;
        for (Ref<EntityStore> ref : list) {
            if (ref == null || !ref.isValid()) continue;
            ownerContext.commandBuffer().removeEntity(ref, RemoveReason.REMOVE);
            removed++;
        }

        if (DEBUG) {
            LOGGER.atInfo().log(
                    "[SummonClear] type=%s removed=%d owner=%s summonId=%s",
                    type,
                    removed,
                    ownerContext.ownerUuid(),
                    summonId
            );
        }
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