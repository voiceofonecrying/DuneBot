package utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class CardImages {
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

    private static Optional<FileUpload> getCardImage(Guild guild, String channelName, Pattern pattern, String cardName) {
        if (guild.getCategoriesByName("Mod Area", true).size() == 0) return Optional.empty();
        Category modArea = guild.getCategoriesByName("Mod Area", true).get(0);

        Optional<TextChannel> channel = modArea.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase(channelName))
                .findFirst();

        if (channel.isEmpty()) return Optional.empty();
        MessageHistory history = MessageHistory.getHistoryFromBeginning(channel.get()).complete();

        List<Message> messages = history.getRetrievedHistory();

        Optional<Message> message = messages.stream()
                .filter(m -> pattern.matcher(m.getContentRaw()).matches())
                .findFirst();

        if (message.isEmpty()) return Optional.empty();

        List<Message.Attachment> attachments = message.get().getAttachments();

        if (attachments.size() == 0) return Optional.empty();

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
