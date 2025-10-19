package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockThreadChannelStateTest {

    private MockThreadChannelState thread;

    @BeforeEach
    void setUp() {
        thread = new MockThreadChannelState(6000001L, "strategy-discussion", 2000001L, 12345L);
    }

    @Test
    void constructor_setsThreadIdNameParentChannelIdAndGuildId() {
        assertThat(thread.getThreadId()).isEqualTo(6000001L);
        assertThat(thread.getThreadName()).isEqualTo("strategy-discussion");
        assertThat(thread.getParentChannelId()).isEqualTo(2000001L);
        assertThat(thread.getGuildId()).isEqualTo(12345L);
    }

    @Test
    void constructor_setsDefaultAutoArchiveDuration() {
        assertThat(thread.getAutoArchiveDuration()).isEqualTo(60);
    }

    @Test
    void constructor_setsArchivedToFalse() {
        assertThat(thread.isArchived()).isFalse();
    }

    @Test
    void constructor_setsCreatedTimestamp() {
        assertThat(thread.getCreatedTimestamp()).isNotNull();
    }

    @Test
    void setAutoArchiveDuration_updatesValue() {
        thread.setAutoArchiveDuration(1440);

        assertThat(thread.getAutoArchiveDuration()).isEqualTo(1440);
    }

    @Test
    void setArchived_updatesArchivedStatus() {
        thread.setArchived(true);

        assertThat(thread.isArchived()).isTrue();
    }

    @Test
    void getMessages_initiallyReturnsEmptyList() {
        List<MockMessageState> messages = thread.getMessages();

        assertThat(messages).isEmpty();
    }

    @Test
    void addMessage_addsMessageToThreadHistory() {
        MockMessageState message = new MockMessageState(1000001L, thread.getThreadId(), 100L, "Test message");
        thread.addMessage(message);

        List<MockMessageState> messages = thread.getMessages();

        assertThat(messages).containsExactly(message);
    }

    @Test
    void addMessage_addsMultipleMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, thread.getThreadId(), 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, thread.getThreadId(), 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, thread.getThreadId(), 100L, "Third");

        thread.addMessage(message1);
        thread.addMessage(message2);
        thread.addMessage(message3);

        List<MockMessageState> messages = thread.getMessages();

        assertThat(messages).containsExactly(message1, message2, message3);
    }

    @Test
    void getMessages_returnsNewListEachTime() {
        MockMessageState message = new MockMessageState(1000001L, thread.getThreadId(), 100L, "Test");
        thread.addMessage(message);

        List<MockMessageState> messages1 = thread.getMessages();
        List<MockMessageState> messages2 = thread.getMessages();

        assertThat(messages1).isNotSameAs(messages2);
    }

    @Test
    void getMessages_modifyingReturnedListDoesNotAffectState() {
        MockMessageState message = new MockMessageState(1000001L, thread.getThreadId(), 100L, "Test");
        thread.addMessage(message);

        List<MockMessageState> messages = thread.getMessages();
        MockMessageState fakeMessage = new MockMessageState(9999L, thread.getThreadId(), 100L, "Fake");
        messages.add(fakeMessage);

        assertThat(thread.getMessages()).containsExactly(message);
        assertThat(thread.getMessages()).doesNotContain(fakeMessage);
    }

    @Test
    void getRecentMessages_returnsRequestedNumberOfMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, thread.getThreadId(), 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, thread.getThreadId(), 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, thread.getThreadId(), 100L, "Third");
        MockMessageState message4 = new MockMessageState(1000004L, thread.getThreadId(), 100L, "Fourth");

        thread.addMessage(message1);
        thread.addMessage(message2);
        thread.addMessage(message3);
        thread.addMessage(message4);

        List<MockMessageState> recent = thread.getRecentMessages(2);

        assertThat(recent).containsExactly(message3, message4);
    }

    @Test
    void getRecentMessages_returnsAllMessagesIfLimitExceedsMessageCount() {
        MockMessageState message1 = new MockMessageState(1000001L, thread.getThreadId(), 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, thread.getThreadId(), 100L, "Second");

        thread.addMessage(message1);
        thread.addMessage(message2);

        List<MockMessageState> recent = thread.getRecentMessages(10);

        assertThat(recent).containsExactly(message1, message2);
    }

    @Test
    void getRecentMessages_withLimitZeroReturnsEmptyList() {
        MockMessageState message = new MockMessageState(1000001L, thread.getThreadId(), 100L, "Test");
        thread.addMessage(message);

        List<MockMessageState> recent = thread.getRecentMessages(0);

        assertThat(recent).isEmpty();
    }

    @Test
    void clearMessages_removesAllMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, thread.getThreadId(), 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, thread.getThreadId(), 100L, "Second");

        thread.addMessage(message1);
        thread.addMessage(message2);

        thread.clearMessages();

        assertThat(thread.getMessages()).isEmpty();
    }

    @Test
    void getMemberIds_initiallyReturnsEmptyList() {
        List<Long> memberIds = thread.getMemberIds();

        assertThat(memberIds).isEmpty();
    }

    @Test
    void addMember_addsMemberToThread() {
        thread.addMember(100L);

        List<Long> memberIds = thread.getMemberIds();

        assertThat(memberIds).containsExactly(100L);
    }

    @Test
    void addMember_addsMultipleMembers() {
        thread.addMember(100L);
        thread.addMember(200L);
        thread.addMember(300L);

        List<Long> memberIds = thread.getMemberIds();

        assertThat(memberIds).containsExactly(100L, 200L, 300L);
    }

    @Test
    void addMember_ignoresDuplicates() {
        thread.addMember(100L);
        thread.addMember(100L);
        thread.addMember(100L);

        List<Long> memberIds = thread.getMemberIds();

        assertThat(memberIds).containsExactly(100L);
    }

    @Test
    void removeMember_removesMemberFromThread() {
        thread.addMember(100L);
        thread.addMember(200L);

        thread.removeMember(100L);

        List<Long> memberIds = thread.getMemberIds();

        assertThat(memberIds).containsExactly(200L);
    }

    @Test
    void removeMember_onNonexistentMember_doesNothing() {
        thread.addMember(100L);

        thread.removeMember(9999L);

        List<Long> memberIds = thread.getMemberIds();

        assertThat(memberIds).containsExactly(100L);
    }

    @Test
    void hasMember_returnsTrueForAddedMember() {
        thread.addMember(100L);

        assertThat(thread.hasMember(100L)).isTrue();
    }

    @Test
    void hasMember_returnsFalseForNonMember() {
        thread.addMember(100L);

        assertThat(thread.hasMember(200L)).isFalse();
    }

    @Test
    void hasMember_returnsFalseAfterRemovingMember() {
        thread.addMember(100L);
        thread.removeMember(100L);

        assertThat(thread.hasMember(100L)).isFalse();
    }

    @Test
    void getMemberIds_returnsNewListEachTime() {
        thread.addMember(100L);

        List<Long> memberIds1 = thread.getMemberIds();
        List<Long> memberIds2 = thread.getMemberIds();

        assertThat(memberIds1).isNotSameAs(memberIds2);
    }

    @Test
    void getMemberIds_modifyingReturnedListDoesNotAffectState() {
        thread.addMember(100L);

        List<Long> memberIds = thread.getMemberIds();
        memberIds.add(9999L);

        assertThat(thread.getMemberIds()).containsExactly(100L);
        assertThat(thread.getMemberIds()).doesNotContain(9999L);
    }
}
