package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockChannelStateTest {

    private MockChannelState channel;

    @BeforeEach
    void setUp() {
        channel = new MockChannelState(2001L, "game-actions", 5000L, 12345L);
    }

    @Test
    void constructor_setsChannelIdNameCategoryIdAndGuildId() {
        assertThat(channel.getChannelId()).isEqualTo(2001L);
        assertThat(channel.getChannelName()).isEqualTo("game-actions");
        assertThat(channel.getCategoryId()).isEqualTo(5000L);
        assertThat(channel.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void getMessages_initiallyReturnsEmptyList() {
        List<MockMessageState> messages = channel.getMessages();

        assertThat(messages).isEmpty();
    }

    @Test
    void addMessage_addsMessageToList() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test message");
        channel.addMessage(message);

        List<MockMessageState> messages = channel.getMessages();

        assertThat(messages).containsExactly(message);
    }

    @Test
    void addMessage_addsMultipleMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First message");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second message");
        MockMessageState message3 = new MockMessageState(1000003L, 2001L, 100L, "Third message");

        channel.addMessage(message1);
        channel.addMessage(message2);
        channel.addMessage(message3);

        List<MockMessageState> messages = channel.getMessages();

        assertThat(messages).containsExactly(message1, message2, message3);
    }

    @Test
    void getMessages_returnsMessagesInChronologicalOrder() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, 2001L, 100L, "Third");

        channel.addMessage(message1);
        channel.addMessage(message2);
        channel.addMessage(message3);

        List<MockMessageState> messages = channel.getMessages();

        assertThat(messages).containsExactly(message1, message2, message3);
    }

    @Test
    void getMessages_returnsNewListEachTime() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        channel.addMessage(message);

        List<MockMessageState> messages1 = channel.getMessages();
        List<MockMessageState> messages2 = channel.getMessages();

        assertThat(messages1).isNotSameAs(messages2);
    }

    @Test
    void getMessages_modifyingReturnedListDoesNotAffectState() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        channel.addMessage(message);

        List<MockMessageState> messages = channel.getMessages();
        MockMessageState fakeMessage = new MockMessageState(9999L, 2001L, 100L, "Fake");
        messages.add(fakeMessage);

        assertThat(channel.getMessages()).containsExactly(message);
        assertThat(channel.getMessages()).doesNotContain(fakeMessage);
    }

    @Test
    void getRecentMessages_returnsRequestedNumberOfMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, 2001L, 100L, "Third");
        MockMessageState message4 = new MockMessageState(1000004L, 2001L, 100L, "Fourth");

        channel.addMessage(message1);
        channel.addMessage(message2);
        channel.addMessage(message3);
        channel.addMessage(message4);

        List<MockMessageState> recent = channel.getRecentMessages(2);

        assertThat(recent).containsExactly(message3, message4);
    }

    @Test
    void getRecentMessages_returnsAllMessagesIfLimitExceedsMessageCount() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");

        channel.addMessage(message1);
        channel.addMessage(message2);

        List<MockMessageState> recent = channel.getRecentMessages(10);

        assertThat(recent).containsExactly(message1, message2);
    }

    @Test
    void getRecentMessages_returnsEmptyListWhenNoMessages() {
        List<MockMessageState> recent = channel.getRecentMessages(5);

        assertThat(recent).isEmpty();
    }

    @Test
    void getRecentMessages_withLimitZeroReturnsEmptyList() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        channel.addMessage(message);

        List<MockMessageState> recent = channel.getRecentMessages(0);

        assertThat(recent).isEmpty();
    }

    @Test
    void getRecentMessages_withLimitOneReturnsLastMessage() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, 2001L, 100L, "Third");

        channel.addMessage(message1);
        channel.addMessage(message2);
        channel.addMessage(message3);

        List<MockMessageState> recent = channel.getRecentMessages(1);

        assertThat(recent).containsExactly(message3);
    }

    @Test
    void clearMessages_removesAllMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");

        channel.addMessage(message1);
        channel.addMessage(message2);

        channel.clearMessages();

        assertThat(channel.getMessages()).isEmpty();
    }

    @Test
    void clearMessages_onEmptyChannelDoesNothing() {
        channel.clearMessages();

        assertThat(channel.getMessages()).isEmpty();
    }

    @Test
    void clearMessages_allowsNewMessagesAfterClearing() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "Before clear");
        channel.addMessage(message1);

        channel.clearMessages();

        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "After clear");
        channel.addMessage(message2);

        assertThat(channel.getMessages()).containsExactly(message2);
    }

    @Test
    void getLatestMessageId_returnsZeroWhenChannelIsEmpty() {
        assertThat(channel.getLatestMessageId()).isEqualTo(0L);
    }

    @Test
    void getLatestMessageId_returnsIdOfMostRecentMessage() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, 2001L, 100L, "Third");

        channel.addMessage(message1);
        channel.addMessage(message2);
        channel.addMessage(message3);

        assertThat(channel.getLatestMessageId()).isEqualTo(1000003L);
    }

    @Test
    void getLatestMessageId_returnsIdOfOnlyMessage() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Only message");
        channel.addMessage(message);

        assertThat(channel.getLatestMessageId()).isEqualTo(1000001L);
    }

    @Test
    void removeMessage_removesMessageById() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, 2001L, 100L, "Third");

        channel.addMessage(message1);
        channel.addMessage(message2);
        channel.addMessage(message3);

        boolean removed = channel.removeMessage(1000002L);

        assertThat(removed).isTrue();
        assertThat(channel.getMessages()).containsExactly(message1, message3);
    }

    @Test
    void removeMessage_returnsFalseWhenMessageNotFound() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Test");
        channel.addMessage(message);

        boolean removed = channel.removeMessage(9999L);

        assertThat(removed).isFalse();
        assertThat(channel.getMessages()).containsExactly(message);
    }

    @Test
    void removeMessage_returnsFalseOnEmptyChannel() {
        boolean removed = channel.removeMessage(1000001L);

        assertThat(removed).isFalse();
    }

    @Test
    void removeMessage_updatesLatestMessageId() {
        MockMessageState message1 = new MockMessageState(1000001L, 2001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2001L, 100L, "Second");

        channel.addMessage(message1);
        channel.addMessage(message2);

        assertThat(channel.getLatestMessageId()).isEqualTo(1000002L);

        channel.removeMessage(1000002L);

        assertThat(channel.getLatestMessageId()).isEqualTo(1000001L);
    }

    @Test
    void removeMessage_resultsInZeroIdWhenLastMessageRemoved() {
        MockMessageState message = new MockMessageState(1000001L, 2001L, 100L, "Only message");
        channel.addMessage(message);

        channel.removeMessage(1000001L);

        assertThat(channel.getLatestMessageId()).isEqualTo(0L);
        assertThat(channel.getMessages()).isEmpty();
    }
}
