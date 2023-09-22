package controller.channels;

import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;
import java.util.Optional;

public class TurnSummary extends DiscordChannel {
    boolean thread;

    public TurnSummary(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        super(discordGame);
        thread = true;
        String turnSummaryName = "turn-summary";
        String turnNumSummaryName = "turn-" + game.getTurn() + "-summary";
        TextChannel frontOfShield = discordGame.getTextChannel("front-of-shield");
        Optional<ThreadChannel> optThread = frontOfShield.getThreadChannels().stream().filter(channel -> channel.getName().equals(turnNumSummaryName)).findFirst();
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            optThread = frontOfShield.getThreadChannels().stream().filter(channel -> channel.getName().equals(turnSummaryName)).findFirst();
            if (optThread.isPresent()) {
                this.messageChannel = optThread.get();
            } else {
                this.messageChannel = discordGame.getTextChannel(turnSummaryName);
                thread = false;
            }
        }
    }

    public void addUser(String playerName) {
        if (thread) discordGame.addUsersToThread((ThreadChannel)messageChannel, List.of(playerName));
    }
}
