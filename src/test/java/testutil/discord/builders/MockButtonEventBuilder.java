package testutil.discord.builders;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import testutil.discord.StatefulMockFactory;
import testutil.discord.state.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Builder for creating mock ButtonInteractionEvent objects backed by stateful mocks.
 *
 * <p>This builder creates realistic mock button events that can be used in E2E tests.
 * All mocks are backed by the stateful mock infrastructure, so interactions
 * with Discord entities (Guild, Member, etc.) persist state correctly.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockDiscordServer server = MockDiscordServer.create();
 * MockGuildState guildState = server.createGuild(12345L, "Test Server");
 * MockUserState user = guildState.createUser("TestUser");
 * MockMemberState member = guildState.createMember(user.getUserId());
 * MockChannelState channel = guildState.createTextChannel("test-channel", 0L);
 * MockMessageState message = new MockMessageState(111L, "Choose an option", member);
 * message.addButton(new MockButtonState("btn-confirm", "Confirm", "PRIMARY"));
 *
 * ButtonInteractionEvent event = new MockButtonEventBuilder(guildState)
 *     .setMember(member)
 *     .setButtonId("btn-confirm")
 *     .setChannel(channel)
 *     .setMessage(message)
 *     .build();
 *
 * // Use event in tests
 * buttonManager.onButtonInteraction(event);
 * }</pre>
 */
public class MockButtonEventBuilder {
    private final MockGuildState guildState;
    private final Guild mockGuild;
    private MockMemberState memberState;
    private Member mockMember;
    private String buttonId;
    private MockChannelState channelState;
    private MockThreadChannelState threadChannelState;
    private MockMessageState messageState;

    /**
     * Creates a new builder for a button interaction event.
     *
     * @param guildState The guild state backing this event
     */
    public MockButtonEventBuilder(MockGuildState guildState) {
        this.guildState = guildState;
        this.mockGuild = StatefulMockFactory.mockGuild(guildState);
    }

    /**
     * Sets the member (and user) who clicked this button.
     *
     * @param memberState The member state
     * @return This builder for chaining
     */
    public MockButtonEventBuilder setMember(MockMemberState memberState) {
        this.memberState = memberState;
        this.mockMember = StatefulMockFactory.mockMember(memberState, guildState);
        return this;
    }

    /**
     * Sets the button component ID that was clicked.
     *
     * @param buttonId The button component ID
     * @return This builder for chaining
     */
    public MockButtonEventBuilder setButtonId(String buttonId) {
        this.buttonId = buttonId;
        return this;
    }

    /**
     * Sets the text channel where the button was clicked.
     *
     * @param channelState The channel state
     * @return This builder for chaining
     */
    public MockButtonEventBuilder setChannel(MockChannelState channelState) {
        this.channelState = channelState;
        this.threadChannelState = null;
        return this;
    }

    /**
     * Sets the thread channel where the button was clicked.
     *
     * @param threadChannelState The thread channel state
     * @return This builder for chaining
     */
    public MockButtonEventBuilder setChannel(MockThreadChannelState threadChannelState) {
        this.threadChannelState = threadChannelState;
        this.channelState = null;
        return this;
    }

    /**
     * Sets the message that contained the button.
     *
     * @param messageState The message state
     * @return This builder for chaining
     */
    public MockButtonEventBuilder setMessage(MockMessageState messageState) {
        this.messageState = messageState;
        return this;
    }

    /**
     * Builds the mock ButtonInteractionEvent.
     *
     * @return The mocked event
     */
    @SuppressWarnings("unchecked")
    public ButtonInteractionEvent build() {
        if (buttonId == null) {
            throw new IllegalStateException("Button ID must be set");
        }

        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);

        // Track the channel/thread where responses should go
        MockChannelState responseChannel = channelState;
        MockThreadChannelState responseThread = threadChannelState;

        // Basic event properties
        when(event.getComponentId()).thenReturn(buttonId);
        when(event.getGuild()).thenReturn(mockGuild);

        // Member and user
        if (mockMember != null) {
            // Extract user first to avoid nested stubbing issues
            User mockUser = mockMember.getUser();
            when(event.getMember()).thenReturn(mockMember);
            when(event.getUser()).thenReturn(mockUser);
        }

