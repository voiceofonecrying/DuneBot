package controller.channels;

import controller.DiscordGame;
import model.topics.DuneTopic;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;

public class DiscordChannel implements DuneTopic {
    DiscordGame discordGame;
    MessageChannel messageChannel;

    DiscordChannel(DiscordGame discordGame) {
        this.discordGame = discordGame;
    }

    public DiscordChannel(DiscordGame discordGame, MessageChannel messageChannel) {
        this.discordGame = discordGame;
        this.messageChannel = messageChannel;
    }

    @Override
    public void publish(String message) {
        queueMessage(message);
    }

    public void queueMessage(String message) {
        discordGame.getMessageQueue().add(messageChannel.sendMessage(message));
    }

    public void queueMessage(String message, List<Button> buttons) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                .addContent(message);
        int i = 0;
        while (i + 5 < buttons.size()) {
            messageCreateBuilder.addActionRow(buttons.subList(i, i + 5));
            i += 5;
        }
        if (i < buttons.size()) {
            messageCreateBuilder.addActionRow(buttons.subList(i, buttons.size()));
        }
        discordGame.getMessageQueue().add(messageChannel.sendMessage(messageCreateBuilder.build()));
    }

    public void queueMessage(MessageCreateBuilder messageCreateBuilder) {
        discordGame.getMessageQueue().add(messageChannel.sendMessage(messageCreateBuilder.build()));
    }

    public void queueMessage(String message, FileUpload fileUpload) {
        discordGame.getMessageQueue().add(messageChannel.sendMessage(message).addFiles(fileUpload));
    }

    public void queueMessage(FileUpload fileUpload) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                .addFiles(fileUpload);
        discordGame.getMessageQueue().add(messageChannel.sendMessage(messageCreateBuilder.build()));

    }
}
