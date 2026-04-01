package me.s3b4s5.summonlib.interaction;

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
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public final class SummonRemoveLastInteraction extends Interaction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final boolean DEBUG = false;

    public static final BuilderCodec<SummonRemoveLastInteraction> CODEC =
            BuilderCodec.builder(SummonRemoveLastInteraction.class, SummonRemoveLastInteraction::new, Interaction.ABSTRACT_CODEC)

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

                    .build();

    protected String summonId = "";
    protected int amount = 1;

    public SummonRemoveLastInteraction() {}

    public SummonRemoveLastInteraction(@Nonnull String id) {
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

        ArrayList<Entry> list = collectOwnerSummons(store, ownerUuid, summonId);
        if (list.isEmpty()) {
            if (DEBUG) LOGGER.atInfo().log("[SummonRemoveLast] none. owner=%s summonId=%s", ownerUuid, summonId);
            return;
        }

        list.sort(Comparator.comparingLong((Entry e) -> e.spawnSeq).reversed());

        int removed = 0;
        int toRemove = Math.max(1, amount);

        for (int i = 0; i < list.size() && removed < toRemove; i++) {
            Ref<EntityStore> r = list.get(i).ref;
            if (r == null || !r.isValid()) continue;
            cb.removeEntity(r, RemoveReason.REMOVE);
            removed++;
        }

        if (DEBUG) {
            LOGGER.atInfo().log("[SummonRemoveLast] type=%s removed=%d/%d owner=%s summonId=%s",
                    type, removed, toRemove, ownerUuid, summonId);
        }
    }

    private static final class Entry {
        final Ref<EntityStore> ref;
        final long spawnSeq;
        Entry(Ref<EntityStore> ref, long spawnSeq) { this.ref = ref; this.spawnSeq = spawnSeq; }
    }

    private static ArrayList<Entry> collectOwnerSummons(Store<EntityStore> store, UUID ownerUuid, String summonIdFilter) {
        var tagType = SummonLib.summonComponentType();
        Query<EntityStore> q = Query.and(tagType);

        final boolean filterById = summonIdFilter != null && !summonIdFilter.isEmpty();
        ArrayList<Entry> out = new ArrayList<>();

        store.forEachChunk(q, (chunk, ccb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonComponent t = chunk.getComponent(i, tagType);
                if (t == null) continue;
                if (!ownerUuid.equals(t.owner)) continue;
                if (filterById && !summonIdFilter.equals(t.summonId)) continue;

                Ref<EntityStore> r = chunk.getReferenceTo(i);
                if (r == null || !r.isValid()) continue;

                out.add(new Entry(r, t.spawnSeq));
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



