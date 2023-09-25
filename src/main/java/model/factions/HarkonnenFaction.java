package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;
import model.Territory;

import java.io.IOException;

public class HarkonnenFaction extends Faction {
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
        game.getTerritories().put("Giedi Prime", new Territory("Giedi Prime", -1, false, false, false));
        game.getTerritory("Giedi Prime").addForce(new Force("Harkonnen", 10));
        game.getTerritories().get("Carthag").getForces().add(new Force("Harkonnen", 10));
        game.getHomeworlds().put(getName(), homeworld);
        this.handLimit = 8;
    }
}
