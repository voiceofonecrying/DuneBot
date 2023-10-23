package controller.listeners;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import utils.CardImages;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EventListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        CompletableFuture.runAsync(() -> runOnMessageReceived(event));
    }

    public void runOnMessageReceived(@NotNull MessageReceivedEvent event) {
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
        } else if (message.matches(".*transparent_worm:.*:transparent_worm.*")) {
            String cardName = message.split("<:transparent_worm:>")[1].strip();
            sendNexusImage(event, cardName);
        }

        if (event.getMember().getUser().isBot()) return;

        //Add any other text based commands here
        DiscordGame discordGame;
        try {
            discordGame = new DiscordGame(event);
        } catch (ChannelNotFoundException | IOException e) {
            return;
        }
        Game game;
        try {
            game = discordGame.getGame();
        } catch (ChannelNotFoundException e) {
            return;
        }

        for (Faction faction : game.getFactions()) {
            if (event.getMember().getUser().getAsMention().equals(faction.getPlayer())) {
                String emojiName = faction.getEmoji().replace("<:", "").replaceAll(":.*>", "");
                long id = Long.parseLong(faction.getEmoji().replaceAll("<:.*:", "").replace(">", ""));
                event.getMessage().addReaction(Emoji.fromCustom(emojiName, id, false)).queue();
            }
            if (message.matches(".*@ " + faction.getEmoji().replaceAll("[0-9]", "") + ".*"))
                event.getChannel().sendMessage(faction.getPlayer()).queue();
        }

        if (message.matches(".*@ " + Emojis.MOD_EMPEROR.replaceAll("[0-9]", "") + ".*")) event.getChannel().sendMessage(game.getMod()).queue();
        if (event.getMember().getRoles().stream().anyMatch(role -> role.getName().equals(game.getModRole()))) {
            String emojiName = Emojis.MOD_EMPEROR.replace("<:", "").replaceAll(":.*>", "");
            long id = Long.parseLong(Emojis.MOD_EMPEROR.replaceAll("<:.*:", "").replace(">", ""));
            event.getMessage().addReaction(Emoji.fromCustom(emojiName, id, false)).queue();
        }

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

    public void sendNexusImage(MessageReceivedEvent event, String cardName) {
        Guild guild = event.getGuild();
        Optional<FileUpload> fileUpload = CardImages.getNexusImage(guild, cardName);

        if (fileUpload.isEmpty()) return;
        sendImage(event, fileUpload.get());
    }

    private void sendImage(MessageReceivedEvent event, FileUpload fileUpload) {
        event.getChannel().sendFiles(fileUpload).queue();
    }
}
