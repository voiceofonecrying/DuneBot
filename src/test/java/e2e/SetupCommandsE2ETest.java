package e2e;

import caches.EmojiCache;
import controller.commands.CommandManager;
import enums.GameOption;
import model.Game;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testutil.discord.StatefulMockFactory;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests for the /setup slash command subcommands.
 *
 * <p>These tests verify the complete flow of setting up a game through
 * Discord slash commands, including:
 * <ul>
 *   <li>Adding factions to the game</li>
 *   <li>Managing game options (add/remove/show)</li>
 *   <li>Adding players to the game role</li>
 *   <li>Managing moderators</li>
 * </ul>
 */
@DisplayName("Setup Commands E2E Tests")
class SetupCommandsE2ETest {

    private MockDiscordServer server;
    private MockGuildState guildState;
    private Guild guild;
    private CommandManager commandManager;
    private MockRoleState modRole;
    private MockRoleState gameRole;
    private MockRoleState observerRole;
    private MockMemberState moderatorMember;
    private MockCategoryState gameCategory;
    private MockChannelState botDataChannel;
    private Game game;
    private MockedStatic<MessageHistory> messageHistoryMock;

    @BeforeEach
    void setUp() throws Exception {
        // Enable synchronous command execution for tests
        CommandManager.setRunSynchronously(true);

        // Mock MessageHistory.getHistoryFromBeginning() to avoid ClassCastException
        messageHistoryMock = mockStatic(MessageHistory.class);
        messageHistoryMock.when(() -> MessageHistory.getHistoryFromBeginning(any(TextChannel.class)))
                .thenAnswer(inv -> {
                    // Create a mock MessageHistory.MessageRetrieveAction (the actual return type)
                    MessageHistory.MessageRetrieveAction action =
                        mock(MessageHistory.MessageRetrieveAction.class);

                    // Create a mock MessageHistory with empty retrieved history
                    MessageHistory history = mock(MessageHistory.class);
                    when(history.getRetrievedHistory()).thenReturn(Collections.emptyList());

                    // complete() returns the MessageHistory
                    when(action.complete()).thenReturn(history);

                    return action;
                });

        // Create mock Discord server and guild
        server = MockDiscordServer.create();
        guildState = server.createGuild(123456789L, "Test Server");
        guild = StatefulMockFactory.mockGuild(guildState);

        // Create waiting-list channel (guild-level channel, not in any category)
        guildState.createTextChannel("waiting-list", 0L);

        // Create required roles
        modRole = guildState.createRole("Moderators");
        gameRole = guildState.createRole("Game #1");
        observerRole = guildState.createRole("Observer");
        guildState.createRole("EasyPoll");

        // Create a moderator user and member
        MockUserState modUser = guildState.createUser("TestModerator");
        moderatorMember = guildState.createMember(modUser.getUserId());
        moderatorMember.addRole(modRole.getRoleId());

        // Initialize EmojiCache with empty emoji list for this guild
        EmojiCache.setEmojis(String.valueOf(guildState.getGuildId()), Collections.emptyList());

        // Create CommandManager instance
        commandManager = new CommandManager();

        // Create a game with the new-game command first
        SlashCommandInteractionEvent newGameEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("new-game")
                .addStringOption("name", "Test Game")
                .addRoleOption("gamerole", gameRole)
                .addRoleOption("modrole", modRole)
                .build();

        try {
            commandManager.onSlashCommandInteraction(newGameEvent);
        } catch (Exception e) {
            System.err.println("Failed to create new game:");
            e.printStackTrace();
            throw e;
        }

        // Get the created game category and channels
        gameCategory = guildState.getCategories().stream()
                .filter(c -> c.getCategoryName().equals("Test Game"))
                .findFirst()
                .orElseThrow();

        botDataChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("bot-data"))
                .findFirst()
                .orElseThrow();

        // Note: For now, we don't track the turn-summary thread state separately
        // as it's created by the newGame command. Future tests could verify thread creation.

        // Parse the game state from bot-data channel
        try {
            game = parseGameFromBotData();
        } catch (Exception e) {
            System.err.println("Failed to parse game from bot-data:");
            e.printStackTrace();
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        // Reset synchronous mode
        CommandManager.setRunSynchronously(false);

        // Close the static mock to avoid memory leaks and interference with other tests
        if (messageHistoryMock != null) {
            messageHistoryMock.close();
        }
    }

