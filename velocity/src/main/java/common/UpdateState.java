package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Records actual installed version separately from pending and latest releases. */
public final class UpdateState {
    private final Path file;
    private final Properties properties = new Properties();

    public UpdateState(Path directory) throws IOException {
        Files.createDirectories(directory);
        this.file = directory.resolve("update-state.properties");
        if (Files.exists(file)) try (InputStream in = Files.newInputStream(file)) { properties.load(in); }
    }

    public String get(String key) { return properties.getProperty(key, ""); }
    public void set(String key, String value) { properties.setProperty(key, value == null ? "" : value); }
    public void save() throws IOException {
        Path temp = Files.createTempFile(file.getParent(), "state-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { properties.store(out, "ERRSA ViaVersion update state"); }
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public boolean pendingMatches(UpdateVias.Release release, Path stagedJar) {
        return Files.isRegularFile(stagedJar) && get("staged.build").equals(Integer.toString(release.build()))
                && get("staged.version").equals(release.version());
    }
}
