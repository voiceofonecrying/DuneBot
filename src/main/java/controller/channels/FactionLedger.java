package controller.channels;

import exceptions.ChannelNotFoundException;
import model.DiscordGame;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.Optional;

public class FactionLedger extends DiscordChannel {
    public FactionLedger(DiscordGame discordGame, String factionName) throws ChannelNotFoundException {
        super(discordGame);
        TextChannel infoChannel = discordGame.getTextChannel(factionName.toLowerCase() + "-info");
        Optional<ThreadChannel> optThread = infoChannel.getThreadChannels().stream()
                .filter(channel -> channel.getName().equals("ledger")).findFirst();
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            this.messageChannel = discordGame.getTextChannel(factionName.toLowerCase() + "-info");
        }
    }
}
