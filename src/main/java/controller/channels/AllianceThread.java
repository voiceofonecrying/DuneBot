package controller.channels;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import model.factions.Faction;

public class AllianceThread extends DiscordChannel {
    public AllianceThread(DiscordGame discordGame, Faction faction, String allyName) throws ChannelNotFoundException {
        super(discordGame);
        try {
            this.messageChannel = discordGame.getThreadChannel("chat", faction.getName() + " " + allyName + " " + "Alliance");
        } catch (ChannelNotFoundException ignored) {}
        if (this.messageChannel == null)
            this.messageChannel = discordGame.getThreadChannel("chat", allyName + " " + faction.getName() + " " + "Alliance");
    }
}
