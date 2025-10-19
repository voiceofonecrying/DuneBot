package testutil.discord;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import testutil.discord.state.*;

import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Factory for creating Mockito mocks backed by {@link MockDiscordServer} state.
 *
 * <p>This factory bridges the gap between JDA Discord entities (Guild, TextChannel, etc.)
 * and the stateful mock infrastructure. When you call methods on the returned mocks,
 * they read from and write to the underlying state objects.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Stateful Mocks</b> - Calling {@code guild.getTextChannels()} returns channels from state</li>
 *   <li><b>Persistent Changes</b> - {@code channel.sendMessage()} stores messages in state</li>
 *   <li><b>Verifiable Actions</b> - Tests can verify state changes after actions</li>
 * </ul>
 *
 * <p><b>Architecture:</b>
 * <pre>
 * MockDiscordServer (state storage)
 *   └─ MockGuildState
 *       ├─ MockChannelState (stores messages)
 *       ├─ MockUserState
 *       └─ MockMemberState
 *
 * StatefulMockFactory (creates JDA mocks)
 *   ├─ mockGuild() → Guild (reads channels from state)
 *   ├─ mockTextChannel() → TextChannel (sendMessage writes to state!)
 *   ├─ mockUser() → User
 *   └─ mockMember() → Member (getRoles reads from state)
 * </pre>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create state
 * MockDiscordServer server = MockDiscordServer.create();
 * MockGuildState guildState = server.createGuild(12345L, "Test Guild");
 * MockCategoryState categoryState = guildState.createCategory("game-channels");
 * MockChannelState channelState = guildState.createTextChannel("game-actions", categoryState.getCategoryId());
 *
 * // Create stateful mocks
 * Guild guild = StatefulMockFactory.mockGuild(guildState);
 * TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guildState);
 *
 * // Use the mocks - they interact with state!
 * channel.sendMessage("Test message").queue();
 *
 * // Verify state was updated
 * List<MockMessageState> messages = channelState.getMessages();
 * assertThat(messages).hasSize(1);
 * assertThat(messages.get(0).getContent()).isEqualTo("Test message");
 *
 * // Guild methods also read from state
 * List<TextChannel> channels = guild.getTextChannels();
 * assertThat(channels).hasSize(1);
 * assertThat(channels.get(0).getName()).isEqualTo("game-actions");
 * }</pre>
 *
 * <p><b>Important Notes:</b>
 * <ul>
 *   <li>All mocks use lenient stubbing to avoid UnnecessaryStubbingException</li>
 *   <li>Circular references are handled (e.g., guild → channel → guild)</li>
 *   <li>State changes are immediate and synchronous (no async behavior)</li>
 * </ul>
 *
 * @see MockDiscordServer
 * @see MockGuildState
 * @see MockChannelState
 */
public class StatefulMockFactory {

    /**
     * Creates a Guild mock backed by state.
     *
     * <p>The returned mock reads all guild data (channels, categories, roles, members)
     * from the provided {@link MockGuildState}. Methods like {@code getTextChannels()}
     * return mocks created from the current state, so state changes are immediately
     * visible when you call guild methods again.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns guild ID from state</li>
     *   <li>{@code getId()} - Returns guild ID as string</li>
     *   <li>{@code getName()} - Returns guild name from state</li>
     *   <li>{@code getTextChannels()} - Creates mocks for all channels in state</li>
     *   <li>{@code getCategories()} - Creates mocks for all categories in state</li>
     *   <li>{@code getRoles()} - Creates mocks for all roles in state</li>
     *   <li>{@code getMembers()} - Creates mocks for all members in state</li>
     * </ul>
     *
     * <p><b>Note:</b> This method may be called multiple times for the same guild
     * (e.g., when mocking circular references like channel → guild). Each call
     * creates a new mock instance, but all read from the same state.
     *
     * @param guildState The guild state containing all guild data
     * @return A Mockito mock of {@link Guild} backed by the provided state
     */
    public static Guild mockGuild(MockGuildState guildState) {
        Guild guild = mock(Guild.class);

        lenient().when(guild.getIdLong()).thenReturn(guildState.getGuildId());
        lenient().when(guild.getId()).thenReturn(String.valueOf(guildState.getGuildId()));
        lenient().when(guild.getName()).thenReturn(guildState.getGuildName());

        // getTextChannels() reads from state
        lenient().when(guild.getTextChannels()).thenAnswer(inv -> {
            return guildState.getChannels().stream()
                .map(channelState -> mockTextChannel(channelState, guildState))
                .collect(Collectors.toList());
        });

        // getCategories() reads from state
        lenient().when(guild.getCategories()).thenAnswer(inv -> {
            return guildState.getCategories().stream()
                .map(categoryState -> mockCategory(categoryState, guildState))
                .collect(Collectors.toList());
        });

        // getRoles() reads from state
        lenient().when(guild.getRoles()).thenAnswer(inv -> {
            return guildState.getRoles().stream()
                .map(StatefulMockFactory::mockRole)
                .collect(Collectors.toList());
        });

        // getMembers() reads from state
        lenient().when(guild.getMembers()).thenAnswer(inv -> {
            return guildState.getMembers().stream()
                .map(memberState -> mockMember(memberState, guildState))
                .collect(Collectors.toList());
        });

        return guild;
    }

