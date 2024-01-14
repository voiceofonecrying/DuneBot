package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;

public class FactionLedger extends DiscordChannel {
    public FactionLedger(DiscordGame discordGame, String factionName) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getThreadChannel(factionName.toLowerCase() + "-info", "ledger");
    }
}
