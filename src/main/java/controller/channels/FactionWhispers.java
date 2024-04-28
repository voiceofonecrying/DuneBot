package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.Game;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FactionWhispers extends DiscordChannel {
    boolean thread;

    public FactionWhispers(DiscordGame discordGame, Game game, String factionName, String interlocutorName) throws ChannelNotFoundException {
        super(discordGame);
        String infoChannelName = factionName.toLowerCase() + "-info";
        TextChannel factionInfo = discordGame.getTextChannel(infoChannelName);

        thread = true;
        String whisperThreadName = interlocutorName.toLowerCase() + "-whispers";
        Optional<ThreadChannel> optThread = discordGame.getOptionalThreadChannel(infoChannelName, whisperThreadName);
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            List<String> userIds = new ArrayList<>();
            userIds.add(game.getFaction(factionName).getPlayer());
            discordGame.createPrivateThread(factionInfo, whisperThreadName, userIds);
            optThread = factionInfo.getThreadChannels().stream()
                    .filter(channel -> channel.getName().equals(whisperThreadName))
                    .findFirst();
            optThread.ifPresent(threadChannel -> this.messageChannel = threadChannel);
            optThread.ifPresent(threadChannel -> threadChannel.getManager().setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_3_DAYS).queue());
        }
    }
}
