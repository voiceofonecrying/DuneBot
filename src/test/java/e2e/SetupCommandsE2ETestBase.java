package e2e;

import caches.EmojiCache;
import controller.buttons.ButtonManager;
import controller.commands.CommandManager;
import model.Game;
import utils.CardImages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import testutil.discord.StatefulMockFactory;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockChannelState;
import testutil.discord.state.MockCategoryState;
import testutil.discord.state.MockDiscordServer;
import testutil.discord.state.MockGuildState;
import testutil.discord.state.MockMemberState;
import testutil.discord.state.MockMessageState;
import testutil.discord.state.MockRoleState;
import testutil.discord.state.MockThreadChannelState;
import testutil.discord.state.MockUserState;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
    private static final Logger logger = LoggerFactory.getLogger(SetupCommandsE2ETestBase.class);

    protected MockDiscordServer server;
    protected MockGuildState guildState;
    protected CommandManager commandManager;
    protected ButtonManager buttonManager;
    protected MockRoleState modRole;
    protected MockRoleState gameRole;
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
        ButtonManager.setRunSynchronously(true);

        // Clear CardImages static cache to prevent stale data across tests
        clearCardImagesCache();

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

        // Mock MessageHistory.getHistoryAfter() for CardImages lookups
        messageHistoryMock.when(() -> MessageHistory.getHistoryAfter(any(), anyString()))
                .thenAnswer(inv -> {
                    Object channelObj = inv.getArgument(0);
                    String afterId = inv.getArgument(1);

                    // Create a mock MessageHistory.MessageRetrieveAction
                    MessageHistory.MessageRetrieveAction action =
                        mock(MessageHistory.MessageRetrieveAction.class);

                    // Create a mock MessageHistory
                    MessageHistory history = mock(MessageHistory.class);

                    // Find messages from the channel state
                    List<Message> messages = new ArrayList<>();
                    if (channelObj instanceof TextChannel channel) {
                        // Try to find the MockChannelState for this channel
                        MockChannelState channelState = guildState.getChannels().stream()
                                .filter(ch -> ch.getChannelId() == channel.getIdLong())
                                .findFirst()
                                .orElse(null);

                        if (channelState != null) {
                            long afterIdLong = Long.parseLong(afterId);
                            // Get messages with ID > afterId
                            for (MockMessageState msgState : channelState.getMessages()) {
                                if (msgState.getMessageId() > afterIdLong) {
                                    Message msg = mock(Message.class);
                                    when(msg.getContentRaw()).thenReturn(msgState.getContent());
                                    when(msg.getIdLong()).thenReturn(msgState.getMessageId());
                                    when(msg.getId()).thenReturn(String.valueOf(msgState.getMessageId()));

                                    // Mock attachments with valid URLs and proxy for download
                                    List<Message.Attachment> attachments = new ArrayList<>();
                                    for (FileUpload fileUpload : msgState.getAttachments()) {
                                        Message.Attachment attachment = mock(Message.Attachment.class);
                                        String fileName = fileUpload.getName();
                                        when(attachment.getUrl()).thenReturn("https://mock-discord-attachment.local/" + fileName);
                                        when(attachment.getFileName()).thenReturn(fileName);

                                        // Mock the proxy for file download (needed by CardImages.getCardImage)
                                        net.dv8tion.jda.api.utils.NamedAttachmentProxy proxy =
                                                mock(net.dv8tion.jda.api.utils.NamedAttachmentProxy.class);
                                        byte[] mockImageData = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
                                        java.util.concurrent.CompletableFuture<java.io.InputStream> future =
                                                java.util.concurrent.CompletableFuture.completedFuture(
                                                        new java.io.ByteArrayInputStream(mockImageData));
                                        when(proxy.download()).thenReturn(future);
                                        when(attachment.getProxy()).thenReturn(proxy);

                                        attachments.add(attachment);
                                    }
                                    when(msg.getAttachments()).thenReturn(attachments);

                                    // Mock getJumpUrl() to return a valid Discord message URL
                                    String jumpUrl = "https://discord.com/channels/" +
                                            guildState.getGuildId() + "/" +
                                            channelState.getChannelId() + "/" +
                                            msgState.getMessageId();
                                    when(msg.getJumpUrl()).thenReturn(jumpUrl);

                                    messages.add(msg);
                                }
                            }
                        }
                    }

                    when(history.getRetrievedHistory()).thenReturn(messages);
                    when(history.isEmpty()).thenReturn(messages.isEmpty());
                    when(action.complete()).thenReturn(history);

                    return action;
                });

        // Create mock Discord server and guild
        server = MockDiscordServer.create();
        guildState = server.createGuild(123456789L, "Test Server");
        StatefulMockFactory.mockGuild(guildState);

        // Create waiting-list channel (guild-level channel, not in any category)
        guildState.createTextChannel("waiting-list", 0L);

        // Create Game Resources category (needed for card images when drawing the board)
        guildState.createCategory("Game Resources");

        // Create required roles
        modRole = guildState.createRole("Moderators");
        gameRole = guildState.createRole("Game #1");
        guildState.createRole("Observer");
        guildState.createRole("EasyPoll");

        // Create a moderator user and member
        moderatorUser = guildState.createUser("TestModerator");
        moderatorMember = guildState.createMember(moderatorUser.getUserId());
        moderatorMember.addRole(modRole.getRoleId());

        // Initialize EmojiCache with mock emojis for this guild
        setupMockEmojis();

        // Create CommandManager instance
        commandManager = new CommandManager();

        // Create ButtonManager instance and enable mod button press for tests
        buttonManager = new ButtonManager();
        ButtonManager.setAllowModButtonPress();

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
            logger.error("Failed to create new game during test setup", e);
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

        // Create turn-summary thread in game-actions channel
        // This thread is used during the START_GAME step to post game beginning messages
        MockChannelState gameActionsChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("game-actions channel not found"));
        guildState.createThread("turn-summary", gameActionsChannel.getChannelId());

        // Parse the game state from bot-data channel
        try {
            game = parseGameFromBotData();
        } catch (Exception e) {
            logger.error("Failed to parse game from bot-data during test setup", e);
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        // Reset synchronous mode
        CommandManager.setRunSynchronously(false);
        ButtonManager.setRunSynchronously(false);

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
        // Refresh bot-data channel to get latest messages (channel references can become stale)
        MockChannelState freshBotData = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("bot-data"))
                .findFirst()
                .orElse(botDataChannel); // Fallback to original if not found

        // Parse the actual game JSON from bot-data channel
        List<MockMessageState> messages = freshBotData.getMessages();
        if (messages.isEmpty()) {
            throw new IllegalStateException("No messages in bot-data channel");
        }

        // Find the most recent message with a valid game JSON attachment
        // Button press logs have attachments but with empty/invalid data in the mock environment
        // We try parsing from the most recent message backwards until we find valid JSON
        for (int i = messages.size() - 1; i >= 0; i--) {
            MockMessageState msg = messages.get(i);
            if (msg.getAttachments().isEmpty()) {
                continue;
            }

            try {
                java.io.InputStream is = msg.getAttachments().get(0).getData();
                // Reset the stream to beginning in case it was previously read
                try {
                    is.reset();
                } catch (IOException ignored) {
                    // Stream may not support reset, try reading anyway
                }
                byte[] jsonData = is.readAllBytes();
                if (jsonData.length == 0) {
                    continue; // Skip empty attachments (button press logs)
                }

                String gameJson = new String(jsonData, StandardCharsets.UTF_8);

                // Use DiscordGame's deserializer
                com.google.gson.Gson gson = controller.DiscordGame.createGsonDeserializer();
                Game game = gson.fromJson(gameJson, Game.class);

                if (game != null) {
                    // Add back-references from factions to game (not serialized)
                    controller.DiscordGame.addGameReferenceToFactions(game);
                    return game; // Successfully parsed
                }
                // If gson returned null, try next message
            } catch (Exception e) {
                // Failed to parse this message, try next
            }
        }

        throw new IllegalStateException("No messages with valid game JSON found in bot-data channel");
    }

    /**
     * Set up mock faction emojis for the test guild.
     * These emojis are used by the bot for faction identification in messages and buttons.
     * Creates properly formatted emoji strings that JDA's Emoji.fromFormatted() can parse.
     */
    protected void setupMockEmojis() {
        List<net.dv8tion.jda.api.entities.emoji.RichCustomEmoji> mockEmojis = new ArrayList<>();

        // Common faction emoji names (without colons, as stored in EmojiCache)
        String[] emojiNames = {"atreides", "harkonnen", "emperor", "fremen", "guild", "bt", "ix", "tleilaxu", "bg", "choam", "richese", "ecaz", "moritani"};

        for (int i = 0; i < emojiNames.length; i++) {
            String emojiName = emojiNames[i];
            long emojiId = 100000000L + i;

            net.dv8tion.jda.api.entities.emoji.RichCustomEmoji emoji =
                    mock(net.dv8tion.jda.api.entities.emoji.RichCustomEmoji.class);

            // Mock the methods that EmojiCache and DiscordGame use
            when(emoji.getName()).thenReturn(emojiName);
            when(emoji.getIdLong()).thenReturn(emojiId);
            when(emoji.getAsMention()).thenReturn("<:" + emojiName + ":" + emojiId + ">");
            // CRITICAL: getFormatted() must return properly formatted string for Emoji.fromFormatted() to work
            when(emoji.getFormatted()).thenReturn("<:" + emojiName + ":" + emojiId + ">");

            mockEmojis.add(emoji);
        }

        // Set the emojis in EmojiCache so DiscordGame can find them
        EmojiCache.setEmojis(String.valueOf(guildState.getGuildId()), mockEmojis);
    }

    // ========== Helper Methods for Tests ==========

    /**
     * Gets the game-actions channel from the current game.
     *
     * @return The game-actions channel state
     */
    protected MockChannelState getGameActionsChannel() {
        return guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("game-actions"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("game-actions channel not found"));
    }

    /**
     * Gets the mod-info channel from the current game.
     *
     * @return The mod-info channel state
     */
    protected MockChannelState getModInfoChannel() {
        return guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("mod-info"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("mod-info channel not found"));
    }

    /**
     * Adds a faction to the game.
     *
     * <p>Creates a player user, member, and executes the /setup faction command
     * to add the specified faction to the game.
     *
     * @param factionName The name of the faction to add (e.g., "Atreides", "Harkonnen")
     * @throws Exception if the command fails
     */
    protected void addFaction(String factionName) throws Exception {
        MockUserState playerUser = guildState.createUser(factionName + "Player");
        guildState.createMember(playerUser.getUserId());

        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", factionName)
                .addUserOption("player", playerUser)
                .setChannel(getGameActionsChannel())
                .build();

        commandManager.onSlashCommandInteraction(event);
    }

    /**
     * Adds multiple factions to the game.
     *
     * @param factionNames The names of the factions to add
     * @throws Exception if any command fails
     */
    protected void addFactions(String... factionNames) throws Exception {
        for (String factionName : factionNames) {
            addFaction(factionName);
        }
    }

    /**
     * Adds six base factions to the game for testing setup advance.
     * Uses: Atreides, Harkonnen, Emperor, Fremen, Guild, BG
     *
     * @throws Exception if any command fails
     */
    protected void addSixBaseFactions() throws Exception {
        addFactions("Atreides", "Harkonnen", "Emperor", "Fremen", "Guild", "BG");
    }

    /**
     * Executes the /setup advance command to progress through setup steps.
     *
     * @throws Exception if the command fails
     */
    protected void executeSetupAdvance() throws Exception {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("advance")
                .setChannel(getGameActionsChannel())
                .build();

        commandManager.onSlashCommandInteraction(event);
    }

    /**
     * Gets the turn-summary thread from the game-actions channel.
     * Note: The turn-summary thread is created under game-actions, not a separate channel.
     *
     * @return The turn-summary thread state, or null if not found
     */
    protected MockThreadChannelState getTurnSummaryThread() {
        // Find the front-of-shield channel (where turn summary threads are created)
        MockChannelState frontOfShieldChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("front-of-shield"))
                .findFirst()
                .orElse(null);

        if (frontOfShieldChannel == null) {
            return null;
        }

        // Get the turn-0-summary thread from this channel (setup happens at turn 0)
        return guildState.getThreadsInChannel(frontOfShieldChannel.getChannelId()).stream()
                .filter(thread -> thread.getThreadName().equals("turn-0-summary"))
                .findFirst()
                .orElse(null);
    }

    // ========== Button Interaction Helpers ==========

    /**
     * Simulates clicking a button in a thread.
     *
     * @param buttonId The button component ID to click
     * @param thread The thread containing the button
     * @param member The member clicking the button
     * @throws Exception if button interaction fails
     */
    protected void clickButton(String buttonId, MockThreadChannelState thread, testutil.discord.state.MockMemberState member) throws Exception {
        // Refresh the thread state to get latest messages
        // The thread parameter might be stale if messages were added since it was retrieved
        MockThreadChannelState freshThread = guildState.getThreadsInChannel(thread.getParentChannelId()).stream()
                .filter(t -> t.getThreadId() == thread.getThreadId())
                .findFirst()
                .orElse(thread); // Fallback to original if not found

        // Find the message containing this button
        testutil.discord.state.MockMessageState message = freshThread.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getButtons().stream()
                        .anyMatch(btn -> btn.getComponentId().equals(buttonId)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Button not found: " + buttonId + " in thread " + freshThread.getThreadName()));

        // Create button event
        net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent buttonEvent =
                new testutil.discord.builders.MockButtonEventBuilder(guildState)
                        .setMember(member)
                        .setButtonId(buttonId)
                        .setChannel(freshThread)
                        .setMessage(message)
                        .build();

        // Execute button interaction
        buttonManager.onButtonInteraction(buttonEvent);
    }

    /**
     * Gets a faction's chat thread.
     *
     * @param factionName The faction name (e.g., "Fremen", "BG")
     * @return The faction's chat thread
     */
    protected MockThreadChannelState getFactionChatThread(String factionName) {
        String factionPrefix = factionName.toLowerCase();
        MockChannelState factionChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals(factionPrefix + "-info"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(factionName + " info channel not found"));

        return guildState.getThreadsInChannel(factionChannel.getChannelId()).stream()
                .filter(thread -> thread.getThreadName().equals("chat"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(factionName + " chat thread not found"));
    }

    /**
     * Gets the member state for a faction's player.
     *
     * @param factionName The faction name
     * @return The member state for the faction's player
     */
    protected testutil.discord.state.MockMemberState getFactionMember(String factionName) {
        // Case-insensitive search since faction names may be uppercase (e.g., "CHOAM")
        // but players are created with capitalized names (e.g., "ChoamPlayer")
        testutil.discord.state.MockUserState user = guildState.getUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(factionName + "Player"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(factionName + " player not found"));

        return guildState.getMember(user.getUserId());
    }

    /**
     * Places Fremen forces in a single territory (part of multi-step placement).
     * This simulates one placement iteration - Fremen can place multiple times until reaching 10 total.
     *
     * @param territory The territory to place in (e.g., "Sietch Tabr", "False Wall South")
     * @param regularForces Number of regular Fremen troops (0-10)
     * @param fedaykin Number of Fremen Fedaykin (0-10)
     * @throws Exception if placement fails
     */
    protected void placeFremenForces(String territory, int regularForces, int fedaykin) throws Exception {
        MockThreadChannelState fremenChat = getFactionChatThread("Fremen");
        testutil.discord.state.MockMemberState fremenMember = getFactionMember("Fremen");

        // Step 1: Click territory button (e.g., "ship-starting-forces-Sietch Tabr")
        String territoryButtonId = "ship-starting-forces-" + territory;
        clickButton(territoryButtonId, fremenChat, fremenMember);

        // Step 2: Add regular forces
        if (regularForces > 0) {
            String addForcesButtonId = "add-force-shipment-starting-forces-" + regularForces;
            clickButton(addForcesButtonId, fremenChat, fremenMember);
        }

        // Step 3: Add fedaykin
        if (fedaykin > 0) {
            String addFedaykinButtonId = "add-special-force-shipment-starting-forces-" + fedaykin;
            clickButton(addFedaykinButtonId, fremenChat, fremenMember);
        }

        // Step 4: Execute placement
        clickButton("execute-shipment-starting-forces", fremenChat, fremenMember);

        // Note: If total placed < 10, Fremen will get new territory buttons for next placement
        // If total = 10, step completes and setup advances
    }

    /**
     * Completes Fremen starting forces placement by placing in 2 territories.
     * Places 7 forces in Sietch Tabr and 3 forces in False Wall South.
     *
     * @throws Exception if placement fails
     */
    protected void completeFremenForcePlacement() throws Exception {
        placeFremenForces("Sietch Tabr", 9, 1);      // 10 total (all in one territory)
    }

    /**
     * Completes BG starting advisor/fighter placement.
     * BG follows a category-based selection flow:
     * 1. Select category (stronghold, spice-blow, rock, or other)
     * 2. Select specific territory from that category
     * 3. Execute placement (BG auto-sets force count to 1)
     * Note: Polar Sink is in the "other" category since it's not a stronghold, spice blow territory, or rock territory.
     *
     * @param category The category button to click (e.g., "stronghold", "spice-blow", "rock", "other")
     * @param territory The specific territory to place in (e.g., "Polar Sink")
     * @throws Exception if placement fails
     */
    protected void completeBGForcePlacement(String category, String territory) throws Exception {
        MockThreadChannelState bgChat = getFactionChatThread("BG");
        testutil.discord.state.MockMemberState bgMember = getFactionMember("BG");

        // Step 1: Click category button (e.g., stronghold-starting-forces)
        String categoryButtonId = category + "-starting-forces";
        clickButton(categoryButtonId, bgChat, bgMember);

        // Step 2: Click specific territory button
        String territoryButtonId = "ship-starting-forces-" + territory;
        clickButton(territoryButtonId, bgChat, bgMember);

        // Step 3: Execute placement (BG auto-sets force count to 1)
        clickButton("execute-shipment-starting-forces", bgChat, bgMember);
    }

    /**
     * Asserts that at least one message in the channel contains the specified text.
     * Useful for verifying that expected messages were sent during setup.
     *
     * @param channel The channel to search for messages
     * @param expectedText The text that should appear in at least one message
     */
    protected void assertMessageContains(MockChannelState channel, String expectedText) {
        boolean found = channel.getMessages().stream()
                .anyMatch(msg -> msg.getContent().toLowerCase().contains(expectedText.toLowerCase()));

        if (!found) {
            // Build helpful error message showing what messages were actually sent
            String messageList = channel.getMessages().stream()
                    .map(msg -> "  - " + msg.getContent().substring(0, Math.min(200, msg.getContent().length())))
                    .collect(java.util.stream.Collectors.joining("\n"));

            org.assertj.core.api.Assertions.fail(
                "Expected to find message containing '%s' in channel '%s', but it was not found.\nMessages in channel:\n%s",
                expectedText, channel.getChannelName(), messageList
            );
        }
    }

    /**
     * Asserts that at least one message in the thread contains the specified text.
     * Useful for verifying that expected messages were sent to faction threads during setup.
     *
     * @param thread The thread to search for messages
     * @param expectedText The text that should appear in at least one message
     */
    protected void assertMessageContains(MockThreadChannelState thread, String expectedText) {
        boolean found = thread.getMessages().stream()
                .anyMatch(msg -> msg.getContent().toLowerCase().contains(expectedText.toLowerCase()));

        if (!found) {
            // Build helpful error message showing what messages were actually sent
            String messageList = thread.getMessages().stream()
                    .map(msg -> "  - " + msg.getContent().substring(0, Math.min(100, msg.getContent().length())))
                    .collect(java.util.stream.Collectors.joining("\n"));

            org.assertj.core.api.Assertions.fail(
                "Expected to find message containing '%s' in thread '%s', but it was not found.\nMessages in thread:\n%s",
                expectedText, thread.getThreadName(), messageList
            );
        }
    }

    /**
     * Asserts that a faction info channel contains a message with the correct spice value in text mode.
     * This verifies:
     * 1. The channel is using text mode (contains "__Spice:__ " format, not emoji/graphic mode)
     * 2. The numeric spice value matches the expected value
     *
     * @param channel The faction info channel to check
     * @param factionName The faction name (for error messages)
     * @param expectedSpice The expected spice value
     */
    protected void assertMessageContainsSpiceValue(MockChannelState channel, String factionName, int expectedSpice) {
        // Find a message containing "__Spice:__ " (text mode format)
        java.util.Optional<MockMessageState> spiceMessage = channel.getMessages().stream()
                .filter(msg -> msg.getContent().contains("__Spice:__ "))
                .findFirst();

        if (spiceMessage.isEmpty()) {
            // Build helpful error message showing what messages were actually sent
            String messageList = channel.getMessages().stream()
                    .map(msg -> "  - " + msg.getContent().substring(0, Math.min(200, msg.getContent().length())))
                    .collect(java.util.stream.Collectors.joining("\n"));

            org.assertj.core.api.Assertions.fail(
                "Expected to find text mode spice message (containing '__Spice:__ ') in %s-info channel, but it was not found.\n" +
                "This indicates the channel may be in graphic mode instead of text mode.\nMessages in channel:\n%s",
                factionName, messageList
            );
        }

        // Extract the spice value from the message
        // Format is: "__Spice:__ <number>" possibly followed by more text
        String content = spiceMessage.orElseThrow().getContent();
        int spiceIndex = content.indexOf("__Spice:__ ");
        if (spiceIndex == -1) {
            org.assertj.core.api.Assertions.fail("Found message but couldn't locate '__Spice:__ ' marker");
        }

        // Extract the substring starting after "__Spice:__ "
        String afterSpiceMarker = content.substring(spiceIndex + "__Spice:__ ".length());

        // Find the first non-digit character to determine where the number ends
        int endIndex = 0;
        while (endIndex < afterSpiceMarker.length() && Character.isDigit(afterSpiceMarker.charAt(endIndex))) {
            endIndex++;
        }

        if (endIndex == 0) {
            org.assertj.core.api.Assertions.fail(
                "Could not parse spice value from message in %s-info channel.\nMessage content: %s",
                factionName, content
            );
        }

        String spiceValueStr = afterSpiceMarker.substring(0, endIndex);
        int actualSpice = Integer.parseInt(spiceValueStr);

        // Verify the actual spice matches expected
        org.assertj.core.api.Assertions.assertThat(actualSpice)
                .as("Spice value for %s in text mode", factionName)
                .isEqualTo(expectedSpice);
    }

    // ========== Common Setup Flow Helpers ==========

    /**
     * Executes BG prediction via button interactions in the BG faction chat thread.
     * Clicks the faction prediction button and then the turn prediction button.
     *
     * @param targetFaction The faction BG predicts will win (e.g., "Atreides")
     * @param turn The turn BG predicts the faction will win on (1-10)
     * @throws Exception if button interaction fails
     */
    protected void executeBGPrediction(String targetFaction, int turn) throws Exception {
        MockThreadChannelState bgChat = getFactionChatThread("BG");
        testutil.discord.state.MockMemberState bgMember = getFactionMember("BG");

        // Step 1: Find and click the faction prediction button
        testutil.discord.state.MockMessageState factionMessage = bgChat.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getContent().contains("Which faction do you predict to win"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No message asking for faction prediction"));

        String factionButtonId = factionMessage.getButtons().stream()
                .map(testutil.discord.state.MockButtonState::getComponentId)
                .filter(id -> id.endsWith("-" + targetFaction))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Button for faction " + targetFaction + " not found"));

        clickButton(factionButtonId, bgChat, bgMember);

        // Step 2: Find and click the turn prediction button by its label
        bgChat = getFactionChatThread("BG"); // Refresh thread

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

    /**
     * Selects a traitor for a faction by clicking the first available traitor button.
     *
     * @param factionName The faction name to select a traitor for
     * @throws Exception if button interaction fails
     */
    protected void selectTraitorForFaction(String factionName) throws Exception {
        MockThreadChannelState chat = getFactionChatThread(factionName);
        testutil.discord.state.MockMemberState member = getFactionMember(factionName);

        testutil.discord.state.MockMessageState traitorMsg = chat.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getContent().toLowerCase().contains("select your traitor"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No traitor selection message for " + factionName));

        String firstButton = traitorMsg.getButtons().get(0).getComponentId();
        clickButton(firstButton, chat, member);
    }

    /**
     * Submits a storm dial value for a faction by clicking the button with matching label.
     *
     * @param factionName The faction submitting the dial
     * @param dialValue The dial value to submit (typically 1-20)
     * @throws Exception if button interaction fails
     */
    protected void submitStormDial(String factionName, int dialValue) throws Exception {
        MockThreadChannelState chat = getFactionChatThread(factionName);
        testutil.discord.state.MockMemberState member = getFactionMember(factionName);

        testutil.discord.state.MockMessageState stormMsg = chat.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getContent().contains("storm"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No storm dial message for " + factionName));

        String dialLabel = String.valueOf(dialValue);
        String buttonId = stormMsg.getButtons().stream()
                .filter(btn -> btn.getLabel().equals(dialLabel))
                .findFirst()
                .map(testutil.discord.state.MockButtonState::getComponentId)
                .orElseThrow(() -> new IllegalStateException(
                        "No button with label " + dialLabel + " for storm dial"));

        clickButton(buttonId, chat, member);
    }

    /**
     * Finds a message in a thread that has buttons matching a pattern.
     *
     * @param thread The thread to search
     * @param buttonIdPattern The pattern to match in button component IDs
     * @return The message state, or null if not found
     */
    protected testutil.discord.state.MockMessageState findMessageWithButtonPattern(
            MockThreadChannelState thread, String buttonIdPattern) {
        return thread.getMessages().stream()
                .filter(testutil.discord.state.MockMessageState::hasButtons)
                .filter(msg -> msg.getButtons().stream()
                        .anyMatch(btn -> btn.getComponentId().contains(buttonIdPattern)))
                .findFirst()
                .orElse(null);
    }

    // ========== Game Option Helpers ==========

    /**
     * Adds a game option to the current game.
     *
     * @param optionName The name of the game option (e.g., "HOMEWORLDS", "LEADER_SKILLS")
     * @throws Exception if the command fails
     */
    protected void addGameOption(String optionName) throws Exception {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("add-game-option")
                .addStringOption("add-game-option", optionName)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);
    }

    /**
     * Sets up card image channels in the Game Resources category.
     * Creates channels with mock messages containing card images for expansion factions.
     * This is required for tests that use factions with card images (like Ecaz ambassadors).
     */
    protected void setupCardImageChannels() {
        // Get Game Resources category
        MockCategoryState gameResources = guildState.getCategories().stream()
                .filter(c -> c.getCategoryName().equals("Game Resources"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Game Resources category not found"));

        // Create ecaz-ambassadors channel for Ecaz faction ambassador images
        MockChannelState ambassadorsChannel = guildState.createTextChannel(
                "ecaz-ambassadors",
                gameResources.getCategoryId()
        );

        // Add mock messages for all ambassador types
        String[] ambassadorNames = {"Ecaz", "Atreides", "BG", "CHOAM", "Emperor",
                "Fremen", "Harkonnen", "Ix", "Richese", "Guild", "BT"};

        for (String name : ambassadorNames) {
            addCardImageMessage(ambassadorsChannel, name);
        }
    }

    /**
     * Adds a mock card image message to a channel.
     * The message content is set to the card name (for pattern matching) and includes
     * a mock image attachment with a valid URL.
     *
     * @param channel The channel to add the message to
     * @param cardName The name of the card (used as message content and attachment filename)
     */
    private void addCardImageMessage(MockChannelState channel, String cardName) {
        // Create a minimal mock image data (PNG header bytes)
        byte[] mockImageData = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        FileUpload fileUpload = FileUpload.fromData(mockImageData, cardName + ".png");

        MockMessageState message = new MockMessageState(
                guildState.getServer().nextMessageId(),
                channel.getChannelId(),
                0L,  // Bot author
                cardName,  // Content must match card name for CardImages pattern matching
                Collections.singletonList(fileUpload)
        );

        channel.addMessage(message);
    }

    /**
     * Clears the static CardImages cache to prevent stale data across tests.
     * Uses reflection since the cache field is private.
     */
    private void clearCardImagesCache() {
        try {
            Field cacheField = CardImages.class.getDeclaredField("cardChannelMessages");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> cache = (Map<String, ?>) cacheField.get(null);
            cache.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Log but don't fail - cache clearing is best-effort
            logger.warn("Failed to clear CardImages cache: {}", e.getMessage());
        }
    }

    /**
     * Pre-populates the CardImages cache with mock messages for a given channel.
     * This bypasses the MessageHistory retrieval which doesn't work with our mock infrastructure.
     *
     * @param channelName The name of the channel (e.g., "homeworld-images", "leader-skills")
     * @param cardNames Array of card names to create mock messages for
     */
    protected void populateCardImagesCache(String channelName, String[] cardNames) {
        populateCardImagesCache(channelName, cardNames, true);
    }

    /**
     * Pre-populates the CardImages cache with mock messages for a given channel.
     * This bypasses the MessageHistory retrieval which doesn't work with our mock infrastructure.
     *
     * @param channelName The name of the channel (e.g., "homeworld-images", "leader-skills")
     * @param cardNames Array of card names to create mock messages for
     * @param includeAttachments Whether to include mock attachments (set to false for cases where
     *                           file upload causes issues)
     */
    protected void populateCardImagesCache(String channelName, String[] cardNames, boolean includeAttachments) {
        try {
            Field cacheField = CardImages.class.getDeclaredField("cardChannelMessages");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, List<Message>> cache = (Map<String, List<Message>>) cacheField.get(null);

            List<Message> messages = new ArrayList<>();
            for (String cardName : cardNames) {
                Message mockMessage = createMockCardMessage(cardName, includeAttachments);
                messages.add(mockMessage);
            }
            cache.put(channelName, messages);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to populate CardImages cache", e);
        }
    }

    /**
     * Creates a mock Message with a matching content and attachment URL for CardImages pattern matching.
     */
    private Message createMockCardMessage(String cardName, boolean includeAttachments) {
        Message message = mock(Message.class);
        lenient().when(message.getContentRaw()).thenReturn(cardName);
        lenient().when(message.getId()).thenReturn(String.valueOf(guildState.getServer().nextMessageId()));
        lenient().when(message.getJumpUrl()).thenReturn("https://discord.com/channels/123/456/789");

        if (includeAttachments) {
            // Create a mock attachment with a valid URL
            // URL-encode the card name to handle spaces and special characters
            String encodedCardName;
            try {
                encodedCardName = java.net.URLEncoder.encode(cardName, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedCardName = cardName.replace(" ", "_");
            }
            Message.Attachment attachment = mock(Message.Attachment.class);
            lenient().when(attachment.getUrl()).thenReturn("https://cdn.discordapp.com/attachments/123/456/" + encodedCardName + ".png");
            lenient().when(attachment.getFileName()).thenReturn(cardName + ".png");

            // Mock the proxy for file download
            net.dv8tion.jda.api.utils.NamedAttachmentProxy proxy = mock(net.dv8tion.jda.api.utils.NamedAttachmentProxy.class);
            byte[] mockImageData = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            java.util.concurrent.CompletableFuture<java.io.InputStream> future =
                    java.util.concurrent.CompletableFuture.completedFuture(new java.io.ByteArrayInputStream(mockImageData));
            lenient().when(proxy.download()).thenReturn(future);
            lenient().when(attachment.getProxy()).thenReturn(proxy);

            lenient().when(message.getAttachments()).thenReturn(List.of(attachment));
        } else {
            // Return empty attachments list - CardImages will return Optional.empty()
            lenient().when(message.getAttachments()).thenReturn(List.of());
        }

        return message;
    }
}
