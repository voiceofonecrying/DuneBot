package testutil.discord.builders;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.state.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for MockSlashCommandEventBuilder to ensure it correctly creates
 * mock slash command events with all options and properties.
 */
@DisplayName("MockSlashCommandEventBuilder Tests")
class MockSlashCommandEventBuilderTest {

    private MockDiscordServer server;
    private MockGuildState guildState;
    private MockUserState userState;
    private MockMemberState memberState;
    private MockRoleState roleState;
    private MockChannelState channelState;
    private MockCategoryState categoryState;

    @BeforeEach
    void setUp() {
        server = MockDiscordServer.create();
        guildState = server.createGuild(123456789L, "Test Server");

        userState = guildState.createUser("TestUser");
        memberState = guildState.createMember(userState.getUserId());
        roleState = guildState.createRole("TestRole");

        categoryState = guildState.createCategory("test-category");
        channelState = guildState.createTextChannel("test-channel", categoryState.getCategoryId());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when command name is not set")
    void shouldThrowWhenCommandNameNotSet() {
        MockSlashCommandEventBuilder builder = new MockSlashCommandEventBuilder(guildState);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Command name must be set");
    }

    @Test
    @DisplayName("Should create event with command name")
    void shouldCreateEventWithCommandName() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        assertThat(event.getName()).isEqualTo("test-command");
        assertThat(event.getCommandString()).isEqualTo("/test-command");
    }

