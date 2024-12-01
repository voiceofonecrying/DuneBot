package controller.listeners;

import caches.LastChannelMessageCache;
import constants.Emojis;
import controller.CommandCompletionGuard;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import utils.CardImages;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventListener extends ListenerAdapter {
    JDA jda;

    public EventListener(JDA jda) {
        this.jda = jda;
    }

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

        removeEmojiFromLastMessage(event);
        LastChannelMessageCache.setMessage(event.getChannel().getId(), event.getMessage());

        Matcher mentionMatcher = mentionPattern.matcher(message);

        Set<String> mentionedPlayers = new HashSet<>();

        while (mentionMatcher.find()) {
            String emojiName = mentionMatcher.group(1);

            if (emojiName.equals(Emojis.MOD_EMPEROR)) {
                mentionedPlayers.add(game.getModOrRoleMention());
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
                    .findFirst().ifPresent(emoji -> {
                        List<Button> buttons = new ArrayList<>();
                        String recipientLowerCase = threadChannel.getName().replace("-whispers", "");
                        buttons.add(Button.primary("whisper-" + recipientLowerCase + "-yes-" + event.getMessage().getContentRaw(), "Yes"));
                        buttons.add(Button.primary("whisper-" + recipientLowerCase + "-no", "No"));
                        MessageCreateBuilder response = new MessageCreateBuilder()
                                .setContent("Would you like to send this message as a whisper to " + emoji + "?\n" + event.getMessage().getContentRaw())
                                .addActionRow(buttons);
                        event.getChannel().sendMessage(response.build()).queue();
                    });
        }
    }

    /**
     * Removes all emoji reactions added by the bot from the last message in the channel where the event was triggered.
     * The method verifies if the last message in the cache originated from the same member as the current event's member
     * before attempting to remove reactions.
     *
     * @param event the event from which the channel and member context is obtained; provides information about
     *              the message received and the channel it was received in.
     */
    private void removeEmojiFromLastMessage(MessageReceivedEvent event) {
        if (LastChannelMessageCache.hasMessage(event.getChannel().getId())) {
            Message lastMessage = LastChannelMessageCache.getMessage(event.getChannel().getId());
            System.out.println("Message found in cache with message id: " + lastMessage.getId());
            try {
                lastMessage = lastMessage.getChannel().retrieveMessageById(lastMessage.getId()).complete();
            } catch (Exception e) {
                return;
            }
            System.out.println("Message retrieved.");
            if (lastMessage.getMember() != null && event.getMember() != null && event.getMember().getId().equals(lastMessage.getMember().getId())) {
                System.out.println("Getting reactions for the last message and deleting them.");
                lastMessage.getReactions().forEach(r -> r.removeReaction(jda.getSelfUser()).queue());
            }
        }
    }
}
