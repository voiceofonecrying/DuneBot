package e2e;

import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockChannelState;
import testutil.discord.state.MockUserState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the /setup remove-double-powered-treachery subcommand.
 *
 * <p>These tests verify removal of specific expansion treachery cards including:
 * <ul>
 *   <li>Removing Poison Blade from the deck</li>
 *   <li>Removing Shield Snooper from the deck</li>
 *   <li>Verifying other expansion cards remain in the deck</li>
 * </ul>
 *
 * <p>The base test class provides proper emoji mocking that allows JDA's real
 * Button and Emoji classes to work correctly with properly formatted emoji strings.
 */
@DisplayName("Setup Remove Double-Powered Treachery E2E Tests")
class SetupRemoveTreacheryE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should successfully call remove-double-powered-treachery command and advance setup")
    void shouldRemoveDoublePoweredTreacheryCards() throws Exception {
        // Given: A game with 6 factions and advance has been called once
        addAllSixFactions();

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        // Add expansion treachery cards option
        SlashCommandInteractionEvent addOptionEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "EXPANSION_TREACHERY_CARDS")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(addOptionEvent);

        // Call advance to create decks (will stop at CREATE_DECKS step)
        SlashCommandInteractionEvent advanceEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("advance")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(advanceEvent);

        Game gameBefore = parseGameFromBotData();

        // Verify decks were created
        assertThat(gameBefore.getTreacheryDeck())
                .as("Treachery deck should exist after advance")
                .isNotEmpty();

        long poisonBladeCountBefore = gameBefore.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Poison Blade"))
                .count();
        long shieldSnooperCountBefore = gameBefore.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Shield Snooper"))
                .count();

        // When: Calling remove-double-powered-treachery
        SlashCommandInteractionEvent removeEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("remove-double-powered-treachery")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(removeEvent);

        // Then: We should be able to parse the new game state
        Game gameAfter = parseGameFromBotData();
        assertThat(gameAfter).isNotNull();
        assertThat(gameAfter.getFactions()).hasSize(6);

        // And: Poison Blade and Shield Snooper should be removed
        long poisonBladeCountAfter = gameAfter.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Poison Blade"))
                .count();
        long shieldSnooperCountAfter = gameAfter.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Shield Snooper"))
                .count();

        assertThat(poisonBladeCountAfter)
                .as("Poison Blade should be removed")
                .isLessThan(poisonBladeCountBefore);
        assertThat(shieldSnooperCountAfter)
                .as("Shield Snooper should be removed")
                .isLessThan(shieldSnooperCountBefore);
    }

    @Test
    @DisplayName("Should verify other expansion cards remain in deck after removal")
    void shouldKeepOtherExpansionCardsInDeck() throws Exception {
        // Given: A game with expansion treachery cards
        addAllSixFactions();

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        SlashCommandInteractionEvent addOptionEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "EXPANSION_TREACHERY_CARDS")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(addOptionEvent);

        SlashCommandInteractionEvent advanceEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("advance")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(advanceEvent);

        // Count some other expansion cards before removal
        Game gameBefore = parseGameFromBotData();
        long amalCountBefore = gameBefore.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Amal"))
                .count();
        long artilleryStrikeCountBefore = gameBefore.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Artillery Strike"))
                .count();

        // When: Removing double-powered cards
        SlashCommandInteractionEvent removeEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("remove-double-powered-treachery")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(removeEvent);

        // Then: Other expansion cards should remain
        Game gameAfter = parseGameFromBotData();
        long amalCountAfter = gameAfter.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Amal"))
                .count();
        long artilleryStrikeCountAfter = gameAfter.getTreacheryDeck().stream()
                .filter(card -> card.name().equals("Artillery Strike"))
                .count();

        assertThat(amalCountAfter)
                .as("Amal should still be in the deck")
                .isEqualTo(amalCountBefore);
        assertThat(artilleryStrikeCountAfter)
                .as("Artillery Strike should still be in the deck")
                .isEqualTo(artilleryStrikeCountBefore);
    }

    /**
     * Helper method to add all six factions needed for game setup.
     * The advance command requires exactly 6 factions.
     */
    private void addAllSixFactions() throws Exception {
        // Use 6 factions (excluding BG to simplify test setup)
        String[] factions = {"Atreides", "Harkonnen", "Emperor", "Fremen", "Guild", "BT"};
        for (String factionName : factions) {
            MockUserState playerUser = guildState.createUser(factionName + "Player");
            guildState.createMember(playerUser.getUserId());

            MockChannelState gameActionsChannel = guildState.getChannels().stream()
                    .filter(ch -> ch.getChannelName().equals("game-actions"))
                    .findFirst()
                    .orElseThrow();

            SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                    .setMember(moderatorMember)
                    .setCommandName("setup")
                    .setSubcommandName("faction")
                    .addStringOption("faction", factionName)
                    .addUserOption("player", playerUser)
                    .setChannel(gameActionsChannel)
                    .build();
            commandManager.onSlashCommandInteraction(event);
        }
    }
}
