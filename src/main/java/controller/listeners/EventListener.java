package controller.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import utils.CardImages;

import java.util.Optional;

public class EventListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().replaceAll("\\d", "");

        if (message.matches(".*<:treachery:>.*<:treachery:>.*")) {
            String cardName = message.split("<:treachery:>")[1].strip();
            sendTreacheryImage(event, cardName);
        } else if (message.matches(".*<:weirding:>.*<:weirding:>.*")) {
            String cardName = message.split("<:weirding:>")[1].strip();
            sendLeaderSkillImage(event, cardName);
        } else if (message.matches(".*:worm:.*:worm:.*")) {
            String cardName = message.split(":worm:")[1].strip();
            sendStrongholdImage(event, cardName);
        }

        //Add any other text based commands here
    }

    public void sendTreacheryImage(MessageReceivedEvent event, String cardName) {
        Guild guild = event.getGuild();
        Optional<FileUpload> fileUpload = CardImages.getTreacheryCardImage(guild, cardName);

        if (fileUpload.isEmpty()) return;
        sendImage(event, fileUpload.get());
    }

    public void sendLeaderSkillImage(MessageReceivedEvent event, String cardName) {
        Guild guild = event.getGuild();
        Optional<FileUpload> fileUpload = CardImages.getLeaderSkillImage(guild, cardName);

        if (fileUpload.isEmpty()) return;
        sendImage(event, fileUpload.get());
    }

    public void sendStrongholdImage(MessageReceivedEvent event, String cardName) {
        Guild guild = event.getGuild();
        Optional<FileUpload> fileUpload = CardImages.getStrongholdImage(guild, cardName);

        if (fileUpload.isEmpty()) return;
        sendImage(event, fileUpload.get());
    }

    private void sendImage(MessageReceivedEvent event, FileUpload fileUpload) {
        event.getChannel().sendFiles(fileUpload).queue();
    }
}
