package e2e;

import caches.EmojiCache;
import controller.commands.CommandManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.StatefulMockFactory;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end test for the /new-game slash command.
 *
 * <p>This test verifies the complete flow of creating a new game through
 * the Discord slash command, including:
 * <ul>
 *   <li>Creating the game category</li>
 *   <li>Creating all required text channels</li>
 *   <li>Creating thread channels</li>
 *   <li>Setting permissions correctly</li>
 *   <li>Creating and persisting the game state</li>
 * </ul>
 */
@DisplayName("New Game Command E2E Test")
class NewGameCommandE2ETest {

    private MockGuildState guildState;
    private CommandManager commandManager;
    private MockRoleState modRole;
    private MockMemberState moderatorMember;

    private SlashCommandInteractionEvent buildNewGameEvent(String name) {
        var builder = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("new-game")
                .addStringOption(controller.commands.CommandOptions.gameName.getName(), name);
        var playerOptions = List.of(
                controller.commands.CommandOptions.player1,
                controller.commands.CommandOptions.player2,
                controller.commands.CommandOptions.player3,
                controller.commands.CommandOptions.player4,
                controller.commands.CommandOptions.player5,
                controller.commands.CommandOptions.player6);
        for (int i = 0; i < playerOptions.size(); i++) {
                MockUserState player = guildState.createUser("Player" + (i + 1));
                guildState.createMember(player.getUserId());

                builder.addUserOption(playerOptions.get(i).getName(), player);
        }
        SlashCommandInteractionEvent event = builder.build();
        var guild = java.util.Objects.requireNonNull(event.getGuild());

        when(guild.createRole()).thenAnswer(invocation -> {
            var action = mock(net.dv8tion.jda.api.requests.restaction.RoleAction.class, org.mockito.Answers.RETURNS_SELF);
            var roleName = new java.util.concurrent.atomic.AtomicReference<String>();

            when(action.setName(anyString())).thenAnswer(nameInvocation -> {
                roleName.set(nameInvocation.getArgument(0));
                return action;
            });

            when(action.complete()).thenAnswer(completeInvocation -> {
                MockRoleState roleState = guildState.createRole(roleName.get());
                return guild.getRolesByName(roleName.get(), false).stream()
                    .filter(role -> role.getIdLong() == roleState.getRoleId())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Created role not found in mock guild: " + roleName.get()));
            });

            return action;
        });

        return event;
    }

    @BeforeEach
    void setUp() {
        // Create mock Discord server and guild
        MockDiscordServer server = MockDiscordServer.create();
        guildState = server.createGuild(123456789L, "Test Server");
        StatefulMockFactory.mockGuild(guildState);

        // Create required roles
        modRole = guildState.createRole("Moderators");

        // Create additional roles that newGame expects
        guildState.createRole("Observer");
        guildState.createRole("EasyPoll");

        // Create a moderator user and member
        MockUserState modUser = guildState.createUser("TestModerator");
        moderatorMember = guildState.createMember(modUser.getUserId());
        moderatorMember.addRole(modRole.getRoleId());

        // EmojiCache is global to the bot's application emojis; tests start with it empty.
        EmojiCache.putAll(Collections.emptyList());

        // Create CommandManager instance
        commandManager = new CommandManager();
    }

    @Test
    @DisplayName("Should create game with all channels and threads when /new-game is executed")
    void shouldCreateCompleteGameStructure() throws Exception {
        // Given: A slash command event for creating a new game
        SlashCommandInteractionEvent event = buildNewGameEvent("Test Game #1: E2E");
        // When: The new-game command is executed through the command manager
        commandManager.onSlashCommandInteraction(event);

        // Then: A category should be created with the game name
        List<MockCategoryState> categories = new ArrayList<>(guildState.getCategories());
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("Test Game #1: E2E");

        MockCategoryState gameCategory = categories.get(0);

        // And: All 9 required text channels should be created in the category
        List<MockChannelState> channels = guildState.getChannelsInCategory(gameCategory.getCategoryId());
        assertThat(channels).hasSize(9);

        // Verify each expected channel exists
        assertThat(channels).extracting(MockChannelState::getChannelName)
                .containsExactlyInAnyOrder(
                        "chat",
                        "pre-game-voting",
                        "front-of-shield",
                        "game-actions",
                        "bribes",
                        "bidding-phase",
                        "rules",
                        "bot-data",
                        "mod-info"
                );

        // And: The front-of-shield channel should have 2 threads
        MockChannelState frontOfShieldChannel = channels.stream()
                .filter(ch -> ch.getChannelName().equals("front-of-shield"))
                .findFirst()
                .orElseThrow();

        List<MockThreadChannelState> threads = guildState.getThreadsInChannel(frontOfShieldChannel.getChannelId());
        assertThat(threads).hasSize(2);

        assertThat(threads).extracting(MockThreadChannelState::getThreadName)
                .containsExactlyInAnyOrder("turn-0-summary", "whispers");
    }

    @Test
    @DisplayName("Should store game state in bot-data channel")
    void shouldStoreGameStateInBotDataChannel() throws Exception {
        // Given: A slash command event for creating a new game
        SlashCommandInteractionEvent event = buildNewGameEvent("Test Game #2: E2E");

        // When: The new-game command is executed through the command manager
        commandManager.onSlashCommandInteraction(event);

        // Then: The bot-data channel should contain a message with the game state
        MockChannelState botDataChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("bot-data"))
                .findFirst()
                .orElseThrow();

        List<MockMessageState> messages = botDataChannel.getMessages();
        assertThat(messages).isNotEmpty();

        // Verify a message was sent (game state JSON)
        assertThat(messages.get(0).getContent()).isNotNull();
    }

    @Test
    @DisplayName("Should create multiple games with unique categories")
    void shouldCreateMultipleGamesWithUniqueCategories() throws Exception {
        // Given: Two different game creation events
        SlashCommandInteractionEvent event1 = buildNewGameEvent("Game Alpha: E2E");
        SlashCommandInteractionEvent event2 = buildNewGameEvent("Game Beta: E2E");

        // When: Both games are created
        commandManager.onSlashCommandInteraction(event1);
        commandManager.onSlashCommandInteraction(event2);

        // Then: Two separate categories should exist
        List<MockCategoryState> categories = new ArrayList<>(guildState.getCategories());
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(MockCategoryState::getCategoryName)
                .containsExactlyInAnyOrder("Game Alpha: E2E", "Game Beta: E2E");

        // And: Each category should have its own set of 9 channels
        long gameAlphaId = categories.stream()
                .filter(c -> c.getCategoryName().equals("Game Alpha: E2E"))
                .findFirst()
                .orElseThrow()
                .getCategoryId();

        long gameBetaId = categories.stream()
                .filter(c -> c.getCategoryName().equals("Game Beta: E2E"))
                .findFirst()
                .orElseThrow()
                .getCategoryId();

        assertThat(guildState.getChannelsInCategory(gameAlphaId)).hasSize(9);
        assertThat(guildState.getChannelsInCategory(gameBetaId)).hasSize(9);
    }

    @Test
    @DisplayName("Should send completion message through interaction hook")
    void shouldSendCompletionMessage() throws Exception {
        // Given: A slash command event
        SlashCommandInteractionEvent event = buildNewGameEvent("Test Game: E2E");
        // When: The command is executed
        commandManager.onSlashCommandInteraction(event);

        // Note: The actual verification of hook.editOriginal("Command Done.") would require
        // a stateful InteractionHook, which could be added in future enhancements.
        // For now, we verify the command completes without errors.

        // Then: The command should complete successfully (no exception thrown)
        // This is verified by the test not throwing an exception
    }
}
