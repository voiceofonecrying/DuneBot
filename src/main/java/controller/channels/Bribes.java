package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;

public class Bribes extends DiscordChannel {
    public Bribes(DiscordGame discordGame) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getTextChannel("bribes");
    }
}
