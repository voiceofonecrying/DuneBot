package controller.channels;

import controller.DiscordGame;
import model.DuneChoice;
import model.topics.DuneTopic;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.ArrayList;
import java.util.List;

public class DiscordChannel implements DuneTopic {
    final DiscordGame discordGame;
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

    @Override
    public void publish(String message, List<DuneChoice> choices) {
        List<Button> buttons = new ArrayList<>();
        for (DuneChoice choice : choices) {
            switch (choice.getType()) {
                case "secondary" -> buttons.add(Button.secondary(choice.getId(), choice.getLabel()));
                case "danger" -> buttons.add(Button.danger(choice.getId(), choice.getLabel()));
                case "success" -> buttons.add(Button.success(choice.getId(), choice.getLabel()));
                default -> buttons.add(Button.primary(choice.getId(), choice.getLabel()));
            }
        }
        queueMessage(message, buttons);
    }

    public void queueMessage(String message) {
        discordGame.queueMessage(messageChannel.sendMessage(message));
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
        discordGame.queueMessage(messageChannel.sendMessage(messageCreateBuilder.build()));
    }

    public void queueMessage(MessageCreateBuilder messageCreateBuilder) {
        discordGame.queueMessage(messageChannel.sendMessage(messageCreateBuilder.build()));
    }

    public void queueMessage(String message, FileUpload fileUpload) {
        discordGame.queueMessage(messageChannel.sendMessage(message).addFiles(fileUpload));
    }

    public void queueMessage(FileUpload fileUpload) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder()
                .addFiles(fileUpload);
        discordGame.queueMessage(messageChannel.sendMessage(messageCreateBuilder.build()));

    }
}
