package testutil.discord.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord text channel, including complete message history.
 *
 * <p>This is a critical component of the stateful mock infrastructure. When you use
 * {@link StatefulMockFactory#mockTextChannel(MockChannelState, MockGuildState)} to
 * create a mocked TextChannel, calling {@code channel.sendMessage()} will actually
 * store the message in this state object.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Message Persistence</b> - All messages sent to this channel are stored</li>
 *   <li><b>Message History</b> - Retrieve all messages or recent N messages</li>
 *   <li><b>Verifiable State</b> - Test assertions can verify messages were sent</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create channel
 * MockChannelState channelState = guild.createTextChannel("game-actions", categoryId);
 * TextChannel mockChannel = StatefulMockFactory.mockTextChannel(channelState, guild);
 *
 * // Send messages through the mock - they get stored!
 * mockChannel.sendMessage("Player moved to Arrakeen").queue();
 * mockChannel.sendMessage("Storm moved 3 sectors").queue();
 *
 * // Verify messages were stored in state
 * List<MockMessageState> messages = channelState.getMessages();
 * assertThat(messages).hasSize(2);
 * assertThat(messages.get(0).getContent()).isEqualTo("Player moved to Arrakeen");
 *
 * // Get recent messages
 * List<MockMessageState> recent = channelState.getRecentMessages(1);
 * assertThat(recent.get(0).getContent()).isEqualTo("Storm moved 3 sectors");
 * }</pre>
 *
 * @see MockMessageState
 * @see StatefulMockFactory#mockTextChannel(MockChannelState, MockGuildState)
 */
public class MockChannelState {
    private final long channelId;
    private final String channelName;
    private final long categoryId;
    private final long guildId;
    private final List<MockMessageState> messages = new ArrayList<>();

    public MockChannelState(long channelId, String channelName, long categoryId, long guildId) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.categoryId = categoryId;
        this.guildId = guildId;
    }

    /**
     * Gets the unique ID of this channel.
     *
     * @return The channel ID
     */
    public long getChannelId() {
        return channelId;
    }

    /**
     * Gets the display name of this channel.
     *
     * @return The channel name (e.g., "game-actions")
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Gets the ID of the category this channel belongs to.
     *
     * @return The parent category ID
     */
    public long getCategoryId() {
        return categoryId;
    }

    /**
     * Gets the ID of the guild this channel belongs to.
     *
     * @return The parent guild ID
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Adds a message to this channel's history.
     *
     * <p>This is automatically called when {@code sendMessage()} is invoked on a
     * stateful mock channel created by {@link StatefulMockFactory#mockTextChannel}.
     *
     * @param message The message to add
     */
    public void addMessage(MockMessageState message) {
        messages.add(message);
    }

    /**
     * Gets all messages in this channel.
     *
     * <p>Messages are returned in chronological order (oldest first).
     *
     * @return A new list containing all messages (modifications won't affect the channel)
     */
    public List<MockMessageState> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Gets the most recent N messages from this channel.
     *
     * <p>If the channel has fewer messages than the limit, all messages are returned.
     *
     * @param limit Maximum number of messages to return
     * @return List of the most recent messages, up to the specified limit
     */
    public List<MockMessageState> getRecentMessages(int limit) {
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }

    /**
     * Clears all messages from this channel.
     *
     * <p>Useful for resetting state between tests.
     */
    public void clearMessages() {
        messages.clear();
    }

    /**
     * Gets the ID of the latest (most recent) message in this channel.
     *
     * <p>This is used by Discord API methods like {@code channel.getLatestMessageId()}.
     *
     * @return The ID of the latest message, or 0 if the channel is empty
     */
    public long getLatestMessageId() {
        if (messages.isEmpty()) {
            return 0L;
        }
        return messages.get(messages.size() - 1).getMessageId();
    }

    /**
     * Removes a specific message from this channel by its ID.
     *
     * <p>This is called when {@code message.delete()} is invoked on a message
     * to simulate Discord's message deletion behavior.
     *
     * @param messageId The ID of the message to remove
     * @return true if a message was removed, false if no message with that ID was found
     */
    public boolean removeMessage(long messageId) {
        return messages.removeIf(msg -> msg.getMessageId() == messageId);
    }
}
