package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockGuildStateTest {

    private MockDiscordServer server;
    private MockGuildState guild;

    @BeforeEach
    void setUp() {
        server = MockDiscordServer.create();
        guild = server.createGuild(12345L, "Test Guild");
    }

    @Test
    void constructor_setsGuildIdAndName() {
        assertThat(guild.getGuildId()).isEqualTo(12345L);
        assertThat(guild.getGuildName()).isEqualTo("Test Guild");
    }

    @Test
    void getServer_returnsCorrectServer() {
        assertThat(guild.getServer()).isSameAs(server);
    }

    @Test
    void createCategory_createsAndStoresCategory() {
        MockCategoryState category = guild.createCategory("game-channels");

        assertThat(category).isNotNull();
        assertThat(category.getCategoryName()).isEqualTo("game-channels");
        assertThat(category.getGuildId()).isEqualTo(12345L);
        assertThat(category.getCategoryId()).isPositive();
    }

    @Test
    void createCategory_assignsUniqueCategoryIds() {
        MockCategoryState category1 = guild.createCategory("category-1");
        MockCategoryState category2 = guild.createCategory("category-2");

        assertThat(category1.getCategoryId()).isNotEqualTo(category2.getCategoryId());
    }

    @Test
    void getCategory_returnsStoredCategory() {
        MockCategoryState category = guild.createCategory("game-channels");

        MockCategoryState retrieved = guild.getCategory(category.getCategoryId());

        assertThat(retrieved).isSameAs(category);
    }

    @Test
    void getCategory_returnsNullForNonexistentCategory() {
        MockCategoryState retrieved = guild.getCategory(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getCategories_returnsAllCategories() {
        MockCategoryState category1 = guild.createCategory("category-1");
        MockCategoryState category2 = guild.createCategory("category-2");
        MockCategoryState category3 = guild.createCategory("category-3");

        Collection<MockCategoryState> categories = guild.getCategories();

        assertThat(categories).containsExactlyInAnyOrder(category1, category2, category3);
    }

    @Test
    void getCategories_returnsEmptyListWhenNoCategories() {
        Collection<MockCategoryState> categories = guild.getCategories();

        assertThat(categories).isEmpty();
    }

    @Test
    void createTextChannel_createsAndStoresChannel() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());

        assertThat(channel).isNotNull();
        assertThat(channel.getChannelName()).isEqualTo("game-actions");
        assertThat(channel.getCategoryId()).isEqualTo(category.getCategoryId());
        assertThat(channel.getGuildId()).isEqualTo(12345L);
        assertThat(channel.getChannelId()).isPositive();
    }

    @Test
    void createTextChannel_addsChannelToCategory() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());

        assertThat(category.getChannelIds()).contains(channel.getChannelId());
    }

    @Test
    void createTextChannel_assignsUniqueChannelIds() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel1 = guild.createTextChannel("channel-1", category.getCategoryId());
        MockChannelState channel2 = guild.createTextChannel("channel-2", category.getCategoryId());

        assertThat(channel1.getChannelId()).isNotEqualTo(channel2.getChannelId());
    }

    @Test
    void getChannel_returnsStoredChannel() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());

        MockChannelState retrieved = guild.getChannel(channel.getChannelId());

        assertThat(retrieved).isSameAs(channel);
    }

    @Test
    void getChannel_returnsNullForNonexistentChannel() {
        MockChannelState retrieved = guild.getChannel(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getChannels_returnsAllChannels() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel1 = guild.createTextChannel("channel-1", category.getCategoryId());
        MockChannelState channel2 = guild.createTextChannel("channel-2", category.getCategoryId());
        MockChannelState channel3 = guild.createTextChannel("channel-3", category.getCategoryId());

        Collection<MockChannelState> channels = guild.getChannels();

        assertThat(channels).containsExactlyInAnyOrder(channel1, channel2, channel3);
    }

    @Test
    void getChannels_returnsEmptyListWhenNoChannels() {
        Collection<MockChannelState> channels = guild.getChannels();

        assertThat(channels).isEmpty();
    }

    @Test
    void getChannelsInCategory_returnsOnlyChannelsInThatCategory() {
        MockCategoryState category1 = guild.createCategory("category-1");
        MockCategoryState category2 = guild.createCategory("category-2");

        MockChannelState channel1 = guild.createTextChannel("channel-1", category1.getCategoryId());
        MockChannelState channel2 = guild.createTextChannel("channel-2", category1.getCategoryId());
        MockChannelState channel3 = guild.createTextChannel("channel-3", category2.getCategoryId());

        List<MockChannelState> channelsInCategory1 = guild.getChannelsInCategory(category1.getCategoryId());

        assertThat(channelsInCategory1).containsExactlyInAnyOrder(channel1, channel2);
        assertThat(channelsInCategory1).doesNotContain(channel3);
    }

    @Test
    void getChannelsInCategory_returnsEmptyListForCategoryWithNoChannels() {
        MockCategoryState category = guild.createCategory("empty-category");

        List<MockChannelState> channels = guild.getChannelsInCategory(category.getCategoryId());

        assertThat(channels).isEmpty();
    }

    @Test
    void createUser_createsAndStoresUser() {
        MockUserState user = guild.createUser("Alice");

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("Alice");
        assertThat(user.getUserId()).isPositive();
        assertThat(user.getDiscriminator()).isEqualTo("0000");
    }

    @Test
    void createUser_assignsUniqueUserIds() {
        MockUserState user1 = guild.createUser("Alice");
        MockUserState user2 = guild.createUser("Bob");

        assertThat(user1.getUserId()).isNotEqualTo(user2.getUserId());
    }

    @Test
    void getUser_returnsStoredUser() {
        MockUserState user = guild.createUser("Alice");

        MockUserState retrieved = guild.getUser(user.getUserId());

        assertThat(retrieved).isSameAs(user);
    }

    @Test
    void getUser_returnsNullForNonexistentUser() {
        MockUserState retrieved = guild.getUser(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getUsers_returnsAllUsers() {
        MockUserState user1 = guild.createUser("Alice");
        MockUserState user2 = guild.createUser("Bob");
        MockUserState user3 = guild.createUser("Charlie");

        Collection<MockUserState> users = guild.getUsers();

        assertThat(users).containsExactlyInAnyOrder(user1, user2, user3);
    }

    @Test
    void getUsers_returnsEmptyListWhenNoUsers() {
        Collection<MockUserState> users = guild.getUsers();

        assertThat(users).isEmpty();
    }

    @Test
    void createMember_createsAndStoresMember() {
        MockUserState user = guild.createUser("Alice");
        MockMemberState member = guild.createMember(user.getUserId());

        assertThat(member).isNotNull();
        assertThat(member.getUserId()).isEqualTo(user.getUserId());
        assertThat(member.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void getMember_returnsStoredMember() {
        MockUserState user = guild.createUser("Alice");
        MockMemberState member = guild.createMember(user.getUserId());

        MockMemberState retrieved = guild.getMember(user.getUserId());

        assertThat(retrieved).isSameAs(member);
    }

    @Test
    void getMember_returnsNullForNonexistentMember() {
        MockMemberState retrieved = guild.getMember(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getMembers_returnsAllMembers() {
        MockUserState user1 = guild.createUser("Alice");
        MockUserState user2 = guild.createUser("Bob");
        MockUserState user3 = guild.createUser("Charlie");

        MockMemberState member1 = guild.createMember(user1.getUserId());
        MockMemberState member2 = guild.createMember(user2.getUserId());
        MockMemberState member3 = guild.createMember(user3.getUserId());

        Collection<MockMemberState> members = guild.getMembers();

        assertThat(members).containsExactlyInAnyOrder(member1, member2, member3);
    }

    @Test
    void getMembers_returnsEmptyListWhenNoMembers() {
        Collection<MockMemberState> members = guild.getMembers();

        assertThat(members).isEmpty();
    }

    @Test
    void createRole_createsAndStoresRole() {
        MockRoleState role = guild.createRole("Moderator");

        assertThat(role).isNotNull();
        assertThat(role.getRoleName()).isEqualTo("Moderator");
        assertThat(role.getRoleId()).isPositive();
    }

    @Test
    void createRole_assignsUniqueRoleIds() {
        MockRoleState role1 = guild.createRole("Moderator");
        MockRoleState role2 = guild.createRole("Player");

        assertThat(role1.getRoleId()).isNotEqualTo(role2.getRoleId());
    }

    @Test
    void getRole_returnsStoredRole() {
        MockRoleState role = guild.createRole("Moderator");

        MockRoleState retrieved = guild.getRole(role.getRoleId());

        assertThat(retrieved).isSameAs(role);
    }

    @Test
    void getRole_returnsNullForNonexistentRole() {
        MockRoleState retrieved = guild.getRole(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getRoles_returnsAllRoles() {
        MockRoleState role1 = guild.createRole("Moderator");
        MockRoleState role2 = guild.createRole("Player");
        MockRoleState role3 = guild.createRole("Admin");

        Collection<MockRoleState> roles = guild.getRoles();

        assertThat(roles).containsExactlyInAnyOrder(role1, role2, role3);
    }

    @Test
    void getRoles_returnsEmptyListWhenNoRoles() {
        Collection<MockRoleState> roles = guild.getRoles();

        assertThat(roles).isEmpty();
    }

    @Test
    void multipleGuilds_haveIndependentState() {
        MockGuildState guild2 = server.createGuild(67890L, "Another Guild");

        MockUserState user1 = guild.createUser("Alice");
        MockUserState user2 = guild2.createUser("Bob");

        assertThat(guild.getUsers()).containsExactly(user1);
        assertThat(guild2.getUsers()).containsExactly(user2);

        assertThat(guild.getUser(user2.getUserId())).isNull();
        assertThat(guild2.getUser(user1.getUserId())).isNull();
    }

    // ========== Thread Management Tests ==========

    @Test
    void createThread_createsAndStoresThread() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
        MockThreadChannelState thread = guild.createThread("strategy-discussion", channel.getChannelId());

        assertThat(thread).isNotNull();
        assertThat(thread.getThreadName()).isEqualTo("strategy-discussion");
        assertThat(thread.getParentChannelId()).isEqualTo(channel.getChannelId());
        assertThat(thread.getGuildId()).isEqualTo(12345L);
        assertThat(thread.getThreadId()).isPositive();
    }

    @Test
    void createThread_assignsUniqueThreadIds() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
        MockThreadChannelState thread1 = guild.createThread("thread-1", channel.getChannelId());
        MockThreadChannelState thread2 = guild.createThread("thread-2", channel.getChannelId());

        assertThat(thread1.getThreadId()).isNotEqualTo(thread2.getThreadId());
    }

    @Test
    void getThread_returnsStoredThread() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
        MockThreadChannelState thread = guild.createThread("strategy-discussion", channel.getChannelId());

        MockThreadChannelState retrieved = guild.getThread(thread.getThreadId());

        assertThat(retrieved).isSameAs(thread);
    }

    @Test
    void getThread_returnsNullForNonexistentThread() {
        MockThreadChannelState retrieved = guild.getThread(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getThreads_returnsAllThreads() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
        MockThreadChannelState thread1 = guild.createThread("thread-1", channel.getChannelId());
        MockThreadChannelState thread2 = guild.createThread("thread-2", channel.getChannelId());
        MockThreadChannelState thread3 = guild.createThread("thread-3", channel.getChannelId());

        Collection<MockThreadChannelState> threads = guild.getThreads();

        assertThat(threads).containsExactlyInAnyOrder(thread1, thread2, thread3);
    }

    @Test
    void getThreads_returnsEmptyListWhenNoThreads() {
        Collection<MockThreadChannelState> threads = guild.getThreads();

        assertThat(threads).isEmpty();
    }

    @Test
    void getThreadsInChannel_returnsOnlyThreadsInThatChannel() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel1 = guild.createTextChannel("channel-1", category.getCategoryId());
        MockChannelState channel2 = guild.createTextChannel("channel-2", category.getCategoryId());

        MockThreadChannelState thread1 = guild.createThread("thread-1", channel1.getChannelId());
        MockThreadChannelState thread2 = guild.createThread("thread-2", channel1.getChannelId());
        MockThreadChannelState thread3 = guild.createThread("thread-3", channel2.getChannelId());

        List<MockThreadChannelState> threadsInChannel1 = guild.getThreadsInChannel(channel1.getChannelId());

        assertThat(threadsInChannel1).containsExactlyInAnyOrder(thread1, thread2);
        assertThat(threadsInChannel1).doesNotContain(thread3);
    }

    @Test
    void getThreadsInChannel_returnsEmptyListForChannelWithNoThreads() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("empty-channel", category.getCategoryId());

        List<MockThreadChannelState> threads = guild.getThreadsInChannel(channel.getChannelId());

        assertThat(threads).isEmpty();
    }

    // ========== Emoji Management Tests ==========

    @Test
    void createEmoji_createsAndStoresEmoji() {
        MockEmojiState emoji = guild.createEmoji("atreides", false);

        assertThat(emoji).isNotNull();
        assertThat(emoji.getName()).isEqualTo("atreides");
        assertThat(emoji.isAnimated()).isFalse();
        assertThat(emoji.getGuildId()).isEqualTo(12345L);
        assertThat(emoji.getEmojiId()).isPositive();
    }

    @Test
    void createEmoji_createsAnimatedEmoji() {
        MockEmojiState emoji = guild.createEmoji("storm", true);

        assertThat(emoji.getName()).isEqualTo("storm");
        assertThat(emoji.isAnimated()).isTrue();
    }

    @Test
    void createEmoji_assignsUniqueEmojiIds() {
        MockEmojiState emoji1 = guild.createEmoji("emoji-1", false);
        MockEmojiState emoji2 = guild.createEmoji("emoji-2", false);

        assertThat(emoji1.getEmojiId()).isNotEqualTo(emoji2.getEmojiId());
    }

    @Test
    void getEmoji_returnsStoredEmoji() {
        MockEmojiState emoji = guild.createEmoji("atreides", false);

        MockEmojiState retrieved = guild.getEmoji(emoji.getEmojiId());

        assertThat(retrieved).isSameAs(emoji);
    }

    @Test
    void getEmoji_returnsNullForNonexistentEmoji() {
        MockEmojiState retrieved = guild.getEmoji(99999L);

        assertThat(retrieved).isNull();
    }

    @Test
    void getEmojiByName_returnsStoredEmoji() {
        MockEmojiState emoji = guild.createEmoji("atreides", false);

        MockEmojiState retrieved = guild.getEmojiByName("atreides");

        assertThat(retrieved).isSameAs(emoji);
    }

    @Test
    void getEmojiByName_returnsNullForNonexistentEmoji() {
        MockEmojiState retrieved = guild.getEmojiByName("nonexistent");

        assertThat(retrieved).isNull();
    }

    @Test
    void getEmojiByName_returnsFirstEmojiWithMatchingName() {
        MockEmojiState emoji1 = guild.createEmoji("test", false);
        guild.createEmoji("test", true); // Same name, different emoji

        MockEmojiState retrieved = guild.getEmojiByName("test");

        assertThat(retrieved).isSameAs(emoji1);
    }

    @Test
    void getEmojis_returnsAllEmojis() {
        MockEmojiState emoji1 = guild.createEmoji("emoji-1", false);
        MockEmojiState emoji2 = guild.createEmoji("emoji-2", true);
        MockEmojiState emoji3 = guild.createEmoji("emoji-3", false);

        Collection<MockEmojiState> emojis = guild.getEmojis();

        assertThat(emojis).containsExactlyInAnyOrder(emoji1, emoji2, emoji3);
    }

    @Test
    void getEmojis_returnsEmptyListWhenNoEmojis() {
        Collection<MockEmojiState> emojis = guild.getEmojis();

        assertThat(emojis).isEmpty();
    }

    @Test
    void guild_canHaveThreadsAndEmojis() {
        MockCategoryState category = guild.createCategory("game-channels");
        MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
        MockThreadChannelState thread = guild.createThread("strategy-discussion", channel.getChannelId());
        MockEmojiState emoji = guild.createEmoji("atreides", false);

        assertThat(guild.getThreads()).hasSize(1);
        assertThat(guild.getEmojis()).hasSize(1);
        assertThat(guild.getThread(thread.getThreadId())).isSameAs(thread);
        assertThat(guild.getEmoji(emoji.getEmojiId())).isSameAs(emoji);
    }
}
