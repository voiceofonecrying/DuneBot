package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;
import java.util.Optional;

public class ModLedger extends DiscordChannel {
    public ModLedger(DiscordGame discordGame) throws ChannelNotFoundException {
        super(discordGame);
        TextChannel modInfo = discordGame.getTextChannel("mod-info");

        String modLedgerThreadName = "ledger";
        Optional<ThreadChannel> optThread = discordGame.getOptionalThreadChannel("mod-info", modLedgerThreadName);
        if (optThread.isPresent()) {
            this.messageChannel = optThread.get();
        } else {
            discordGame.createPrivateThread(modInfo, modLedgerThreadName, List.of());
            optThread = modInfo.getThreadChannels().stream()
                    .filter(channel -> channel.getName().equals(modLedgerThreadName))
                    .findFirst();
            optThread.ifPresent(threadChannel -> this.messageChannel = threadChannel);
        }
    }
}
