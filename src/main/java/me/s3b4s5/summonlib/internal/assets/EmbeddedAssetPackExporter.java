package me.s3b4s5.summonlib.internal.assets;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Internal bootstrap helper that exports the embedded asset pack beside the mod jar.
 */
public final class EmbeddedAssetPackExporter {

    private static final boolean ALWAYS_EXTRACT = false;

    private static final String[] REQUIRED_RELATIVE_PATHS = {
            "manifest.json",
            "Server",
            "Server/Entity/Stats/Max Minions.json",
            "Server/Entity/Stats/Summon Damage.json",
            "Server/NPC/Roles/SummonLib_Fly_Abstract.json",
            "Server/NPC/Roles/SummonLib_Walk_Abstract.json"
    };

    private static final String MARKER_FILE = ".summonlib_export_marker";

    private EmbeddedAssetPackExporter() {}

    public static void exportEmbeddedAssetPackToModsFolder(JavaPlugin plugin) throws IOException {
        if (!plugin.getManifest().includesAssetPack()) return;

        Path jarPath = plugin.getFile().toAbsolutePath().normalize();
        Path modsDir = jarPath.getParent();
        if (modsDir == null) return;

        String folderName = "000_" + plugin.getManifest().getGroup() + "." + plugin.getManifest().getName();
        Path outDir = modsDir.resolve(folderName);

        if (!ALWAYS_EXTRACT && !needsExport(outDir, jarPath)) {
            return;
        }

        Path tmpDir = modsDir.resolve(folderName + ".__tmp__");
        deleteIfExists(tmpDir);
        Files.createDirectories(tmpDir);

        try {
            exportFromJarToFolder(jarPath, tmpDir);

            // Write marker AFTER successful extraction.
            writeMarker(tmpDir, jarPath);

            // Final sanity check before swapping.
            if (validateExportedPack(tmpDir)) {
                throw new IOException("Exported asset pack validation failed (tmpDir).");
            }

            // Replace outDir atomically-ish: delete old then atomic move, with fallback.
            deleteIfExists(outDir);
            try {
                Files.move(tmpDir, outDir, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpDir, outDir, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // Ensure tmp cleanup on failure.
            try { deleteIfExists(tmpDir); } catch (IOException ignored) {}
            throw e;
        }
    }

    private static boolean needsExport(Path outDir, Path jarPath) throws IOException {
        if (!Files.isDirectory(outDir)) return true;

        // Basic required files check
        if (validateExportedPack(outDir)) return true;

        // Fingerprint check
        Path marker = outDir.resolve(MARKER_FILE);
        if (!Files.exists(marker)) return true;

        String expected = computeJarFingerprint(jarPath);
        String actual;
        try {
            actual = Files.readString(marker, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return true;
        }

        return !expected.equals(actual);
    }

    private static boolean validateExportedPack(Path dir) {
        // Must have manifest.json, must have Server folder.
        for (String rel : REQUIRED_RELATIVE_PATHS) {
            Path p = dir.resolve(rel);
            if (rel.endsWith("/") || rel.equals("Server")) {
                if (!Files.isDirectory(p)) return true;
            } else {
                if (!Files.isRegularFile(p)) return true;
            }
        }
        return false;
    }

    private static void writeMarker(Path dir, Path jarPath) throws IOException {
        String fp = computeJarFingerprint(jarPath);
        Files.writeString(
                dir.resolve(MARKER_FILE),
                fp,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static String computeJarFingerprint(Path jarPath) throws IOException {
        // Enough to detect changes reliably without hashing the whole jar.
        long mtime = Files.getLastModifiedTime(jarPath).toMillis();
        long size = Files.size(jarPath);
        return mtime + ":" + size;
    }

    private static void exportFromJarToFolder(Path jarPath, Path tmpDir) throws IOException {
        URI jarUri = URI.create("jar:" + jarPath.toUri());

        try (FileSystem jarFs = openJarFileSystem(jarUri)) {
            Path jarManifest = jarFs.getPath("/manifest.json");
            if (!Files.exists(jarManifest)) {
                throw new IOException("manifest.json not found at jar root");
            }
            Files.copy(jarManifest, tmpDir.resolve("manifest.json"), StandardCopyOption.REPLACE_EXISTING);

            Path jarServer = jarFs.getPath("/Server");
            Path dstServer = tmpDir.resolve("Server");

            if (Files.exists(jarServer) && Files.isDirectory(jarServer)) {
                copyDirectoryTree(jarServer, dstServer);
            } else {
                Files.createDirectories(dstServer);
            }
        }
    }

    /**
     * Robust jar FS opener: handles FileSystemAlreadyExistsException.
     */
    private static FileSystem openJarFileSystem(URI jarUri) throws IOException {
        try {
            return FileSystems.newFileSystem(jarUri, Map.of());
        } catch (FileSystemAlreadyExistsException e) {
            return FileSystems.getFileSystem(jarUri);
        } catch (ProviderNotFoundException | FileSystemNotFoundException e) {
            throw new IOException("Unable to open jar filesystem: " + jarUri, e);
        }
    }

    /**
     * Copies a directory tree using walkFileTree (avoids Stream leaks and lets IOExceptions propagate cleanly).
     */
    private static void copyDirectoryTree(Path srcDir, Path dstDir) throws IOException {
        Files.createDirectories(dstDir);

        Files.walkFileTree(srcDir, new SimpleFileVisitor<>() {
            @Nonnull
            @Override
            public FileVisitResult preVisitDirectory(@Nonnull Path dir, @Nonnull BasicFileAttributes attrs) throws IOException {
                Path rel = srcDir.relativize(dir);
                Path target = dstDir.resolve(rel.toString());
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }
            @Nonnull
            @Override
            public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
                Path rel = srcDir.relativize(file);
                Path target = dstDir.resolve(rel.toString());
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) return;

        // Delete tree safely
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Nonnull
            @Override
            public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) {
                try { Files.deleteIfExists(file); } catch (IOException ignored) {}
                return FileVisitResult.CONTINUE;
            }

            @Nonnull
            @Override
            public FileVisitResult postVisitDirectory(@Nonnull Path dir, IOException exc) {
                try { Files.deleteIfExists(dir); } catch (IOException ignored) {}
                return FileVisitResult.CONTINUE;
            }
        });
    }
}