    /**
     * Creates a Category mock backed by state.
     *
     * <p>The returned mock represents a Discord channel category (channel group).
     * It reads category metadata and child channels from the provided state objects.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns category ID from state</li>
     *   <li>{@code getId()} - Returns category ID as string</li>
     *   <li>{@code getName()} - Returns category name from state</li>
     *   <li>{@code getTextChannels()} - Creates mocks for all channels in this category</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     * </ul>
     *
     * @param categoryState The category state containing category metadata
     * @param guildState The parent guild state (needed to look up channels and create guild mock)
     * @return A Mockito mock of {@link Category} backed by the provided state
     */
    public static Category mockCategory(MockCategoryState categoryState, MockGuildState guildState) {
        Category category = mock(Category.class);

        lenient().when(category.getIdLong()).thenReturn(categoryState.getCategoryId());
        lenient().when(category.getId()).thenReturn(String.valueOf(categoryState.getCategoryId()));
        lenient().when(category.getName()).thenReturn(categoryState.getCategoryName());

        // getTextChannels() reads from state
        lenient().when(category.getTextChannels()).thenAnswer(inv -> {
            return guildState.getChannelsInCategory(categoryState.getCategoryId()).stream()
                .map(channelState -> mockTextChannel(channelState, guildState))
                .collect(Collectors.toList());
        });

        // Mock guild reference
        Guild guild = mockGuild(guildState);
        lenient().when(category.getGuild()).thenReturn(guild);

        return category;
    }

    /**
     * Creates a TextChannel mock backed by state.
     *
     * <p><b>CRITICAL FEATURE:</b> This is the heart of the stateful mock system.
     * When you call {@code channel.sendMessage()}, the message is actually stored
     * in the {@link MockChannelState}! This enables true message verification in tests.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns channel ID from state</li>
     *   <li>{@code getId()} - Returns channel ID as string</li>
     *   <li>{@code getName()} - Returns channel name from state</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     *   <li>{@code sendMessage(String)} - <b>STORES THE MESSAGE IN STATE</b></li>
     *   <li>{@code sendMessage(MessageCreateData)} - <b>STORES THE MESSAGE IN STATE</b></li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * MockChannelState channelState = guild.createTextChannel("test", categoryId);
     * TextChannel channel = StatefulMockFactory.mockTextChannel(channelState, guild);
     *
     * // This message gets stored in state!
     * channel.sendMessage("Hello world").queue();
     *
     * // Verify it was stored
     * List<MockMessageState> messages = channelState.getMessages();
     * assertThat(messages.get(0).getContent()).isEqualTo("Hello world");
     * }</pre>
     *
     * @param channelState The channel state where messages will be stored
     * @param guildState The parent guild state (needed to generate message IDs and create guild mock)
     * @return A Mockito mock of {@link TextChannel} backed by the provided state
     */
    public static TextChannel mockTextChannel(MockChannelState channelState, MockGuildState guildState) {
        TextChannel channel = mock(TextChannel.class);

        lenient().when(channel.getIdLong()).thenReturn(channelState.getChannelId());
        lenient().when(channel.getId()).thenReturn(String.valueOf(channelState.getChannelId()));
        lenient().when(channel.getName()).thenReturn(channelState.getChannelName());

        // getGuild() returns stateful guild mock
        Guild guild = mockGuild(guildState);
        lenient().when(channel.getGuild()).thenReturn(guild);

        // sendMessage() STORES the message in state
        lenient().when(channel.sendMessage(anyString())).thenAnswer(inv -> {
            String content = inv.getArgument(0);
            long messageId = guildState.getServer().nextMessageId();
            MockMessageState message = new MockMessageState(messageId, channelState.getChannelId(), 0L, content);
            channelState.addMessage(message);

            // Return mock action
            MessageCreateAction action = mock(MessageCreateAction.class);
            doNothing().when(action).queue();
            return action;
        });

        lenient().when(channel.sendMessage(any(MessageCreateData.class))).thenAnswer(inv -> {
            MessageCreateData data = inv.getArgument(0);
            long messageId = guildState.getServer().nextMessageId();
            MockMessageState message = new MockMessageState(messageId, channelState.getChannelId(), 0L, data.getContent());
            channelState.addMessage(message);

            // Return mock action
            MessageCreateAction action = mock(MessageCreateAction.class);
            doNothing().when(action).queue();
            return action;
        });

        return channel;
    }

