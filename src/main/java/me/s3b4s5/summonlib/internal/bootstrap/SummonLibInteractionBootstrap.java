package me.s3b4s5.summonlib.internal.bootstrap;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.api.SummonLibApi;

public final class SummonLibInteractionBootstrap {

    private SummonLibInteractionBootstrap() {}

    public static void registerInteractions(JavaPlugin plugin) {
        SummonLibApi.registerBuiltinInteractions(plugin);
    }
}
