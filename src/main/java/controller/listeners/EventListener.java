package controller.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class EventListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().replaceAll("\\d", "");

        if (message.matches(".*<:treachery:> .* <:treachery:>.*")) {
            String cardName = message.split("<:treachery:>")[1].strip();
            sendTreacheryImage(message, event, cardName);
        } else if (message.matches(".*<:weirding:> .* <:weirding:>.*")) {
            String cardName = message.split("<:weirding:>")[1].strip();
            sendLeaderSkillImage(message, event, cardName);
        } else if (message.matches(".*<:worm:> .* <:worm:>.*")) {
            String cardName = message.split("<:worm:>")[1].strip();
            sendStrongholdImage(message, event, cardName);
        }

        //Add any other text based commands here
    }

    public void sendTreacheryImage(String message, MessageReceivedEvent event, String cardName) {
        List<Message> messages = getChannelMessages(event, "treachery-cards");

        Pattern cardPattern = Pattern.compile(
                ".*Name:\\s*" +
                        Pattern.quote(cardName) +
                        "\\s*\\R.*(Expansion: Ixian and Tleilaxu|Expansion: CHOAM and Richese|Expansion: Base Game).*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Optional<Message> channelMessage = messages.stream()
                .filter(m -> cardPattern.matcher(m.getContentRaw()).matches())
                .findFirst();

        sendImage(message, event, cardName, channelMessage);
    }

    public void sendLeaderSkillImage(String message, MessageReceivedEvent event, String cardName) {
        List<Message> messages = getChannelMessages(event, "leader-skills");

        Pattern cardPattern = Pattern.compile(
                ".*" + Pattern.quote(cardName) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Optional<Message> channelMessage = messages.stream()
                .filter(m -> cardPattern.matcher(m.getContentRaw()).matches())
                .findFirst();

        sendImage(message, event, cardName, channelMessage);
    }

    public void sendStrongholdImage(String message, MessageReceivedEvent event, String cardName) {
        List<Message> messages = getChannelMessages(event, "stronghold-cards");

        Pattern cardPattern = Pattern.compile(
                ".*" + Pattern.quote(cardName) + ".*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Optional<Message> channelMessage = messages.stream()
                .filter(m -> cardPattern.matcher(m.getContentRaw()).matches())
                .findFirst();

        sendImage(message, event, cardName, channelMessage);
    }

    private List<Message> getChannelMessages(MessageReceivedEvent event,  String channelName) {
        if (event.getGuild().getCategoriesByName("Mod Area", true).size() == 0) return new ArrayList<>();
        Category modArea = event.getGuild().getCategoriesByName("Mod Area", true).get(0);

        Optional<TextChannel> channel = modArea.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase(channelName))
                .findFirst();

        if (channel.isEmpty()) return new ArrayList<>();
        MessageHistory history = MessageHistory.getHistoryFromBeginning(channel.get()).complete();

        return history.getRetrievedHistory();
    }

    private void sendImage(String message, MessageReceivedEvent event, String cardName, Optional<Message> channelMessage) {
        if (channelMessage.isEmpty()) return;

        List<Message.Attachment> attachments = channelMessage.get().getAttachments();

        if (attachments.size() == 0) return;

        Message.Attachment attachment = attachments.get(0);
        CompletableFuture<InputStream> future = attachment.getProxy().download();

        try {
            InputStream inputStream = future.get();
            if (inputStream == null) {
                System.out.println("No Card named " + message);
            } else {
                FileUpload fileUpload = FileUpload.fromData(inputStream, cardName + ".jpg");
                event.getChannel().sendFiles(fileUpload).queue();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
