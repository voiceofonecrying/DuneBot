package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;

import java.io.IOException;

public class HarkonnenFaction extends Faction {
    private Boolean hasTriggeredHT;
    public HarkonnenFaction(String player, String userName, Game game) throws IOException {
        super("Harkonnen", player, userName, game);

        setSpice(10);
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.emoji = Emojis.HARKONNEN;
        this.highThreshold = 7;
        this.lowThreshold = 6;
        this.occupiedIncome = 2;
        this.homeworld = "Giedi Prime";
        this.hasTriggeredHT = false;
        game.getTerritories().get("Carthag").addForces("Harkonnen", 10);
        Territory giediPrime = game.getTerritories().addHomeworld(homeworld);
        giediPrime.addForces(name, 10);
        game.getHomeworlds().put(name, homeworld);
        this.handLimit = 8;
    }

    public boolean hasTriggeredHT() {
        if (hasTriggeredHT == null) hasTriggeredHT = false;
        return hasTriggeredHT;
    }

    public void setTriggeredHT(boolean hasTriggeredHT) {
        this.hasTriggeredHT = hasTriggeredHT;
    }
}
