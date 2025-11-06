package testutil.discord;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.state.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StatefulMockFactory.
 *
 * Tests verify that the factory creates proper mocks backed by state,
 * and that all critical features work correctly.
 */
@DisplayName("StatefulMockFactory Tests")
class StatefulMockFactoryTest {

    private MockDiscordServer server;
    private MockGuildState guildState;

    @BeforeEach
    void setUp() {
        server = MockDiscordServer.create();
        guildState = server.createGuild(123456789L, "Test Server");
    }

    // ========== Guild Mock Tests ==========

    @Test
    @DisplayName("mockGuild() should return guild with correct ID and name")
    void mockGuild_shouldReturnGuildWithCorrectIdAndName() {
        Guild guild = StatefulMockFactory.mockGuild(guildState);

        assertThat(guild.getIdLong()).isEqualTo(123456789L);
        assertThat(guild.getId()).isEqualTo("123456789");
        assertThat(guild.getName()).isEqualTo("Test Server");
    }

    @Test
    @DisplayName("mockGuild() should return text channels from state")
    void mockGuild_shouldReturnTextChannelsFromState() {
        // Create channels in state
        guildState.createTextChannel("channel-1", 0L);
        guildState.createTextChannel("channel-2", 0L);

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<TextChannel> channels = guild.getTextChannels();

        assertThat(channels).hasSize(2);
        assertThat(channels).extracting(TextChannel::getName)
                .containsExactlyInAnyOrder("channel-1", "channel-2");
    }

    @Test
    @DisplayName("mockGuild() should return categories from state")
    void mockGuild_shouldReturnCategoriesFromState() {
        // Create categories in state
        guildState.createCategory("Category 1");
        guildState.createCategory("Category 2");

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<Category> categories = guild.getCategories();

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Category 1", "Category 2");
    }

    @Test
    @DisplayName("mockGuild() should return roles from state")
    void mockGuild_shouldReturnRolesFromState() {
        // Create roles in state
        guildState.createRole("Moderator");
        guildState.createRole("Player");

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<Role> roles = guild.getRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder("Moderator", "Player");
    }