    /**
     * Creates a User mock backed by state.
     *
     * <p>The returned mock represents a Discord user account with basic identity
     * information (ID, username, discriminator).
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns user ID from state</li>
     *   <li>{@code getId()} - Returns user ID as string</li>
     *   <li>{@code getName()} - Returns username from state</li>
     *   <li>{@code getDiscriminator()} - Returns discriminator from state (e.g., "0000")</li>
     *   <li>{@code getAsTag()} - Returns full user tag from state (e.g., "Alice#0000")</li>
     * </ul>
     *
     * @param userState The user state containing user identity information
     * @return A Mockito mock of {@link User} backed by the provided state
     */
    public static User mockUser(MockUserState userState) {
        User user = mock(User.class);

        lenient().when(user.getIdLong()).thenReturn(userState.getUserId());
        lenient().when(user.getId()).thenReturn(String.valueOf(userState.getUserId()));
        lenient().when(user.getName()).thenReturn(userState.getUsername());
        lenient().when(user.getDiscriminator()).thenReturn(userState.getDiscriminator());
        lenient().when(user.getAsTag()).thenReturn(userState.getAsTag());

        return user;
    }

    /**
     * Creates a Member mock backed by state.
     *
     * <p>The returned mock represents a user's membership in a specific guild.
     * It combines user information with guild-specific data like roles. The
     * {@code getRoles()} method reads the member's current roles from state.
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getUser()} - Returns a user mock backed by the associated user state</li>
     *   <li>{@code getIdLong()} - Returns user ID from state</li>
     *   <li>{@code getId()} - Returns user ID as string</li>
     *   <li>{@code getRoles()} - Creates mocks for all roles assigned to this member in state</li>
     *   <li>{@code getGuild()} - Returns a guild mock backed by the provided guild state</li>
     * </ul>
     *
     * @param memberState The member state containing guild-specific user data (e.g., roles)
     * @param guildState The parent guild state (needed to look up user and role information)
     * @return A Mockito mock of {@link Member} backed by the provided state
     */
    public static Member mockMember(MockMemberState memberState, MockGuildState guildState) {
        Member member = mock(Member.class);

        MockUserState userState = guildState.getUser(memberState.getUserId());
        User user = mockUser(userState);

        lenient().when(member.getUser()).thenReturn(user);
        lenient().when(member.getIdLong()).thenReturn(memberState.getUserId());
        lenient().when(member.getId()).thenReturn(String.valueOf(memberState.getUserId()));

        // getRoles() reads from state
        lenient().when(member.getRoles()).thenAnswer(inv -> {
            return memberState.getRoleIds().stream()
                .map(roleId -> guildState.getRole(roleId))
                .filter(roleState -> roleState != null)
                .map(StatefulMockFactory::mockRole)
                .collect(Collectors.toList());
        });

        Guild guild = mockGuild(guildState);
        lenient().when(member.getGuild()).thenReturn(guild);

        return member;
    }

    /**
     * Creates a Role mock backed by state.
     *
     * <p>The returned mock represents a Discord role (permission group) with
     * basic role information (ID and name).
     *
     * <p><b>Stubbed Methods:</b>
     * <ul>
     *   <li>{@code getIdLong()} - Returns role ID from state</li>
     *   <li>{@code getId()} - Returns role ID as string</li>
     *   <li>{@code getName()} - Returns role name from state (e.g., "Moderator")</li>
     * </ul>
     *
     * @param roleState The role state containing role information
     * @return A Mockito mock of {@link Role} backed by the provided state
     */
    public static Role mockRole(MockRoleState roleState) {
        Role role = mock(Role.class);

        lenient().when(role.getIdLong()).thenReturn(roleState.getRoleId());
        lenient().when(role.getId()).thenReturn(String.valueOf(roleState.getRoleId()));
        lenient().when(role.getName()).thenReturn(roleState.getRoleName());

        return role;
    }
}
