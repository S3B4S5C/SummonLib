package me.s3b4s5.summonlib.interaction;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.SummonLib;
import me.s3b4s5.summonlib.internal.component.SummonComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BiFunction;

final class SummonInteractionSupport {

    private SummonInteractionSupport() {
    }

    @Nullable
    static OwnerInteractionContext resolveOwnerContext(@Nonnull InteractionContext context) {
        Ref<EntityStore> entityRef = context.getEntity();
        if (!entityRef.isValid()) {
            return null;
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return null;
        }

        Store<EntityStore> store = entityRef.getStore();

        UUIDComponent uuidComponent = commandBuffer.getComponent(entityRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return null;
        }

        return new OwnerInteractionContext(
                entityRef,
                commandBuffer,
                store,
                uuidComponent.getUuid()
        );
    }

    @Nonnull
    static <T> ArrayList<T> collectOwnerSummons(
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID ownerUuid,
            @Nullable String summonIdFilter,
            @Nonnull BiFunction<Ref<EntityStore>, SummonComponent, T> mapper
    ) {
        var summonComponentType = SummonLib.summonComponentType();
        Query<EntityStore> query = Query.and(summonComponentType);

        boolean filterById = summonIdFilter != null && !summonIdFilter.isEmpty();
        ArrayList<T> result = new ArrayList<>();

        store.forEachChunk(query, (chunk, _) -> {
            for (int i = 0; i < chunk.size(); i++) {
                SummonComponent summon = chunk.getComponent(i, summonComponentType);
                if (summon == null) continue;
                if (!ownerUuid.equals(summon.owner)) continue;
                if (filterById && !summonIdFilter.equals(summon.summonId)) continue;

                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (!ref.isValid()) continue;

                result.add(mapper.apply(ref, summon));
            }
        });

        return result;
    }

    record OwnerInteractionContext(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID ownerUuid
    ) {
    }
}