    @Test
    @DisplayName("mockGuild() should return members from state")
    void mockGuild_shouldReturnMembersFromState() {
        // Create users and members
        MockUserState user1 = guildState.createUser("Alice");
        MockUserState user2 = guildState.createUser("Bob");
        guildState.createMember(user1.getUserId());
        guildState.createMember(user2.getUserId());

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<Member> members = guild.getMembers();

        assertThat(members).hasSize(2);
        assertThat(members).extracting(m -> m.getUser().getName())
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    @DisplayName("mockGuild() getTextChannelsByName() should find channels by exact name")
    void mockGuild_getTextChannelsByName_shouldFindChannelsByExactName() {
        guildState.createTextChannel("test-channel", 0L);
        guildState.createTextChannel("other-channel", 0L);

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<TextChannel> channels = guild.getTextChannelsByName("test-channel", false);

        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).getName()).isEqualTo("test-channel");
    }

    @Test
    @DisplayName("mockGuild() getTextChannelsByName() should find channels ignoring case")
    void mockGuild_getTextChannelsByName_shouldFindChannelsIgnoringCase() {
        guildState.createTextChannel("Test-Channel", 0L);

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<TextChannel> channels = guild.getTextChannelsByName("test-channel", true);

        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).getName()).isEqualTo("Test-Channel");
    }

    @Test
    @DisplayName("mockGuild() getCategoriesByName() should find categories")
    void mockGuild_getCategoriesByName_shouldFindCategories() {
        guildState.createCategory("Game Channels");
        guildState.createCategory("Admin Channels");

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<Category> categories = guild.getCategoriesByName("Game Channels", false);

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Game Channels");
    }

    @Test
    @DisplayName("mockGuild() getRolesByName() should find roles")
    void mockGuild_getRolesByName_shouldFindRoles() {
        guildState.createRole("Moderator");
        guildState.createRole("Player");

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        List<Role> roles = guild.getRolesByName("Moderator", false);

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getName()).isEqualTo("Moderator");
    }

    @Test
    @DisplayName("mockGuild() getMemberById() should retrieve member from state")
    void mockGuild_getMemberById_shouldRetrieveMemberFromState() {
        MockUserState user = guildState.createUser("Alice");
        MockMemberState member = guildState.createMember(user.getUserId());

        Guild guild = StatefulMockFactory.mockGuild(guildState);
        Member retrievedMember = guild.getMemberById(String.valueOf(user.getUserId()));

        assertThat(retrievedMember).isNotNull();
        assertThat(retrievedMember.getUser().getName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("mockGuild() createCategory() should create category in state")
    void mockGuild_createCategory_shouldCreateCategoryInState() {
        Guild guild = StatefulMockFactory.mockGuild(guildState);

        ChannelAction<Category> action = guild.createCategory("New Category");
        Category category = action.complete();

        assertThat(category).isNotNull();
        assertThat(category.getName()).isEqualTo("New Category");

        // Verify it was added to state
        assertThat(guildState.getCategories()).hasSize(1);
        assertThat(guildState.getCategories().stream().findFirst().get().getCategoryName()).isEqualTo("New Category");
    }

    @Test
    @DisplayName("mockTextChannel() guild.getSelfMember() should return member with full permissions")
    void mockTextChannel_guildGetSelfMember_shouldReturnMemberWithFullPermissions() {
        // getSelfMember() is only set up when creating a TextChannel, not in mockGuild() directly
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        Guild guild = channel.getGuild();
        Member selfMember = guild.getSelfMember();

        assertThat(selfMember).isNotNull();
        assertThat(selfMember.getIdLong()).isEqualTo(1L);
        assertThat(selfMember.hasAccess(null)).isTrue();
    }

    // ========== TextChannel Mock Tests ==========

    @Test
    @DisplayName("mockTextChannel() should return channel with correct properties")
    void mockTextChannel_shouldReturnChannelWithCorrectProperties() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);

        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        assertThat(channel.getIdLong()).isEqualTo(channelState.getChannelId());
        assertThat(channel.getId()).isEqualTo(String.valueOf(channelState.getChannelId()));
        assertThat(channel.getName()).isEqualTo("test-channel");
    }

    @Test
    @DisplayName("mockTextChannel() should return guild")
    void mockTextChannel_shouldReturnGuild() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);

        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);
        Guild guild = channel.getGuild();

        assertThat(guild).isNotNull();
        assertThat(guild.getIdLong()).isEqualTo(guildState.getGuildId());
    }

    @Test
    @DisplayName("mockTextChannel() should return parent category")
    void mockTextChannel_shouldReturnParentCategory() {
        MockCategoryState categoryState = guildState.createCategory("Test Category");
        MockChannelState channelState = guildState.createTextChannel("test-channel", categoryState.getCategoryId());

        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);
        Category category = channel.getParentCategory();

        assertThat(category).isNotNull();
        assertThat(category.getName()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("mockTextChannel() sendMessage() should store message in state")
    void mockTextChannel_sendMessage_shouldStoreMessageInState() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        channel.sendMessage("Test message").queue();

        List<MockMessageState> messages = channelState.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Test message");
    }

    @Test
    @DisplayName("mockTextChannel() sendMessage() should support file attachments")
    void mockTextChannel_sendMessage_shouldSupportFileAttachments() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Create a file attachment
        byte[] fileData = "test file content".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "test.txt");

        channel.sendMessage("Message with attachment")
                .addFiles(fileUpload)
                .queue();

        List<MockMessageState> messages = channelState.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Message with attachment");
        assertThat(messages.get(0).getAttachments()).hasSize(1);
    }

    @Test
    @DisplayName("mockTextChannel() getHistory() should retrieve messages with attachments")
    void mockTextChannel_getHistory_shouldRetrieveMessagesWithAttachments() throws Exception {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Send message with attachment
        byte[] fileData = "test content".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "test.txt");
        channel.sendMessage("Test").addFiles(fileUpload).queue();

        // Retrieve message history
        MessageHistory history = channel.getHistory();
        List<Message> messages = history.retrievePast(10).complete();

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContentRaw()).isEqualTo("Test");
        assertThat(messages.get(0).getAttachments()).hasSize(1);

        // Verify attachment can be downloaded
        Message.Attachment attachment = messages.get(0).getAttachments().get(0);
        InputStream downloadedData = attachment.getProxy().download().join();
        byte[] downloadedBytes = downloadedData.readAllBytes();
        assertThat(downloadedBytes).isEqualTo(fileData);
    }

    @Test
    @DisplayName("mockTextChannel() MessageCreateAction should support getContent() and setContent()")
    void mockTextChannel_messageCreateAction_shouldSupportGetContentAndSetContent() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        MessageCreateAction action = channel.sendMessage("Initial content");
        assertThat(action.getContent()).isEqualTo("Initial content");

        action.setContent("Modified content");
        assertThat(action.getContent()).isEqualTo("Modified content");

        action.queue();

        // Verify modified content was stored
        assertThat(channelState.getMessages().get(0).getContent()).isEqualTo("Modified content");
    }

    @Test
    @DisplayName("mockTextChannel() createThreadChannel() should create thread in state")
    void mockTextChannel_createThreadChannel_shouldCreateThreadInState() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        ThreadChannelAction action = channel.createThreadChannel("New Thread");
        ThreadChannel thread = action.complete();

        assertThat(thread).isNotNull();
        assertThat(thread.getName()).isEqualTo("New Thread");

        // Verify it was added to state
        List<MockThreadChannelState> threads = guildState.getThreadsInChannel(channelState.getChannelId());
        assertThat(threads).hasSize(1);
        assertThat(threads.get(0).getThreadName()).isEqualTo("New Thread");
    }

    @Test
    @DisplayName("mockTextChannel() retrieveArchivedPublicThreadChannels() should return threads from state")
    void mockTextChannel_retrieveArchivedPublicThreadChannels_shouldReturnThreadsFromState() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        guildState.createThread("Thread 1", channelState.getChannelId());
        guildState.createThread("Thread 2", channelState.getChannelId());

        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);
        List<ThreadChannel> threads = channel.retrieveArchivedPublicThreadChannels().complete();

        assertThat(threads).hasSize(2);
        assertThat(threads).extracting(ThreadChannel::getName)
                .containsExactlyInAnyOrder("Thread 1", "Thread 2");
    }

    @Test
    @DisplayName("mockTextChannel() getThreadChannels() should return threads from state")
    void mockTextChannel_getThreadChannels_shouldReturnThreadsFromState() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        guildState.createThread("chat", channelState.getChannelId());
        guildState.createThread("notes", channelState.getChannelId());

        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);
        List<ThreadChannel> threads = channel.getThreadChannels();

        assertThat(threads).hasSize(2);
        assertThat(threads).extracting(ThreadChannel::getName)
                .containsExactlyInAnyOrder("chat", "notes");
    }

    @Test
    @DisplayName("mockTextChannel() should have JDA with SelfUser")
    void mockTextChannel_shouldHaveJDAWithSelfUser() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        assertThat(channel.getJDA()).isNotNull();
        assertThat(channel.getJDA().getSelfUser()).isNotNull();
        assertThat(channel.getJDA().getSelfUser().getIdLong()).isEqualTo(1L);
    }

    @Test
    @DisplayName("mockTextChannel() sendMessage(MessageCreateData) with empty content should handle getContent() safely")
    void mockTextChannel_sendMessageWithMessageCreateData_emptyContent_shouldHandleGetContentSafely() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Create MessageCreateData with file-only message (empty content)
        byte[] fileData = "attachment data".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "file.txt");
        net.dv8tion.jda.api.utils.messages.MessageCreateData messageData =
                new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .addFiles(fileUpload)
                        .build();

        MessageCreateAction action = channel.sendMessage(messageData);

        // getContent() should return empty string (never null) even for file-only messages
        String content = action.getContent();
        assertThat(content).isNotNull();
        assertThat(content).isEmpty();

        // Should be able to call setContent() without NPE
        action.setContent("Tagged content");
        assertThat(action.getContent()).isEqualTo("Tagged content");
    }

    @Test
    @DisplayName("mockTextChannel() sendMessage(MessageCreateData) should support setContent() after creation")
    void mockTextChannel_sendMessageWithMessageCreateData_shouldSupportSetContent() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Create MessageCreateData with content
        net.dv8tion.jda.api.utils.messages.MessageCreateData messageData =
                new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .setContent("Initial content")
                        .build();

        MessageCreateAction action = channel.sendMessage(messageData);

        // Should be able to modify content via setContent()
        action.setContent("Modified content");
        assertThat(action.getContent()).isEqualTo("Modified content");

        action.queue();

        // Verify modified content was stored
        assertThat(channelState.getMessages()).hasSize(1);
        assertThat(channelState.getMessages().get(0).getContent()).isEqualTo("Modified content");
    }

    @Test
    @DisplayName("mockTextChannel() sendMessage(MessageCreateData) should store file attachments")
    void mockTextChannel_sendMessageWithMessageCreateData_shouldStoreFileAttachments() {
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);
        TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Create MessageCreateData with file attachment
        byte[] fileData = "test attachment".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "test.txt");
        net.dv8tion.jda.api.utils.messages.MessageCreateData messageData =
                new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .setContent("Message with file")
                        .addFiles(fileUpload)
                        .build();

        channel.sendMessage(messageData).queue();

        // Verify file attachment was stored
        assertThat(channelState.getMessages()).hasSize(1);
        assertThat(channelState.getMessages().get(0).getContent()).isEqualTo("Message with file");
        assertThat(channelState.getMessages().get(0).getAttachments()).hasSize(1);
    }

    // ========== Category Mock Tests ==========

    @Test
    @DisplayName("mockCategory() should return category with correct properties")
    void mockCategory_shouldReturnCategoryWithCorrectProperties() {
        MockCategoryState categoryState = guildState.createCategory("Test Category");

        Category category = StatefulMockFactory.mockCategory(categoryState, guildState);

        assertThat(category.getIdLong()).isEqualTo(categoryState.getCategoryId());
        assertThat(category.getId()).isEqualTo(String.valueOf(categoryState.getCategoryId()));
        assertThat(category.getName()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("mockCategory() should return channels in category")
    void mockCategory_shouldReturnChannelsInCategory() {
        MockCategoryState categoryState = guildState.createCategory("Test Category");
        guildState.createTextChannel("channel-1", categoryState.getCategoryId());
        guildState.createTextChannel("channel-2", categoryState.getCategoryId());

        Category category = StatefulMockFactory.mockCategory(categoryState, guildState);
        List<TextChannel> channels = category.getTextChannels();

        assertThat(channels).hasSize(2);
        assertThat(channels).extracting(TextChannel::getName)
                .containsExactlyInAnyOrder("channel-1", "channel-2");
    }

    @Test
    @DisplayName("mockCategory() createTextChannel() should create channel in state")
    void mockCategory_createTextChannel_shouldCreateChannelInState() {
        MockCategoryState categoryState = guildState.createCategory("Test Category");
        Category category = StatefulMockFactory.mockCategory(categoryState, guildState);

        ChannelAction<TextChannel> action = category.createTextChannel("new-channel");
        TextChannel channel = action.complete();

        assertThat(channel).isNotNull();
        assertThat(channel.getName()).isEqualTo("new-channel");

        // Verify it was added to state in the correct category
        assertThat(guildState.getChannels())
                .filteredOn(ch -> ch.getCategoryId() == categoryState.getCategoryId())
                .hasSize(1);
    }

    // ========== ThreadChannel Mock Tests ==========

    @Test
    @DisplayName("mockThreadChannel() should return thread with correct properties")
    void mockThreadChannel_shouldReturnThreadWithCorrectProperties() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());

        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        assertThat(thread.getIdLong()).isEqualTo(threadState.getThreadId());
        assertThat(thread.getId()).isEqualTo(String.valueOf(threadState.getThreadId()));
        assertThat(thread.getName()).isEqualTo("Test Thread");
    }

    @Test
    @DisplayName("mockThreadChannel() sendMessage() should store message in state")
    void mockThreadChannel_sendMessage_shouldStoreMessageInState() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());
        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        thread.sendMessage("Thread message").queue();

        List<MockMessageState> messages = threadState.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Thread message");
    }

    @Test
    @DisplayName("mockThreadChannel() MessageCreateAction should support getContent() and setContent()")
    void mockThreadChannel_messageCreateAction_shouldSupportGetContentAndSetContent() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());
        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        MessageCreateAction action = thread.sendMessage("Initial");
        assertThat(action.getContent()).isEqualTo("Initial");

        action.setContent("Modified");
        assertThat(action.getContent()).isEqualTo("Modified");

        action.queue();
        assertThat(threadState.getMessages().get(0).getContent()).isEqualTo("Modified");
    }

    @Test
    @DisplayName("mockThreadChannel() JDA.retrieveUserById() should return CacheRestAction")
    void mockThreadChannel_jdaRetrieveUserById_shouldReturnCacheRestAction() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());
        MockUserState userState = guildState.createUser("TestUser");

        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        // This is the critical fix: retrieveUserById must return CacheRestAction, not RestAction
        CacheRestAction<User> action = thread.getJDA().retrieveUserById(String.valueOf(userState.getUserId()));

        assertThat(action).isNotNull();
        User user = action.complete();
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("TestUser");
    }

    @Test
    @DisplayName("mockThreadChannel() sendMessage(MessageCreateData) with empty content should handle getContent() safely")
    void mockThreadChannel_sendMessageWithMessageCreateData_emptyContent_shouldHandleGetContentSafely() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());
        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        // Create MessageCreateData with file-only message (empty content)
        byte[] fileData = "board image data".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "board.png");
        net.dv8tion.jda.api.utils.messages.MessageCreateData messageData =
                new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .addFiles(fileUpload)
                        .build();

        MessageCreateAction action = thread.sendMessage(messageData);

        // getContent() should return empty string (never null) even for file-only messages
        String content = action.getContent();
        assertThat(content).isNotNull();
        assertThat(content).isEmpty();

        // Should be able to call setContent() without NPE
        action.setContent("Tagged content");
        assertThat(action.getContent()).isEqualTo("Tagged content");
    }

    @Test
    @DisplayName("mockThreadChannel() sendMessage(MessageCreateData) should support setContent() after creation")
    void mockThreadChannel_sendMessageWithMessageCreateData_shouldSupportSetContent() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());
        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        // Create MessageCreateData with null content
        byte[] fileData = "test data".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "test.png");
        net.dv8tion.jda.api.utils.messages.MessageCreateData messageData =
                new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .addFiles(fileUpload)
                        .build();

        MessageCreateAction action = thread.sendMessage(messageData);

        // Should be able to modify content via setContent()
        action.setContent("Modified content with emoji tags");
        assertThat(action.getContent()).isEqualTo("Modified content with emoji tags");

        action.queue();

        // Verify modified content was stored
        assertThat(threadState.getMessages()).hasSize(1);
        assertThat(threadState.getMessages().get(0).getContent()).isEqualTo("Modified content with emoji tags");
    }

    @Test
    @DisplayName("mockThreadChannel() sendMessage(MessageCreateData) should store file attachments")
    void mockThreadChannel_sendMessageWithMessageCreateData_shouldStoreFileAttachments() {
        MockChannelState channelState = guildState.createTextChannel("parent-channel", 0L);
        MockThreadChannelState threadState = guildState.createThread("Test Thread", channelState.getChannelId());
        ThreadChannel thread = StatefulMockFactory.mockThreadChannel(threadState, guildState);

        // Create MessageCreateData with file attachment
        byte[] fileData = "board image".getBytes();
        FileUpload fileUpload = FileUpload.fromData(new ByteArrayInputStream(fileData), "board.png");
        net.dv8tion.jda.api.utils.messages.MessageCreateData messageData =
                new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .addFiles(fileUpload)
                        .build();

        thread.sendMessage(messageData).queue();

        // Verify file attachment was stored
        assertThat(threadState.getMessages()).hasSize(1);
        assertThat(threadState.getMessages().get(0).getAttachments()).hasSize(1);
    }

    // ========== User Mock Tests ==========

    @Test
    @DisplayName("mockUser() should return user with correct properties")
    void mockUser_shouldReturnUserWithCorrectProperties() {
        MockUserState userState = guildState.createUser("Alice");

        User user = StatefulMockFactory.mockUser(userState);

        assertThat(user.getIdLong()).isEqualTo(userState.getUserId());
        assertThat(user.getId()).isEqualTo(String.valueOf(userState.getUserId()));
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getDiscriminator()).isEqualTo(userState.getDiscriminator());
        assertThat(user.getAsTag()).isEqualTo(userState.getAsTag());
    }

    @Test
    @DisplayName("mockUser() should return correct mention format")
    void mockUser_shouldReturnCorrectMentionFormat() {
        MockUserState userState = guildState.createUser("Alice");
        User user = StatefulMockFactory.mockUser(userState);

        assertThat(user.getAsMention()).isEqualTo("<@" + userState.getUserId() + ">");
    }

    // ========== Member Mock Tests ==========

    @Test
    @DisplayName("mockMember() should return member with user")
    void mockMember_shouldReturnMemberWithUser() {
        MockUserState userState = guildState.createUser("Bob");
        MockMemberState memberState = guildState.createMember(userState.getUserId());

        Member member = StatefulMockFactory.mockMember(memberState, guildState);

        assertThat(member.getIdLong()).isEqualTo(userState.getUserId());
        assertThat(member.getUser()).isNotNull();
        assertThat(member.getUser().getName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("mockMember() should return correct mention format")
    void mockMember_shouldReturnCorrectMentionFormat() {
        MockUserState userState = guildState.createUser("Bob");
        MockMemberState memberState = guildState.createMember(userState.getUserId());

        Member member = StatefulMockFactory.mockMember(memberState, guildState);

        assertThat(member.getAsMention()).isEqualTo("<@" + userState.getUserId() + ">");
    }

    @Test
    @DisplayName("mockMember() getRoles() should return roles from state")
    void mockMember_getRoles_shouldReturnRolesFromState() {
        MockUserState userState = guildState.createUser("Bob");
        MockMemberState memberState = guildState.createMember(userState.getUserId());
        MockRoleState role1 = guildState.createRole("Moderator");
        MockRoleState role2 = guildState.createRole("Player");

        memberState.addRole(role1.getRoleId());
        memberState.addRole(role2.getRoleId());

        Member member = StatefulMockFactory.mockMember(memberState, guildState);
        List<Role> roles = member.getRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder("Moderator", "Player");
    }

    @Test
    @DisplayName("mockMember() getGuild() should return guild")
    void mockMember_getGuild_shouldReturnGuild() {
        MockUserState userState = guildState.createUser("Bob");
        MockMemberState memberState = guildState.createMember(userState.getUserId());

        Member member = StatefulMockFactory.mockMember(memberState, guildState);
        Guild guild = member.getGuild();

        assertThat(guild).isNotNull();
        assertThat(guild.getIdLong()).isEqualTo(guildState.getGuildId());
    }

    // ========== Role Mock Tests ==========

    @Test
    @DisplayName("mockRole() should return role with correct properties")
    void mockRole_shouldReturnRoleWithCorrectProperties() {
        MockRoleState roleState = guildState.createRole("Administrator");

        Role role = StatefulMockFactory.mockRole(roleState);

        assertThat(role.getIdLong()).isEqualTo(roleState.getRoleId());
        assertThat(role.getId()).isEqualTo(String.valueOf(roleState.getRoleId()));
        assertThat(role.getName()).isEqualTo("Administrator");
    }

    @Test
    @DisplayName("mockRole() should return correct mention format")
    void mockRole_shouldReturnCorrectMentionFormat() {
        MockRoleState roleState = guildState.createRole("Administrator");
        Role role = StatefulMockFactory.mockRole(roleState);

        assertThat(role.getAsMention()).isEqualTo("<@&" + roleState.getRoleId() + ">");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Integration: Guild -> TextChannel -> sendMessage should persist across calls")
    void integration_guildToChannelToSendMessage_shouldPersistAcrossCalls() {
        // Create initial state
        MockChannelState channelState = guildState.createTextChannel("test-channel", 0L);

        // Get guild and find channel
        Guild guild = StatefulMockFactory.mockGuild(guildState);
        TextChannel channel = guild.getTextChannels().get(0);

        // Send message
        channel.sendMessage("Persistent message").queue();

        // Get a fresh guild mock and verify message is still there
        Guild freshGuild = StatefulMockFactory.mockGuild(guildState);
        TextChannel freshChannel = freshGuild.getTextChannels().get(0);

        // The channel should still have the message in state
        assertThat(channelState.getMessages()).hasSize(1);
        assertThat(channelState.getMessages().get(0).getContent()).isEqualTo("Persistent message");
    }

    @Test
    @DisplayName("Integration: Member roles should update when state changes")
    void integration_memberRoles_shouldUpdateWhenStateChanges() {
        MockUserState userState = guildState.createUser("Alice");
        MockMemberState memberState = guildState.createMember(userState.getUserId());
        MockRoleState roleState = guildState.createRole("Moderator");

        Member member1 = StatefulMockFactory.mockMember(memberState, guildState);
        assertThat(member1.getRoles()).isEmpty();

        // Add role to member in state
        memberState.addRole(roleState.getRoleId());

        // Create a new mock - it should see the updated roles
        Member member2 = StatefulMockFactory.mockMember(memberState, guildState);
        assertThat(member2.getRoles()).hasSize(1);
        assertThat(member2.getRoles().get(0).getName()).isEqualTo("Moderator");
    }

    @Test
    @DisplayName("TextChannel getHistoryAround() returns messages from state")
    void textChannel_getHistoryAround_returnsMessagesFromState() {
        MockCategoryState categoryState = guildState.createCategory("test-category");
        MockChannelState channelState = guildState.createTextChannel("test-channel", categoryState.getCategoryId());

        // Add messages to state
        channelState.addMessage(new MockMessageState(1001L, channelState.getChannelId(), 100L, "Message 1"));
        channelState.addMessage(new MockMessageState(1002L, channelState.getChannelId(), 100L, "Message 2"));
        channelState.addMessage(new MockMessageState(1003L, channelState.getChannelId(), 100L, "Message 3"));

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel =
            StatefulMockFactory.mockTextChannel(channelState, guildState);

        // Get message history
        net.dv8tion.jda.api.entities.MessageHistory history =
            channel.getHistoryAround(channel.getLatestMessageId(), 100).complete();

        assertThat(history.getRetrievedHistory()).hasSize(3);
        assertThat(history.getRetrievedHistory().get(0).getContentRaw()).isEqualTo("Message 1");
        assertThat(history.getRetrievedHistory().get(1).getContentRaw()).isEqualTo("Message 2");
        assertThat(history.getRetrievedHistory().get(2).getContentRaw()).isEqualTo("Message 3");
    }

    @Test
    @DisplayName("ThreadChannel getHistoryAround() returns messages from state")
    void threadChannel_getHistoryAround_returnsMessagesFromState() {
        MockCategoryState categoryState = guildState.createCategory("test-category");
        MockChannelState channelState = guildState.createTextChannel("test-channel", categoryState.getCategoryId());
        MockThreadChannelState threadState = guildState.createThread("test-thread", channelState.getChannelId());

        // Add messages to state
        threadState.addMessage(new MockMessageState(1001L, threadState.getThreadId(), 100L, "Thread Message 1"));
        threadState.addMessage(new MockMessageState(1002L, threadState.getThreadId(), 100L, "Thread Message 2"));

        net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel thread =
            StatefulMockFactory.mockThreadChannel(threadState, guildState);

        // Get message history
        net.dv8tion.jda.api.entities.MessageHistory history =
            thread.getHistoryAround(thread.getLatestMessageId(), 100).complete();

        assertThat(history.getRetrievedHistory()).hasSize(2);
        assertThat(history.getRetrievedHistory().get(0).getContentRaw()).isEqualTo("Thread Message 1");
        assertThat(history.getRetrievedHistory().get(1).getContentRaw()).isEqualTo("Thread Message 2");
    }

    // Note: Message deletion testing is verified in E2E tests
    // Unit testing message.delete() has complex Mockito/JDA type casting issues
    // that don't affect the actual E2E functionality

    @Test
    @DisplayName("TextChannel getLatestMessageIdLong() returns correct ID from state")
    void textChannel_getLatestMessageIdLong_returnsCorrectId() {
        MockCategoryState categoryState = guildState.createCategory("test-category");
        MockChannelState channelState = guildState.createTextChannel("test-channel", categoryState.getCategoryId());

        channelState.addMessage(new MockMessageState(1001L, channelState.getChannelId(), 100L, "Message 1"));
        channelState.addMessage(new MockMessageState(1002L, channelState.getChannelId(), 100L, "Message 2"));

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel =
            StatefulMockFactory.mockTextChannel(channelState, guildState);

        assertThat(channel.getLatestMessageIdLong()).isEqualTo(1002L);
    }

    @Test
    @DisplayName("ThreadChannel getLatestMessageIdLong() returns correct ID from state")
    void threadChannel_getLatestMessageIdLong_returnsCorrectId() {
        MockCategoryState categoryState = guildState.createCategory("test-category");
        MockChannelState channelState = guildState.createTextChannel("test-channel", categoryState.getCategoryId());
        MockThreadChannelState threadState = guildState.createThread("test-thread", channelState.getChannelId());

        threadState.addMessage(new MockMessageState(1001L, threadState.getThreadId(), 100L, "Thread Message 1"));
        threadState.addMessage(new MockMessageState(1002L, threadState.getThreadId(), 100L, "Thread Message 2"));

        net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel thread =
            StatefulMockFactory.mockThreadChannel(threadState, guildState);

        assertThat(thread.getLatestMessageIdLong()).isEqualTo(1002L);
    }
}
