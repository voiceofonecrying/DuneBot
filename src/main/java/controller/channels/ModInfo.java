package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;

public class ModInfo extends DiscordChannel {
    public ModInfo(DiscordGame discordGame) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getTextChannel("mod-info");
    }
}
