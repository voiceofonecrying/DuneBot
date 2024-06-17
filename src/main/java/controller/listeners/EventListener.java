package controller.listeners;

import constants.Emojis;
import controller.CommandCompletionGuard;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import utils.CardImages;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventListener extends ListenerAdapter {

    private static final Pattern cardPattern = Pattern
            .compile(":(treachery|leader|stronghold|nexus):([^:\\v]*):\\1:");

    private static final Pattern mentionPattern = Pattern.compile("@\\h*(:[a-zA-Z0-9_]+:)");

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        CommandCompletionGuard.incrementCommandCount();
        CompletableFuture
                .runAsync(() -> runOnMessageReceived(event))
                .thenRunAsync(CommandCompletionGuard::decrementCommandCount);
    }

    public void runOnMessageReceived(@NotNull MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        String message = event.getMessage().getContentStripped();

        Matcher cardMatcher = cardPattern.matcher(message);

        List<FileUpload> fileUploads = new ArrayList<>();

        while (cardMatcher.find()) {
            String cardName = cardMatcher.group(2).strip();
            String cardType = cardMatcher.group(1);

            switch (cardType) {
                case "treachery" -> CardImages.getTreacheryCardImage(guild, cardName).ifPresent(fileUploads::add);
                case "leader" -> CardImages.getLeaderSkillImage(guild, cardName).ifPresent(fileUploads::add);
                case "stronghold" -> CardImages.getStrongholdImage(guild, cardName).ifPresent(fileUploads::add);
                case "nexus" -> CardImages.getNexusImage(guild, cardName).ifPresent(fileUploads::add);
            }
        }

        if (!fileUploads.isEmpty()) {
            event.getChannel().sendFiles(fileUploads).queue();
        }

        if (Objects.requireNonNull(event.getMember()).getUser().isBot()) return;

        //Add any other text based commands here
        DiscordGame discordGame;
        try {
            discordGame = new DiscordGame(event);
        } catch (ChannelNotFoundException e) {
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

        if (!mentionedPlayers.isEmpty())
            event.getChannel().sendMessage(StringUtils.join(mentionedPlayers, " ")).queue();

        if (event.getChannel() instanceof ThreadChannel threadChannel) {
            String channelName = threadChannel.getName();
            game.getFactions().stream()
                    .filter(f -> channelName.endsWith("-whispers")
                            && threadChannel.getParentChannel().getName().equals(f.getName().toLowerCase() + "-info"))
                    .map(f -> channelName.substring(0, channelName.indexOf("-")))
                    .map(n -> discordGame.tagEmojis(Emojis.getFactionEmoji(n)))
                    .findFirst().ifPresent(emoji -> event.getChannel()
                            .sendMessage("Use /player whisper if you wish to send a private message to " + emoji + ".").queue());
        }
    }
}
