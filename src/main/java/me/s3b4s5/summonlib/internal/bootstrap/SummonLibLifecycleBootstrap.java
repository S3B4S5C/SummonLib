package me.s3b4s5.summonlib.internal.bootstrap;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.lifecycle.SummonOwnerLifecycle;

public final class SummonLibLifecycleBootstrap {

    private SummonLibLifecycleBootstrap() {}

    public static SummonOwnerLifecycle createOwnerLifecycle(
            SummonLibComponentBootstrap.ComponentBindings bindings
    ) {
        return new SummonOwnerLifecycle(bindings.summonComponentType(), true, true);
    }

    public static void registerLifecycle(JavaPlugin plugin, SummonOwnerLifecycle ownerLifecycle) {
        ownerLifecycle.register(plugin);
    }
}