    private Game parseGameFromBotData() throws IOException {
        // Parse the actual game JSON from bot-data channel
        List<MockMessageState> messages = botDataChannel.getMessages();
        if (messages.isEmpty()) {
            throw new IllegalStateException("No messages in bot-data channel");
        }

        MockMessageState latestMessage = messages.get(messages.size() - 1);
        if (latestMessage.getAttachments().isEmpty()) {
            throw new IllegalStateException("No attachments in bot-data message");
        }

        try {
            byte[] jsonData = latestMessage.getAttachments().get(0).getData().readAllBytes();
            String gameJson = new String(jsonData, StandardCharsets.UTF_8);

            // Use DiscordGame's deserializer
            com.google.gson.Gson gson = controller.DiscordGame.createGsonDeserializer();
            return gson.fromJson(gameJson, Game.class);
        } catch (Exception e) {
            throw new IOException("Failed to parse game JSON", e);
        }
    }

    @Test
    @DisplayName("Should add Atreides faction to game")
    void shouldAddAtreidesFactionToGame() throws Exception {
        // Given: A player user and member
        MockUserState playerUser = guildState.createUser("AtreidesPlayer");
        MockMemberState playerMember = guildState.createMember(playerUser.getUserId());

        // And: A setup faction command event in the game-actions channel
        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", playerUser)
                .setChannel(gameActionsChannel)
                .build();

        // When: The setup faction command is executed
        commandManager.onSlashCommandInteraction(event);

        // Then: An atreides-info channel should be created
        MockChannelState atreidesChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .findFirst()
                .orElseThrow();

        assertThat(atreidesChannel).isNotNull();
        assertThat(atreidesChannel.getCategoryId()).isEqualTo(gameCategory.getCategoryId());

        // And: Three threads should be created for the faction
        List<MockThreadChannelState> threads = guildState.getThreadsInChannel(atreidesChannel.getChannelId());
        assertThat(threads).hasSize(3);
        assertThat(threads).extracting(MockThreadChannelState::getThreadName)
                .containsExactlyInAnyOrder("notes", "chat", "ledger");
    }

    @Test
    @DisplayName("Should add multiple factions to game")
    void shouldAddMultipleFactions() throws Exception {
        // Given: Multiple player users
        MockUserState atreidesPlayer = guildState.createUser("AtreidesPlayer");
        MockMemberState atreidesMember = guildState.createMember(atreidesPlayer.getUserId());

        MockUserState harkonnenPlayer = guildState.createUser("HarkonnenPlayer");
        MockMemberState harkonnenMember = guildState.createMember(harkonnenPlayer.getUserId());

        MockUserState emperorPlayer = guildState.createUser("EmperorPlayer");
        MockMemberState emperorMember = guildState.createMember(emperorPlayer.getUserId());

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        // When: Multiple factions are added
        SlashCommandInteractionEvent atreidesEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", atreidesPlayer)
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(atreidesEvent);

        SlashCommandInteractionEvent harkonnenEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Harkonnen")
                .addUserOption("player", harkonnenPlayer)
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(harkonnenEvent);

        SlashCommandInteractionEvent emperorEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Emperor")
                .addUserOption("player", emperorPlayer)
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(emperorEvent);

        // Then: Three faction info channels should be created
        List<MockChannelState> factionChannels = guildState.getChannelsInCategory(gameCategory.getCategoryId())
                .stream()
                .filter(ch -> ch.getChannelName().endsWith("-info") && !ch.getChannelName().equals("mod-info"))
                .toList();

        assertThat(factionChannels).hasSize(3);
        assertThat(factionChannels).extracting(MockChannelState::getChannelName)
                .containsExactlyInAnyOrder("atreides-info", "harkonnen-info", "emperor-info");
    }

    @Test
    @DisplayName("Should not allow adding the same faction twice")
    void shouldNotAllowAddingSameFactionTwice() throws Exception {
        // Given: Atreides faction is already added
        MockUserState firstPlayer = guildState.createUser("FirstAtreidesPlayer");
        guildState.createMember(firstPlayer.getUserId());

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        SlashCommandInteractionEvent firstEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", firstPlayer)
                .setChannel(gameActionsChannel)
                .build();

        commandManager.onSlashCommandInteraction(firstEvent);

        // Verify first faction was added successfully
        MockChannelState atreidesChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("First Atreides faction should be added successfully"));

