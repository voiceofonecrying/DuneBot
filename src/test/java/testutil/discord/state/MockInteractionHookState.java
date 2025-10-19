package testutil.discord.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord interaction hook.
 *
 * <p>Interaction hooks are created when an interaction (button, slash command, etc.)
 * is deferred or replied to. They allow sending follow-up messages and editing
 * the original response.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Interaction ID</b> - Unique identifier for the interaction</li>
 *   <li><b>Deferred</b> - Whether the interaction was deferred</li>
 *   <li><b>Ephemeral</b> - Whether responses should be ephemeral (only visible to user)</li>
 *   <li><b>Sent Messages</b> - Messages sent via this hook</li>
 *   <li><b>Original Message</b> - The initial response message</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockInteractionHookState hookState = new MockInteractionHookState(interactionId);
 * hookState.setDeferred(true);
 * hookState.setEphemeral(false);
 *
 * // Send messages via hook
 * MockMessageState message = new MockMessageState(...);
 * hookState.addSentMessage(message);
 *
 * assertThat(hookState.isDeferred()).isTrue();
 * assertThat(hookState.getSentMessages()).hasSize(1);
 * }</pre>
 *
 * @see MockMessageState
 */
public class MockInteractionHookState {
    private final long interactionId;
    private boolean deferred;
    private boolean ephemeral;
    private final List<MockMessageState> sentMessages = new ArrayList<>();
    private MockMessageState originalMessage;

    /**
     * Creates an interaction hook state.
     *
     * @param interactionId The unique interaction ID
     */
    public MockInteractionHookState(long interactionId) {
        this.interactionId = interactionId;
        this.deferred = false;
        this.ephemeral = false;
    }

    /**
     * Gets the unique ID of this interaction.
     *
     * @return The interaction ID
     */
    public long getInteractionId() {
        return interactionId;
    }

    /**
     * Checks if this interaction was deferred.
     *
     * <p>Deferred interactions show a "thinking..." state while processing.
     *
     * @return {@code true} if deferred, {@code false} otherwise
     */
    public boolean isDeferred() {
        return deferred;
    }

    /**
     * Sets whether this interaction was deferred.
     *
     * @param deferred {@code true} if deferred, {@code false} otherwise
     */
    public void setDeferred(boolean deferred) {
        this.deferred = deferred;
    }

    /**
     * Checks if responses to this interaction are ephemeral.
     *
     * <p>Ephemeral messages are only visible to the user who triggered the interaction.
     *
     * @return {@code true} if ephemeral, {@code false} otherwise
     */
    public boolean isEphemeral() {
        return ephemeral;
    }

    /**
     * Sets whether responses should be ephemeral.
     *
     * @param ephemeral {@code true} for ephemeral messages, {@code false} for public
     */
    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    /**
     * Adds a message sent via this interaction hook.
     *
     * @param message The message to add
     */
    public void addSentMessage(MockMessageState message) {
        sentMessages.add(message);
    }

    /**
     * Gets all messages sent via this interaction hook.
     *
     * @return A new list containing all sent messages (modifications won't affect the hook)
     */
    public List<MockMessageState> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }

    /**
     * Gets the original response message.
     *
     * <p>This is the first message sent in response to the interaction.
     *
     * @return The original message, or {@code null} if not set
     */
    public MockMessageState getOriginalMessage() {
        return originalMessage;
    }

    /**
     * Sets the original response message.
     *
     * @param originalMessage The original message
     */
    public void setOriginalMessage(MockMessageState originalMessage) {
        this.originalMessage = originalMessage;
    }

    /**
     * Checks if any messages have been sent via this hook.
     *
     * @return {@code true} if messages have been sent, {@code false} otherwise
     */
    public boolean hasSentMessages() {
        return !sentMessages.isEmpty();
    }
}
