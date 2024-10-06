package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.factions.Faction;

public class FactionChat extends DiscordChannel {
    public FactionChat(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getThreadChannel(faction.getName().toLowerCase() + "-info", "chat");
    }
}
