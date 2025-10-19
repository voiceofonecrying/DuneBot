package testutil.discord.builders;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import testutil.discord.StatefulMockFactory;
import testutil.discord.state.*;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builder for creating mock SlashCommandInteractionEvent objects backed by stateful mocks.
 *
 * <p>This builder creates realistic mock events that can be used in E2E tests.
 * All mocks are backed by the stateful mock infrastructure, so interactions
 * with Discord entities (Guild, Member, etc.) persist state correctly.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockDiscordServer server = MockDiscordServer.create();
 * MockGuildState guildState = server.createGuild(12345L, "Test Server");
 * MockRoleState modRole = guildState.createRole("Moderators");
 * MockRoleState gameRole = guildState.createRole("Game #1");
 * MockUserState user = guildState.createUser("TestMod");
 * MockMemberState member = guildState.createMember(user.getUserId());
 * member.addRole(modRole.getRoleId());
 *
 * SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
 *     .setMember(member)
 *     .setCommandName("new-game")
 *     .addStringOption("name", "Test Game #1")
 *     .addRoleOption("gamerole", gameRole)
 *     .addRoleOption("modrole", modRole)
 *     .build();
 *
 * // Use event in tests
 * commandManager.onSlashCommandInteraction(event);
 * }</pre>
 */
public class MockSlashCommandEventBuilder {
    private final MockGuildState guildState;
    private final Guild mockGuild;
    private MockMemberState memberState;
    private Member mockMember;
    private String commandName;
    private final Map<String, Object> options = new HashMap<>();
    private TextChannel mockChannel;

    /**
     * Creates a new builder for a slash command event.
     *
     * @param guildState The guild state backing this event
     */
    public MockSlashCommandEventBuilder(MockGuildState guildState) {
        this.guildState = guildState;
        this.mockGuild = StatefulMockFactory.mockGuild(guildState);
    }

    /**
     * Sets the member (and user) who triggered this slash command.
     *
     * @param memberState The member state
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder setMember(MockMemberState memberState) {
        this.memberState = memberState;
        this.mockMember = StatefulMockFactory.mockMember(memberState, guildState);
        return this;
    }

    /**
     * Sets the command name (e.g., "new-game").
     *
     * @param commandName The command name
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder setCommandName(String commandName) {
        this.commandName = commandName;
        return this;
    }

    /**
     * Adds a string option to the command.
     *
     * @param name The option name
     * @param value The string value
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder addStringOption(String name, String value) {
        options.put(name, value);
        return this;
    }

    /**
     * Adds a role option to the command.
     *
     * @param name The option name
     * @param roleState The role state
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder addRoleOption(String name, MockRoleState roleState) {
        Role mockRole = StatefulMockFactory.mockRole(roleState);
        options.put(name, mockRole);
        return this;
    }

    /**
     * Adds a user option to the command.
     *
     * @param name The option name
     * @param userState The user state
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder addUserOption(String name, MockUserState userState) {
        User mockUser = StatefulMockFactory.mockUser(userState);
        Member mockMember = StatefulMockFactory.mockMember(guildState.getMember(userState.getUserId()), guildState);
        options.put(name, new UserOption(mockUser, mockMember));
        return this;
    }

    /**
     * Sets the channel context for this event.
     *
     * @param channelState The channel state
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder setChannel(MockChannelState channelState) {
        this.mockChannel = StatefulMockFactory.mockTextChannel(channelState, guildState);
        return this;
    }

    /**
     * Builds the mock SlashCommandInteractionEvent.
     *
     * @return The mocked event
     */
    public SlashCommandInteractionEvent build() {
        if (commandName == null) {
            throw new IllegalStateException("Command name must be set");
        }

        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);

        // Basic event properties
        when(event.getName()).thenReturn(commandName);
        when(event.getGuild()).thenReturn(mockGuild);

        // Member and user
        if (mockMember != null) {
            // Extract user first to avoid nested stubbing issues
            User mockUser = mockMember.getUser();
            when(event.getMember()).thenReturn(mockMember);
            when(event.getUser()).thenReturn(mockUser);
        }

        // Channel context (can be null)
        if (mockChannel != null) {
            when(event.getChannel()).thenReturn(mock(MessageChannelUnion.class));
            when(event.getChannel().asTextChannel()).thenReturn(mockChannel);
        }

        // Command options
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String optionName = entry.getKey();
            Object optionValue = entry.getValue();

            OptionMapping mockOption = mock(OptionMapping.class);

            if (optionValue instanceof String) {
                when(mockOption.getAsString()).thenReturn((String) optionValue);
            } else if (optionValue instanceof Role) {
                when(mockOption.getAsRole()).thenReturn((Role) optionValue);
            } else if (optionValue instanceof UserOption) {
                UserOption userOpt = (UserOption) optionValue;
                when(mockOption.getAsUser()).thenReturn(userOpt.user);
                when(mockOption.getAsMember()).thenReturn(userOpt.member);
            }

            when(event.getOption(optionName)).thenReturn(mockOption);
        }

        // InteractionHook for deferred replies
        InteractionHook mockHook = mock(InteractionHook.class);

        @SuppressWarnings("unchecked")
        WebhookMessageEditAction editAction = mock(WebhookMessageEditAction.class);
        doNothing().when(editAction).queue();

        when(mockHook.editOriginal(anyString())).thenReturn(editAction);

        when(event.getHook()).thenReturn(mockHook);

        // deferReply support
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);
        doNothing().when(mockReplyAction).queue();
        when(event.deferReply(anyBoolean())).thenReturn(mockReplyAction);

        // Command string for debugging
        when(event.getCommandString()).thenReturn("/" + commandName);

        return event;
    }

    /**
     * Helper class to hold both User and Member for user options.
     */
    private static class UserOption {
        final User user;
        final Member member;

        UserOption(User user, Member member) {
            this.user = user;
            this.member = member;
        }
    }
}
