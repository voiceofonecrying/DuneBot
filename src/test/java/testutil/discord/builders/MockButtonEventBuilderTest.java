package testutil.discord.builders;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.state.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for MockButtonEventBuilder.
 * Verifies that button interaction events are correctly constructed with all required mocks.
 */
@DisplayName("MockButtonEventBuilder Unit Tests")
class MockButtonEventBuilderTest {

    private MockDiscordServer server;
    private MockGuildState guildState;
    private MockMemberState memberState;
    private MockChannelState channelState;
    private MockThreadChannelState threadState;
    private MockMessageState messageState;

    @BeforeEach
    void setUp() {
        // Create test infrastructure
        server = MockDiscordServer.create();
        guildState = server.createGuild(123456789L, "Test Guild");

        // Create a user and member
        MockUserState userState = guildState.createUser("TestUser");
        memberState = guildState.createMember(userState.getUserId());

        // Create a channel and thread
        channelState = guildState.createTextChannel("test-channel", 0L);
        threadState = guildState.createThread("test-thread", channelState.getChannelId());

        // Create a message with buttons
        messageState = new MockMessageState(111L, threadState.getThreadId(), memberState.getUserId(), "Click a button");
        messageState.addButton(new MockButtonState("btn-test", "Test Button", "PRIMARY", false));
    }

    @Test
    @DisplayName("Should build button event with required button ID")
    void shouldBuildButtonEventWithRequiredButtonId() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setButtonId("btn-test")
                .build();

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getComponentId()).isEqualTo("btn-test");
        assertThat(event.getGuild()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when button ID is not set")
    void shouldThrowExceptionWhenButtonIdNotSet() {
        // When/Then
        assertThatThrownBy(() ->
            new MockButtonEventBuilder(guildState).build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessage("Button ID must be set");
    }

    @Test
    @DisplayName("Should set member and extract user correctly")
    void shouldSetMemberAndExtractUserCorrectly() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .build();

        // Then
        assertThat(event.getMember()).isNotNull();
        assertThat(event.getUser()).isNotNull();
        Member member = event.getMember();
        User user = event.getUser();
        assertThat(user.getName()).isEqualTo("TestUser");
    }

    @Test
    @DisplayName("Should set text channel context correctly")
    void shouldSetTextChannelContextCorrectly() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .setChannel(channelState)
                .build();

        // Then
        MessageChannelUnion channel = event.getChannel();
        assertThat(channel).isNotNull();
        TextChannel textChannel = channel.asTextChannel();
        assertThat(textChannel.getName()).isEqualTo("test-channel");
        assertThat(textChannel.getIdLong()).isEqualTo(channelState.getChannelId());
    }

    @Test
    @DisplayName("Should set thread channel context with parent channel")
    void shouldSetThreadChannelContextWithParentChannel() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .setChannel(threadState)
                .build();

        // Then
        MessageChannelUnion channel = event.getChannel();
        assertThat(channel).isNotNull();

        // Verify thread channel properties
        ThreadChannel threadChannel = channel.asThreadChannel();
        assertThat(threadChannel.getName()).isEqualTo("test-thread");
        assertThat(threadChannel.getIdLong()).isEqualTo(threadState.getThreadId());

        // Verify parent channel is properly mocked
        var parentChannel = threadChannel.getParentChannel();
        assertThat(parentChannel).isNotNull();
        TextChannel parentText = parentChannel.asTextChannel();
        assertThat(parentText.getName()).isEqualTo("test-channel");
        assertThat(parentText.getIdLong()).isEqualTo(channelState.getChannelId());
    }

    @Test
    @DisplayName("Should set message containing the button")
    void shouldSetMessageContainingButton() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .setMessage(messageState)
                .build();

        // Then
        Message message = event.getMessage();
        assertThat(message).isNotNull();
        assertThat(message.getIdLong()).isEqualTo(messageState.getMessageId());
        assertThat(message.getContentRaw()).isEqualTo(messageState.getContent());
    }

    @Test
    @DisplayName("Should provide button component")
    void shouldProvideButtonComponent() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .build();

        // Then
        assertThat(event.getButton()).isNotNull();
    }

    @Test
    @DisplayName("Should provide interaction hook for deferred replies")
    void shouldProvideInteractionHookForDeferredReplies() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .build();

        // Then
        InteractionHook hook = event.getHook();
        assertThat(hook).isNotNull();
    }

    @Test
    @DisplayName("Should mock deferReply without arguments")
    void shouldMockDeferReplyWithoutArguments() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .build();

        // Then
        ReplyCallbackAction action = event.deferReply();
        assertThat(action).isNotNull();

        // Should be able to call queue without exception
        action.queue();
    }

    @Test
    @DisplayName("Should mock deferReply with ephemeral flag")
    void shouldMockDeferReplyWithEphemeralFlag() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .build();

        // Then
        ReplyCallbackAction action = event.deferReply(true);
        assertThat(action).isNotNull();

        // Should be able to call queue without exception
        action.queue();
    }

    @Test
    @DisplayName("Should mock deferEdit")
    void shouldMockDeferEdit() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-test")
                .build();

        // Then
        var editAction = event.deferEdit();
        assertThat(editAction).isNotNull();
    }

    @Test
    @DisplayName("Should build complete event with all optional fields")
    void shouldBuildCompleteEventWithAllOptionalFields() {
        // When
        ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
                .setMember(memberState)
                .setButtonId("btn-complete")
                .setChannel(threadState)
                .setMessage(messageState)
                .build();

        // Then - verify all components are set
        assertThat(event.getComponentId()).isEqualTo("btn-complete");
        assertThat(event.getGuild()).isNotNull();
        assertThat(event.getMember()).isNotNull();
        assertThat(event.getUser()).isNotNull();
        assertThat(event.getChannel()).isNotNull();
        assertThat(event.getMessage()).isNotNull();
        assertThat(event.getButton()).isNotNull();
        assertThat(event.getHook()).isNotNull();

        // Verify thread channel with parent
        ThreadChannel thread = event.getChannel().asThreadChannel();
        assertThat(thread.getName()).isEqualTo("test-thread");
        var parentChannel = thread.getParentChannel();
        assertThat(parentChannel).isNotNull();
        assertThat(parentChannel.asTextChannel().getName()).isEqualTo("test-channel");
    }
}