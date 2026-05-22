package caches;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Reconciles the bot's application-level emojis with image files bundled in the
 * {@code emojis/} resource directory.
 *
 * <p>Each resource file's expected emoji name is {@code <basename>_<xxx>}, where
 * {@code xxx} is the last three decimal digits of a CRC32 over the file bytes.
 * Changing an image (without renaming it) therefore yields a new expected name,
 * which is what lets the next startup detect drift by name alone.
 *
 * <p>{@link #sync(JDA)} uploads missing emojis synchronously and queues stale
 * deletes in the background, returning a future that callers may ignore.
 */
public class ApplicationEmojiSync {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationEmojiSync.class);
    private static final String RESOURCE_DIR = "emojis";

    private record EmojiResource(String name, byte[] bytes) {}

    /**
     * Reconcile application emojis with the {@code emojis/} resource directory.
     * Uploads complete before this method returns; deletes run asynchronously.
     *
     * @param jda a connected JDA instance (typically post-{@code awaitReady}).
     * @return a future that completes when stale-emoji deletes finish.
     */
    public static CompletableFuture<Void> sync(JDA jda) {
        List<EmojiResource> resources;
        try {
            resources = loadResourceEmojis();
        } catch (IOException e) {
            logger.error("Failed to read application emoji resources", e);
            return CompletableFuture.completedFuture(null);
        }

        List<ApplicationEmoji> existing = jda.retrieveApplicationEmojis().complete();
        Set<String> existingNames = new HashSet<>();
        for (ApplicationEmoji e : existing) existingNames.add(e.getName());

        Set<String> expectedNames = new HashSet<>();
        for (EmojiResource r : resources) expectedNames.add(r.name());

        List<ApplicationEmoji> uploaded = uploadMissing(jda, resources, existingNames);

        List<ApplicationEmoji> survivors = new ArrayList<>(uploaded);
        for (ApplicationEmoji e : existing) {
            if (expectedNames.contains(e.getName())) survivors.add(e);
        }
        EmojiCache.putAll(survivors);

        return deleteStaleAsync(existing, expectedNames);
    }

    private static List<ApplicationEmoji> uploadMissing(JDA jda, List<EmojiResource> resources, Set<String> existingNames) {
        List<ApplicationEmoji> uploaded = new ArrayList<>();
        for (EmojiResource r : resources) {
            if (existingNames.contains(r.name())) continue;
            try {
                logger.info("Uploading application emoji: {}", r.name());
                uploaded.add(jda.createApplicationEmoji(r.name(), Icon.from(r.bytes())).complete());
            } catch (RuntimeException e) {
                logger.error("Failed to upload application emoji {}", r.name(), e);
            }
        }
        return uploaded;
    }

    private static CompletableFuture<Void> deleteStaleAsync(List<ApplicationEmoji> existing, Set<String> expectedNames) {
        List<ApplicationEmoji> toDelete = existing.stream()
                .filter(e -> !expectedNames.contains(e.getName()))
                .toList();
        if (toDelete.isEmpty()) return CompletableFuture.completedFuture(null);

        CompletableFuture<?>[] futures = toDelete.stream()
                .map(e -> {
                    logger.info("Deleting stale application emoji: {}", e.getName());
                    return e.delete().submit().exceptionally(t -> {
                        logger.error("Failed to delete application emoji {}", e.getName(), t);
                        return null;
                    });
                })
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private static List<EmojiResource> loadResourceEmojis() throws IOException {
        URL url = ApplicationEmojiSync.class.getClassLoader().getResource(RESOURCE_DIR);
        if (url == null) {
            logger.warn("No application emoji resource directory found at /{}", RESOURCE_DIR);
            return List.of();
        }

        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        if ("jar".equals(uri.getScheme())) {
            return readFromJar(uri);
        }
        return readFromDirectory(Paths.get(uri));
    }

    private static List<EmojiResource> readFromJar(URI uri) throws IOException {
        FileSystem fs;
        boolean ownsFileSystem = false;
        try {
            fs = FileSystems.newFileSystem(uri, Map.of());
            ownsFileSystem = true;
        } catch (FileSystemAlreadyExistsException ignored) {
            fs = FileSystems.getFileSystem(uri);
        }
        try {
            return readFromDirectory(fs.getPath("/" + RESOURCE_DIR));
        } finally {
            if (ownsFileSystem) fs.close();
        }
    }

    private static List<EmojiResource> readFromDirectory(Path dir) throws IOException {
        List<EmojiResource> out = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String fileName = p.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
                byte[] bytes = Files.readAllBytes(p);
                out.add(new EmojiResource(base + "_" + checksumSuffix(bytes), bytes));
            }
        }
        return out;
    }

    private static String checksumSuffix(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return String.format("%03d", crc.getValue() % 1000);
    }
}
