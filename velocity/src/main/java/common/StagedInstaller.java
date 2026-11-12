package common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/** Applies a previously staged JAR only in Velocity's startup initialization phase. */
public final class StagedInstaller {
    private static final ObjectMapper JSON = new ObjectMapper();
    private StagedInstaller() {}

    public static Path findSingleViaVersion(Path plugins) throws IOException {
        List<Path> matches = new ArrayList<>();
        if (!Files.isDirectory(plugins)) throw new IOException("Missing plugins directory: " + plugins);
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(plugins, "*.jar")) {
            for (Path file : entries) {
                if (Files.isSymbolicLink(file) || !Files.isRegularFile(file)) continue;
                if (isViaVersion(file)) matches.add(file);
            }
        }
        if (matches.size() != 1) throw new IOException("Expected exactly one active ViaVersion JAR, found " + matches.size() + ": " + matches);
        return matches.get(0);
    }

    public static boolean isViaVersion(Path candidate) throws IOException {
        try (JarFile jar = new JarFile(candidate.toFile())) {
            ZipEntry metadata = jar.getEntry("velocity-plugin.json");
            if (metadata == null) return false;
            try (InputStream in = jar.getInputStream(metadata)) {
                JsonNode node = JSON.readTree(in);
                return "viaversion".equalsIgnoreCase(node.path("id").asText());
            }
        } catch (java.util.zip.ZipException ex) {
            throw new IOException("Invalid plugin JAR " + candidate, ex);
        }
    }

    public static Path install(Path plugins, Path stagedJar, Path backupDirectory) throws IOException {
        if (!Files.isRegularFile(stagedJar) || Files.isSymbolicLink(stagedJar)) throw new IOException("Staged ViaVersion JAR missing or invalid");
        UpdateVias.validateJar(stagedJar);
        if (!isViaVersion(stagedJar)) throw new IOException("Staged JAR is not a Velocity ViaVersion plugin");
        Path active = findSingleViaVersion(plugins);
        if (Files.isSymbolicLink(active)) throw new IOException("Refusing to replace symbolic link");
        Files.createDirectories(backupDirectory);
        Path backup = backupDirectory.resolve("ViaVersion-previous.jar");
        // Backups live outside plugins/, so Velocity will never load a second copy.
        Files.copy(active, backup, StandardCopyOption.REPLACE_EXISTING);
        Path temporary = Files.createTempFile(plugins, ".errsa-viaversion-", ".tmp");
        try {
            Files.copy(stagedJar, temporary, StandardCopyOption.REPLACE_EXISTING);
            UpdateVias.validateJar(temporary);
            try {
                Files.move(temporary, active, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException("Atomic JAR replacement unavailable; original preserved", ex);
            }
        } finally { Files.deleteIfExists(temporary); }
        return active;
    }
}
