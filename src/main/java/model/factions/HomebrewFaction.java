package model.factions;

import constants.Colors;
import constants.Emojis;
import model.*;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class HomebrewFaction extends Faction{
    Boolean emojisSetup = false;
    String colorHexCode;
    String color;
    String factionProxy;
    String homeworldProxy;
    String colorProxy;
    String highDescription;
    String lowDescription;
    String occupiedDescription;
    int highBattleExplosion;
    int lowBattleExplosion;
    int lowRevivalCharity;

    public HomebrewFaction(String name, String player, String userName) throws IOException {
        super(name, player, userName);
    }

    public static class FactionSpecs {
        public static class LeaderSpecs {
            String name;
            int value;
        }

        Boolean emojisSetup;
        String factionProxy;
        String color;
        int spice;
        int handLimit = 4;
        int freeRevival = 1;
        int maxRevival = 3;
        List<LeaderSpecs> leaders;
        String homeworld;
        String homeworldProxy;
        int highThreshold = 12;
        String highDescription;
        int lowThreshold = 11;
        String lowDescription;
        String occupiedDescription;
        int occupiedIncome = 2;
        int highBattleExplosion;
        int lowBattleExplosion;
        int lowRevivalCharity;

        public String getFactionProxy() {
            return factionProxy;
        }
    }

    public void initalizeFromSpecs(FactionSpecs specs) {
        if (specs.emojisSetup != null)
            emojisSetup = specs.emojisSetup;

        setFactionProxy(specs.factionProxy);
        if (specs.color != null)
            colorHexCode = specs.color;
        else {
            Color decodedColor = Colors.getFactionColor(factionProxy);
            colorHexCode = String.format("#%02x%02x%02x", decodedColor.getRed(), decodedColor.getGreen(), decodedColor.getBlue());
        }
        spice = specs.spice;
        handLimit = specs.handLimit;
        freeRevival = specs.freeRevival;
        maxRevival = specs.maxRevival;
        String emojiFaction = emojisSetup ? name : factionProxy;
        for (FactionSpecs.LeaderSpecs ls : specs.leaders) {
            Leader leader = new Leader(ls.name, ls.value, name, emojiFaction,  null, false);
            leaders.add(leader);
            game.getTraitorDeck().add(new TraitorCard(ls.name, name, emojiFaction, ls.value));
        }
        homeworld = specs.homeworld;
        if (specs.homeworldProxy != null)
            homeworldProxy = specs.homeworldProxy;
        highThreshold = specs.highThreshold;
        highDescription = specs.highDescription;
        lowThreshold = specs.lowThreshold;
        lowDescription = specs.lowDescription;
        occupiedDescription = specs.occupiedDescription;
        occupiedIncome = specs.occupiedIncome;
        highBattleExplosion = specs.highBattleExplosion;
        lowBattleExplosion = specs.lowBattleExplosion;
        lowRevivalCharity = specs.lowRevivalCharity;

        Territory hwTerritory = game.getTerritories().addHomeworld(game, homeworld, name);
        hwTerritory.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    public String getFactionProxy() {
        return factionProxy;
    }

    // Temporary migration function. Can be removed after games 183, 186, and 187 reload from json
    public void setupColorHexCode() {
        if (color != null)
            colorHexCode = color;
        else if (colorProxy != null) {
            Color decodedColor = Colors.getFactionColor(colorProxy);
            colorHexCode = String.format("#%02x%02x%02x", decodedColor.getRed(), decodedColor.getGreen(), decodedColor.getBlue());
        } else {
            Color decodedColor = Colors.getFactionColor(factionProxy);
            colorHexCode = String.format("#%02x%02x%02x", decodedColor.getRed(), decodedColor.getGreen(), decodedColor.getBlue());
        }
    }

    public void setFactionProxy(String factionProxy) {
        if(emojisSetup) {
            emoji = ":" + name.toLowerCase() + ":";
            forceEmoji = ":" + name.toLowerCase() + "_troop:";
            this.factionProxy = name;
        } else {
            this.factionProxy = factionProxy;
            emoji = Emojis.getFactionEmoji(factionProxy);
            forceEmoji = Emojis.getForceEmoji(factionProxy);

            game.getTraitorDeck().stream().filter(t -> t.getFactionName().equals(name)).forEach(t -> t.setEmojiFaction(factionProxy));
            for (Faction f : game.getFactions())
                f.getTraitorHand().stream().filter(t -> t.getFactionName().equals(name)).forEach(t -> t.setEmojiFaction(factionProxy));
        }

        HashMap<String, String> homeworldName = new HashMap<>();
        homeworldName.put("Atreides", "Caladan");
        homeworldName.put("BG", "Wallach IX");
        homeworldName.put("BT", "Tleilax");
        homeworldName.put("CHOAM", "Tupile");
        homeworldName.put("Ecaz", "Ecaz");
        homeworldName.put("Emperor", "Salusa Secundus");
        homeworldName.put("Fremen", "Southern Hemisphere");
        homeworldName.put("Guild", "Junction");
        homeworldName.put("Harkonnen", "Giedi Prime");
        homeworldName.put("Ix", "Ix");
        homeworldName.put("Moritani", "Grumman");
        homeworldName.put("Richese", "Richese");
        homeworldProxy = homeworldName.get(factionProxy);
    }

    public String getHomeworldProxy() {
        return homeworldProxy;
    }

    public String getHighDescription() {
        return highDescription;
    }

    public String getLowDescription() {
        return lowDescription;
    }

    public String getOccupiedDescription() {
        return occupiedDescription;
    }

    public int getHighBattleExplosion() {
        return highBattleExplosion;
    }

    public int getLowBattleExplosion() {
        return lowBattleExplosion;
    }

    public int getLowRevivalCharity() {
        return lowRevivalCharity;
    }

    @Override
    public Color getColor() {
        return Color.decode(colorHexCode);
    }

    @Override
    protected HomeworldTerritory getHomeworldTerritory() {
        Territory territory = game.getTerritory(homeworld);
        if (!(territory instanceof HomeworldTerritory)) {
            game.getTerritories().remove(homeworld, territory);
            HomeworldTerritory hwt = game.getTerritories().addHomeworld(game, homeworld, name);
            territory.getForces().forEach(f -> hwt.callParentAddForces(f.getName(), f.getStrength()));
        }
        return (HomeworldTerritory) game.getTerritory(homeworld);
    }
}
