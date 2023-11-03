package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TurnSummary extends DiscordChannel {
    boolean thread;

    public TurnSummary(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        super(discordGame);
        TextChannel frontOfShield = discordGame.getTextChannel("front-of-shield");
        Optional<ThreadChannel> optThread;
        thread = true;
        String turnNumSummaryName = "turn-" + game.getTurn() + "-summary";
        optThread = frontOfShield.getThreadChannels().stream().filter(channel -> channel.getName().equals(turnNumSummaryName)).findFirst();
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            List<String> userIds = new ArrayList<>();
            for (Faction faction : game.getFactions()) {
                userIds.add(faction.getPlayer());
            }
            userIds.add(game.getMod());
            discordGame.createPublicThread(frontOfShield, turnNumSummaryName, userIds);
            optThread = frontOfShield.getThreadChannels().stream()
                    .filter(channel -> channel.getName().equals(turnNumSummaryName))
                    .findFirst();
            optThread.ifPresent(threadChannel -> this.messageChannel = threadChannel);
        }
        String prevTurnSummaryName = "turn-" + (game.getTurn() - 1) + "-summary";
        optThread = frontOfShield.getThreadChannels().stream().filter(channel -> channel.getName().equals(prevTurnSummaryName)).findFirst();
        optThread.ifPresent(threadChannel -> threadChannel.getManager().setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS).queue());
    }

    public void addUser(String playerName) {
        if (thread) discordGame.addUsersToThread((ThreadChannel) messageChannel, List.of(playerName));
    }
}
