package model;

import constants.Emojis;
import controller.channels.TurnSummary;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import model.factions.Faction;

public class TechToken {
    private final String name;
    private int spice;

    public TechToken(String name) {
        this.name = name;
        this.spice = 0;
    }

    public static void addSpice (Game game, DiscordGame discordGame, String techToken) throws ChannelNotFoundException {
        for (Faction faction : game.getFactions()) {
            if (faction.getTechTokens().isEmpty()) continue;
            for (TechToken tt : faction.getTechTokens()) {
                if (tt.getName().equals(techToken) && tt.spice == 0) {
                    tt.spice = faction.getTechTokens().size();
                    discordGame.getTurnSummary().queueMessage(tt.spice + " " + Emojis.SPICE + " is placed on " + Emojis.getTechTokenEmoji(techToken));
                }
            }
        }
    }

    public static void collectSpice(Game game, DiscordGame discordGame, String techToken) throws ChannelNotFoundException {
        for (Faction faction : game.getFactions()) {
            if (faction.getTechTokens().isEmpty()) continue;
            for (TechToken tt : faction.getTechTokens()) {
                if (tt.getName().equals(techToken) && tt.spice > 0) {
                    faction.addSpice(tt.spice);
                    discordGame.getTurnSummary().queueMessage(faction.getEmoji() +  " collects " + tt.spice + " " + Emojis.SPICE + " for " + Emojis.getTechTokenEmoji(techToken));
                    CommandManager.spiceMessage(discordGame, tt.spice, faction.getName(), "for " + Emojis.getTechTokenEmoji(techToken), true);
                    tt.spice = 0;
                    break;
                }
            }
        }
    }

    public String getName() {
        return name;
    }
}
