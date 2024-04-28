package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Whispers extends DiscordChannel {
    public Whispers(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        super(discordGame);
        TextChannel frontOfShield = discordGame.getTextChannel("front-of-shield");

        String whispersThreadName = "whispers";
        Optional<ThreadChannel> optThread = discordGame.getOptionalThreadChannel("front-of-shield", whispersThreadName);
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            List<String> userIds = new ArrayList<>();
            for (Faction faction : game.getFactions()) {
                userIds.add(faction.getPlayer());
            }
            userIds.add(game.getMod());
            discordGame.createPublicThread(frontOfShield, whispersThreadName, userIds);
            optThread = frontOfShield.getThreadChannels().stream()
                    .filter(channel -> channel.getName().equals(whispersThreadName))
                    .findFirst();
            optThread.ifPresent(threadChannel -> this.messageChannel = threadChannel);
        }
    }
}
