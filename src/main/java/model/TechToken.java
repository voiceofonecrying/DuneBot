package model;

import constants.Emojis;
import model.factions.Faction;

public class TechToken {
    public static final String AXLOTL_TANKS = "Axlotl Tanks";
    public static final String HEIGHLINERS = "Heighliners";
    public static final String SPICE_PRODUCTION = "Spice Production";

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
                    game.getTurnSummary().publish(faction.getEmoji() + " collects " + tt.spice + " " + Emojis.SPICE + " for " + Emojis.getTechTokenEmoji(techToken));
                    faction.addSpice(tt.spice, "for " + Emojis.getTechTokenEmoji(techToken));
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
