package model.factions;

import constants.Colors;
import constants.Emojis;
import model.*;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class HomebrewFaction extends Faction{
    String factionProxy;
    String homeworldProxy;
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

        String factionProxy;
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
        setFactionProxy(specs.factionProxy);
        spice = specs.spice;
        handLimit = specs.handLimit;
        freeRevival = specs.freeRevival;
        maxRevival = specs.maxRevival;
        for (FactionSpecs.LeaderSpecs ls : specs.leaders) {
            Leader leader = new Leader(ls.name, ls.value, name, factionProxy,  null, false);
            leaders.add(leader);
            game.getTraitorDeck().add(new TraitorCard(ls.name, name, factionProxy, ls.value));
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

    public void setFactionProxy(String factionProxy) {
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
        this.factionProxy = factionProxy;
        emoji = Emojis.getFactionEmoji(factionProxy);
        forceEmoji = Emojis.getForceEmoji(factionProxy);
        homeworldProxy = homeworldName.get(factionProxy);
        game.getTraitorDeck().stream().filter(t -> t.getFactionName().equals(name)).forEach(t -> t.setEmojiFaction(factionProxy));
        for (Faction f : game.getFactions())
            f.getTraitorHand().stream().filter(t -> t.getFactionName().equals(name)).forEach(t -> t.setEmojiFaction(factionProxy));
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
        return Colors.getFactionColor(factionProxy);
    }

    @Override
    protected HomeworldTerritory getHomeworldTerritory() {
        Territory territory = game.getTerritory(homeworld);
        if (!(territory instanceof HomeworldTerritory)) {
            game.getTerritories().remove(homeworld, territory);
            Territory hwt = game.getTerritories().addHomeworld(game, homeworld, name);
            territory.getForces().forEach(f -> hwt.addForces(f.getName(), f.getStrength()));
        }
        return (HomeworldTerritory) game.getTerritory(homeworld);
    }
}
