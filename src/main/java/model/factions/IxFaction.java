package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class IxFaction extends Faction {
    public IxFaction(String player, String userName, Game game) {
        super("Ix", player, userName, game);

        setSpice(10);
        this.freeRevival = 1;
        this.reserves = new Force("Ix", 10);
        this.specialReserves = new Force("Ix*", 4);
        this.emoji = Emojis.IX;
        game.getTerritories().get("Hidden Mobile Stronghold").getForces().add(new Force("Ix", 3));
        game.getTerritories().get("Hidden Mobile Stronghold").getForces().add(new Force("Ix*", 3));
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
