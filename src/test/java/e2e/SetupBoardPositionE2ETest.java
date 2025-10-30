package e2e;

import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockChannelState;
import testutil.discord.state.MockMemberState;
import testutil.discord.state.MockUserState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the /setup faction-board-position subcommand.
 *
 * <p>These tests verify faction board position management including:
 * <ul>
 *   <li>Setting faction positions (1-6)</li>
 *   <li>Swapping positions between factions</li>
 *   <li>Validating position range constraints</li>
 * </ul>
 */
@DisplayName("Setup Board Position E2E Tests")
class SetupBoardPositionE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should set faction board position and swap with another faction")
    void shouldSetFactionBoardPosition() throws Exception {
        // Given: Add three factions to the game
        addFaction("Atreides");
        addFaction("Harkonnen");
        addFaction("Emperor");

        Game gameBefore = parseGameFromBotData();
        List<Faction> factionsBefore = gameBefore.getFactions();

        assertThat(factionsBefore).hasSize(3);
        String firstFactionBefore = factionsBefore.get(0).getName();
        String secondFactionBefore = factionsBefore.get(1).getName();
        String thirdFactionBefore = factionsBefore.get(2).getName();

        // When: Move the third faction (position 3) to position 1
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction-board-position")
                .addStringOption("factionname", thirdFactionBefore)
                .addIntegerOption("dot-position", 1)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: The third faction should now be in position 1, and the former first faction in position 3
        Game gameAfter = parseGameFromBotData();
        List<Faction> factionsAfter = gameAfter.getFactions();

        assertThat(factionsAfter).hasSize(3);
        assertThat(factionsAfter.get(0).getName())
                .as("Former third faction should now be in position 1")
                .isEqualTo(thirdFactionBefore);
        assertThat(factionsAfter.get(2).getName())
                .as("Former first faction should now be in position 3")
                .isEqualTo(firstFactionBefore);
        assertThat(factionsAfter.get(1).getName())
                .as("Second faction should remain in position 2")
                .isEqualTo(secondFactionBefore);
    }

    @Test
    @DisplayName("Should swap adjacent faction positions")
    void shouldSwapAdjacentPositions() throws Exception {
        // Given: Add two factions
        addFaction("Fremen");
        addFaction("Guild");

        Game gameBefore = parseGameFromBotData();
        String firstFaction = gameBefore.getFactions().get(0).getName();
        String secondFaction = gameBefore.getFactions().get(1).getName();

        // When: Swap the first faction to position 2
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction-board-position")
                .addStringOption("factionname", firstFaction)
                .addIntegerOption("dot-position", 2)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: The positions should be swapped
        Game gameAfter = parseGameFromBotData();
        assertThat(gameAfter.getFactions().get(0).getName()).isEqualTo(secondFaction);
        assertThat(gameAfter.getFactions().get(1).getName()).isEqualTo(firstFaction);
    }

    @Test
    @DisplayName("Should handle setting faction to its current position")
    void shouldHandleSettingToCurrentPosition() throws Exception {
        // Given: Add three factions
        addFaction("BG");
        addFaction("BT");
        addFaction("Ix");

        Game gameBefore = parseGameFromBotData();
        String secondFaction = gameBefore.getFactions().get(1).getName();

        // When: Set the second faction to position 2 (its current position)
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction-board-position")
                .addStringOption("factionname", secondFaction)
                .addIntegerOption("dot-position", 2)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: The order should remain the same
        Game gameAfter = parseGameFromBotData();
        assertThat(gameAfter.getFactions().get(0).getName()).isEqualTo(gameBefore.getFactions().get(0).getName());
        assertThat(gameAfter.getFactions().get(1).getName()).isEqualTo(gameBefore.getFactions().get(1).getName());
        assertThat(gameAfter.getFactions().get(2).getName()).isEqualTo(gameBefore.getFactions().get(2).getName());
    }

    @Test
    @DisplayName("Should handle all six faction positions")
    void shouldHandleAllSixPositions() throws Exception {
        // Given: Add all six factions
        addFaction("Atreides");
        addFaction("Harkonnen");
        addFaction("Emperor");
        addFaction("Fremen");
        addFaction("Guild");
        addFaction("BG");

        Game gameBefore = parseGameFromBotData();
        assertThat(gameBefore.getFactions()).hasSize(6);

        String lastFaction = gameBefore.getFactions().get(5).getName();

        // When: Move the last faction (position 6) to position 1
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction-board-position")
                .addStringOption("factionname", lastFaction)
                .addIntegerOption("dot-position", 1)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: The last faction should be first
        Game gameAfter = parseGameFromBotData();
        assertThat(gameAfter.getFactions().get(0).getName()).isEqualTo(lastFaction);
        assertThat(gameAfter.getFactions()).hasSize(6);
    }
}
