package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

import java.io.IOException;

public class HarkonnenFaction extends Faction {
    public HarkonnenFaction(String player, String userName, Game game) throws IOException {
        super("Harkonnen", player, userName, game);

        setSpice(10);
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.reserves = new Force("Harkonnen", 10);
        this.emoji = Emojis.HARKONNEN;
        this.highThreshold = 7;
        this.lowThreshold = 6;
        this.occupiedIncome = 2;
        game.getTerritories().get("Carthag").getForces().add(new Force("Harkonnen", 10));
        this.handLimit = 8;
    }
}
