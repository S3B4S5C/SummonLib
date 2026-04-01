package me.s3b4s5.summonlib.internal.bootstrap;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.assets.store.SummonConfigStore;
import me.s3b4s5.summonlib.assets.store.follow.FollowConfigStore;
import me.s3b4s5.summonlib.assets.store.motion.NpcMotionControllerStore;

public final class SummonLibAssetStoreBootstrap {

    private SummonLibAssetStoreBootstrap() {}

    public static void registerAssetStores(JavaPlugin plugin) {
        plugin.getAssetRegistry().register(SummonConfigStore.create());
        plugin.getAssetRegistry().register(FollowConfigStore.create());
        plugin.getAssetRegistry().register(NpcMotionControllerStore.create());
    }
}
