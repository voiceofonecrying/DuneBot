package e2e;

import model.Game;
import model.factions.BGFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the /setup bg-prediction subcommand.
 *
 * <p>These tests verify BG faction prediction management including:
 * <ul>
 *   <li>Setting BG prediction for faction and turn</li>
 *   <li>Updating prediction values</li>
 *   <li>Validating turn range (1-10)</li>
 * </ul>
 */
@DisplayName("Setup BG Prediction E2E Tests")
class SetupBGPredictionE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should set BG prediction for faction and turn")
    void shouldSetBGPrediction() throws Exception {
        // Given: BG faction and another faction are added to the game
        addFaction("BG");
        addFaction("Atreides");

        Game gameBefore = parseGameFromBotData();
        BGFaction bgBefore = gameBefore.getBGFaction();

        // Initially, BG should have no prediction set
        assertThat(bgBefore.getPredictionFactionName())
                .as("BG should not have a prediction faction initially")
                .isNull();
        assertThat(bgBefore.getPredictionRound())
                .as("BG should not have a prediction round initially")
                .isEqualTo(0);

        // When: Setting BG prediction for Atreides to win on turn 5
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("bg-prediction")
                .addStringOption("factionname", "Atreides")
                .addIntegerOption("turn", 5)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: BG prediction should be set
        Game gameAfter = parseGameFromBotData();
        BGFaction bgAfter = gameAfter.getBGFaction();

        assertThat(bgAfter.getPredictionFactionName())
                .as("BG should predict Atreides")
                .isEqualTo("Atreides");
        assertThat(bgAfter.getPredictionRound())
                .as("BG should predict turn 5")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("Should update existing BG prediction")
    void shouldUpdateExistingPrediction() throws Exception {
        // Given: BG faction with an existing prediction
        addFaction("BG");
        addFaction("Harkonnen");
        addFaction("Emperor");

        // Set initial prediction for Harkonnen on turn 3
        SlashCommandInteractionEvent initialEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("bg-prediction")
                .addStringOption("factionname", "Harkonnen")
                .addIntegerOption("turn", 3)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(initialEvent);

        Game gameBefore = parseGameFromBotData();
        assertThat(gameBefore.getBGFaction().getPredictionFactionName()).isEqualTo("Harkonnen");
        assertThat(gameBefore.getBGFaction().getPredictionRound()).isEqualTo(3);

        // When: Updating prediction to Emperor on turn 8
        SlashCommandInteractionEvent updateEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("bg-prediction")
                .addStringOption("factionname", "Emperor")
                .addIntegerOption("turn", 8)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(updateEvent);

        // Then: BG prediction should be updated
        Game gameAfter = parseGameFromBotData();
        BGFaction bgAfter = gameAfter.getBGFaction();

        assertThat(bgAfter.getPredictionFactionName())
                .as("BG should now predict Emperor")
                .isEqualTo("Emperor");
        assertThat(bgAfter.getPredictionRound())
                .as("BG should now predict turn 8")
                .isEqualTo(8);
    }

    @Test
    @DisplayName("Should handle prediction for turn 1")
    void shouldHandlePredictionForTurn1() throws Exception {
        // Given: BG faction and another faction
        addFaction("BG");
        addFaction("Fremen");

        // When: Setting prediction for turn 1 (minimum valid turn)
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("bg-prediction")
                .addStringOption("factionname", "Fremen")
                .addIntegerOption("turn", 1)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: Prediction should be set to turn 1
        Game gameAfter = parseGameFromBotData();
        assertThat(gameAfter.getBGFaction().getPredictionRound()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle prediction for turn 10")
    void shouldHandlePredictionForTurn10() throws Exception {
        // Given: BG faction and another faction
        addFaction("BG");
        addFaction("Guild");

        // When: Setting prediction for turn 10 (maximum valid turn)
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("bg-prediction")
                .addStringOption("factionname", "Guild")
                .addIntegerOption("turn", 10)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: Prediction should be set to turn 10
        Game gameAfter = parseGameFromBotData();
        assertThat(gameAfter.getBGFaction().getPredictionRound()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle BG predicting themselves to win")
    void shouldHandleBGPredictingThemselves() throws Exception {
        // Given: BG faction and another faction
        addFaction("BG");
        addFaction("Emperor");

        // When: BG predicts themselves to win
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("bg-prediction")
                .addStringOption("factionname", "BG")
                .addIntegerOption("turn", 6)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);

        // Then: Prediction should be set to BG
        Game gameAfter = parseGameFromBotData();
        BGFaction bgAfter = gameAfter.getBGFaction();

        assertThat(bgAfter.getPredictionFactionName())
                .as("BG should be able to predict themselves")
                .isEqualTo("BG");
        assertThat(bgAfter.getPredictionRound()).isEqualTo(6);
    }
}
