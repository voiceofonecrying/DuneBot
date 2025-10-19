package testutil.discord.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord message reaction.
 *
 * <p>Reactions are emoji responses to messages. Multiple users can react with
 * the same emoji, and the reaction tracks who has reacted and the count.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Emoji</b> - The emoji used for this reaction (can be custom or unicode)</li>
 *   <li><b>Message ID</b> - Which message this reaction is on</li>
 *   <li><b>User IDs</b> - List of users who reacted with this emoji</li>
 *   <li><b>Count</b> - Number of users who reacted (derived from user list)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockEmojiState emoji = guild.createEmoji("atreides", false);
 * MockReactionState reaction = new MockReactionState(emoji, messageId);
 *
 * // Users react
 * reaction.addReaction(user1.getUserId());
 * reaction.addReaction(user2.getUserId());
 *
 * assertThat(reaction.getCount()).isEqualTo(2);
 * assertThat(reaction.hasUser(user1.getUserId())).isTrue();
 * }</pre>
 *
 * @see MockEmojiState
 * @see MockMessageState
 */
public class MockReactionState {
    private final Object emoji; // Can be MockEmojiState or String (unicode emoji)
    private final long messageId;
    private final List<Long> userIds = new ArrayList<>();

    /**
     * Creates a reaction state with a custom emoji.
     *
     * @param emoji The custom emoji
     * @param messageId The message ID this reaction is on
     */
    public MockReactionState(MockEmojiState emoji, long messageId) {
        this.emoji = emoji;
        this.messageId = messageId;
    }

    /**
     * Creates a reaction state with a unicode emoji.
     *
     * @param emojiUnicode The unicode emoji string (e.g., "👍", "❤️")
     * @param messageId The message ID this reaction is on
     */
    public MockReactionState(String emojiUnicode, long messageId) {
        this.emoji = emojiUnicode;
        this.messageId = messageId;
    }

    /**
     * Gets the emoji for this reaction.
     *
     * @return The emoji (either MockEmojiState for custom emoji or String for unicode)
     */
    public Object getEmoji() {
        return emoji;
    }

    /**
     * Checks if this reaction uses a custom emoji.
     *
     * @return {@code true} if custom emoji, {@code false} if unicode emoji
     */
    public boolean isCustomEmoji() {
        return emoji instanceof MockEmojiState;
    }

    /**
     * Gets the custom emoji if this is a custom emoji reaction.
     *
     * @return The custom emoji state, or {@code null} if this is a unicode emoji
     */
    public MockEmojiState getCustomEmoji() {
        return emoji instanceof MockEmojiState ? (MockEmojiState) emoji : null;
    }

    /**
     * Gets the unicode emoji if this is a unicode emoji reaction.
     *
     * @return The unicode emoji string, or {@code null} if this is a custom emoji
     */
    public String getUnicodeEmoji() {
        return emoji instanceof String ? (String) emoji : null;
    }

    /**
     * Gets the message ID this reaction is on.
     *
     * @return The message ID
     */
    public long getMessageId() {
        return messageId;
    }

    /**
     * Adds a user's reaction.
     *
     * <p>Duplicate user IDs are ignored (a user can only react once with the same emoji).
     *
     * @param userId The user ID to add
     */
    public void addReaction(long userId) {
        if (!userIds.contains(userId)) {
            userIds.add(userId);
        }
    }

    /**
     * Removes a user's reaction.
     *
     * @param userId The user ID to remove
     */
    public void removeReaction(long userId) {
        userIds.remove(userId);
    }

    /**
     * Gets the count of users who reacted with this emoji.
     *
     * @return The reaction count
     */
    public int getCount() {
        return userIds.size();
    }

    /**
     * Checks if a specific user has reacted with this emoji.
     *
     * @param userId The user ID to check
     * @return {@code true} if the user has reacted, {@code false} otherwise
     */
    public boolean hasUser(long userId) {
        return userIds.contains(userId);
    }

    /**
     * Gets all user IDs who have reacted with this emoji.
     *
     * @return A new list containing all user IDs (modifications won't affect the reaction)
     */
    public List<Long> getUserIds() {
        return new ArrayList<>(userIds);
    }
}
