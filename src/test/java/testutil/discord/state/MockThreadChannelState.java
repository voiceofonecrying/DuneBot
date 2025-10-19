package testutil.discord.state;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores state for a Discord thread channel.
 *
 * <p>Threads are temporary sub-channels within a text channel, used for
 * focused discussions. They have their own message history and member list.
 *
 * <p><b>State Stored:</b>
 * <ul>
 *   <li><b>Thread ID</b> - Unique identifier for the thread</li>
 *   <li><b>Thread Name</b> - Display name of the thread</li>
 *   <li><b>Parent Channel ID</b> - The text channel this thread belongs to</li>
 *   <li><b>Guild ID</b> - The guild this thread is in</li>
 *   <li><b>Message History</b> - All messages sent in this thread</li>
 *   <li><b>Thread Members</b> - User IDs of members in the thread</li>
 *   <li><b>Auto-Archive Duration</b> - Minutes until thread auto-archives</li>
 *   <li><b>Archived Status</b> - Whether the thread is archived</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * MockGuildState guild = server.createGuild(12345L, "Test Guild");
 * MockCategoryState category = guild.createCategory("game-channels");
 * MockChannelState channel = guild.createTextChannel("game-actions", category.getCategoryId());
 * MockThreadChannelState thread = guild.createThread("strategy-discussion", channel.getChannelId());
 *
 * // Add members to thread
 * thread.addMember(user1.getUserId());
 * thread.addMember(user2.getUserId());
 *
 * // Add messages to thread
 * MockMessageState message = new MockMessageState(1000001L, thread.getThreadId(), user1.getUserId(), "Let's discuss strategy");
 * thread.addMessage(message);
 * }</pre>
 *
 * @see MockChannelState
 * @see MockMessageState
 */
public class MockThreadChannelState {
    private final long threadId;
    private final String threadName;
    private final long parentChannelId;
    private final long guildId;
    private final List<MockMessageState> messages = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private int autoArchiveDuration = 60; // Default 60 minutes
    private boolean archived = false;
    private final Instant createdTimestamp;

    public MockThreadChannelState(long threadId, String threadName, long parentChannelId, long guildId) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.parentChannelId = parentChannelId;
        this.guildId = guildId;
        this.createdTimestamp = Instant.now();
    }

    /**
     * Gets the unique ID of this thread.
     *
     * @return The thread ID
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Gets the display name of this thread.
     *
     * @return The thread name
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Gets the ID of the parent text channel this thread belongs to.
     *
     * @return The parent channel ID
     */
    public long getParentChannelId() {
        return parentChannelId;
    }

    /**
     * Gets the ID of the guild this thread belongs to.
     *
     * @return The guild ID
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Gets the timestamp when this thread was created.
     *
     * @return The creation timestamp
     */
    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Gets the auto-archive duration in minutes.
     *
     * @return Minutes until the thread auto-archives (default: 60)
     */
    public int getAutoArchiveDuration() {
        return autoArchiveDuration;
    }

    /**
     * Sets the auto-archive duration.
     *
     * @param minutes Minutes until auto-archive
     */
    public void setAutoArchiveDuration(int minutes) {
        this.autoArchiveDuration = minutes;
    }

    /**
     * Checks if this thread is archived.
     *
     * @return {@code true} if archived, {@code false} otherwise
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * Sets the archived status of this thread.
     *
     * @param archived Whether the thread should be archived
     */
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    /**
     * Adds a message to this thread's history.
     *
     * @param message The message to add
     */
    public void addMessage(MockMessageState message) {
        messages.add(message);
    }

    /**
     * Gets all messages in this thread.
     *
     * <p>Messages are returned in chronological order (oldest first).
     *
     * @return A new list containing all messages (modifications won't affect the thread)
     */
    public List<MockMessageState> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Gets the most recent N messages from this thread.
     *
     * <p>If the thread has fewer messages than the limit, all messages are returned.
     *
     * @param limit Maximum number of messages to return
     * @return List of the most recent messages, up to the specified limit
     */
    public List<MockMessageState> getRecentMessages(int limit) {
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }

    /**
     * Clears all messages from this thread.
     *
     * <p>Useful for resetting state between tests.
     */
    public void clearMessages() {
        messages.clear();
    }

    /**
     * Adds a member to this thread.
     *
     * <p>Duplicate member IDs are ignored.
     *
     * @param userId The user ID to add to the thread
     */
    public void addMember(long userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    /**
     * Removes a member from this thread.
     *
     * @param userId The user ID to remove from the thread
     */
    public void removeMember(long userId) {
        memberIds.remove(userId);
    }

    /**
     * Gets all member IDs in this thread.
     *
     * @return A new list containing all member IDs (modifications won't affect the thread)
     */
    public List<Long> getMemberIds() {
        return new ArrayList<>(memberIds);
    }

    /**
     * Checks if a user is a member of this thread.
     *
     * @param userId The user ID to check
     * @return {@code true} if the user is a member, {@code false} otherwise
     */
    public boolean hasMember(long userId) {
        return memberIds.contains(userId);
    }
}
