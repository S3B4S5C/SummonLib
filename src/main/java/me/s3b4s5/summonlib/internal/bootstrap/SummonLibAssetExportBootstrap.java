package me.s3b4s5.summonlib.internal.bootstrap;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.internal.assets.EmbeddedAssetPackExporter;

public final class SummonLibAssetExportBootstrap {

    private SummonLibAssetExportBootstrap() {}

    public static void exportEmbeddedAssets(JavaPlugin plugin) throws Exception {
        EmbeddedAssetPackExporter.exportEmbeddedAssetPackToModsFolder(plugin);
    }
}
