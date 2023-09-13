package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;
import model.Territory;

import java.io.IOException;

public class IxFaction extends Faction {
    public IxFaction(String player, String userName, Game game) throws IOException {
        super("Ix", player, userName, game);

        setSpice(10);
        this.freeRevival = 1;
        this.reserves = new Force("Ix", 10);
        this.specialReserves = new Force("Ix*", 4);
        this.emoji = Emojis.IX;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
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

    /**
     * Get the total spice that would be collected from a territory.  This function does not actually add or subtract
     * spice.  It only calculates the total
     * @param territory The territory to calculate the spice from
     * @return The total spice that would be collected from the territory
     */
    @Override
    public int getSpiceCollectedFromTerritory(Territory territory) {
        int multiplier = hasMiningEquipment() ? 3 : 2;
        int spiceFromSuboids = territory.hasForce("Ix") ? territory.getForce("Ix").getStrength() * multiplier : 0;
        int spiceFromCyborge = territory.hasForce("Ix*") ? territory.getForce("Ix*").getStrength() * 3 : 0;

        int totalSpice = spiceFromSuboids + spiceFromCyborge;
        return Math.min(totalSpice, territory.getSpice());
    }
}