        // Channel context
        if (threadChannelState != null) {
            // For thread channels
            ThreadChannel mockThreadChannel = StatefulMockFactory.mockThreadChannel(threadChannelState, guildState);

            MessageChannelUnion channelUnion = mock(MessageChannelUnion.class,
                withSettings().extraInterfaces(ThreadChannel.class));

            when(channelUnion.asThreadChannel()).thenReturn(mockThreadChannel);

            // Extract values from mockThreadChannel first
            String channelName = mockThreadChannel.getName();
            long channelId = mockThreadChannel.getIdLong();
            String channelIdStr = mockThreadChannel.getId();
            Guild channelGuild = mockThreadChannel.getGuild();

            // Create parent channel union directly from state
            net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion parentChannelUnion = null;
            MockChannelState parentChannelState = guildState.getChannel(threadChannelState.getParentChannelId());
            if (parentChannelState != null) {
                TextChannel parentChannel = StatefulMockFactory.mockTextChannel(parentChannelState, guildState);

                parentChannelUnion = mock(net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion.class,
                        withSettings().extraInterfaces(TextChannel.class));

                when(parentChannelUnion.asTextChannel()).thenReturn(parentChannel);

                // Extract values from parentChannel
                String parentName = parentChannel.getName();
                long parentId = parentChannel.getIdLong();
                String parentIdStr = parentChannel.getId();
                net.dv8tion.jda.api.entities.channel.concrete.Category parentCategory = parentChannel.getParentCategory();
                Guild parentGuild = parentChannel.getGuild();

                // Make the union act as the TextChannel
                TextChannel unionAsTextChannel = (TextChannel) parentChannelUnion;
                when(unionAsTextChannel.getName()).thenReturn(parentName);
                when(unionAsTextChannel.getIdLong()).thenReturn(parentId);
                when(unionAsTextChannel.getId()).thenReturn(parentIdStr);
                when(unionAsTextChannel.getParentCategory()).thenReturn(parentCategory);
                when(unionAsTextChannel.getGuild()).thenReturn(parentGuild);

                // IMPORTANT: Also configure the mockThreadChannel to return the parent
                when(mockThreadChannel.getParentChannel()).thenReturn(parentChannelUnion);
            }

            // Make the union act as the ThreadChannel
            ThreadChannel channelAsThread = (ThreadChannel) channelUnion;
            when(channelAsThread.getName()).thenReturn(channelName);
            when(channelAsThread.getIdLong()).thenReturn(channelId);
            when(channelAsThread.getId()).thenReturn(channelIdStr);
            when(channelAsThread.getGuild()).thenReturn(channelGuild);
            when(channelAsThread.getParentChannel()).thenReturn(parentChannelUnion);

            when(event.getChannel()).thenReturn(channelUnion);
            when(event.getMessageChannel()).thenReturn(mockThreadChannel);
        } else if (channelState != null) {
            // For text channels
            TextChannel mockChannel = StatefulMockFactory.mockTextChannel(channelState, guildState);

            MessageChannelUnion channelUnion = mock(MessageChannelUnion.class,
                withSettings().extraInterfaces(TextChannel.class));

            when(channelUnion.asTextChannel()).thenReturn(mockChannel);

            // Extract values from mockChannel first
            String channelName = mockChannel.getName();
            long channelId = mockChannel.getIdLong();
            String channelIdStr = mockChannel.getId();
            net.dv8tion.jda.api.entities.channel.concrete.Category parentCategory = mockChannel.getParentCategory();
            Guild channelGuild = mockChannel.getGuild();

            // Make the union act as the TextChannel
            TextChannel channelAsText = (TextChannel) channelUnion;
            when(channelAsText.getName()).thenReturn(channelName);
            when(channelAsText.getIdLong()).thenReturn(channelId);
            when(channelAsText.getId()).thenReturn(channelIdStr);
            when(channelAsText.getParentCategory()).thenReturn(parentCategory);
            when(channelAsText.getGuild()).thenReturn(channelGuild);

            when(event.getChannel()).thenReturn(channelUnion);
            when(event.getMessageChannel()).thenReturn(mockChannel);
        }

