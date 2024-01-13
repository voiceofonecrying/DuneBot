package controller.channels;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.Optional;

public class FactionChat extends DiscordChannel {
    public FactionChat(DiscordGame discordGame, String factionName) throws ChannelNotFoundException {
        super(discordGame);
        this.messageChannel = discordGame.getThreadChannel(factionName.toLowerCase() + "-info", "chat");
    }
}
