package testutil.discord.state;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.StatefulMockFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the stateful mock Discord server.
 * Shows how state persists and can be verified across interactions.
 */
@DisplayName("MockDiscordServer State Tests")
class MockDiscordServerTest {

    private MockDiscordServer server;
    private MockGuildState guildState;
    private Guild guild;

    @BeforeEach
    void setUp() {
        // Create the mock Discord server
        server = MockDiscordServer.create();

        // Create a guild
        guildState = server.createGuild(12345L, "Test Guild");

        // Get a mocked Guild backed by state
        guild = StatefulMockFactory.mockGuild(guildState);
    }

    @Test
    @DisplayName("Should create guild with correct ID and name")
    void shouldCreateGuild() {
        assertThat(guild.getIdLong()).isEqualTo(12345L);
        assertThat(guild.getName()).isEqualTo("Test Guild");
    }

    @Test
    @DisplayName("Should create category and retrieve it from guild")
    void shouldCreateCategory() {
        // Create category in state
        MockCategoryState categoryState = guildState.createCategory("game-channels");

        // Verify guild returns the category
        List<Category> categories = guild.getCategories();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("game-channels");
    }

    @Test
    @DisplayName("Should create text channel in category")
    void shouldCreateTextChannel() {
        // Create category
        MockCategoryState categoryState = guildState.createCategory("game-channels");

        // Create channel in category
        MockChannelState channelState = guildState.createTextChannel("game-actions", categoryState.getCategoryId());

        // Verify channel exists in guild
        List<TextChannel> channels = guild.getTextChannels();
        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).getName()).isEqualTo("game-actions");
        assertThat(channels.get(0).getIdLong()).isEqualTo(channelState.getChannelId());
    }

    @Test
    @DisplayName("Should store messages when sendMessage is called")
    void shouldStoreMessages() {
        // Create channel
        MockCategoryState categoryState = guildState.createCategory("game-channels");
        MockChannelState channelState = guildState.createTextChannel("game-actions", categoryState.getCategoryId());
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Send messages through the mocked channel
        channel.sendMessage("First message").queue();
        channel.sendMessage("Second message").queue();
        channel.sendMessage("Third message").queue();

        // Verify messages are stored in state
        List<MockMessageState> messages = channelState.getMessages();
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getContent()).isEqualTo("First message");
        assertThat(messages.get(1).getContent()).isEqualTo("Second message");
        assertThat(messages.get(2).getContent()).isEqualTo("Third message");
    }

    @Test
    @DisplayName("Should create users and members with roles")
    void shouldCreateUsersAndMembers() {
        // Create role
        MockRoleState roleState = guildState.createRole("Player");

        // Create user
        MockUserState userState = guildState.createUser("Alice");

        // Create member (user + guild association)
        MockMemberState memberState = guildState.createMember(userState.getUserId());
        memberState.addRole(roleState.getRoleId());

        // Get mocked member
        Member member = StatefulMockFactory.mockMember(memberState, guildState);

        // Verify member has correct user and role
        assertThat(member.getUser().getName()).isEqualTo("Alice");
        assertThat(member.getRoles()).hasSize(1);
        assertThat(member.getRoles().get(0).getName()).isEqualTo("Player");
    }

    @Test
    @DisplayName("Should retrieve recent messages from channel")
    void shouldRetrieveRecentMessages() {
        // Create channel and send many messages
        MockCategoryState categoryState = guildState.createCategory("game-channels");
        MockChannelState channelState = guildState.createTextChannel("game-actions", categoryState.getCategoryId());
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        for (int i = 1; i <= 10; i++) {
            channel.sendMessage("Message " + i).queue();
        }

        // Get last 3 messages
        List<MockMessageState> recentMessages = channelState.getRecentMessages(3);
        assertThat(recentMessages).hasSize(3);
        assertThat(recentMessages.get(0).getContent()).isEqualTo("Message 8");
        assertThat(recentMessages.get(1).getContent()).isEqualTo("Message 9");
        assertThat(recentMessages.get(2).getContent()).isEqualTo("Message 10");
    }

    @Test
    @DisplayName("Should maintain state across multiple channels")
    void shouldMaintainStateAcrossChannels() {
        // Create multiple channels
        MockCategoryState categoryState = guildState.createCategory("game-channels");
        MockChannelState channel1State = guildState.createTextChannel("channel-1", categoryState.getCategoryId());
        MockChannelState channel2State = guildState.createTextChannel("channel-2", categoryState.getCategoryId());

        TextChannel channel1 = StatefulMockFactory.mockTextChannel(channel1State, guildState);
        TextChannel channel2 = StatefulMockFactory.mockTextChannel(channel2State, guildState);

        // Send messages to different channels
        channel1.sendMessage("Message in channel 1").queue();
        channel2.sendMessage("Message in channel 2").queue();
        channel1.sendMessage("Another message in channel 1").queue();

        // Verify each channel has its own message history
        assertThat(channel1State.getMessages()).hasSize(2);
        assertThat(channel2State.getMessages()).hasSize(1);
        assertThat(channel1State.getMessages().get(0).getContent()).isEqualTo("Message in channel 1");
        assertThat(channel2State.getMessages().get(0).getContent()).isEqualTo("Message in channel 2");
    }

    // ========== ID Generator Tests ==========

    @Test
    @DisplayName("nextThreadId should generate sequential thread IDs starting from 6000000")
    void nextThreadId_generatesSequentialIds() {
        long id1 = server.nextThreadId();
        long id2 = server.nextThreadId();
        long id3 = server.nextThreadId();

        assertThat(id1).isEqualTo(6000000L);
        assertThat(id2).isEqualTo(6000001L);
        assertThat(id3).isEqualTo(6000002L);
    }

    @Test
    @DisplayName("nextEmojiId should generate sequential emoji IDs starting from 7000000")
    void nextEmojiId_generatesSequentialIds() {
        long id1 = server.nextEmojiId();
        long id2 = server.nextEmojiId();
        long id3 = server.nextEmojiId();

        assertThat(id1).isEqualTo(7000000L);
        assertThat(id2).isEqualTo(7000001L);
        assertThat(id3).isEqualTo(7000002L);
    }

    @Test
    @DisplayName("nextInteractionId should generate sequential interaction IDs starting from 8000000")
    void nextInteractionId_generatesSequentialIds() {
        long id1 = server.nextInteractionId();
        long id2 = server.nextInteractionId();
        long id3 = server.nextInteractionId();

        assertThat(id1).isEqualTo(8000000L);
        assertThat(id2).isEqualTo(8000001L);
        assertThat(id3).isEqualTo(8000002L);
    }

    @Test
    @DisplayName("Thread IDs should be unique across guilds")
    void threadIds_areUniqueAcrossGuilds() {
        MockGuildState guild2 = server.createGuild(67890L, "Another Guild");

        MockThreadChannelState thread1 = guildState.createThread("thread-1", 2000001L);
        MockThreadChannelState thread2 = guild2.createThread("thread-2", 2000002L);

        assertThat(thread1.getThreadId()).isNotEqualTo(thread2.getThreadId());
    }

    @Test
    @DisplayName("Emoji IDs should be unique across guilds")
    void emojiIds_areUniqueAcrossGuilds() {
        MockGuildState guild2 = server.createGuild(67890L, "Another Guild");

        MockEmojiState emoji1 = guildState.createEmoji("emoji-1", false);
        MockEmojiState emoji2 = guild2.createEmoji("emoji-2", false);

        assertThat(emoji1.getEmojiId()).isNotEqualTo(emoji2.getEmojiId());
    }

    @Test
    @DisplayName("All ID generators should produce unique IDs in their respective ranges")
    void idGenerators_produceUniqueIdsInRanges() {
        long messageId = server.nextMessageId();
        long channelId = server.nextChannelId();
        long userId = server.nextUserId();
        long roleId = server.nextRoleId();
        long categoryId = server.nextCategoryId();
        long threadId = server.nextThreadId();
        long emojiId = server.nextEmojiId();
        long interactionId = server.nextInteractionId();

        // Verify all IDs are in their expected ranges
        assertThat(messageId).isBetween(1000000L, 1999999L);
        assertThat(channelId).isBetween(2000000L, 2999999L);
        assertThat(userId).isBetween(3000000L, 3999999L);
        assertThat(roleId).isBetween(4000000L, 4999999L);
        assertThat(categoryId).isBetween(5000000L, 5999999L);
        assertThat(threadId).isBetween(6000000L, 6999999L);
        assertThat(emojiId).isBetween(7000000L, 7999999L);
        assertThat(interactionId).isBetween(8000000L, 8999999L);

        // Verify all IDs are unique
        assertThat(messageId).isNotEqualTo(channelId);
        assertThat(messageId).isNotEqualTo(userId);
        assertThat(messageId).isNotEqualTo(roleId);
        assertThat(channelId).isNotEqualTo(userId);
    }

    @Test
    @DisplayName("Multiple servers should have independent ID sequences")
    void multipleServers_haveIndependentIdSequences() {
        MockDiscordServer server2 = MockDiscordServer.create();

        long thread1 = server.nextThreadId();
        long thread2 = server2.nextThreadId();

        assertThat(thread1).isEqualTo(6000000L);
        assertThat(thread2).isEqualTo(6000000L); // Server 2 starts from the same base

        long emoji1 = server.nextEmojiId();
        long emoji2 = server2.nextEmojiId();

        assertThat(emoji1).isEqualTo(7000000L);
        assertThat(emoji2).isEqualTo(7000000L);
    }
}
