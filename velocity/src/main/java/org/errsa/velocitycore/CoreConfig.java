package org.errsa.velocitycore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Creates module configuration files and safely copies pre-existing Velocity plugin settings. */
public final class CoreConfig {
    private final Path root;

    public CoreConfig(Path root) {
        this.root = root;
    }

    public Path modulePath(String module) {
        return root.resolve(module);
    }

    public Properties loadModule(String module, String oldPath, String resource) throws IOException {
        Path directory = modulePath(module);
        Files.createDirectories(directory);
        Path config = directory.resolve("config.properties");
        if (Files.notExists(config)) {
            Path legacy = oldPath == null ? null : Path.of(oldPath);
            if (legacy != null && Files.isRegularFile(legacy)) {
                Files.copy(legacy, config, StandardCopyOption.COPY_ATTRIBUTES);
            } else {
                try (InputStream in = getClass().getResourceAsStream(resource)) {
                    if (in == null) throw new IOException("Missing bundled config: " + resource);
                    Files.copy(in, config);
                }
            }
        }
        Properties values = new Properties();
        try (InputStream in = Files.newInputStream(config)) {
            values.load(in);
        }
        return values;
    }

    public boolean isEnabled(String module) throws IOException {
        Path main = root.resolve("config.properties");
        Files.createDirectories(root);
        if (Files.notExists(main)) {
            try (InputStream in = getClass().getResourceAsStream("/core.properties")) {
                Files.copy(in, main);
            }
        }
        Properties values = new Properties();
        try (InputStream in = Files.newInputStream(main)) {
            values.load(in);
        }
        return Boolean.parseBoolean(values.getProperty("modules." + module + ".enabled", "false"));
    }
}
