package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.factions.Faction;

public class FactionLedger extends DiscordChannel {
    public FactionLedger(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getThreadChannel(faction.getInfoChannelPrefix() + "-info", "ledger");
    }
}
