package me.s3b4s5.summonlib;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.s3b4s5.summonlib.assets.SummonConfig;
import me.s3b4s5.summonlib.assets.SummonConfigStore;
import me.s3b4s5.summonlib.cleanup.SummonOwnerCleanupEvents;
import me.s3b4s5.summonlib.interactions.SummonCastInteraction;
import me.s3b4s5.summonlib.interactions.SummonClearSummonsInteraction;
import me.s3b4s5.summonlib.interactions.SummonRemoveLastInteraction;
import me.s3b4s5.summonlib.systems.SummonCombatFollowSystem;
import me.s3b4s5.summonlib.tags.SummonTag;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;

public final class SummonLib extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile ComponentType<EntityStore, SummonTag> SUMMON_TAG_TYPE;

    public SummonLib(@NonNullDecl JavaPluginInit init) {
        super(init);

        // Must run before AssetModule scans mods/ so our pack exists as a folder-pack
        try {
            exportEmbeddedAssetPackToModsFolder();
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("[SummonLib] Failed exporting embedded asset pack");
        }

        LOGGER.atInfo().log("[SummonLib] Loaded %s v%s", getName(), getManifest().getVersion());
    }

    public static ComponentType<EntityStore, SummonTag> summonTagType() {
        ComponentType<EntityStore, SummonTag> t = SUMMON_TAG_TYPE;
        if (t == null) throw new IllegalStateException("SummonLib not initialized yet (SUMMON_TAG_TYPE is null).");
        return t;
    }

    @Override
    protected void setup() {
        // Codecs / assets
        getCodecRegistry(SummonConfig.CODEC)
                .register("SummonConfig", SummonConfig.class, SummonConfig.ABSTRACT_CODEC);
        getAssetRegistry().register(SummonConfigStore.create());

        // Components / systems
        SUMMON_TAG_TYPE = getEntityStoreRegistry().registerComponent(SummonTag.class, SummonTag::new);
        getEntityStoreRegistry().registerSystem(new SummonCombatFollowSystem(SUMMON_TAG_TYPE));

        // Interactions
        getCodecRegistry(Interaction.CODEC)
                .register("SummonCast", SummonCastInteraction.class, SummonCastInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("SummonRemoveLast", SummonRemoveLastInteraction.class, SummonRemoveLastInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("SummonClearSummons", SummonClearSummonsInteraction.class, SummonClearSummonsInteraction.CODEC);

        // Cleanup
        SummonOwnerCleanupEvents cleanup = new SummonOwnerCleanupEvents(SUMMON_TAG_TYPE, true);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, cleanup::onPlayerLeave);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, cleanup::onPlayerDisconnect);
    }

    @Override
    protected void start() {
        // no-op
    }

    /**
     * Exports the embedded asset pack (manifest.json + Server/**) from this jar into mods/ as a folder-pack.
     * AssetModule ignores *.jar but loads folders/zips containing manifest.json.
     */
    private void exportEmbeddedAssetPackToModsFolder() throws IOException {
        if (!getManifest().includesAssetPack()) return;

        Path jarPath = getFile().toAbsolutePath().normalize();
        Path modsDir = jarPath.getParent();
        if (modsDir == null) return;

        String folderName = "000_" + getManifest().getGroup() + "." + getManifest().getName();
        Path outDir = modsDir.resolve(folderName);

        // Already exported
        if (Files.isDirectory(outDir) && Files.exists(outDir.resolve("manifest.json"))) {
            return;
        }

        Path tmpDir = modsDir.resolve(folderName + ".__tmp__");
        deleteIfExists(tmpDir);
        Files.createDirectories(tmpDir);

        URI jarUri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem jarFs = FileSystems.newFileSystem(jarUri, Map.of())) {
            Path jarManifest = jarFs.getPath("/manifest.json");
            if (!Files.exists(jarManifest)) {
                throw new IOException("manifest.json not found at jar root");
            }
            Files.copy(jarManifest, tmpDir.resolve("manifest.json"), StandardCopyOption.REPLACE_EXISTING);

            Path jarServer = jarFs.getPath("/Server");
            if (Files.exists(jarServer) && Files.isDirectory(jarServer)) {
                copyDirectory(jarServer, tmpDir.resolve("Server"));
            } else {
                Files.createDirectories(tmpDir.resolve("Server"));
            }
        }

        deleteIfExists(outDir);
        Files.move(tmpDir, outDir, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void copyDirectory(Path srcDir, Path dstDir) throws IOException {
        Files.walk(srcDir).forEach(src -> {
            try {
                Path rel = srcDir.relativize(src);
                Path dst = dstDir.resolve(rel.toString());

                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Path parent = dst.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
    }
}
