package testutil.discord.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord category (channel group).
 *
 * <p>A category is a container for organizing related channels. For example,
 * a game might have a "GAME CHANNELS" category containing "game-actions",
 * "bot-data", and other related channels.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li>Category ID and name</li>
 *   <li>Guild (server) ID this category belongs to</li>
 *   <li>List of channel IDs contained in this category</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 * MockCategoryState category = guild.createCategory("game-channels");
 *
 * // Create channels in this category
 * guild.createTextChannel("game-actions", category.getCategoryId());
 * guild.createTextChannel("bot-data", category.getCategoryId());
 *
 * // Category tracks all its channels
 * assertThat(category.getChannelIds()).hasSize(2);
 * }</pre>
 *
 * @see MockGuildState
 * @see MockChannelState
 */
public class MockCategoryState {
    private final long categoryId;
    private final String categoryName;
    private final long guildId;
    private final List<Long> channelIds = new ArrayList<>();

    public MockCategoryState(long categoryId, String categoryName, long guildId) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.guildId = guildId;
    }

    /**
     * Gets the unique ID of this category.
     *
     * @return The category ID
     */
    public long getCategoryId() {
        return categoryId;
    }

    /**
     * Gets the display name of this category.
     *
     * @return The category name (e.g., "GAME CHANNELS")
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Gets the ID of the guild this category belongs to.
     *
     * @return The parent guild ID
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Gets the IDs of all channels in this category.
     *
     * @return A new list containing all channel IDs (modifications won't affect the category)
     */
    public List<Long> getChannelIds() {
        return new ArrayList<>(channelIds);
    }

    /**
     * Adds a channel to this category.
     *
     * <p>This is automatically called by {@link MockGuildState#createTextChannel(String, long)}.
     * Duplicate channel IDs are ignored.
     *
     * @param channelId The channel ID to add
     */
    public void addChannel(long channelId) {
        if (!channelIds.contains(channelId)) {
            channelIds.add(channelId);
        }
    }

    /**
     * Removes a channel from this category.
     *
     * @param channelId The channel ID to remove
     */
    public void removeChannel(long channelId) {
        channelIds.remove(channelId);
    }
}
