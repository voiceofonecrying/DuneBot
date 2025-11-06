package e2e;

import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockChannelState;
import testutil.discord.state.MockMemberState;
import testutil.discord.state.MockThreadChannelState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for complete setup flows from start to finish.
 * Tests various combinations of factions and game options.
 */
@DisplayName("Setup Complete Flow E2E Tests")
class SetupCompleteFlowE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should progress through vanilla setup flow with 6 factions")
    void shouldProgressThroughVanillaSetupFlow() throws Exception {
        // Given: A basic 6-faction game (Atreides, Harkonnen, Emperor, Fremen, Guild, BG)
        addSixBaseFactions();

        // Refresh game state after adding factions
        Game gameAfterFactions = parseGameFromBotData();
        assertThat(gameAfterFactions.getFactions()).hasSize(6);
        assertThat(gameAfterFactions.isSetupStarted()).isFalse();

        // When: Progress through the entire setup flow
        int initialModInfoMessageCount = getModInfoChannel().getMessages().size();
        int initialGameActionsMessageCount = getGameActionsChannel().getMessages().size();

        // Step 1: CREATE_DECKS (auto-executes)
        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step CREATE_DECKS");

        // Verify decks were created with expected sizes
        Game gameAfterDecks = parseGameFromBotData();
        assertThat(gameAfterDecks.getTreacheryDeck())
                .as("Treachery deck should have cards")
                .isNotEmpty();
        assertThat(gameAfterDecks.getSpiceDeck())
                .as("Spice deck should have cards")
                .isNotEmpty();
        assertThat(gameAfterDecks.getTraitorDeck())
                .as("Traitor deck should have cards (leaders from all factions)")
                .isNotEmpty();

        // Verify each faction has their leaders
        for (model.factions.Faction faction : gameAfterDecks.getFactions()) {
            assertThat(faction.getLeaders())
                    .as(faction.getName() + " should have leader cards")
                    .isNotEmpty();
        }

        // Step 2: BG_PREDICTION (BG faction specific step)
        // Since we have BG faction, this step should happen
        MockThreadChannelState bgChat = getFactionChatThread("BG");

        executeBGPrediction("Atreides", 5);

        // Verify BG chat shows prediction confirmation
        assertMessageContains(bgChat, "You predict");

        // Verify BG prediction was stored
        Game gameAfterPrediction = parseGameFromBotData();
        model.factions.Faction bgFaction = gameAfterPrediction.getFaction("BG");
        assertThat(bgFaction).isInstanceOf(model.factions.BGFaction.class);
        model.factions.BGFaction bg = (model.factions.BGFaction) bgFaction;
        assertThat(bg.getPredictionFactionName()).as("BG prediction faction").isEqualTo("Atreides");
        assertThat(bg.getPredictionRound()).as("BG prediction turn").isEqualTo(5);

        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step FACTION_POSITIONS");

        // Step 3: FACTION_POSITIONS (auto-executes)
        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step TRAITORS");

        // Step 4: TRAITORS (auto-executes)
        // Verify traitor selection messages were sent to faction chat threads
        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step FREMEN_FORCES");

        // Check that each faction received traitor selection prompts
        // Atreides, Emperor, Fremen, Guild, and BG get selection prompts with buttons
        for (String factionName : new String[]{"Atreides", "Emperor", "Fremen", "Guild", "BG"}) {
            MockThreadChannelState factionChat = getFactionChatThread(factionName);
            assertMessageContains(factionChat, "Please select your traitor");
        }

        // Harkonnen gets all 4 traitors automatically (no selection prompt)
        // They keep all 4 and can mulligan if desired
        MockThreadChannelState harkChat = getFactionChatThread("Harkonnen");
        // Harkonnen should NOT have a traitor selection message
        boolean harkHasSelection = harkChat.getMessages().stream()
                .anyMatch(msg -> msg.getContent().toLowerCase().contains("please select your traitor"));
        assertThat(harkHasSelection).as("Harkonnen should not get traitor selection prompt").isFalse();

        // Select traitors for each faction (except Harkonnen who keeps all 4)
        for (String factionName : new String[]{"Atreides", "Emperor", "Fremen", "Guild", "BG"}) {
            MockThreadChannelState factionChat = getFactionChatThread(factionName);
            MockMemberState factionMember = getFactionMember(factionName);

            // Find the first traitor selection button and click it
            testutil.discord.state.MockMessageState traitorMessage = factionChat.getMessages().stream()
                    .filter(testutil.discord.state.MockMessageState::hasButtons)
                    .filter(msg -> msg.getContent().toLowerCase().contains("please select your traitor"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(factionName + " has no traitor selection message"));

            String firstButtonId = traitorMessage.getButtons().get(0).getComponentId();
            clickButton(firstButtonId, factionChat, factionMember);
        }

        // Verify traitor selections
        Game gameAfterTraitors = parseGameFromBotData();
        model.factions.Faction harkonnen = gameAfterTraitors.getFaction("Harkonnen");
        assertThat(harkonnen.getTraitorHand()).as("Harkonnen should have 4 traitors").hasSize(4);

        // Verify other factions received 1 traitor each (they selected one and discarded 3)
        for (String factionName : new String[]{"Atreides", "Emperor", "Fremen", "Guild", "BG"}) {
            model.factions.Faction faction = gameAfterTraitors.getFaction(factionName);
            assertThat(faction.getTraitorHand())
                    .as(factionName + " should have 1 traitor after selection")
                    .hasSize(1);
        }

        // Verify traitor deck still has cards (should have remaining leaders)
        assertThat(gameAfterTraitors.getTraitorDeck())
                .as("Traitor deck should still have cards after traitor selection")
                .isNotEmpty();

        // Step 5: FREMEN_FORCES - Use button interactions!
        MockThreadChannelState fremenChat = getFactionChatThread("Fremen");
        int fremenMessagesBefore = fremenChat.getMessages().size();

        // Verify Fremen gets prompted to place forces
        assertMessageContains(fremenChat, "Where would you like to place");

        completeFremenForcePlacement();

        // Verify placement messages were sent
        assertThat(fremenChat.getMessages().size()).isGreaterThan(fremenMessagesBefore);
        assertMessageContains(fremenChat, "Sietch Tabr");

        // Verify Fremen forces are actually placed on the board
        Game gameAfterFremenPlacement = parseGameFromBotData();
        model.Territory sietchTabr = gameAfterFremenPlacement.getTerritory("Sietch Tabr");
        int fremenTotalStrength = sietchTabr.getForces().stream()
                .filter(f -> f.getFactionName().equals("Fremen"))
                .mapToInt(model.Force::getStrength)
                .sum();
        assertThat(fremenTotalStrength).as("Fremen should have 10 forces in Sietch Tabr").isEqualTo(10);

        // Note: Fremen placement completion automatically advances to BG_FORCE step
        // No need to call executeSetupAdvance() here
        assertMessageContains(getModInfoChannel(), "Starting step BG_FORCE");

        // Step 6: BG_FORCE - Use button interactions!
        // Polar Sink is in the "other" category (not stronghold, spice-blow, or rock)

        // Verify BG gets prompted to place advisor
        assertMessageContains(bgChat, "Where would you like to place");

        completeBGForcePlacement("other", "Polar Sink");

        // Verify placement completed
        assertMessageContains(bgChat, "Initial force placement complete");

        // Verify BG advisor is actually placed on the board
        Game gameAfterBGPlacement = parseGameFromBotData();
        model.Territory polarSink = gameAfterBGPlacement.getTerritory("Polar Sink");
        int bgTotalStrength = polarSink.getForces().stream()
                .filter(f -> f.getFactionName().equals("BG"))
                .mapToInt(model.Force::getStrength)
                .sum();
        assertThat(bgTotalStrength).as("BG should have 1 force in Polar Sink").isEqualTo(1);

        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step TREACHERY_CARDS");

        // Step 7: TREACHERY_CARDS (auto-executes)
        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step STORM_SELECTION");

        // Verify each faction received treachery cards
        // Most factions get 1 card, Harkonnen gets 2
        Game gameAfterCards = parseGameFromBotData();
        for (model.factions.Faction faction : gameAfterCards.getFactions()) {
            int expectedCards = faction.getName().equals("Harkonnen") ? 2 : 1;
            assertThat(faction.getTreacheryHand())
                    .as(faction.getName() + " should have " + expectedCards + " treachery card(s)")
                    .hasSize(expectedCards);
        }

        // Verify treachery deck still has cards remaining
        assertThat(gameAfterCards.getTreacheryDeck())
                .as("Treachery deck should still have cards after initial dealing")
                .isNotEmpty();

        // Step 8: STORM_SELECTION - First and last factions submit storm dials
        // Storm dials go to first and last factions in game.getFactions() list
        Game gameBeforeStorm = parseGameFromBotData();
        String firstFactionName = gameBeforeStorm.getFactions().getFirst().getName();
        String lastFactionName = gameBeforeStorm.getFactions().getLast().getName();

        MockThreadChannelState firstFactionChat = getFactionChatThread(firstFactionName);
        MockThreadChannelState lastFactionChat = getFactionChatThread(lastFactionName);

        // Verify both first and last factions get prompted for storm dials
        assertMessageContains(firstFactionChat, "Please submit your dial for initial storm position");
        assertMessageContains(lastFactionChat, "Please submit your dial for initial storm position");

        // Submit storm dials for both factions
        executeStormDialSubmission(firstFactionName, 10);
        executeStormDialSubmission(lastFactionName, 15);

        // Verify storm position and movement are set
        Game gameAfterStorm = parseGameFromBotData();
        assertThat(gameAfterStorm.getStorm())
                .as("Storm position should be set to a valid sector (1-18)")
                .isBetween(1, 18);
        assertThat(gameAfterStorm.getStormMovement())
                .as("Storm movement should be set to a valid value (1-6)")
                .isBetween(1, 6);

        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step START_GAME");

        // Verify each faction's info channel shows correct starting spice in text mode
        // Expected starting spice values per faction
        var expectedSpice = new java.util.HashMap<String, Integer>();
        expectedSpice.put("Atreides", 10);
        expectedSpice.put("Harkonnen", 10);
        expectedSpice.put("Emperor", 10);
        expectedSpice.put("Fremen", 3);
        expectedSpice.put("Guild", 5);
        expectedSpice.put("BG", 5);

        for (String factionName : new String[]{"Atreides", "Harkonnen", "Emperor", "Fremen", "Guild", "BG"}) {
            String channelName = factionName.toLowerCase() + "-info";
            MockChannelState factionInfo = guildState.getChannels().stream()
                    .filter(ch -> ch.getChannelName().equals(channelName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(channelName + " not found"));

            // Verify text mode is being used (not graphic mode with emojis)
            // In text mode, the message contains "__Spice:__ <number>"
            assertMessageContainsSpiceValue(factionInfo, factionName, expectedSpice.get(factionName));
        }

        // Step 9: START_GAME (final step to complete setup)
        executeSetupAdvance();

        // Verify turn summary thread was created and contains game beginning message
        MockThreadChannelState turnSummaryThread = getTurnSummaryThread();
        assertThat(turnSummaryThread).as("Turn summary thread should be created").isNotNull();
        assertMessageContains(turnSummaryThread, "The game is beginning");

        // Complete the START_GAME step
        executeSetupAdvance();

        // Verify that setup messages were sent to both channels
        int finalModInfoMessageCount = getModInfoChannel().getMessages().size();
        int finalGameActionsMessageCount = getGameActionsChannel().getMessages().size();

        assertThat(finalModInfoMessageCount).as("Setup should have sent step messages to mod-info channel")
                .isGreaterThan(initialModInfoMessageCount);
        assertThat(finalGameActionsMessageCount).as("Setup should have sent game messages to game-actions channel")
                .isGreaterThan(initialGameActionsMessageCount);

        // Then: Setup should be complete
        Game finalGame = parseGameFromBotData();
        assertThat(finalGame).isNotNull();
        assertThat(finalGame.isSetupFinished()).isTrue();

        // Verify game state after setup completion
        assertThat(finalGame.getTurn())
                .as("Game should be at turn 1 after setup")
                .isEqualTo(1);
        assertThat(finalGame.getPhase())
                .as("Game should be at phase 1 (storm phase) after setup")
                .isEqualTo(1);

        // Verify key setup results
        assertThat(finalGame.getFactions()).hasSize(6);
        assertThat(finalGame.getTreacheryDeck()).isNotEmpty();
        assertThat(finalGame.getSpiceDeck()).isNotEmpty();

        // Verify storm values are still valid
        assertThat(finalGame.getStorm())
                .as("Storm position should remain valid after setup completion")
                .isBetween(1, 18);
        assertThat(finalGame.getStormMovement())
                .as("Storm movement should remain valid after setup completion")
                .isBetween(1, 6);
    }

    private void executeBGPrediction(String targetFaction, int turn) throws Exception {
        // BG predictions are done via button interactions in the faction chat thread
        MockThreadChannelState bgChat = getFactionChatThread("BG");
        MockMemberState bgMember = getFactionMember("BG");

        // Step 1: Find and click the faction prediction button
        // Faction buttons have empty labels (they show emojis), so we match by button ID pattern
        testutil.discord.state.MockMessageState factionMessage = bgChat.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getContent().contains("Which faction do you predict to win"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No message asking for faction prediction"));

        String factionButtonId = factionMessage.getButtons().stream()
                .filter(btn -> btn.getComponentId().endsWith("-" + targetFaction))
                .map(testutil.discord.state.MockButtonState::getComponentId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Button for faction " + targetFaction + " not found"));

        clickButton(factionButtonId, bgChat, bgMember);

        // Step 2: Find and click the turn prediction button by its label
        // Refresh thread to get the new message with turn buttons
        bgChat = getFactionChatThread("BG");

        String turnLabel = String.valueOf(turn);
        testutil.discord.state.MockMessageState turnMessage = bgChat.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getContent().contains("Which turn do you predict"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No message asking for turn prediction"));

        String turnButtonId = turnMessage.getButtons().stream()
                .filter(btn -> btn.getLabel().equals(turnLabel))
                .map(testutil.discord.state.MockButtonState::getComponentId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Button with label " + turnLabel + " not found"));

        clickButton(turnButtonId, bgChat, bgMember);
    }

    private void executeStormDialSubmission(String factionName, int dialValue) throws Exception {
        MockThreadChannelState factionChat = getFactionChatThread(factionName);
        MockMemberState factionMember = getFactionMember(factionName);

        // Find the storm dial message
        testutil.discord.state.MockMessageState stormMessage = factionChat.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getContent().contains("Please submit your dial for initial storm position"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No storm dial message found for " + factionName));

        // Find the button with the matching dial value label
        String dialLabel = String.valueOf(dialValue);
        String buttonId = stormMessage.getButtons().stream()
                .filter(btn -> btn.getLabel().equals(dialLabel))
                .map(testutil.discord.state.MockButtonState::getComponentId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Button with label " + dialLabel + " not found"));

        clickButton(buttonId, factionChat, factionMember);
    }
}
