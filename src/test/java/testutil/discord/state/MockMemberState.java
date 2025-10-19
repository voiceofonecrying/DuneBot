package testutil.discord.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord member (a user's association with a specific guild).
 *
 * <p>A member represents a user within the context of a guild. The same user can be
 * a member of multiple guilds with different roles in each guild. This class tracks
 * guild-specific information like roles.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>User ID</b> - Reference to the user</li>
 *   <li><b>Guild ID</b> - Which guild this membership is for</li>
 *   <li><b>Role IDs</b> - List of roles this member has in the guild</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 *
 * // Create user and role
 * MockUserState alice = guild.createUser("Alice");
 * MockRoleState playerRole = guild.createRole("Player");
 *
 * // Add user to guild as member with role
 * MockMemberState aliceMember = guild.createMember(alice.getUserId());
 * aliceMember.addRole(playerRole.getRoleId());
 *
 * // Verify membership
 * assertThat(aliceMember.hasRole(playerRole.getRoleId())).isTrue();
 * assertThat(aliceMember.getRoleIds()).contains(playerRole.getRoleId());
 * }</pre>
 *
 * @see MockUserState
 * @see MockRoleState
 * @see MockGuildState#createMember(long)
 */
public class MockMemberState {
    private final long userId;
    private final long guildId;
    private final List<Long> roleIds = new ArrayList<>();

    public MockMemberState(long userId, long guildId) {
        this.userId = userId;
        this.guildId = guildId;
    }

    /**
     * Gets the user ID of this member.
     *
     * @return The user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Gets the guild ID this member belongs to.
     *
     * @return The guild ID
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Gets all role IDs assigned to this member.
     *
     * @return A new list containing all role IDs (modifications won't affect the member)
     */
    public List<Long> getRoleIds() {
        return new ArrayList<>(roleIds);
    }

    /**
     * Assigns a role to this member.
     *
     * <p>Duplicate role IDs are ignored.
     *
     * @param roleId The role ID to add
     */
    public void addRole(long roleId) {
        if (!roleIds.contains(roleId)) {
            roleIds.add(roleId);
        }
    }

    /**
     * Removes a role from this member.
     *
     * @param roleId The role ID to remove
     */
    public void removeRole(long roleId) {
        roleIds.remove(roleId);
    }

    /**
     * Checks if this member has a specific role.
     *
     * @param roleId The role ID to check
     * @return {@code true} if the member has this role, {@code false} otherwise
     */
    public boolean hasRole(long roleId) {
        return roleIds.contains(roleId);
    }
}
