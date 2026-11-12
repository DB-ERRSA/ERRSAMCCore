package common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class UpdateVias {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    interface DownloadAction {
        void download() throws IOException;
    }

    public record Release(int build, String version, String artifactPath) {}

    public static Release latest(boolean allowSnapshots) throws IOException {
        String job = "job/ViaVersion";
        JsonNode builds = readJson("https://ci.viaversion.com/" + job + "/api/json?tree=builds[number]").path("builds");
        if (!builds.isArray()) throw new IOException("No ViaVersion build list available");
        for (JsonNode build : builds) {
            int number = build.path("number").asInt(-1);
            if (number < 0) continue;
            JsonNode artifacts = readJson("https://ci.viaversion.com/" + job + "/" + number + "/api/json").path("artifacts");
            if (!artifacts.isArray()) continue;
            for (JsonNode artifact : artifacts) {
                String name = artifact.path("fileName").asText("");
                if (!name.startsWith("ViaVersion-") || !name.endsWith(".jar") ||
                        name.contains("-sources") || name.contains("-javadoc") || name.contains("-api")) continue;
                String version = name.substring("ViaVersion-".length(), name.length() - 4);
                if (!allowSnapshots && version.contains("SNAPSHOT")) continue;
                String relative = artifact.path("relativePath").asText("");
                if (relative.isBlank()) continue;
                return new Release(number, version, "https://ci.viaversion.com/" + job + "/" + number + "/artifact/" + relative);
            }
        }
        throw new IOException("No qualifying ViaVersion JAR artifact found");
    }

    public static void stage(Release release, Path stagedDirectory) throws IOException {
        Files.createDirectories(stagedDirectory);
        downloadToTarget(new URL(release.artifactPath()), stagedDirectory.resolve("ViaVersion.jar"));
    }

    static void downloadToTarget(final URL downloadUrl, Path target) throws IOException {
        SafeFileReplace.replace(target, temporaryFile -> {
            URLConnection conn = downloadUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(temporaryFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            validateJar(temporaryFile);
        });
    }

    static void validateJar(Path candidate) throws IOException {
        try (JarFile jar = new JarFile(candidate.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            byte[] buffer = new byte[8192];
            boolean containsFile = false;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                containsFile = true;
                try (InputStream in = jar.getInputStream(entry)) {
                    while (in.read(buffer) != -1) {
                        // Read every entry so corrupt compressed data is rejected.
                    }
                }
            }

            if (!containsFile) {
                throw new IOException("Downloaded update JAR contains no files: " + candidate.getFileName());
            }
        } catch (IOException invalidJar) {
            throw new IOException("Downloaded update is not a valid JAR: " + candidate.getFileName(), invalidJar);
        }
    }

    private static JsonNode readJson(String url) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        try (InputStream in = conn.getInputStream()) {
            return MAPPER.readTree(in);
        }
    }
}
