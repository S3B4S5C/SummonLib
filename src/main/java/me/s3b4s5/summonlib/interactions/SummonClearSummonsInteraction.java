package me.s3b4s5.summonlib.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.SummonLib;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.UUID;

public final class SummonClearSummonsInteraction extends Interaction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    public static final BuilderCodec<SummonClearSummonsInteraction> CODEC =
            BuilderCodec.builder(SummonClearSummonsInteraction.class, SummonClearSummonsInteraction::new, Interaction.ABSTRACT_CODEC)

                    .appendInherited(
                            new KeyedCodec<>("SummonId", Codec.STRING),
                            (o, v) -> o.summonId = (v == null ? "" : v),
                            (o) -> o.summonId,
                            (o, p) -> o.summonId = p.summonId
                    )
                    .addValidator(Validators.nonNull())
                    .add()

                    .build();

    protected String summonId = "";

    public SummonClearSummonsInteraction() {}

    public SummonClearSummonsInteraction(@Nonnull String id) {
        super(id);
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

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) return;

        CommandBuffer<EntityStore> cb = context.getCommandBuffer();
        if (cb == null) return;

        Store<EntityStore> store = entityRef.getStore();
        UUIDComponent uc = cb.getComponent(entityRef, UUIDComponent.getComponentType());
        if (uc == null) return;

        UUID ownerUuid = uc.getUuid();

        ArrayList<Ref<EntityStore>> list = collectOwnerSummons(store, ownerUuid, summonId);
        int removed = 0;

        for (Ref<EntityStore> r : list) {
            if (r == null || !r.isValid()) continue;
            cb.removeEntity(r, RemoveReason.REMOVE);
            removed++;
        }

        if (DEBUG) {
            LOGGER.atInfo().log("[SummonClear] type=%s removed=%d owner=%s summonId=%s",
                    type, removed, ownerUuid, summonId);
        }
    }

    private static ArrayList<Ref<EntityStore>> collectOwnerSummons(Store<EntityStore> store, UUID ownerUuid, String summonIdFilter) {
        var tagType = SummonLib.summonTagType();
        Query<EntityStore> q = Query.and(tagType);

        final boolean filterById = summonIdFilter != null && !summonIdFilter.isEmpty();
        ArrayList<Ref<EntityStore>> out = new ArrayList<>();

        store.forEachChunk(q, (chunk, ccb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonTag t = chunk.getComponent(i, tagType);
                if (t == null) continue;
                if (!ownerUuid.equals(t.owner)) continue;
                if (filterById && !summonIdFilter.equals(t.summonId)) continue;

                Ref<EntityStore> r = chunk.getReferenceTo(i);
                if (r == null || !r.isValid()) continue;

                out.add(r);
            }
        });

        return out;
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @NonNullDecl InteractionType type,
            @Nonnull InteractionContext context,
            @NonNullDecl CooldownHandler cooldownHandler
    ) {
        // No removals in simulation.
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
