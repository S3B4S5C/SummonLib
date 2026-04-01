package me.s3b4s5.summonlib.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.runtime.SummonActions;

import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import java.util.UUID;

public class SummonCastInteraction extends Interaction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    public enum Mode { ADD, SET, CLEAR }

    private static final Codec<Mode> MODE_CODEC = new EnumCodec<>(Mode.class);

    public static final BuilderCodec<SummonCastInteraction> CODEC =
            BuilderCodec.builder(SummonCastInteraction.class, SummonCastInteraction::new, Interaction.ABSTRACT_CODEC)

                    .appendInherited(
                            new KeyedCodec<>("SummonId", Codec.STRING),
                            (o, v) -> o.summonId = (v == null ? "" : v),
                            (o) -> o.summonId,
                            (o, p) -> o.summonId = p.summonId
                    )
                    .addValidator(Validators.nonNull())
                    .add()

                    .appendInherited(
                            new KeyedCodec<>("Amount", Codec.INTEGER),
                            (o, v) -> o.amount = (v == null ? 1 : v),
                            (o) -> o.amount,
                            (o, p) -> o.amount = p.amount
                    )
                    .addValidator(Validators.greaterThanOrEqual(1))
                    .add()

                    .appendInherited(
                            new KeyedCodec<>("Mode", MODE_CODEC),
                            (o, v) -> o.mode = (v == null ? Mode.ADD : v),
                            (o) -> o.mode,
                            (o, p) -> o.mode = p.mode
                    )
                    .add()
                    .build();

    protected String summonId = "";
    protected int amount = 1;
    protected Mode mode = Mode.ADD;

    public SummonCastInteraction() {}

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

        Ref<EntityStore> entityRef = context.getEntity();
        if (!entityRef.isValid()) {
            if (DEBUG) LOGGER.atWarning().log("invalid entityRef");
            return;
        }

        CommandBuffer<EntityStore> cb = context.getCommandBuffer();
        if (cb == null) {
            if (DEBUG) LOGGER.atWarning().log("null CommandBuffer");
            return;
        }

        Store<EntityStore> store = entityRef.getStore();

        try {
            var uuidType = com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType();
            var uuidComp = cb.getComponent(entityRef, uuidType);
            if (uuidComp == null) uuidComp = store.getComponent(entityRef, uuidType);

            if (uuidComp == null) {
                LOGGER.atWarning().log("UUIDComponent missing on caster entityRef=%s", entityRef);
                return;
            }

            UUID ownerUuid = uuidComp.getUuid();

            if (DEBUG) {
                LOGGER.atInfo().log("type=%s mode=%s summonId=%s amount=%d owner=%s",
                        type, mode, summonId, amount, ownerUuid);
            }

            SummonActions.Mode m = switch (mode) {
                case ADD -> SummonActions.Mode.ADD;
                case SET -> SummonActions.Mode.SET;
                case CLEAR -> SummonActions.Mode.CLEAR;
            };

            if (DEBUG) LOGGER.atInfo().log("calling SummonActions.cast(...)");
            SummonActions.cast(store, cb, ownerUuid, entityRef, summonId, amount, m);
            if (DEBUG) LOGGER.atInfo().log("cast() returned");

        } catch (Throwable t) {
            LOGGER.atSevere().log("EXCEPTION: %s", String.valueOf(t));
            t.printStackTrace();
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
        // Do not spawn/remove entities in simulation.
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext context) {
        return false;
    }

    @NonNullDecl
    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        // No special client packet needed; world changes replicate naturally.
        return new com.hypixel.hytale.protocol.SimpleInteraction();
    }

    @Override
    public boolean needsRemoteSync() {
        return false;
    }
}


