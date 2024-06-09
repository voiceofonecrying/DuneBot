package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;

import java.io.IOException;

public class FremenFaction extends Faction {
    public FremenFaction(String player, String userName, Game game) throws IOException {
        super("Fremen", player, userName, game);

        setSpice(3);
        this.freeRevival = 3;
        this.emoji = Emojis.FREMEN;
        this.highThreshold = 3;
        this.lowThreshold = 2;
        this.occupiedIncome = 0;
        this.homeworld = "Southern Hemisphere";
        Territory southernHemisphere = game.getTerritories().addHomeworld(homeworld);
        southernHemisphere.addForces(name, 17);
        southernHemisphere.addForces(name + "*", 3);
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
}
