package testutil.discord.state;

/**
 * Stores state for a Discord user.
 *
 * <p>Represents a Discord user account with a unique ID and username.
 * Users exist independently of guilds, but must be added to a guild as
 * a {@link MockMemberState} to participate in guild activities.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>User ID</b> - Unique identifier (e.g., 100001L)</li>
 *   <li><b>Username</b> - Display name (e.g., "Alice")</li>
 *   <li><b>Discriminator</b> - Four-digit tag (defaults to "0000")</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 *
 * // Create users
 * MockUserState alice = guild.createUser("Alice");
 * MockUserState bob = guild.createUser("Bob");
 *
 * assertThat(alice.getUsername()).isEqualTo("Alice");
 * assertThat(alice.getAsTag()).isEqualTo("Alice#0000");
 * }</pre>
 *
 * @see MockMemberState
 * @see MockGuildState#createUser(String)
 */
public class MockUserState {
    private final long userId;
    private final String username;
    private final String discriminator;

    public MockUserState(long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.discriminator = "0000";
    }

    public MockUserState(long userId, String username, String discriminator) {
        this.userId = userId;
        this.username = username;
        this.discriminator = discriminator;
    }

    /**
     * Gets the unique ID of this user.
     *
     * @return The user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Gets the username of this user.
     *
     * @return The username (e.g., "Alice")
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the discriminator (four-digit tag) of this user.
     *
     * @return The discriminator (defaults to "0000")
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * Gets the full user tag (username#discriminator).
     *
     * @return The user tag (e.g., "Alice#0000")
     */
    public String getAsTag() {
        return username + "#" + discriminator;
    }
}