        // Message that contained the button
        if (messageState != null) {
            Message mockMessage = mock(Message.class);
            when(mockMessage.getId()).thenReturn(String.valueOf(messageState.getMessageId()));
            when(mockMessage.getIdLong()).thenReturn(messageState.getMessageId());
            when(mockMessage.getContentRaw()).thenReturn(messageState.getContent());

            // Mock delete() to return an AuditableRestAction that removes the message from state
            net.dv8tion.jda.api.requests.restaction.AuditableRestAction<Void> deleteAction =
                mock(net.dv8tion.jda.api.requests.restaction.AuditableRestAction.class);
            when(deleteAction.complete()).thenAnswer(inv -> {
                // Remove message from channel or thread state
                if (responseThread != null) {
                    responseThread.removeMessage(messageState.getMessageId());
                } else if (responseChannel != null) {
                    responseChannel.removeMessage(messageState.getMessageId());
                }
                return null;
            });
            doNothing().when(deleteAction).queue();
            when(mockMessage.delete()).thenReturn(deleteAction);

            when(event.getMessage()).thenReturn(mockMessage);
        }

        // Button component (only needed for event.getButton(), the ID is accessed via event.getComponentId())
        Button mockButton = mock(Button.class);
        when(event.getButton()).thenReturn(mockButton);

        // InteractionHook for deferred replies
        InteractionHook mockHook = mock(InteractionHook.class, invocation -> {
            String methodName = invocation.getMethod().getName();

            if (methodName.equals("editOriginal")) {
                // Handle editing the original deferred reply
                Object arg0 = invocation.getArguments().length > 0 ? invocation.getArguments()[0] : null;

                if (arg0 instanceof String || arg0 == null) {
                    String content = (String) arg0;
                    if (content == null) content = "";

                    long messageId = guildState.getServer().nextMessageId();
                    MockMessageState newMessage = new MockMessageState(messageId,
                        responseThread != null ? responseThread.getThreadId() : responseChannel.getChannelId(),
                        memberState != null ? memberState.getUserId() : 0L,
                        content);

                    if (responseThread != null) {
                        responseThread.addMessage(newMessage);
                    } else if (responseChannel != null) {
                        responseChannel.addMessage(newMessage);
                    }

                    // Return a mock action
                    net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction<Message> action =
                        mock(net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction.class);
                    doNothing().when(action).queue();
                    return action;
                } else if (arg0 instanceof net.dv8tion.jda.api.utils.messages.MessageEditData) {
                    net.dv8tion.jda.api.utils.messages.MessageEditData data = (net.dv8tion.jda.api.utils.messages.MessageEditData) arg0;
                    // Extract content from MessageEditData - this might need reflection
                    String content = "";
                    try {
                        var getContentMethod = data.getClass().getMethod("getContent");
                        Object contentObj = getContentMethod.invoke(data);
                        if (contentObj != null) {
                            content = contentObj.toString();
                        }
                    } catch (Exception e) {
                        // Silently ignore content extraction errors
                    }

                    long messageId = guildState.getServer().nextMessageId();
                    MockMessageState newMessage = new MockMessageState(messageId,
                        responseThread != null ? responseThread.getThreadId() : responseChannel.getChannelId(),
                        memberState != null ? memberState.getUserId() : 0L,
                        content);

                    // TODO: Extract buttons from MessageEditData if needed

                    if (responseThread != null) {
                        responseThread.addMessage(newMessage);
                    } else if (responseChannel != null) {
                        responseChannel.addMessage(newMessage);
                    }

                    // Return a mock action
                    net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction<Message> action =
                        mock(net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction.class);
                    doNothing().when(action).queue();
                    return action;
                }
            } else if (methodName.equals("sendMessage")) {
                Object arg0 = invocation.getArguments().length > 0 ? invocation.getArguments()[0] : null;

                if (arg0 instanceof String || arg0 == null) {
                    String content = (String) arg0;
                    if (content == null) content = "";

                    long messageId = guildState.getServer().nextMessageId();
                    MockMessageState newMessage = new MockMessageState(messageId,
                        responseThread != null ? responseThread.getThreadId() : responseChannel.getChannelId(),
                        memberState != null ? memberState.getUserId() : 0L,
                        content);

                    if (responseThread != null) {
                        responseThread.addMessage(newMessage);
                    } else if (responseChannel != null) {
                        responseChannel.addMessage(newMessage);
                    }

                    // Return a mock action
                    net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction<Message> action =
                        mock(net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction.class);
                    doNothing().when(action).queue();
                    return action;
                } else if (arg0 instanceof MessageCreateData) {
                    MessageCreateData data = (MessageCreateData) arg0;
                    String content = data.getContent() != null ? data.getContent() : "";

                    long messageId = guildState.getServer().nextMessageId();
                    MockMessageState newMessage = new MockMessageState(messageId,
                        responseThread != null ? responseThread.getThreadId() : responseChannel.getChannelId(),
                        memberState != null ? memberState.getUserId() : 0L,
                        content,
                        data.getFiles());

                    // Extract buttons
                    try {
                        var extractMethod = StatefulMockFactory.class.getDeclaredMethod("extractButtonsFromMessageData", MessageCreateData.class);
                        extractMethod.setAccessible(true);
                        var buttons = (java.util.List<MockButtonState>) extractMethod.invoke(null, data);
                        for (MockButtonState button : buttons) {
                            newMessage.addButton(button);
                        }
                    } catch (Exception e) {
                        // Silently ignore button extraction errors
                    }

                    if (responseThread != null) {
                        responseThread.addMessage(newMessage);
                    } else if (responseChannel != null) {
                        responseChannel.addMessage(newMessage);
                    }

                    // Return a mock action
                    net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction<Message> action =
                        mock(net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction.class);
                    doNothing().when(action).queue();
                    return action;
                }
            }

            // Default behavior
            return org.mockito.Mockito.RETURNS_MOCKS.answer(invocation);
        });
        when(event.getHook()).thenReturn(mockHook);


