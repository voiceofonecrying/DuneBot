package e2e;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockMemberState;
import testutil.discord.state.MockUserState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the /setup role management subcommands.
 *
 * <p>These tests verify role assignment operations including:
 * <ul>
 *   <li>Adding players to the game role</li>
 *   <li>Adding users to the moderator role</li>
 *   <li>Removing users from the moderator role</li>
 *   <li>Protecting the primary moderator from removal</li>
 * </ul>
 */
@DisplayName("Setup Roles E2E Tests")
class SetupRolesE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should add player to game role")
    void shouldAddPlayerToGameRole() {
        // Given: A new player user
        MockUserState newPlayer = guildState.createUser("NewPlayer");
        MockMemberState newMember = guildState.createMember(newPlayer.getUserId());

        // Verify the member doesn't have the game role initially
        assertThat(newMember.getRoleIds())
                .as("New member should not have any roles initially")
                .isEmpty();

        // When: The player is added to game role
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-player-to-game-role")
                .addUserOption("player", newPlayer)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: The game role should be added to the member
        assertThat(newMember.getRoleIds())
                .as("Member should have the game role after command execution")
                .contains(gameRole.getRoleId());

        // Verify only the game role was added (not the mod role)
        assertThat(newMember.getRoleIds())
                .as("Member should only have the game role, not the mod role")
                .containsExactly(gameRole.getRoleId());
    }

    @Test
    @DisplayName("Should add moderator to mod role")
    void shouldAddModeratorToModRole() throws Exception {
        // Given: A new user to become a moderator
        MockUserState newMod = guildState.createUser("NewModerator");
        MockMemberState newModMember = guildState.createMember(newMod.getUserId());

        // Verify the member doesn't have the mod role initially
        assertThat(newModMember.getRoleIds())
                .as("New member should not have any roles initially")
                .isEmpty();

        // When: The user is added to mod role
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-mod")
                .addUserOption("player", newMod)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: The mod role should be added to the member
        assertThat(newModMember.getRoleIds())
                .as("Member should have the mod role after command execution")
                .contains(modRole.getRoleId());

        // Verify only the mod role was added (not the game role)
        assertThat(newModMember.getRoleIds())
                .as("Member should only have the mod role, not the game role")
                .containsExactly(modRole.getRoleId());
    }

    @Test
    @DisplayName("Should remove moderator from mod role")
    void shouldRemoveModeratorFromModRole() throws Exception {
        // Given: A secondary moderator who has the mod role
        MockUserState secondaryMod = guildState.createUser("SecondaryMod");
        MockMemberState secondaryModMember = guildState.createMember(secondaryMod.getUserId());

        // First add the secondary mod to the mod role
        SlashCommandInteractionEvent addEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-mod")
                .addUserOption("player", secondaryMod)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(addEvent);

        // Verify the mod role was added
        assertThat(secondaryModMember.getRoleIds())
                .as("Secondary mod should have the mod role after being added")
                .contains(modRole.getRoleId());

        // When: The secondary moderator is removed from the mod role
        SlashCommandInteractionEvent removeEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("remove-mod")
                .addUserOption("player", secondaryMod)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(removeEvent);

        // Then: The mod role should be removed from the member
        assertThat(secondaryModMember.getRoleIds())
                .as("Secondary mod should not have the mod role after being removed")
                .doesNotContain(modRole.getRoleId());

        // Verify the member has no roles at all
        assertThat(secondaryModMember.getRoleIds())
                .as("Secondary mod should have no roles after removal")
                .isEmpty();
    }

    @Test
    @DisplayName("Should not allow removing the primary moderator")
    void shouldNotAllowRemovingPrimaryModerator() throws Exception {
        // Given: The primary moderator (who created the game)
        // The primary mod should already have the mod role from game creation
        assertThat(moderatorMember.getRoleIds())
                .as("Primary moderator should have the mod role")
                .contains(modRole.getRoleId());

        // When: Attempting to remove the primary moderator
        SlashCommandInteractionEvent removeEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("remove-mod")
                .addUserOption("player", moderatorUser)
                .setChannel(getGameActionsChannel())
                .build();

        // Execute the command (it will fail internally but won't throw to us due to async handling)
        commandManager.onSlashCommandInteraction(removeEvent);

        // Then: Verify the primary moderator still has the mod role
        // (the command should have failed and not removed the role)
        assertThat(moderatorMember.getRoleIds())
                .as("Primary moderator should still have the mod role after failed removal attempt")
                .contains(modRole.getRoleId());
    }
}
