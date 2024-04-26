package utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
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

    public static Optional<FileUpload> getTraitorImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "traitor-cards", pattern, cardName);
    }

    public static Optional<FileUpload> getHomeworldImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "homeworlds", pattern, cardName);
    }

    public static Optional<FileUpload> getPredictionImage(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImage(guild, "bg-prediction-cards", pattern, cardName);
    }

    public static String getHomeworldImageLink(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImageLink(guild, "homeworld-images", pattern);
    }

    public static String getEcazAmbassadorImageLink(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImageLink(guild, "ecaz-ambassadors", pattern);
    }

    public static String getLeaderImageLink(Guild guild, String leaderName) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(leaderName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getCardImageLink(guild, "leaders", pattern);
    }

    public static String getStrongholdCardLink(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getMessageLink(guild, "stronghold-cards", pattern);
    }

    public static String getLeaderSkillCardLink(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getMessageLink(guild, "leader-skills", pattern);
    }

    public static String getHomeworldCardLink(Guild guild, String cardName) {
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(cardName.trim()) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        return getMessageLink(guild, "homeworld-cards", pattern);
    }

    private static void clearChennalMessageCache(String channelName) {
        cardChannelMessages.remove(channelName);
    }

    private static List<Message> getChannelMessages(Guild guild, String channelName) {
        if (cardChannelMessages.containsKey(channelName)) {
            return cardChannelMessages.get(channelName);
        } else {
            if (guild.getCategoriesByName("Game Resources", true).isEmpty()) return new ArrayList<>();
            Category gameResources = guild.getCategoriesByName("Game Resources", true).get(0);

            Optional<TextChannel> channel = gameResources.getTextChannels().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(channelName))
                    .findFirst();

            if (channel.isEmpty()) return new ArrayList<>();
            List<Message> messages = new LinkedList<>();
            String id = "0";

            while (!MessageHistory.getHistoryAfter(channel.get(), id).complete().isEmpty()) {
                MessageHistory history = MessageHistory.getHistoryAfter(channel.get(), id).complete();
                messages.addAll(history.getRetrievedHistory());
                id = history.getRetrievedHistory().get(0).getId();
            }

            cardChannelMessages.put(channelName, messages);
            return messages;
        }
    }

    private static Optional<Message.Attachment> getCardImageAttachment(Guild guild, String channelName, Pattern pattern ) {
        Optional<Message> message = getCardMessage(guild, channelName, pattern);
        if (message.isEmpty()) return Optional.empty();

        List<Message.Attachment> attachments = message.get().getAttachments();

        if (attachments.isEmpty()) return Optional.empty();

        Message.Attachment attachment = attachments.get(0);
        return Optional.of(attachment);
    }

    private static Optional<Message> getCardMessage(Guild guild, String channelName, Pattern pattern) {
        List<Message> messages = getChannelMessages(guild, channelName);

        return messages.stream()
                .filter(m -> pattern.matcher(m.getContentRaw()).matches())
                .findFirst();
    }

    private static String getMessageLink(Guild guild, String channelName, Pattern pattern) {
        Optional<Message> optionalMessage = getCardMessage(guild, channelName, pattern);

        return optionalMessage.map(Message::getJumpUrl).orElse("");
    }

    private static String getCardImageLink(Guild guild, String channelName, Pattern pattern) {
        Optional<Message.Attachment> optionalAttachment = getCardImageAttachment(guild, channelName, pattern);

        if (optionalAttachment.isEmpty()) return "";

        Message.Attachment attachment = optionalAttachment.get();
        return attachment.getUrl();
    }

    private static Optional<FileUpload> getCardImageAttachmentFileUpload(
            Guild guild, String channelName, Pattern pattern, String cardName
    ) throws ExecutionException, InterruptedException {
        Optional<Message.Attachment> optionalAttachment = getCardImageAttachment(guild, channelName, pattern);

        if (optionalAttachment.isEmpty()) return Optional.empty();

        Message.Attachment attachment = optionalAttachment.get();
        CompletableFuture<InputStream> future = attachment.getProxy().download();

        InputStream inputStream = future.get();
        FileUpload fileUpload = FileUpload.fromData(inputStream, cardName + ".jpg");
        return Optional.of(fileUpload);
    }

    private static Optional<FileUpload> getCardImage(Guild guild, String channelName, Pattern pattern, String cardName) {
        try {
            return getCardImageAttachmentFileUpload(guild, channelName, pattern, cardName);
        } catch (InterruptedException | ExecutionException e) {
            clearChennalMessageCache(channelName);
            try {
                return getCardImageAttachmentFileUpload(guild, channelName, pattern, cardName);
            } catch (InterruptedException | ExecutionException ex) {
                return Optional.empty();
            }
        }
    }
}
