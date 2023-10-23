package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class FactionLedger extends DiscordChannel {
    public FactionLedger(DiscordGame discordGame, String factionName) throws ChannelNotFoundException {
        super(discordGame);
        TextChannel infoChannel = discordGame.getTextChannel(factionName.toLowerCase() + "-info");
        this.messageChannel = infoChannel.getThreadChannels().stream()
                .filter(channel -> channel.getName().equals("ledger"))
                .findFirst()
                .orElseThrow(() -> new ChannelNotFoundException("Ledger thread not found for " + factionName));
    }
}
