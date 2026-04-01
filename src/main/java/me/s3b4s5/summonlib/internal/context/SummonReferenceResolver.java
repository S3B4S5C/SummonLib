package me.s3b4s5.summonlib.internal.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.internal.component.SummonComponent;
import me.s3b4s5.summonlib.internal.definition.SummonDefinition;
import me.s3b4s5.summonlib.internal.runtime.service.SummonRuntimeServices;

import javax.annotation.Nullable;

/**
 * Small resolver helpers for summon references and definition lookup.
 */
public final class SummonReferenceResolver {

    private SummonReferenceResolver() {}

    public static @Nullable SummonDefinition resolveDefOrNull(SummonComponent tag) {
        return SummonRuntimeServices.definitions().get(tag.getSummonId());
    }

    public static @Nullable Ref<EntityStore> resolveOwnerRef(SummonComponent tag) {
        return OwnerContextResolver.resolveOwnerRef(tag);
    }
}



