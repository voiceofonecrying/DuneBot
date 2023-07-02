package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

import java.io.IOException;

public class FremenFaction extends Faction {
    public FremenFaction(String player, String userName, Game game) throws IOException {
        super("Fremen", player, userName, game);

        setSpice(3);
        this.freeRevival = 3;
        this.reserves = new Force("Fremen", 17);
        this.specialReserves = new Force("Fremen*", 3);
        this.emoji = Emojis.FREMEN;
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     * @param territoryName The name of the Territory.
     * @param amount The amount of the force.
     * @param isSpecial Whether the force is special or not.
     * @param toTanks Whether the force is going to the tanks or not.
     */
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        String forceName = getName() + (isSpecial ? "*" : "");
        removeForces(territoryName, forceName, amount, toTanks, isSpecial, forceName);
    }
}
