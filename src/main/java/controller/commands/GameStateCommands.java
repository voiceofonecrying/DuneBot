package controller.commands;

import caches.GameCache;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GameStateCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("gamestate", "Commands related to the game state").addSubcommands(
                        new SubcommandData(
                                "rewind",
                                "Rewind to a previous game state"
                        ).addOptions(CommandOptions.gameState),
                        new SubcommandData(
                                "refresh",
                                "Refresh the current game state to match the current JSON file"
                        )
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException, ExecutionException, InterruptedException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "rewind" -> rewind(discordGame);
            case "refresh" -> refresh(discordGame);
        }
    }

    public static void rewind(DiscordGame discordGame) throws ChannelNotFoundException, ExecutionException, InterruptedException {
        MessageChannel botDataChannel = discordGame.getTextChannel("bot-data");
        String messageId = discordGame.required(CommandOptions.gameState).getAsString();
        Message message = botDataChannel.retrieveMessageById(messageId).complete();

        List<Message.Attachment> attachments = message.getAttachments();

        if (attachments.isEmpty()) throw new IllegalArgumentException("No state found");

        Message.Attachment attachment = attachments.get(0);
        CompletableFuture<InputStream> future = attachment.getProxy().download();
        InputStream inputStream = future.get();
        FileUpload fileUpload = FileUpload.fromData(inputStream, "gamestate.json");
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder.setContent("Rewind to previous state");
        messageCreateBuilder.addFiles(fileUpload);

        MessageCreateAction messageCreateAction = botDataChannel
                .sendMessage(messageCreateBuilder.build())
                .setMessageReference(message.getId());

        discordGame.queueMessage(messageCreateAction);
        GameCache.clearGameJson(discordGame.getGameCategory().getName());
    }

    public static void refresh(DiscordGame discordGame) {
        String gameName = discordGame.getGameCategory().getName();
        GameCache.clearGameJson(gameName);
    }
}
