package caches;

import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmojiCache {
    static Map<String, Map<String, RichCustomEmoji>> guildEmojis = new HashMap<>();

    public static void setEmojis(String guildId, List<RichCustomEmoji> emojis) {
        Map<String, RichCustomEmoji> emojiMap = new HashMap<>();

        emojis.forEach((e) -> emojiMap.put(e.getName(), e));

        guildEmojis.put(guildId, emojiMap);
    }

    public static Map<String, RichCustomEmoji> getEmojis(String guildId) {
        return guildEmojis.get(guildId);
    }
}
