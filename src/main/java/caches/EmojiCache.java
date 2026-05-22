package caches;

import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global lookup table for the bot's application emojis, keyed by their base name
 * (i.e., the emoji name in code, without the {@code _xxx} CRC32 suffix that
 * {@link ApplicationEmojiSync} attaches when uploading to Discord).
 *
 * <p>Also hosts the {@link #tagEmojis(String)} pipeline that rewrites
 * {@code :name:} placeholders in outgoing messages into Discord's
 * {@code <:name:id>} render format.
 */
public class EmojiCache {
    private static final Pattern TAGGED = Pattern.compile("<(:[a-zA-Z0-9_]+:)\\d+>");
    private static final Pattern UNTAGGED = Pattern.compile("(?<!<):([a-zA-Z0-9_]+):(?!\\d+>)");
    private static final Pattern SUFFIX = Pattern.compile("_\\d{3}$");

    private static final Map<String, ApplicationEmoji> byBaseName = new HashMap<>();

    /** Replace the cache contents with the given emojis, indexed by base name. */
    public static void putAll(Collection<ApplicationEmoji> emojis) {
        byBaseName.clear();
        for (ApplicationEmoji emoji : emojis) {
            byBaseName.put(stripSuffix(emoji.getName()), emoji);
        }
    }

    /**
     * Look up an emoji by name. Accepts {@code atreides}, {@code :atreides:}, or
     * {@code atreides_438} — colons and a trailing {@code _<3 digits>} suffix are
     * stripped before lookup. Returns null if not present.
     */
    public static ApplicationEmoji get(String name) {
        return byBaseName.get(stripSuffix(name.replace(":", "")));
    }

    /**
     * Render an emoji name as a Discord-formatted tag (e.g. {@code <:atreides_438:id>}),
     * or fall back to {@code :name:} if the emoji isn't in the cache.
     */
    public static String getFormatted(String name) {
        ApplicationEmoji emoji = get(name);
        return emoji == null ? ":" + stripSuffix(name.replace(":", "")) + ":" : emoji.getFormatted();
    }

    /** Strip guild/app IDs from any {@code <:name:id>} tags, leaving bare {@code :name:}. */
    public static String untagEmojis(String message) {
        Matcher matcher = TAGGED.matcher(message);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, matcher.group(1));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Rewrite {@code :name:} placeholders into Discord {@code <:name:id>} tags. */
    public static String tagEmojis(String message) {
        String untagged = untagEmojis(message);
        Matcher matcher = UNTAGGED.matcher(untagged);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(getFormatted(matcher.group(1))));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String stripSuffix(String name) {
        return SUFFIX.matcher(name).replaceFirst("");
    }
}
