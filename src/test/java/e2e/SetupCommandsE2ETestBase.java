package e2e;

import caches.EmojiCache;
import controller.commands.CommandManager;
import model.Game;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import testutil.discord.StatefulMockFactory;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Base class for /setup command E2E tests.
 *
 * <p>Provides common test infrastructure including:
 * <ul>
 *   <li>Mock Discord server and guild setup</li>
 *   <li>Pre-configured game with roles and channels</li>
 *   <li>Helper methods for parsing game state</li>
 *   <li>Proper cleanup after each test</li>
 * </ul>
 *
 * <p>Test classes should extend this base and focus on testing specific
 * aspects of setup commands (factions, game options, roles, etc.).
 */
abstract class SetupCommandsE2ETestBase {

    protected MockDiscordServer server;
    protected MockGuildState guildState;
    protected Guild guild;
    protected CommandManager commandManager;
    protected MockRoleState modRole;
    protected MockRoleState gameRole;
    protected MockRoleState observerRole;
    protected MockUserState moderatorUser;
    protected MockMemberState moderatorMember;
    protected MockCategoryState gameCategory;
    protected MockChannelState botDataChannel;
    protected Game game;
    protected MockedStatic<MessageHistory> messageHistoryMock;

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
        moderatorUser = guildState.createUser("TestModerator");
        moderatorMember = guildState.createMember(moderatorUser.getUserId());
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

    /**
     * Parses the Game object from the bot-data channel's latest message attachment.
     *
     * @return The deserialized Game object
     * @throws IOException if parsing fails or no game data is found
     */
    protected Game parseGameFromBotData() throws IOException {
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
}