        // When: Trying to add Atreides faction again with a different player
        MockUserState secondPlayer = guildState.createUser("SecondAtreidesPlayer");
        guildState.createMember(secondPlayer.getUserId());

        SlashCommandInteractionEvent secondEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", secondPlayer)
                .setChannel(gameActionsChannel)
                .build();

        // Record the number of channels before attempting duplicate
        int channelCountBefore = guildState.getChannels().size();
        int atreidesInfoCountBefore = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .count();

        // Attempt to add the duplicate faction
        commandManager.onSlashCommandInteraction(secondEvent);

        // Then: No new atreides-info channel should be created
        int channelCountAfter = guildState.getChannels().size();
        int atreidesInfoCountAfter = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .count();

        assertThat(atreidesInfoCountAfter)
                .as("Should not create duplicate atreides-info channel")
                .isEqualTo(atreidesInfoCountBefore)
                .isEqualTo(1);

        // The total channel count should not increase (no new faction channels created)
        assertThat(channelCountAfter)
                .as("No new channels should be created for duplicate faction")
                .isEqualTo(channelCountBefore);

        // Verify the game state still has only one Atreides faction
        Game currentGame = parseGameFromBotData();
        long atreidesCount = currentGame.getFactions().stream()
                .filter(faction -> faction.getName().equals("Atreides"))
                .count();

        assertThat(atreidesCount)
                .as("Game should have exactly one Atreides faction")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Should not allow adding the same faction with different case")
    void shouldNotAllowAddingSameFactionWithDifferentCase() throws Exception {
        // Given: Harkonnen faction is already added with lowercase
        MockUserState firstPlayer = guildState.createUser("FirstHarkonnenPlayer");
        guildState.createMember(firstPlayer.getUserId());

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        SlashCommandInteractionEvent firstEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "harkonnen")  // lowercase
                .addUserOption("player", firstPlayer)
                .setChannel(gameActionsChannel)
                .build();

        commandManager.onSlashCommandInteraction(firstEvent);

        // Verify first faction was added successfully
        MockChannelState harkonnenChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("harkonnen-info"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("First Harkonnen faction should be added successfully"));

        // When: Trying to add Harkonnen faction again with different case
        MockUserState secondPlayer = guildState.createUser("SecondHarkonnenPlayer");
        guildState.createMember(secondPlayer.getUserId());

