package controller.listeners;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import utils.CardImages;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventListener extends ListenerAdapter {

    final Pattern cardPattern = Pattern
            .compile(":(treachery|weirding|worm|transparent_worm):([^:]*):(treachery|weirding|worm|transparent_worm):");

    final Pattern mentionPattern = Pattern.compile("@\\s*(:[a-zA-Z0-9_]+:)");

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        CompletableFuture.runAsync(() -> runOnMessageReceived(event));
    }

    public void runOnMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentStripped();

        Matcher cardMatcher = cardPattern.matcher(message);

        while (cardMatcher.find()) {
            String cardName = cardMatcher.group(2).strip();
            String cardType = cardMatcher.group(1);

            switch (cardType) {
                case "treachery" -> sendTreacheryImage(event, cardName);
                case "weirding" -> sendLeaderSkillImage(event, cardName);
                case "worm" -> sendStrongholdImage(event, cardName);
                case "transparent_worm" -> sendNexusImage(event, cardName);
            }
        }

        if (Objects.requireNonNull(event.getMember()).getUser().isBot()) return;

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

        game.getFactions().stream()
                .filter(faction -> faction.getPlayer().equals(event.getMember().getUser().getAsMention()))
                .forEach(faction -> event.getMessage()
                        .addReaction(discordGame.getEmoji(Emojis.getFactionEmoji(faction.getName())))
                        .queue());

        if (event.getMember().getRoles().stream().anyMatch(role -> role.getName().equals(game.getModRole()))) {
            event.getMessage().addReaction(discordGame.getEmoji(Emojis.MOD_EMPEROR)).queue();
        }

        Matcher mentionMatcher = mentionPattern.matcher(message);

        Set<String> mentionedPlayers = new HashSet<>();

        while (mentionMatcher.find()) {
            String emojiName = mentionMatcher.group(1);

            if (emojiName.equals(Emojis.MOD_EMPEROR)) {
                mentionedPlayers.add(game.getMod());
            } else {
                game.getFactions().stream()
                        .filter(faction -> emojiName.equals(Emojis.getFactionEmoji(faction.getName())))
                        .forEach(faction -> mentionedPlayers.add(faction.getPlayer()));
            }
        }

        event.getChannel().sendMessage(StringUtils.join(mentionedPlayers, " ")).queue();
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
