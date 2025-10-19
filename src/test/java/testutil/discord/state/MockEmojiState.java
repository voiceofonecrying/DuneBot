package testutil.discord.state;

/**
 * Stores state for a Discord custom emoji.
 *
 * <p>Custom emojis are guild-specific emojis that can be used in messages,
 * reactions, and buttons. They can be static images or animated GIFs.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Emoji ID</b> - Unique identifier for this emoji</li>
 *   <li><b>Name</b> - The name of the emoji (e.g., "atreides", "storm")</li>
 *   <li><b>Animated</b> - Whether this is an animated emoji</li>
 *   <li><b>Guild ID</b> - Which guild this emoji belongs to</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 * MockEmojiState emoji = guild.createEmoji("atreides", false);
 *
 * assertThat(emoji.getName()).isEqualTo("atreides");
 * assertThat(emoji.isAnimated()).isFalse();
 * assertThat(emoji.getGuildId()).isEqualTo(12345L);
 * }</pre>
 *
 * @see MockReactionState
 * @see MockButtonState
 */
public class MockEmojiState {
    private final long emojiId;
    private final String name;
    private final boolean animated;
    private final long guildId;

    public MockEmojiState(long emojiId, String name, boolean animated, long guildId) {
        this.emojiId = emojiId;
        this.name = name;
        this.animated = animated;
        this.guildId = guildId;
    }

    /**
     * Gets the unique ID of this emoji.
     *
     * @return The emoji ID
     */
    public long getEmojiId() {
        return emojiId;
    }

    /**
     * Gets the name of this emoji.
     *
     * @return The emoji name (e.g., "atreides")
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this emoji is animated.
     *
     * @return {@code true} if animated, {@code false} for static images
     */
    public boolean isAnimated() {
        return animated;
    }

    /**
     * Gets the ID of the guild this emoji belongs to.
     *
     * @return The guild ID
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Gets the formatted emoji string for use in Discord.
     *
     * <p>Format: {@code <:name:id>} for static, {@code <a:name:id>} for animated
     *
     * @return The formatted emoji string
     */
    public String getAsString() {
        return (animated ? "<a:" : "<:") + name + ":" + emojiId + ">";
    }
}
