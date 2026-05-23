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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Reconciles the bot's application-level emojis with image files bundled in the
 * {@code emojis/} resource directory.
 *
 * <p>Each resource file contributes <em>two</em> emojis on Discord:
 * <ul>
 *   <li>a base-name emoji ({@code <basename>}) — the one code references and the
 *       one Discord displays in notifications, reactions, and emoji picker;</li>
 *   <li>a marker emoji ({@code <basename>_<xxx>}) where {@code xxx} is the last
 *       three decimal digits of a CRC32 over the file bytes — a sentinel whose
 *       presence tells the next startup that the base-name emoji is up-to-date.
 *       Nothing in code ever looks the marker up.</li>
 * </ul>
 * Changing an image's bytes (without renaming) yields a new marker name, which
 * is how the next startup detects drift and re-uploads both the base and the
 * marker.
 *
 * <p>{@link #sync(JDA)} uploads missing emojis synchronously and queues stale
 * deletes in the background, returning a future that callers may ignore.
 */
public class ApplicationEmojiSync {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationEmojiSync.class);
    private static final String RESOURCE_DIR = "emojis";
    private static final Pattern MARKER_SUFFIX = Pattern.compile("_\\d{3}$");

    private record EmojiResource(String baseName, byte[] bytes) {}

    private record Plan(String base, String marker, byte[] bytes) {}

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

        List<Plan> plans = new ArrayList<>(resources.size());
        Set<String> expectedNames = new HashSet<>();
        for (EmojiResource r : resources) {
            Plan p = new Plan(r.baseName(), r.baseName() + "_" + checksumSuffix(r.bytes()), r.bytes());
            plans.add(p);
            expectedNames.add(p.base());
            expectedNames.add(p.marker());
        }

        List<ApplicationEmoji> existing = jda.retrieveApplicationEmojis().complete();
        Map<String, ApplicationEmoji> existingByName = new HashMap<>();
        for (ApplicationEmoji e : existing) existingByName.put(e.getName(), e);

        deleteDriftedBases(plans, existingByName);

        List<ApplicationEmoji> uploaded = uploadMissing(jda, plans, existingByName);

        List<ApplicationEmoji> baseSurvivors = new ArrayList<>();
        for (ApplicationEmoji e : uploaded) {
            if (!MARKER_SUFFIX.matcher(e.getName()).find()) baseSurvivors.add(e);
        }
        for (ApplicationEmoji e : existingByName.values()) {
            if (expectedNames.contains(e.getName()) && !MARKER_SUFFIX.matcher(e.getName()).find()) {
                baseSurvivors.add(e);
            }
        }
        EmojiCache.putAll(baseSurvivors);

        return deleteStaleAsync(existingByName.values(), expectedNames);
    }

    /**
     * For every plan whose marker is missing, the base emoji (if it exists) is
     * stale — its bytes no longer match the current resource. Sync-delete it so
     * the upload phase can create a fresh one with the same name.
     */
    private static void deleteDriftedBases(List<Plan> plans, Map<String, ApplicationEmoji> existingByName) {
        for (Plan p : plans) {
            if (existingByName.containsKey(p.marker())) continue;
            ApplicationEmoji staleBase = existingByName.remove(p.base());
            if (staleBase == null) continue;
            try {
                logger.info("Deleting drifted application emoji: {}", staleBase.getName());
                staleBase.delete().complete();
            } catch (RuntimeException e) {
                logger.error("Failed to delete drifted application emoji {}", staleBase.getName(), e);
            }
        }
    }

    private static List<ApplicationEmoji> uploadMissing(JDA jda, List<Plan> plans, Map<String, ApplicationEmoji> existingByName) {
        List<ApplicationEmoji> uploaded = new ArrayList<>();
        for (Plan p : plans) {
            uploaded.addAll(uploadIfAbsent(jda, p.base(), p.bytes(), existingByName));
            uploaded.addAll(uploadIfAbsent(jda, p.marker(), p.bytes(), existingByName));
        }
        return uploaded;
    }

    private static List<ApplicationEmoji> uploadIfAbsent(JDA jda, String name, byte[] bytes, Map<String, ApplicationEmoji> existingByName) {
        if (existingByName.containsKey(name)) return List.of();
        try {
            logger.info("Uploading application emoji: {}", name);
            return List.of(jda.createApplicationEmoji(name, Icon.from(bytes)).complete());
        } catch (RuntimeException e) {
            logger.error("Failed to upload application emoji {}", name, e);
            return List.of();
        }
    }

    private static CompletableFuture<Void> deleteStaleAsync(java.util.Collection<ApplicationEmoji> survivingExisting, Set<String> expectedNames) {
        List<ApplicationEmoji> toDelete = survivingExisting.stream()
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
                out.add(new EmojiResource(base, Files.readAllBytes(p)));
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