        // Track deferred/replied state
        final boolean[] isDeferred = {false};
        final boolean[] isReplied = {false};

        // deferReply and deferEdit support
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);
        doAnswer(inv -> {
            isDeferred[0] = true;
            return null;
        }).when(mockReplyAction).queue();

        when(event.deferReply()).thenAnswer(inv -> {
            return mockReplyAction;
        });
        when(event.deferReply(anyBoolean())).thenAnswer(inv -> {
            return mockReplyAction;
        });

        // isAcknowledged() checks if interaction has been replied or deferred
        when(event.isAcknowledged()).thenAnswer(inv -> {
            return isDeferred[0] || isReplied[0];
        });

        net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction mockEditAction =
            mock(net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction.class);
        doNothing().when(mockEditAction).queue();
        when(event.deferEdit()).thenReturn(mockEditAction);

        // reply() method for direct responses - add message to state when queued
        when(event.reply(anyString())).thenAnswer(inv -> {
            String content = inv.getArgument(0);

            // Create a mock ReplyCallbackAction that adds the message when queue() is called
            ReplyCallbackAction replyAction = mock(ReplyCallbackAction.class);

            doAnswer(inv2 -> {
                isReplied[0] = true;
                long messageId = guildState.getServer().nextMessageId();
                MockMessageState newMessage = new MockMessageState(messageId,
                    responseThread != null ? responseThread.getThreadId() : responseChannel.getChannelId(),
                    memberState != null ? memberState.getUserId() : 0L,
                    content);

                if (responseThread != null) {
                    responseThread.addMessage(newMessage);
                } else if (responseChannel != null) {
                    responseChannel.addMessage(newMessage);
                }
                return null;
            }).when(replyAction).queue();

            return replyAction;
        });

        // editMessage() method for editing the original message
        when(event.editMessage(anyString())).thenAnswer(inv -> {
            String newContent = inv.getArgument(0);

            // Create a mock MessageEditCallbackAction that updates the message when queue() is called
            net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction editAction =
                mock(net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction.class);

            doAnswer(inv2 -> {
                // If we have a message state, update it
                if (messageState != null) {
                    // Note: MockMessageState doesn't have setContent, so we'd need to replace it
                    // For now, we'll add a new message as a workaround
                    long messageId = guildState.getServer().nextMessageId();
                    MockMessageState editedMessage = new MockMessageState(messageId,
                        responseThread != null ? responseThread.getThreadId() : responseChannel.getChannelId(),
                        memberState != null ? memberState.getUserId() : 0L,
                        newContent);

                    if (responseThread != null) {
                        responseThread.addMessage(editedMessage);
                    } else if (responseChannel != null) {
                        responseChannel.addMessage(editedMessage);
                    }
                }
                return null;
            }).when(editAction).queue();

            return editAction;
        });

        return event;
    }
}
