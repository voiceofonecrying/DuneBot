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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class EventListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        //System.out.println(event.getMessage().getContentRaw());
        String message = event.getMessage().getContentRaw().replaceAll("\\d", "");

        //Treachery Card Service
        if (message.matches(".*<:treachery:> .* <:treachery:>.*")) {
            String cardName = message.split("<:treachery:>")[1].strip();
            sendImage(message, event, "treachery-cards", cardName);

        } else if (message.matches(".*<:weirding:> .* <:weirding:>.*")) {
            String cardName = message.split("<:weirding:>")[1].strip();
            sendImage(message, event, "leader-skills", cardName);
        } else if (message.matches(".*<:worm-:> .* <:worm-:>.*")) {
            String cardName = message.split("<:worm-:>")[1].strip();
            sendImage(message, event, "stronghold-cards", cardName);
        }

        //Add any other text based commands here
    }

    public void sendImage(String message, MessageReceivedEvent event,  String channelName, String cardName) {
        if (event.getGuild().getCategoriesByName("Mod Area", true).size() == 0) return;
        Category modArea = event.getGuild().getCategoriesByName("Mod Area", true).get(0);
        for (TextChannel channel : modArea.getTextChannels()) {
            if (!channel.getName().equals(channelName)) continue;

            MessageHistory history = MessageHistory.getHistoryFromBeginning(channel).complete();
            List<Message> messages = history.getRetrievedHistory();

            Pattern cardPattern = Pattern.compile(
                    ".*Name:\\s*" + Pattern.quote(cardName) + "\\s*\\R.*",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            );

            for (Message channelMessage : messages) {
                if (cardPattern.matcher(channelMessage.getContentRaw()).matches()) {
                    Message.Attachment attachment = channelMessage.getAttachments().get(0);
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
            return;
        }
    }
}
