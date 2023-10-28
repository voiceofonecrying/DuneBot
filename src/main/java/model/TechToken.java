package model;

import constants.Emojis;
import model.factions.Faction;

public class TechToken {
    private final String name;
    private int spice;

    public TechToken(String name) {
        this.name = name;
        this.spice = 0;
    }

    public static void addSpice(Game game, String techToken) {
        for (Faction faction : game.getFactions()) {
            if (faction.getTechTokens().isEmpty()) continue;
            for (TechToken tt : faction.getTechTokens()) {
                if (tt.getName().equals(techToken) && tt.spice == 0) {
                    tt.spice = faction.getTechTokens().size();
                    game.getTurnSummary().publish(tt.spice + " " + Emojis.SPICE + " is placed on " + Emojis.getTechTokenEmoji(techToken));
                }
            }
        }
    }

    public static void collectSpice(Game game, String techToken) {
        for (Faction faction : game.getFactions()) {
            if (faction.getTechTokens().isEmpty()) continue;
            for (TechToken tt : faction.getTechTokens()) {
                if (tt.getName().equals(techToken) && tt.spice > 0) {
                    faction.addSpice(tt.spice);
                    game.getTurnSummary().publish(faction.getEmoji() + " collects " + tt.spice + " " + Emojis.SPICE + " for " + Emojis.getTechTokenEmoji(techToken));
                    faction.spiceMessage(tt.spice, "for " + Emojis.getTechTokenEmoji(techToken), true);
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
