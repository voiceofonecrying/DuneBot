package utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class CardImages {
    private static final Map<String, List<Message>> cardChannelMessages = new HashMap<>();
    public static Optional<FileUpload> getTreacheryCardImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*Name:\\s*" +
                        Pattern.quote(cardName.trim()) +
                        "\\s*\\R.*(Expansion: Ixian and Tleilaxu|Expansion: CHOAM and Richese|Expansion: Base Game|Expansion: Richese|Expansion: Ecaz & Moritani).*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "treachery-cards", pattern, cardName);
    }

    public static Optional<FileUpload> getLeaderSkillImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "leader-skills", pattern, cardName);
    }

    public static Optional<FileUpload> getStrongholdImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "stronghold-cards", pattern, cardName);
    }

    public static Optional<FileUpload> getNexusImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "nexus-cards", pattern, cardName);
    }

    private static List<Message> getChannelMessages(Guild guild, String channelName) {
        if (cardChannelMessages.containsKey(channelName)) {
            return cardChannelMessages.get(channelName);
        } else {
            Optional<TextChannel> channel = guild.getTextChannels().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(channelName))
                    .findFirst();

            if (channel.isEmpty()) return new ArrayList<>();
            MessageHistory history = MessageHistory.getHistoryFromBeginning(channel.get()).complete();

            List<Message> messages = history.getRetrievedHistory();
            cardChannelMessages.put(channelName, messages);
            return messages;
        }
    }

    private static Optional<FileUpload> getCardImage(Guild guild, String channelName, Pattern pattern, String cardName) {
        List<Message> messages = getChannelMessages(guild, channelName);

        Optional<Message> message = messages.stream()
                .filter(m -> pattern.matcher(m.getContentRaw()).matches())
                .findFirst();

        if (message.isEmpty()) return Optional.empty();

        List<Message.Attachment> attachments = message.get().getAttachments();

        if (attachments.isEmpty()) return Optional.empty();

        Message.Attachment attachment = attachments.get(0);
        CompletableFuture<InputStream> future = attachment.getProxy().download();

        try {
            InputStream inputStream = future.get();
            FileUpload fileUpload = FileUpload.fromData(inputStream, cardName + ".jpg");
            return Optional.of(fileUpload);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
