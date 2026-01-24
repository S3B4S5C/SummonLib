package me.s3b4s5.summonlib.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;

import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SummonFocusInteraction extends Interaction {

    public static final BuilderCodec<SummonFocusInteraction> CODEC =
            BuilderCodec.builder(SummonFocusInteraction.class, SummonFocusInteraction::new, Interaction.ABSTRACT_CODEC)
                    .appendInherited(
                            new KeyedCodec<Boolean>("Clear", Codec.BOOLEAN),
                            (o, v) -> o.clear = (v != null && v),
                            (o) -> o.clear,
                            (o, p) -> o.clear = p.clear
                    )
                    .add()
                    .build();

    protected boolean clear;

    public SummonFocusInteraction() {}

    public SummonFocusInteraction(@Nonnull String id) {
        super(id);
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.None;
    }

    @Override
    protected void tick0(boolean firstRun, float time, @NonNullDecl InteractionType type,
                         @Nonnull InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        // Server-side focus tagging is handled by your summon system; interaction is just a trigger.
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @NonNullDecl InteractionType type,
                                 @Nonnull InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        tick0(firstRun, time, type, context, cooldownHandler);
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
