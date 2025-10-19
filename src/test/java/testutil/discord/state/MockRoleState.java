package testutil.discord.state;

/**
 * Stores state for a Discord role (permission group).
 *
 * <p>Roles are used to group members and assign permissions. Common examples
 * include "Moderator", "Player", "Admin", etc. Members can have multiple roles.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Role ID</b> - Unique identifier</li>
 *   <li><b>Role Name</b> - Display name (e.g., "Moderator")</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 *
 * // Create roles
 * MockRoleState modRole = guild.createRole("Moderator");
 * MockRoleState playerRole = guild.createRole("Player");
 *
 * assertThat(modRole.getRoleName()).isEqualTo("Moderator");
 * assertThat(playerRole.getRoleName()).isEqualTo("Player");
 * }</pre>
 *
 * @see MockMemberState
 * @see MockGuildState#createRole(String)
 */
public class MockRoleState {
    private final long roleId;
    private final String roleName;

    public MockRoleState(long roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    /**
     * Gets the unique ID of this role.
     *
     * @return The role ID
     */
    public long getRoleId() {
        return roleId;
    }

    /**
     * Gets the display name of this role.
     *
     * @return The role name (e.g., "Moderator", "Player")
     */
    public String getRoleName() {
        return roleName;
    }
}
