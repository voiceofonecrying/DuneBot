package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;

public class BiddingPhase extends DiscordChannel {
    public BiddingPhase(DiscordGame discordGame) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getTextChannel("bidding-phase");
    }
}
