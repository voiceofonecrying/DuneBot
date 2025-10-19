package testutil.discord.state;

import java.util.*;

/**
 * Stores all state for a single Discord guild (server).
 *
 * <p>A guild contains all entities associated with a Discord server, including:
 * <ul>
 *   <li><b>Categories</b> - Channel groups (e.g., "GAME CHANNELS")</li>
 *   <li><b>Text Channels</b> - Individual channels with message history</li>
 *   <li><b>Users</b> - Discord users who exist in the system</li>
 *   <li><b>Members</b> - Users associated with this specific guild (with roles)</li>
 *   <li><b>Roles</b> - Permission groups (e.g., "Moderator", "Player")</li>
 * </ul>
 *
 * <p><b>State Management:</b>
 * <br>All state modifications are stored in-memory and persist for the lifetime of the
 * MockGuildState instance. This enables true integration testing where actions in one
 * part of the test affect state visible in another part.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockDiscordServer server = MockDiscordServer.create();
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 *
 * // Create a complete game setup
 * MockCategoryState gameCategory = guild.createCategory("game-channels");
 * MockChannelState actionsChannel = guild.createTextChannel("game-actions", gameCategory.getCategoryId());
 * MockChannelState botDataChannel = guild.createTextChannel("bot-data", gameCategory.getCategoryId());
 *
 * // Create users and members with roles
 * MockRoleState playerRole = guild.createRole("Player");
 * MockUserState alice = guild.createUser("Alice");
 * MockMemberState aliceMember = guild.createMember(alice.getUserId());
 * aliceMember.addRole(playerRole.getRoleId());
 *
 * // All entities are now stored in state and can be retrieved
 * assertThat(guild.getCategories()).hasSize(1);
 * assertThat(guild.getChannels()).hasSize(2);
 * assertThat(guild.getUsers()).hasSize(1);
 * }</pre>
 *
 * @see MockDiscordServer
 * @see MockCategoryState
 * @see MockChannelState
 */
public class MockGuildState {
    private final long guildId;
    private final String guildName;
    private final MockDiscordServer server;

    private final Map<Long, MockCategoryState> categories = new HashMap<>();
    private final Map<Long, MockChannelState> channels = new HashMap<>();
    private final Map<Long, MockThreadChannelState> threads = new HashMap<>();
    private final Map<Long, MockUserState> users = new HashMap<>();
    private final Map<Long, MockMemberState> members = new HashMap<>();
    private final Map<Long, MockRoleState> roles = new HashMap<>();
    private final Map<Long, MockEmojiState> emojis = new HashMap<>();