        SlashCommandInteractionEvent secondEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "HARKONNEN")  // uppercase
                .addUserOption("player", secondPlayer)
                .setChannel(gameActionsChannel)
                .build();

        int harkonnenInfoCountBefore = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("harkonnen-info"))
                .count();

        // Attempt to add the duplicate faction with different case
        commandManager.onSlashCommandInteraction(secondEvent);

        // Then: No new harkonnen-info channel should be created
        int harkonnenInfoCountAfter = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("harkonnen-info"))
                .count();

        assertThat(harkonnenInfoCountAfter)
                .as("Should not create duplicate harkonnen-info channel regardless of case")
                .isEqualTo(harkonnenInfoCountBefore)
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Should add, remove, and show game options")
    void shouldTestGameOptionsLifecycle() throws Exception {
        // Given: Get references to game channels
        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        MockChannelState modInfoChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("mod-info"))
                .findFirst()
                .orElseThrow();

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
                .setChannel(gameActionsChannel)
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
                .setChannel(gameActionsChannel)
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
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(addHomeworlds);

        // ========== STEP 4: Add third game option (TECH_TOKENS) ==========
        SlashCommandInteractionEvent addTechTokens = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", "TECH_TOKENS")
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(addTechTokens);

        // ========== STEP 5: Show all options - verify all three are present ==========
        SlashCommandInteractionEvent showAllOptions = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("show-game-options")
                .setChannel(gameActionsChannel)
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
                .setChannel(gameActionsChannel)
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
                .setChannel(gameActionsChannel)
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
                .setChannel(gameActionsChannel)
                .build();
        commandManager.onSlashCommandInteraction(addDiscovery);

        // ========== STEP 9: Final show to verify end state ==========
        SlashCommandInteractionEvent showFinal = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("show-game-options")
                .setChannel(gameActionsChannel)
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


    @Test
    @DisplayName("Should add player to game role")
    void shouldAddPlayerToGameRole() throws Exception {
        // Given: A new player user
        MockUserState newPlayer = guildState.createUser("NewPlayer");
        MockMemberState newMember = guildState.createMember(newPlayer.getUserId());

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

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
                .setChannel(gameActionsChannel)
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

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

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
                .setChannel(gameActionsChannel)
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
    @DisplayName("Should handle all major faction types")
    void shouldHandleAllMajorFactionTypes() throws Exception {
        // Test adding all the major factions
        List<String> factions = List.of("Atreides", "Harkonnen", "Emperor", "Fremen", "Guild", "BG");

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        for (String faction : factions) {
            MockUserState player = guildState.createUser(faction + "Player");
            guildState.createMember(player.getUserId());

            SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                    .setMember(moderatorMember)
                    .setCommandName("setup")
                    .setSubcommandName("faction")
                    .addStringOption("faction", faction)
                    .addUserOption("player", player)
                    .setChannel(gameActionsChannel)
                    .build();

            commandManager.onSlashCommandInteraction(event);
        }

        // Verify all faction channels were created
        List<MockChannelState> factionChannels = guildState.getChannelsInCategory(gameCategory.getCategoryId())
                .stream()
                .filter(ch -> ch.getChannelName().endsWith("-info") && !ch.getChannelName().equals("mod-info"))
                .toList();

        assertThat(factionChannels).hasSize(6);
    }

    @Test
    @DisplayName("Should handle all expansion faction types")
    void shouldHandleAllExpansionFactionTypes() throws Exception {
        // Test adding all the expansion factions
        List<String> expansionFactions = List.of("Ix", "BT", "CHOAM", "Richese", "Moritani", "Ecaz");

        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow();

        // Get initial channel count to compare later
        int initialChannelCount = guildState.getChannelsInCategory(gameCategory.getCategoryId()).size();

        for (String faction : expansionFactions) {
            MockUserState player = guildState.createUser(faction + "Player");
            guildState.createMember(player.getUserId());

            SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                    .setMember(moderatorMember)
                    .setCommandName("setup")
                    .setSubcommandName("faction")
                    .addStringOption("faction", faction)
                    .addUserOption("player", player)
                    .setChannel(gameActionsChannel)
                    .build();

            commandManager.onSlashCommandInteraction(event);
        }

        // Verify all expansion faction channels were created
        List<MockChannelState> factionChannels = guildState.getChannelsInCategory(gameCategory.getCategoryId())
                .stream()
                .filter(ch -> ch.getChannelName().endsWith("-info"))
                .filter(ch -> !ch.getChannelName().equals("mod-info"))
                .filter(ch -> {
                    String channelName = ch.getChannelName();
                    // Check if it's one of the expansion faction channels
                    return channelName.equals("ix-info") ||
                           channelName.equals("bt-info") ||
                           channelName.equals("choam-info") ||
                           channelName.equals("richese-info") ||
                           channelName.equals("moritani-info") ||
                           channelName.equals("ecaz-info");
                })
                .toList();

        assertThat(factionChannels)
                .as("All 6 expansion faction info channels should be created")
                .hasSize(6);

        // Verify the faction names in the channels
        List<String> channelNames = factionChannels.stream()
                .map(MockChannelState::getChannelName)
                .toList();

        assertThat(channelNames)
                .as("Should have all expansion faction info channels")
                .containsExactlyInAnyOrder(
                        "ix-info",
                        "bt-info",
                        "choam-info",
                        "richese-info",
                        "moritani-info",
                        "ecaz-info"
                );

        // Verify game state has all the expansion factions
        Game updatedGame = parseGameFromBotData();
        assertThat(updatedGame.getFactions())
                .as("Game should have 6 expansion factions")
                .hasSize(6);

        // Verify each expansion faction type is present
        assertThat(updatedGame.getFactions().stream().map(f -> f.getName()))
                .as("Should have all expansion faction types")
                .containsExactlyInAnyOrder("Ix", "BT", "CHOAM", "Richese", "Moritani", "Ecaz");
    }
}
