package testutil.discord.state;

import net.dv8tion.jda.api.utils.FileUpload;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a single Discord message.
 *
 * <p>Messages are automatically created and stored when {@code channel.sendMessage()}
 * is called on a stateful mock channel. This enables verification that messages
 * were sent with the correct content.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Message ID</b> - Unique identifier for this message</li>
 *   <li><b>Channel ID</b> - Which channel this message was sent to</li>
 *   <li><b>Author ID</b> - User who sent the message (0L for bot messages)</li>
 *   <li><b>Content</b> - The message text</li>
 *   <li><b>Attachments</b> - File uploads (e.g., game state JSON)</li>
 *   <li><b>Timestamp</b> - When the message was created</li>
 *   <li><b>Buttons</b> - Interactive button components attached to the message</li>
 *   <li><b>Embeds</b> - Rich embed content in the message</li>
 *   <li><b>Reactions</b> - Emoji reactions on the message</li>
 *   <li><b>Referenced Message ID</b> - For message replies</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Messages are created automatically when sending
 * mockChannel.sendMessage("Game started!").queue();
 *
 * // Retrieve and verify the message
 * List<MockMessageState> messages = channelState.getMessages();
 * MockMessageState message = messages.get(0);
 *
 * assertThat(message.getContent()).isEqualTo("Game started!");
 * assertThat(message.getChannelId()).isEqualTo(channelState.getChannelId());
 * assertThat(message.getTimestamp()).isNotNull();
 *
 * // Add buttons and embeds
 * message.addButton(new MockButtonState("btn-1", "Click Me", "PRIMARY"));
 * message.addEmbed(new MockEmbedState());
 * }</pre>
 *
 * @see MockChannelState
 * @see MockButtonState
 * @see MockEmbedState
 * @see MockReactionState
 */
public class MockMessageState {
    private final long messageId;
    private final long channelId;
    private final long authorId;
    private final String content;
    private final List<FileUpload> attachments;
    private final Instant timestamp;
    private final List<MockButtonState> buttons = new ArrayList<>();
    private final List<MockEmbedState> embeds = new ArrayList<>();
    private final List<MockReactionState> reactions = new ArrayList<>();
    private Long referencedMessageId;

    public MockMessageState(long messageId, long channelId, long authorId, String content) {
        this(messageId, channelId, authorId, content, new ArrayList<>());
    }

    public MockMessageState(long messageId, long channelId, long authorId, String content, List<FileUpload> attachments) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.authorId = authorId;
        this.content = content;
        this.attachments = new ArrayList<>(attachments);
        this.timestamp = Instant.now();
    }

    /**
     * Gets the unique ID of this message.
     *
     * @return The message ID
     */
    public long getMessageId() {
        return messageId;
    }

    /**
     * Gets the ID of the channel this message was sent to.
     *
     * @return The channel ID
     */
    public long getChannelId() {
        return channelId;
    }

    /**
     * Gets the ID of the user who sent this message.
     *
     * <p>For messages sent by the bot, this is typically 0L.
     *
     * @return The author's user ID
     */
    public long getAuthorId() {
        return authorId;
    }

    /**
     * Gets the text content of this message.
     *
     * @return The message content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets all file attachments for this message.
     *
     * @return A new list containing all attachments (modifications won't affect the message)
     */
    public List<FileUpload> getAttachments() {
        return new ArrayList<>(attachments);
    }

    /**
     * Gets the timestamp when this message was created.
     *
     * @return The creation timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this message has any file attachments.
     *
     * @return {@code true} if there are attachments, {@code false} otherwise
     */
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    /**
     * Adds a button to this message.
     *
     * @param button The button to add
     */
    public void addButton(MockButtonState button) {
        buttons.add(button);
    }

    /**
     * Gets all buttons attached to this message.
     *
     * @return A new list containing all buttons (modifications won't affect the message)
     */
    public List<MockButtonState> getButtons() {
        return new ArrayList<>(buttons);
    }

    /**
     * Checks if this message has any buttons.
     *
     * @return {@code true} if there are buttons, {@code false} otherwise
     */
    public boolean hasButtons() {
        return !buttons.isEmpty();
    }

    /**
     * Adds an embed to this message.
     *
     * @param embed The embed to add
     */
    public void addEmbed(MockEmbedState embed) {
        embeds.add(embed);
    }

    /**
     * Gets all embeds in this message.
     *
     * @return A new list containing all embeds (modifications won't affect the message)
     */
    public List<MockEmbedState> getEmbeds() {
        return new ArrayList<>(embeds);
    }

    /**
     * Checks if this message has any embeds.
     *
     * @return {@code true} if there are embeds, {@code false} otherwise
     */
    public boolean hasEmbeds() {
        return !embeds.isEmpty();
    }

    /**
     * Adds a reaction to this message.
     *
     * @param reaction The reaction to add
     */
    public void addReaction(MockReactionState reaction) {
        reactions.add(reaction);
    }

    /**
     * Gets all reactions on this message.
     *
     * @return A new list containing all reactions (modifications won't affect the message)
     */
    public List<MockReactionState> getReactions() {
        return new ArrayList<>(reactions);
    }

    /**
     * Checks if this message has any reactions.
     *
     * @return {@code true} if there are reactions, {@code false} otherwise
     */
    public boolean hasReactions() {
        return !reactions.isEmpty();
    }

    /**
     * Gets the ID of the message this message is replying to.
     *
     * @return The referenced message ID, or {@code null} if not a reply
     */
    public Long getReferencedMessageId() {
        return referencedMessageId;
    }

    /**
     * Sets the ID of the message this message is replying to.
     *
     * @param referencedMessageId The message ID to reply to
     */
    public void setReferencedMessageId(Long referencedMessageId) {
        this.referencedMessageId = referencedMessageId;
    }
}
