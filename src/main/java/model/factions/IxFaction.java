package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;

import java.io.IOException;

public class IxFaction extends Faction {
    public IxFaction(String player, String userName, Game game) throws IOException {
        super("Ix", player, userName, game);

        setSpice(10);
        this.freeRevival = 1;
        this.emoji = Emojis.IX;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
        this.homeworld = "Ix";
        game.getTerritories().get("Hidden Mobile Stronghold").addForces("Ix", 3);
        game.getTerritories().get("Hidden Mobile Stronghold").addForces("Ix*", 3);
        Territory ix = game.getTerritories().addHomeworld(game, homeworld, name);
        ix.addForces(name, 10);
        ix.addForces(name + "*", 4);
        game.getHomeworlds().put(name, homeworld);
    }

    @Override
    public boolean hasStarredForces() {
        return true;
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     *
     * @param territoryName The name of the Territory.
     * @param amount        The amount of the force.
     * @param isSpecial     Whether the force is special or not.
     * @param toTanks       Whether the force is going to the tanks or not.
     */
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        String forceName = getName() + (isSpecial ? "*" : "");
        removeForces(territoryName, forceName, amount, toTanks, isSpecial);
    }

    /**
     * Get the total spice that would be collected from a territory.  This function does not actually add or subtract
     * spice.  It only calculates the total
     *
     * @param territory The territory to calculate the spice from
     * @return The total spice that would be collected from the territory
     */
    @Override
    public int getSpiceCollectedFromTerritory(Territory territory) {
        int multiplier = hasMiningEquipment() ? 3 : 2;
        int spiceFromSuboids = territory.getForceStrength("Ix") * multiplier;
        int spiceFromCyborgs = territory.getForceStrength("Ix*") * 3;

        int totalSpice = spiceFromSuboids + spiceFromCyborgs;
        return Math.min(totalSpice, territory.getSpice());
    }
}
