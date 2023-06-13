package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;
import model.IntegerResource;

public class AtreidesFaction extends Faction {
    public AtreidesFaction(String player, String userName, Game game) {
        super("Atreides", player, userName, game);

        this.spice = 10;
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.reserves = new Force("Atreides", 10);
        this.emoji = Emojis.ATREIDES;
        this.resources.add(new IntegerResource("forces_lost", 0, 0, 7));
        game.getTerritories().get("Arrakeen").getForces().add(new Force("Atreides", 10));
    }
}
