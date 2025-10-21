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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.withSettings;

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
    private String subcommandName;
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
     * Sets the subcommand name (e.g., "faction" for "/setup faction").
     *
     * @param subcommandName The subcommand name
     * @return This builder for chaining
     */
    public MockSlashCommandEventBuilder setSubcommandName(String subcommandName) {
        this.subcommandName = subcommandName;
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
        when(event.getSubcommandName()).thenReturn(subcommandName);
        when(event.getGuild()).thenReturn(mockGuild);

        // Member and user
        if (mockMember != null) {
            // Extract user first to avoid nested stubbing issues
            User mockUser = mockMember.getUser();
            when(event.getMember()).thenReturn(mockMember);
            when(event.getUser()).thenReturn(mockUser);
        }

        // Channel context (can be null)
        // Create a MessageChannelUnion that also implements TextChannel for instanceof checks
        if (mockChannel != null) {
            MessageChannelUnion channelUnion = mock(MessageChannelUnion.class,
                withSettings().extraInterfaces(TextChannel.class));

            // Make the union return our TextChannel when asTextChannel() is called
            when(channelUnion.asTextChannel()).thenReturn(mockChannel);

            // Extract values from mockChannel first to avoid nested stubbing
            String channelName = mockChannel.getName();
            long channelId = mockChannel.getIdLong();
            String channelIdStr = mockChannel.getId();
            net.dv8tion.jda.api.entities.channel.concrete.Category parentCategory = mockChannel.getParentCategory();
            Guild channelGuild = mockChannel.getGuild();

            // Make the union act as the TextChannel for properties
            // Cast to TextChannel to set up its methods
            TextChannel channelAsText = (TextChannel) channelUnion;
            when(channelAsText.getName()).thenReturn(channelName);
            when(channelAsText.getIdLong()).thenReturn(channelId);
            when(channelAsText.getId()).thenReturn(channelIdStr);
            when(channelAsText.getParentCategory()).thenReturn(parentCategory);
            when(channelAsText.getGuild()).thenReturn(channelGuild);

            when(event.getChannel()).thenReturn(channelUnion);
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

        // InteractionHook for deferred replies - using RETURNS_DEEP_STUBS to handle method chains
        InteractionHook mockHook = mock(InteractionHook.class, RETURNS_DEEP_STUBS);
        when(event.getHook()).thenReturn(mockHook);

        // deferReply support
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);
        doNothing().when(mockReplyAction).queue();
        when(event.deferReply(anyBoolean())).thenReturn(mockReplyAction);

        // Command string for debugging
        String commandString = "/" + commandName;
        if (subcommandName != null) {
            commandString += " " + subcommandName;
        }
        when(event.getCommandString()).thenReturn(commandString);

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
