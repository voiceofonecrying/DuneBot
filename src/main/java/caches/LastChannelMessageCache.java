package caches;

import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * A cache class that stores the last message for each channel identified by its channel ID.
 * This class provides static methods to retrieve, store, and check for the existence of
 * the last message in any given channel.
 */
public class LastChannelMessageCache {
    static Map<String, Message> lastChannelMessage = new HashMap<>();

    /**
     * Retrieves the last message sent in the specified channel.
     *
     * @param channelId the unique identifier for the channel from which to retrieve the last message
     * @return the last Message sent in the specified channel, or null if no message exists for the channel
     */
    public static Message getMessage(String channelId) {
        return lastChannelMessage.get(channelId);
    }

    /**
     * Stores the given message as the last message sent in the specified channel.
     *
     * @param channelId the unique identifier for the channel where the message is to be stored
     * @param message the Message object to be stored as the last message for the specified channel
     */
    public static void setMessage(String channelId, Message message) {
        lastChannelMessage.put(channelId, message);
    }

    /**
     * Checks if there is a last message stored for the given channel.
     *
     * @param channelId the unique identifier for the channel to check for the presence of a last message
     * @return true if there is a last message stored for the specified channel; false otherwise
     */
    public static boolean hasMessage(String channelId) {
        return lastChannelMessage.containsKey(channelId);
    }
}
