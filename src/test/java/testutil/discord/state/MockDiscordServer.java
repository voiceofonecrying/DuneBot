package testutil.discord.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Central in-memory mock Discord server that maintains all state for testing.
 *
 * <p>This class serves as the root of the stateful mock Discord infrastructure.
 * It stores all guilds and provides ID generation for Discord entities.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Maintains state for multiple guilds (servers)</li>
 *   <li>Generates unique IDs for messages, channels, users, roles, and categories</li>
 *   <li>State persists across interactions, enabling true integration testing</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create a mock Discord server
 * MockDiscordServer server = MockDiscordServer.create();
 *
 * // Create a guild (Discord server)
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 *
 * // Create a category and channel
 * MockCategoryState category = guild.createCategory("game-channels");
 * MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
 *
 * // Get a stateful mock that interacts with the state
 * TextChannel mockChannel = StatefulMockFactory.mockTextChannel(channel, guild);
 *
 * // Messages sent through the mock are stored in state!
 * mockChannel.sendMessage("Hello World").queue();
 *
 * // Verify the message was stored
 * List<MockMessageState> messages = channel.getMessages();
 * assertThat(messages).hasSize(1);
 * assertThat(messages.get(0).getContent()).isEqualTo("Hello World");
 * }</pre>
 *
 * @see MockGuildState
 * @see StatefulMockFactory
 */
public class MockDiscordServer {
    private final Map<Long, MockGuildState> guilds = new HashMap<>();
    private long nextMessageId = 1000000L;
    private long nextChannelId = 2000000L;
    private long nextUserId = 3000000L;
    private long nextRoleId = 4000000L;
    private long nextCategoryId = 5000000L;
    private long nextThreadId = 6000000L;
    private long nextEmojiId = 7000000L;
    private long nextInteractionId = 8000000L;

    /**
     * Creates a new mock Discord server instance.
     *
     * <p>This is the entry point for creating a stateful mock Discord environment.
     * Each server maintains its own separate state and ID generators.
     *
     * @return A new MockDiscordServer with no guilds
     */
    public static MockDiscordServer create() {
        return new MockDiscordServer();
    }

    /**
     * Creates a new guild (Discord server) in this mock server.
     *
     * <p>The guild is stored in the server's state and can be retrieved later
     * using {@link #getGuild(long)}.
     *
     * @param guildId The unique guild identifier (e.g., 12345L)
     * @param guildName The guild's display name (e.g., "Test Guild")
     * @return The newly created guild state, ready to have channels and members added
     */
    public MockGuildState createGuild(long guildId, String guildName) {
        MockGuildState guild = new MockGuildState(guildId, guildName, this);
        guilds.put(guildId, guild);
        return guild;
    }

    /**
     * Retrieves a guild by its ID.
     *
     * @param guildId The unique guild identifier
     * @return The guild state if found, or {@code null} if no guild with this ID exists
     */
    public MockGuildState getGuild(long guildId) {
        return guilds.get(guildId);
    }

    /**
     * Generates a unique message ID.
     *
     * <p>Thread-safe counter that increments each time a message is created.
     * Messages start at ID 1000000L and increment from there.
     *
     * @return A new, unique message ID
     */
    public synchronized long nextMessageId() {
        return nextMessageId++;
    }

    /**
     * Generates a unique channel ID.
     *
     * <p>Thread-safe counter that increments each time a channel is created.
     * Channels start at ID 2000000L and increment from there.
     *
     * @return A new, unique channel ID
     */
    public synchronized long nextChannelId() {
        return nextChannelId++;
    }

    /**
     * Generates a unique user ID.
     *
     * <p>Thread-safe counter that increments each time a user is created.
     * Users start at ID 3000000L and increment from there.
     *
     * @return A new, unique user ID
     */
    public synchronized long nextUserId() {
        return nextUserId++;
    }

    /**
     * Generates a unique role ID.
     *
     * <p>Thread-safe counter that increments each time a role is created.
     * Roles start at ID 4000000L and increment from there.
     *
     * @return A new, unique role ID
     */
    public synchronized long nextRoleId() {
        return nextRoleId++;
    }

    /**
     * Generates a unique category ID.
     *
     * <p>Thread-safe counter that increments each time a category is created.
     * Categories start at ID 5000000L and increment from there.
     *
     * @return A new, unique category ID
     */
    public synchronized long nextCategoryId() {
        return nextCategoryId++;
    }

    /**
     * Generates a unique thread ID.
     *
     * <p>Thread-safe counter that increments each time a thread is created.
     * Threads start at ID 6000000L and increment from there.
     *
     * @return A new, unique thread ID
     */
    public synchronized long nextThreadId() {
        return nextThreadId++;
    }

    /**
     * Generates a unique emoji ID.
     *
     * <p>Thread-safe counter that increments each time an emoji is created.
     * Emojis start at ID 7000000L and increment from there.
     *
     * @return A new, unique emoji ID
     */
    public synchronized long nextEmojiId() {
        return nextEmojiId++;
    }

    /**
     * Generates a unique interaction ID.
     *
     * <p>Thread-safe counter that increments each time an interaction is created.
     * Interactions start at ID 8000000L and increment from there.
     *
     * @return A new, unique interaction ID
     */
    public synchronized long nextInteractionId() {
        return nextInteractionId++;
    }
}
