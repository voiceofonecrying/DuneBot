package e2e;

import enums.GameOption;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockChannelState;
import testutil.discord.state.MockMessageState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the /setup game option subcommands.
 *
 * <p>These tests verify game option management including:
 * <ul>
 *   <li>Adding game options</li>
 *   <li>Removing game options</li>
 *   <li>Showing current game options</li>
 *   <li>Full lifecycle (add/show/remove) verification</li>
 * </ul>
 */
@DisplayName("Setup Game Options E2E Tests")
class SetupGameOptionsE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should add, remove, and show game options")
    void shouldTestGameOptionsLifecycle() throws Exception {
        // Given: Get references to game channels
        MockChannelState modInfoChannel = getModInfoChannel();

        // ========== STEP 1: Verify initial state - no game options ==========
        assertThat(game)
                .as("Game should exist after initial setup")
                .isNotNull();

        // Verify initial state has no game options
        if (game.getGameOptions() == null || game.getGameOptions().isEmpty()) {
            assertThat(game.getGameOptions())
                    .as("Initial game should have no game options")
                    .satisfiesAnyOf(
                            options -> assertThat(options).isNull(),
                            options -> assertThat(options).isEmpty()
                    );
        }

        // Show initial game options (should be empty or minimal)
        SlashCommandInteractionEvent showInitial = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("show-game-options")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(showInitial);

        int initialModInfoCount = modInfoChannel.getMessages().size();
        int initialBotDataCount = botDataChannel.getMessages().size();

        // ========== STEP 2: Add first game option (LEADER_SKILLS) ==========
        SlashCommandInteractionEvent addLeaderSkills = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "LEADER_SKILLS")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(addLeaderSkills);

        // Verify bot-data was updated
        assertThat(botDataChannel.getMessages().size())
                .as("Bot-data should be updated after adding LEADER_SKILLS")
                .isGreaterThan(initialBotDataCount);

        // ========== STEP 3: Add second game option (HOMEWORLDS) ==========
        SlashCommandInteractionEvent addHomeworlds = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "HOMEWORLDS")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(addHomeworlds);

        // ========== STEP 4: Add third game option (TECH_TOKENS) ==========
        SlashCommandInteractionEvent addTechTokens = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "TECH_TOKENS")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(addTechTokens);

        // ========== STEP 5: Show all options - verify all three are present ==========
        SlashCommandInteractionEvent showAllOptions = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("show-game-options")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(showAllOptions);

        List<MockMessageState> messagesAfterAdd = modInfoChannel.getMessages();
        assertThat(messagesAfterAdd.size())
                .as("Mod-info should have new message after show-game-options")
                .isGreaterThan(initialModInfoCount);

        MockMessageState allOptionsMessage = messagesAfterAdd.get(messagesAfterAdd.size() - 1);
        assertThat(allOptionsMessage.getContent())
                .as("All three options should be shown")
                .contains("LEADER_SKILLS", "HOMEWORLDS", "TECH_TOKENS");

        // Verify game state has all three options
        Game gameWithAllOptions = parseGameFromBotData();
        assertThat(gameWithAllOptions.getGameOptions())
                .as("Game should have all three options")
                .contains(GameOption.LEADER_SKILLS, GameOption.HOMEWORLDS, GameOption.TECH_TOKENS);

        // ========== STEP 6: Remove one option (HOMEWORLDS) ==========
        int botDataCountBeforeRemove = botDataChannel.getMessages().size();

        SlashCommandInteractionEvent removeHomeworlds = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("remove-game-option")
                .addStringOption("remove-game-option", "HOMEWORLDS")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(removeHomeworlds);

        // Verify bot-data was updated
        assertThat(botDataChannel.getMessages().size())
                .as("Bot-data should be updated after removing HOMEWORLDS")
                .isGreaterThan(botDataCountBeforeRemove);

        MockMessageState removeMessage = botDataChannel.getMessages().get(botDataChannel.getMessages().size() - 1);
        assertThat(removeMessage.getContent())
                .as("Bot-data should log the remove action")
                .contains("remove-game-option");

        // ========== STEP 7: Show options after removal - verify HOMEWORLDS is gone ==========
        SlashCommandInteractionEvent showAfterRemove = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("show-game-options")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(showAfterRemove);

        List<MockMessageState> messagesAfterRemove = modInfoChannel.getMessages();
        MockMessageState afterRemoveMessage = messagesAfterRemove.get(messagesAfterRemove.size() - 1);

        assertThat(afterRemoveMessage.getContent())
                .as("HOMEWORLDS should no longer be shown")
                .doesNotContain("HOMEWORLDS");
        assertThat(afterRemoveMessage.getContent())
                .as("LEADER_SKILLS and TECH_TOKENS should still be shown")
                .contains("LEADER_SKILLS", "TECH_TOKENS");

        // Verify game state no longer has HOMEWORLDS
        Game gameAfterRemove = parseGameFromBotData();
        assertThat(gameAfterRemove.getGameOptions())
                .as("Game should not have HOMEWORLDS option")
                .doesNotContain(GameOption.HOMEWORLDS);
        assertThat(gameAfterRemove.getGameOptions())
                .as("Game should still have LEADER_SKILLS and TECH_TOKENS")
                .contains(GameOption.LEADER_SKILLS, GameOption.TECH_TOKENS);

        // ========== STEP 8: Add another option (DISCOVERY_TOKENS) ==========
        SlashCommandInteractionEvent addDiscovery = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "DISCOVERY_TOKENS")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(addDiscovery);

        // ========== STEP 9: Final show to verify end state ==========
        SlashCommandInteractionEvent showFinal = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("show-game-options")
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(showFinal);

        MockMessageState finalMessage = modInfoChannel.getMessages().get(modInfoChannel.getMessages().size() - 1);
        assertThat(finalMessage.getContent())
                .as("Final state should have LEADER_SKILLS, TECH_TOKENS, and DISCOVERY_TOKENS but not HOMEWORLDS")
                .contains("LEADER_SKILLS", "TECH_TOKENS", "DISCOVERY_TOKENS")
                .doesNotContain("HOMEWORLDS");

        // Final game state verification
        Game finalGame = parseGameFromBotData();
        assertThat(finalGame.getGameOptions())
                .as("Final game should have exactly the expected options")
                .containsExactlyInAnyOrder(
                        GameOption.LEADER_SKILLS,
                        GameOption.TECH_TOKENS,
                        GameOption.DISCOVERY_TOKENS
                );
    }
}
