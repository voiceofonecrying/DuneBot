package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockInteractionHookStateTest {

    private MockInteractionHookState hookState;
    private long interactionId;

    @BeforeEach
    void setUp() {
        interactionId = 8000001L;
        hookState = new MockInteractionHookState(interactionId);
    }

    @Test
    void constructor_setsInteractionId() {
        assertThat(hookState.getInteractionId()).isEqualTo(interactionId);
    }

    @Test
    void constructor_setsDeferredToFalse() {
        assertThat(hookState.isDeferred()).isFalse();
    }

    @Test
    void constructor_setsEphemeralToFalse() {
        assertThat(hookState.isEphemeral()).isFalse();
    }

    @Test
    void constructor_initializesEmptySentMessagesList() {
        assertThat(hookState.getSentMessages()).isEmpty();
    }

    @Test
    void constructor_setsOriginalMessageToNull() {
        assertThat(hookState.getOriginalMessage()).isNull();
    }

    @Test
    void getInteractionId_returnsCorrectId() {
        assertThat(hookState.getInteractionId()).isEqualTo(8000001L);
    }

    @Test
    void isDeferred_returnsFalseByDefault() {
        assertThat(hookState.isDeferred()).isFalse();
    }

    @Test
    void setDeferred_updatesDeferredState() {
        hookState.setDeferred(true);

        assertThat(hookState.isDeferred()).isTrue();
    }

    @Test
    void setDeferred_canBeToggledMultipleTimes() {
        hookState.setDeferred(true);
        assertThat(hookState.isDeferred()).isTrue();

        hookState.setDeferred(false);
        assertThat(hookState.isDeferred()).isFalse();

        hookState.setDeferred(true);
        assertThat(hookState.isDeferred()).isTrue();
    }

    @Test
    void isEphemeral_returnsFalseByDefault() {
        assertThat(hookState.isEphemeral()).isFalse();
    }

    @Test
    void setEphemeral_updatesEphemeralState() {
        hookState.setEphemeral(true);

        assertThat(hookState.isEphemeral()).isTrue();
    }

    @Test
    void setEphemeral_canBeToggledMultipleTimes() {
        hookState.setEphemeral(true);
        assertThat(hookState.isEphemeral()).isTrue();

        hookState.setEphemeral(false);
        assertThat(hookState.isEphemeral()).isFalse();
    }

    @Test
    void addSentMessage_addsMessageToList() {
        MockMessageState message = new MockMessageState(1000001L, 2000001L, 100L, "Test message");
        hookState.addSentMessage(message);

        List<MockMessageState> sentMessages = hookState.getSentMessages();

        assertThat(sentMessages).hasSize(1);
        assertThat(sentMessages.get(0)).isEqualTo(message);
    }

    @Test
    void addSentMessage_addsMultipleMessages() {
        MockMessageState message1 = new MockMessageState(1000001L, 2000001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2000001L, 100L, "Second");
        MockMessageState message3 = new MockMessageState(1000003L, 2000001L, 100L, "Third");

        hookState.addSentMessage(message1);
        hookState.addSentMessage(message2);
        hookState.addSentMessage(message3);

        List<MockMessageState> sentMessages = hookState.getSentMessages();

        assertThat(sentMessages).hasSize(3);
        assertThat(sentMessages).containsExactly(message1, message2, message3);
    }

    @Test
    void getSentMessages_returnsEmptyListInitially() {
        assertThat(hookState.getSentMessages()).isEmpty();
    }

    @Test
    void getSentMessages_returnsNewListEachTime() {
        MockMessageState message = new MockMessageState(1000001L, 2000001L, 100L, "Test");
        hookState.addSentMessage(message);

        List<MockMessageState> messages1 = hookState.getSentMessages();
        List<MockMessageState> messages2 = hookState.getSentMessages();

        assertThat(messages1).isNotSameAs(messages2);
    }

    @Test
    void getSentMessages_modifyingReturnedListDoesNotAffectState() {
        MockMessageState message = new MockMessageState(1000001L, 2000001L, 100L, "Test");
        hookState.addSentMessage(message);

        List<MockMessageState> sentMessages = hookState.getSentMessages();
        MockMessageState fakeMessage = new MockMessageState(9999L, 2000001L, 100L, "Fake");
        sentMessages.add(fakeMessage);

        assertThat(hookState.getSentMessages()).hasSize(1);
        assertThat(hookState.getSentMessages()).doesNotContain(fakeMessage);
    }

    @Test
    void getOriginalMessage_returnsNullInitially() {
        assertThat(hookState.getOriginalMessage()).isNull();
    }

    @Test
    void setOriginalMessage_setsTheMessage() {
        MockMessageState originalMessage = new MockMessageState(1000001L, 2000001L, 100L, "Original");
        hookState.setOriginalMessage(originalMessage);

        assertThat(hookState.getOriginalMessage()).isEqualTo(originalMessage);
        assertThat(hookState.getOriginalMessage().getContent()).isEqualTo("Original");
    }

    @Test
    void setOriginalMessage_canBeChanged() {
        MockMessageState message1 = new MockMessageState(1000001L, 2000001L, 100L, "First");
        MockMessageState message2 = new MockMessageState(1000002L, 2000001L, 100L, "Second");

        hookState.setOriginalMessage(message1);
        hookState.setOriginalMessage(message2);

        assertThat(hookState.getOriginalMessage()).isEqualTo(message2);
    }

    @Test
    void hasSentMessages_returnsFalseInitially() {
        assertThat(hookState.hasSentMessages()).isFalse();
    }

    @Test
    void hasSentMessages_returnsTrueAfterAddingMessage() {
        MockMessageState message = new MockMessageState(1000001L, 2000001L, 100L, "Test");
        hookState.addSentMessage(message);

        assertThat(hookState.hasSentMessages()).isTrue();
    }

    @Test
    void hookState_canHaveBothOriginalAndSentMessages() {
        MockMessageState originalMessage = new MockMessageState(1000001L, 2000001L, 100L, "Original");
        MockMessageState followUpMessage = new MockMessageState(1000002L, 2000001L, 100L, "Follow-up");

        hookState.setOriginalMessage(originalMessage);
        hookState.addSentMessage(followUpMessage);

        assertThat(hookState.getOriginalMessage()).isEqualTo(originalMessage);
        assertThat(hookState.getSentMessages()).containsExactly(followUpMessage);
    }

    @Test
    void hookState_canBeDeferredAndEphemeral() {
        hookState.setDeferred(true);
        hookState.setEphemeral(true);

        assertThat(hookState.isDeferred()).isTrue();
        assertThat(hookState.isEphemeral()).isTrue();
    }

    @Test
    void hookState_tracksAllStateIndependently() {
        hookState.setDeferred(true);
        hookState.setEphemeral(true);
        hookState.setOriginalMessage(new MockMessageState(1000001L, 2000001L, 100L, "Original"));
        hookState.addSentMessage(new MockMessageState(1000002L, 2000001L, 100L, "Follow-up"));

        assertThat(hookState.isDeferred()).isTrue();
        assertThat(hookState.isEphemeral()).isTrue();
        assertThat(hookState.getOriginalMessage()).isNotNull();
        assertThat(hookState.hasSentMessages()).isTrue();
    }
}