    @Test
    @DisplayName("Should create event with guild")
    void shouldCreateEventWithGuild() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        Guild guild = event.getGuild();
        assertThat(guild).isNotNull();
        assertThat(guild.getIdLong()).isEqualTo(123456789L);
        assertThat(guild.getName()).isEqualTo("Test Server");
    }

    @Test
    @DisplayName("Should create event with member and user")
    void shouldCreateEventWithMemberAndUser() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .setMember(memberState)
                .build();

        Member member = event.getMember();
        assertThat(member).isNotNull();
        assertThat(member.getIdLong()).isEqualTo(memberState.getUserId());

        User user = event.getUser();
        assertThat(user).isNotNull();
        assertThat(user.getIdLong()).isEqualTo(userState.getUserId());
        assertThat(user.getName()).isEqualTo("TestUser");
    }

    @Test
    @DisplayName("Should create event with string option")
    void shouldCreateEventWithStringOption() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addStringOption("name", "Test Value")
                .build();

        OptionMapping option = event.getOption("name");
        assertThat(option).isNotNull();
        assertThat(option.getAsString()).isEqualTo("Test Value");
    }

    @Test
    @DisplayName("Should create event with multiple string options")
    void shouldCreateEventWithMultipleStringOptions() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addStringOption("name", "Game #1")
                .addStringOption("description", "A test game")
                .addStringOption("type", "standard")
                .build();

        OptionMapping nameOption = event.getOption("name");
        assertThat(nameOption).isNotNull();
        assertThat(nameOption.getAsString()).isEqualTo("Game #1");

        OptionMapping descOption = event.getOption("description");
        assertThat(descOption).isNotNull();
        assertThat(descOption.getAsString()).isEqualTo("A test game");

        OptionMapping typeOption = event.getOption("type");
        assertThat(typeOption).isNotNull();
        assertThat(typeOption.getAsString()).isEqualTo("standard");
    }

    @Test
    @DisplayName("Should create event with role option")
    void shouldCreateEventWithRoleOption() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addRoleOption("role", roleState)
                .build();

        OptionMapping option = event.getOption("role");
        assertThat(option).isNotNull();

        Role role = option.getAsRole();
        assertThat(role).isNotNull();
        assertThat(role.getIdLong()).isEqualTo(roleState.getRoleId());
        assertThat(role.getName()).isEqualTo("TestRole");
    }

    @Test
    @DisplayName("Should create event with multiple role options")
    void shouldCreateEventWithMultipleRoleOptions() {
        MockRoleState gameRole = guildState.createRole("Game");
        MockRoleState modRole = guildState.createRole("Moderator");

        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addRoleOption("gamerole", gameRole)
                .addRoleOption("modrole", modRole)
                .build();

        OptionMapping gameOption = event.getOption("gamerole");
        assertThat(gameOption).isNotNull();
        assertThat(gameOption.getAsRole().getName()).isEqualTo("Game");

        OptionMapping modOption = event.getOption("modrole");
        assertThat(modOption).isNotNull();
        assertThat(modOption.getAsRole().getName()).isEqualTo("Moderator");
    }

    @Test
    @DisplayName("Should create event with user option")
    void shouldCreateEventWithUserOption() {
        MockUserState targetUser = guildState.createUser("TargetUser");
        guildState.createMember(targetUser.getUserId());

        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addUserOption("target", targetUser)
                .build();

        OptionMapping option = event.getOption("target");
        assertThat(option).isNotNull();

        User user = option.getAsUser();
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("TargetUser");

        Member member = option.getAsMember();
        assertThat(member).isNotNull();
        assertThat(member.getUser().getName()).isEqualTo("TargetUser");
    }

    @Test
    @DisplayName("Should create event with channel context")
    void shouldCreateEventWithChannelContext() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .setChannel(channelState)
                .build();

        assertThat(event.getChannel()).isNotNull();

        TextChannel channel = event.getChannel().asTextChannel();
        assertThat(channel).isNotNull();
        assertThat(channel.getName()).isEqualTo("test-channel");
        assertThat(channel.getIdLong()).isEqualTo(channelState.getChannelId());
    }

    @Test
    @DisplayName("Should create event with interaction hook for deferred replies")
    void shouldCreateEventWithInteractionHook() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        InteractionHook hook = event.getHook();
        assertThat(hook).isNotNull();

        // Should be able to edit original message without errors
        assertThat(hook.editOriginal("Updated message")).isNotNull();
    }

    @Test
    @DisplayName("Should support deferReply without errors")
    void shouldSupportDeferReply() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        // Should not throw when deferring reply
        assertThat(event.deferReply(true)).isNotNull();
        assertThat(event.deferReply(false)).isNotNull();
    }

    @Test
    @DisplayName("Should create event with mixed option types")
    void shouldCreateEventWithMixedOptionTypes() {
        MockUserState targetUser = guildState.createUser("TargetUser");
        guildState.createMember(targetUser.getUserId());

        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("complex-command")
                .addStringOption("name", "Test Game")
                .addRoleOption("role", roleState)
                .addUserOption("user", targetUser)
                .addStringOption("description", "A description")
                .build();

        // Verify all options are present and correct
        assertThat(event.getOption("name").getAsString()).isEqualTo("Test Game");
        assertThat(event.getOption("role").getAsRole().getName()).isEqualTo("TestRole");
        assertThat(event.getOption("user").getAsUser().getName()).isEqualTo("TargetUser");
        assertThat(event.getOption("description").getAsString()).isEqualTo("A description");
    }

    @Test
    @DisplayName("Should return null for non-existent options")
    void shouldReturnNullForNonExistentOptions() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addStringOption("existing", "value")
                .build();

        assertThat(event.getOption("existing")).isNotNull();
        assertThat(event.getOption("nonexistent")).isNull();
    }

    @Test
    @DisplayName("Should create event without member when not set")
    void shouldCreateEventWithoutMemberWhenNotSet() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        assertThat(event.getMember()).isNull();
        assertThat(event.getUser()).isNull();
    }

    @Test
    @DisplayName("Should create event without channel when not set")
    void shouldCreateEventWithoutChannelWhenNotSet() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        assertThat(event.getChannel()).isNull();
    }

    @Test
    @DisplayName("Should support fluent builder pattern")
    void shouldSupportFluentBuilderPattern() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .setMember(memberState)
                .setChannel(channelState)
                .addStringOption("option1", "value1")
                .addStringOption("option2", "value2")
                .addRoleOption("role", roleState)
                .build();

        assertThat(event.getName()).isEqualTo("test-command");
        assertThat(event.getMember()).isNotNull();
        assertThat(event.getChannel()).isNotNull();
        assertThat(event.getOption("option1")).isNotNull();
        assertThat(event.getOption("option2")).isNotNull();
        assertThat(event.getOption("role")).isNotNull();
    }

    @Test
    @DisplayName("Should create independent events using separate builders")
    void shouldCreateIndependentEventsUsingSeparateBuilders() {
        SlashCommandInteractionEvent event1 = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("command1")
                .addStringOption("name", "Event 1")
                .build();

        SlashCommandInteractionEvent event2 = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("command2")
                .addStringOption("name", "Event 2")
                .build();

        // Each event should have its own independent state
        assertThat(event1.getName()).isEqualTo("command1");
        assertThat(event2.getName()).isEqualTo("command2");

        // Each should have their own option values
        assertThat(event1.getOption("name").getAsString()).isEqualTo("Event 1");
        assertThat(event2.getOption("name").getAsString()).isEqualTo("Event 2");
    }

    @Test
    @DisplayName("Should handle member with roles correctly")
    void shouldHandleMemberWithRolesCorrectly() {
        MockRoleState role1 = guildState.createRole("Role1");
        MockRoleState role2 = guildState.createRole("Role2");

        memberState.addRole(role1.getRoleId());
        memberState.addRole(role2.getRoleId());

        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .setMember(memberState)
                .build();

        Member member = event.getMember();
        assertThat(member).isNotNull();
        assertThat(member.getRoles()).hasSize(2);
        assertThat(member.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder("Role1", "Role2");
    }

    @Test
    @DisplayName("Should create events for different guilds")
    void shouldCreateEventsForDifferentGuilds() {
        MockGuildState guild2State = server.createGuild(987654321L, "Second Guild");

        SlashCommandInteractionEvent event1 = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .build();

        SlashCommandInteractionEvent event2 = new MockSlashCommandEventBuilder(guild2State)
                .setCommandName("test-command")
                .build();

        assertThat(event1.getGuild().getIdLong()).isEqualTo(123456789L);
        assertThat(event1.getGuild().getName()).isEqualTo("Test Server");

        assertThat(event2.getGuild().getIdLong()).isEqualTo(987654321L);
        assertThat(event2.getGuild().getName()).isEqualTo("Second Guild");
    }

    @Test
    @DisplayName("Should handle empty string options")
    void shouldHandleEmptyStringOptions() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addStringOption("empty", "")
                .addStringOption("whitespace", "   ")
                .build();

        assertThat(event.getOption("empty").getAsString()).isEmpty();
        assertThat(event.getOption("whitespace").getAsString()).isEqualTo("   ");
    }

    @Test
    @DisplayName("Should handle special characters in string options")
    void shouldHandleSpecialCharactersInStringOptions() {
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setCommandName("test-command")
                .addStringOption("special", "Test @#$% & <emoji> :test:")
                .build();

        assertThat(event.getOption("special").getAsString())
                .isEqualTo("Test @#$% & <emoji> :test:");
    }
}
