package me.s3b4s5.summonlib.assets;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;

/**
 * Exports the embedded asset pack (manifest.json + Server/**) from a mod jar into mods/ as a folder-pack.
 * AssetModule ignores *.jar but loads folders/zips containing manifest.json.
 */
public final class EmbeddedAssetPackExporter {

    private EmbeddedAssetPackExporter() {}

    public static void exportEmbeddedAssetPackToModsFolder(JavaPlugin plugin) throws IOException {
        if (!plugin.getManifest().includesAssetPack()) return;

        Path jarPath = plugin.getFile().toAbsolutePath().normalize();
        Path modsDir = jarPath.getParent();
        if (modsDir == null) return;

        String folderName = "000_" + plugin.getManifest().getGroup() + "." + plugin.getManifest().getName();
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
