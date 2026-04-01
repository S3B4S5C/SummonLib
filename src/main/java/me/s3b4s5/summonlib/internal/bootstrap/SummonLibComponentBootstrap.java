package me.s3b4s5.summonlib.internal.bootstrap;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.experimental.worm.component.WormComponent;
import me.s3b4s5.summonlib.internal.component.SummonComponent;

public final class SummonLibComponentBootstrap {

    private SummonLibComponentBootstrap() {}

    public static ComponentBindings registerComponents(JavaPlugin plugin) {
        ComponentType<EntityStore, SummonComponent> summonComponentType =
                plugin.getEntityStoreRegistry().registerComponent(SummonComponent.class, SummonComponent::new);
        ComponentType<EntityStore, WormComponent> wormComponentType =
                plugin.getEntityStoreRegistry().registerComponent(WormComponent.class, WormComponent::new);

        return new ComponentBindings(summonComponentType, wormComponentType);
    }

    public record ComponentBindings(
            ComponentType<EntityStore, SummonComponent> summonComponentType,
            ComponentType<EntityStore, WormComponent> wormComponentType
    ) {}
}
