package model.factions;

import constants.Colors;
import constants.Emojis;
import model.*;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class HomebrewFaction extends Faction{
    String factionProxy;
    String homeworldProxy;

    public HomebrewFaction(String name, String player, String userName) throws IOException {
        super(name, player, userName);
    }

    public static class FactionSpecs {
        public static class LeaderSpecs {
            String name;
            int value;
        }

        String factionProxy;
        String homeworld;
        String homeworldProxy;
        int spice;
        int handLimit = 4;
        int freeRevival = 1;
        int maxRevival = 3;
        List<LeaderSpecs> leaders;
        int highThreshold = 12;
        int lowThreshold = 11;
        int occupiedIncome = 2;
    }

    public void initalizeFromSpecs(FactionSpecs specs) {
        factionProxy = specs.factionProxy;
        homeworld = specs.homeworld;
        homeworldProxy = specs.homeworldProxy;
        spice = specs.spice;
        handLimit = specs.handLimit;
        freeRevival = specs.freeRevival;
        maxRevival = specs.maxRevival;
        highThreshold = specs.highThreshold;
        lowThreshold = specs.lowThreshold;
        occupiedIncome = specs.occupiedIncome;
        for (FactionSpecs.LeaderSpecs ls : specs.leaders) {
            leaders.add(new Leader(ls.name, ls.value, name, factionProxy,  null, false));
            game.getTraitorDeck().add(new TraitorCard(ls.name, name, factionProxy, ls.value));
        }

        emoji = Emojis.getFactionEmoji(factionProxy);
        Territory hwTerritory = game.getTerritories().addHomeworld(game, homeworld, name);
        hwTerritory.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    public String getFactionProxy() {
        return factionProxy;
    }

    public String getHomeworldProxy() {
        return homeworldProxy;
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
