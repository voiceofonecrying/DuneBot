package controller.channels;

import controller.DiscordGame;
import model.DuneChoice;
import model.topics.DuneTopic;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.ArrayList;
import java.util.List;

public class DiscordChannel implements DuneTopic {
    final DiscordGame discordGame;
    MessageChannel messageChannel;

    public DiscordChannel(DiscordGame discordGame) {
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
        queueMessage(message, convertChoicesToButtons(choices));
    }

    @Override
    public void reply(String message) {
        queueReplyMessage(message);
    }

    @Override
    public void reply(String message, List<DuneChoice> choices) {
        queueReplyMessage(message, convertChoicesToButtons(choices));
    }

    private List<Button> convertChoicesToButtons(List<DuneChoice> choices) {
        List<Button> buttons = new ArrayList<>();
        for (DuneChoice choice : choices) {
            Button button;
            if (choice.getLabel() == null) {
                Emoji emoji = Emoji.fromFormatted(discordGame.tagEmojis(choice.getEmoji()));
                switch (choice.getType()) {
                    case "secondary" -> button = Button.secondary(choice.getId(), emoji).withDisabled(choice.isDisabled());
                    case "danger" -> button = Button.danger(choice.getId(), emoji).withDisabled(choice.isDisabled());
                    case "success" -> button = Button.success(choice.getId(), emoji).withDisabled(choice.isDisabled());
                    default -> button = Button.primary(choice.getId(), emoji).withDisabled(choice.isDisabled());
                }
            } else {
                switch (choice.getType()) {
                    case "secondary" -> button = Button.secondary(choice.getId(), choice.getLabel()).withDisabled(choice.isDisabled());
                    case "danger" -> button = Button.danger(choice.getId(), choice.getLabel()).withDisabled(choice.isDisabled());
                    case "success" -> button = Button.success(choice.getId(), choice.getLabel()).withDisabled(choice.isDisabled());
                    default -> button = Button.primary(choice.getId(), choice.getLabel()).withDisabled(choice.isDisabled());
                }
                if (choice.getEmoji() != null)
                    button = button.withEmoji(Emoji.fromFormatted(discordGame.tagEmojis(choice.getEmoji())));
            }
            buttons.add(button);
        }
        return buttons;
    }

    public void queueMessage(String message) {
        discordGame.queueMessage(messageChannel.sendMessage(message));
    }

    public void queueMessage(String message, List<Button> buttons) {
        MessageCreateBuilder messageCreateBuilder = arrangeButtons(message, buttons);
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

    public void queueReplyMessage(String message) {
        discordGame.queueMessage(message);
    }

    public void queueReplyMessage(String message, List<Button> buttons) {
        discordGame.queueMessage(arrangeButtons(message, buttons));
    }

    private MessageCreateBuilder arrangeButtons(String message, List<Button> buttons) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder().addContent(message);
        int i = 0;
        while (i + 5 < buttons.size()) {
            messageCreateBuilder.addActionRow(buttons.subList(i, i + 5));
            i += 5;
        }
        if (i < buttons.size())
            messageCreateBuilder.addActionRow(buttons.subList(i, buttons.size()));
        return messageCreateBuilder;
    }
}