    public MockGuildState(long guildId, String guildName, MockDiscordServer server) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.server = server;
    }

    // ========== Guild Info ==========

    /**
     * Gets the unique ID of this guild.
     *
     * @return The guild ID (e.g., 12345L)
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Gets the display name of this guild.
     *
     * @return The guild name (e.g., "Test Guild")
     */
    public String getGuildName() {
        return guildName;
    }

    /**
     * Gets the parent MockDiscordServer that this guild belongs to.
     *
     * <p>Useful for accessing ID generators.
     *
     * @return The parent server instance
     */
    public MockDiscordServer getServer() {
        return server;
    }

    // ========== Category Management ==========

    /**
     * Creates a new category (channel group) in this guild.
     *
     * <p>The category is assigned a unique ID and stored in the guild's state.
     * Channels can then be created within this category.
     *
     * @param categoryName The display name for the category (e.g., "GAME CHANNELS")
     * @return The newly created category state
     */
    public MockCategoryState createCategory(String categoryName) {
        long categoryId = server.nextCategoryId();
        MockCategoryState category = new MockCategoryState(categoryId, categoryName, guildId);
        categories.put(categoryId, category);
        return category;
    }

    /**
     * Retrieves a category by its ID.
     *
     * @param categoryId The unique category identifier
     * @return The category state if found, or {@code null} if not found
     */
    public MockCategoryState getCategory(long categoryId) {
        return categories.get(categoryId);
    }

    /**
     * Gets all categories in this guild.
     *
     * @return Collection of all category states (may be empty)
     */
    public Collection<MockCategoryState> getCategories() {
        return categories.values();
    }

    // ========== Channel Management ==========

    /**
     * Creates a new text channel in this guild within the specified category.
     *
     * <p>The channel is assigned a unique ID and stored in the guild's state.
     * It is also automatically added to the parent category's channel list.
     *
     * @param channelName The display name for the channel (e.g., "game-actions")
     * @param categoryId The ID of the category this channel belongs to
     * @return The newly created channel state with empty message history
     */
    public MockChannelState createTextChannel(String channelName, long categoryId) {
        long channelId = server.nextChannelId();
        MockChannelState channel = new MockChannelState(channelId, channelName, categoryId, guildId);
        channels.put(channelId, channel);

        // Add channel to category
        MockCategoryState category = categories.get(categoryId);
        if (category != null) {
            category.addChannel(channelId);
        }

        return channel;
    }

    /**
     * Retrieves a channel by its ID.
     *
     * @param channelId The unique channel identifier
     * @return The channel state if found, or {@code null} if not found
     */
    public MockChannelState getChannel(long channelId) {
        return channels.get(channelId);
    }

    /**
     * Gets all text channels in this guild.
     *
     * @return Collection of all channel states (may be empty)
     */
    public Collection<MockChannelState> getChannels() {
        return channels.values();
    }

    /**
     * Gets all channels that belong to a specific category.
     *
     * @param categoryId The category ID to filter by
     * @return List of channels in the specified category (may be empty)
     */
    public List<MockChannelState> getChannelsInCategory(long categoryId) {
        List<MockChannelState> result = new ArrayList<>();
        for (MockChannelState channel : channels.values()) {
            if (channel.getCategoryId() == categoryId) {
                result.add(channel);
            }
        }
        return result;
    }

    // ========== User Management ==========

    /**
     * Creates a new Discord user.
     *
     * <p>The user is assigned a unique ID and stored in the guild's state.
     * Note that users must be added to the guild as members (via {@link #createMember(long)})
     * before they can participate in guild activities.
     *
     * @param username The user's display name (e.g., "Alice")
     * @return The newly created user state
     */
    public MockUserState createUser(String username) {
        long userId = server.nextUserId();
        MockUserState user = new MockUserState(userId, username);
        users.put(userId, user);
        return user;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId The unique user identifier
     * @return The user state if found, or {@code null} if not found
     */
    public MockUserState getUser(long userId) {
        return users.get(userId);
    }

    /**
     * Gets all users known to this guild.
     *
     * @return Collection of all user states (may be empty)
     */
    public Collection<MockUserState> getUsers() {
        return users.values();
    }

    // ========== Member Management ==========

    /**
     * Creates a guild membership for an existing user.
     *
     * <p>This associates a user with this guild, allowing them to have roles and
     * participate in guild activities. The user must already exist (created via
     * {@link #createUser(String)}).
     *
     * @param userId The ID of an existing user
     * @return The newly created member state with no roles
     * @throws IllegalArgumentException if the user does not exist
     */
    public MockMemberState createMember(long userId) {
        MockUserState user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User " + userId + " does not exist");
        }

        MockMemberState member = new MockMemberState(userId, guildId);
        members.put(userId, member);
        return member;
    }

    /**
     * Retrieves a member by their user ID.
     *
     * @param userId The user ID of the member
     * @return The member state if found, or {@code null} if not found
     */
    public MockMemberState getMember(long userId) {
        return members.get(userId);
    }

    /**
     * Gets all members of this guild.
     *
     * @return Collection of all member states (may be empty)
     */
    public Collection<MockMemberState> getMembers() {
        return members.values();
    }

    // ========== Role Management ==========

    /**
     * Creates a new role (permission group) in this guild.
     *
     * <p>The role is assigned a unique ID and stored in the guild's state.
     * Roles can then be assigned to members via {@link MockMemberState#addRole(long)}.
     *
     * @param roleName The display name for the role (e.g., "Moderator", "Player")
     * @return The newly created role state
     */
    public MockRoleState createRole(String roleName) {
        long roleId = server.nextRoleId();
        MockRoleState role = new MockRoleState(roleId, roleName);
        roles.put(roleId, role);
        return role;
    }

    /**
     * Retrieves a role by its ID.
     *
     * @param roleId The unique role identifier
     * @return The role state if found, or {@code null} if not found
     */
    public MockRoleState getRole(long roleId) {
        return roles.get(roleId);
    }

    /**
     * Gets all roles in this guild.
     *
     * @return Collection of all role states (may be empty)
     */
    public Collection<MockRoleState> getRoles() {
        return roles.values();
    }

    // ========== Thread Management ==========

    /**
     * Creates a new thread channel within a text channel.
     *
     * <p>Threads are temporary sub-channels used for focused discussions.
     *
     * @param threadName The display name for the thread
     * @param parentChannelId The ID of the parent text channel
     * @return The newly created thread state
     */
    public MockThreadChannelState createThread(String threadName, long parentChannelId) {
        long threadId = server.nextThreadId();
        MockThreadChannelState thread = new MockThreadChannelState(threadId, threadName, parentChannelId, guildId);
        threads.put(threadId, thread);
        return thread;
    }

    /**
     * Retrieves a thread by its ID.
     *
     * @param threadId The unique thread identifier
     * @return The thread state if found, or {@code null} if not found
     */
    public MockThreadChannelState getThread(long threadId) {
        return threads.get(threadId);
    }

    /**
     * Gets all thread channels in this guild.
     *
     * @return Collection of all thread states (may be empty)
     */
    public Collection<MockThreadChannelState> getThreads() {
        return threads.values();
    }

    /**
     * Gets all threads that belong to a specific parent channel.
     *
     * @param parentChannelId The parent channel ID
     * @return List of threads in the specified channel
     */
    public List<MockThreadChannelState> getThreadsInChannel(long parentChannelId) {
        List<MockThreadChannelState> result = new ArrayList<>();
        for (MockThreadChannelState thread : threads.values()) {
            if (thread.getParentChannelId() == parentChannelId) {
                result.add(thread);
            }
        }
        return result;
    }

    // ========== Emoji Management ==========

    /**
     * Creates a new custom emoji in this guild.
     *
     * @param name The emoji name (e.g., "atreides", "storm")
     * @param animated Whether this is an animated emoji
     * @return The newly created emoji state
     */
    public MockEmojiState createEmoji(String name, boolean animated) {
        long emojiId = server.nextEmojiId();
        MockEmojiState emoji = new MockEmojiState(emojiId, name, animated, guildId);
        emojis.put(emojiId, emoji);
        return emoji;
    }

    /**
     * Retrieves an emoji by its ID.
     *
     * @param emojiId The unique emoji identifier
     * @return The emoji state if found, or {@code null} if not found
     */
    public MockEmojiState getEmoji(long emojiId) {
        return emojis.get(emojiId);
    }

    /**
     * Retrieves an emoji by its name.
     *
     * @param name The emoji name
     * @return The emoji state if found, or {@code null} if not found
     */
    public MockEmojiState getEmojiByName(String name) {
        for (MockEmojiState emoji : emojis.values()) {
            if (emoji.getName().equals(name)) {
                return emoji;
            }
        }
        return null;
    }

    /**
     * Gets all custom emojis in this guild.
     *
     * @return Collection of all emoji states (may be empty)
     */
    public Collection<MockEmojiState> getEmojis() {
        return emojis.values();
    }
}
