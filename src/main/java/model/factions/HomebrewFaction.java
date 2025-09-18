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
    String homeworldImageLinkTest;
    String homeworldImageMessage;
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
        String homeworldImageMessage;
        int highThreshold = 12;
        String highDescription;
        int lowThreshold = 11;
        String lowDescription;
        String occupiedDescription;
        int occupiedIncome = 2;
        int highBattleExplosion;
        int lowBattleExplosion;
        int lowRevivalCharity;
    }

    public void initalizeFromSpecs(FactionSpecs specs) {
        factionProxy = specs.factionProxy;
        spice = specs.spice;
        handLimit = specs.handLimit;
        freeRevival = specs.freeRevival;
        maxRevival = specs.maxRevival;
        for (FactionSpecs.LeaderSpecs ls : specs.leaders) {
            leaders.add(new Leader(ls.name, ls.value, name, factionProxy,  null, false));
            game.getTraitorDeck().add(new TraitorCard(ls.name, name, factionProxy, ls.value));
        }
        homeworld = specs.homeworld;
        homeworldProxy = specs.homeworldProxy;
        homeworldImageMessage = specs.homeworldImageMessage;
        highThreshold = specs.highThreshold;
        highDescription = specs.highDescription;
        lowThreshold = specs.lowThreshold;
        lowDescription = specs.lowDescription;
        occupiedDescription = specs.occupiedDescription;
        occupiedIncome = specs.occupiedIncome;
        highBattleExplosion = specs.highBattleExplosion;
        lowBattleExplosion = specs.lowBattleExplosion;
        lowRevivalCharity = specs.lowRevivalCharity;

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

    public String getHomeworldImageLinkTest() {
        return homeworldImageLinkTest;
    }

    public void setHomeworldImageLinkTest(String homeworldImageLinkTest) {
        this.homeworldImageLinkTest = homeworldImageLinkTest;
    }

    public String getHomeworldImageMessage() {
        return homeworldImageMessage;
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
