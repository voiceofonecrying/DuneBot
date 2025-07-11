package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TurnSummary extends DiscordChannel {
    public TurnSummary(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        super(discordGame);
        TextChannel frontOfShield = discordGame.getTextChannel("front-of-shield");

        String turnNumSummaryName = "turn-" + game.getTurn() + "-summary";
        Optional<ThreadChannel> optThread = discordGame.getOptionalThreadChannel("front-of-shield", turnNumSummaryName);
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            List<String> userIds = new ArrayList<>();
//            for (Faction faction : game.getFactions()) {
//                userIds.add(faction.getPlayer());
//            }
            discordGame.createPublicThread(frontOfShield, turnNumSummaryName, userIds);
            optThread = frontOfShield.getThreadChannels().stream()
                    .filter(channel -> channel.getName().equals(turnNumSummaryName))
                    .findFirst();
            optThread.ifPresent(threadChannel -> this.messageChannel = threadChannel);
        }
        String prevTurnSummaryName = "turn-" + (game.getTurn() - 1) + "-summary";
        optThread = discordGame.getOptionalThreadChannel("front-of-shield", prevTurnSummaryName);
        optThread.ifPresent(threadChannel -> threadChannel.getManager().setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS).queue());
    }

    public void addUser(String playerName) {
        discordGame.addUsersToThread((ThreadChannel) messageChannel, List.of(playerName));
    }
}
