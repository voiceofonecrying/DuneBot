package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;

public class FactionChat extends DiscordChannel {
    public FactionChat(DiscordGame discordGame, String factionName) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getThreadChannel(factionName.toLowerCase() + "-info", "chat");
    }
}
