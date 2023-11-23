package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;

public class GameActions extends DiscordChannel {
    public GameActions(DiscordGame discordGame) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getTextChannel("game-actions");
    }
}
