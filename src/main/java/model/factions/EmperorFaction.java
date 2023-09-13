package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

import java.io.IOException;

public class EmperorFaction extends Faction {
    private final int secundusHighThreshold;
    private final int secundusLowThreshold;
    private final int secundusOccupiedIncome;

    public EmperorFaction(String player, String userName, Game game) throws IOException {
        super("Emperor", player, userName, game);

        setSpice(10);
        this.freeRevival = 1;
        this.reserves = new Force("Emperor", 15);
        this.specialReserves = new Force("Emperor*", 5);
        this.emoji = Emojis.EMPEROR;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
        this.secundusHighThreshold = 2;
        this.secundusLowThreshold = 2;
        this.secundusOccupiedIncome = 0;
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     * @param territoryName The name of the Territory.
     * @param amount The amount of the force.
     * @param isSpecial Whether the force is special or not.
     * @param toTanks Whether the force is going to the tanks or not.
     */
    @Override
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        String forceName = getName() + (isSpecial ? "*" : "");
        removeForces(territoryName, forceName, amount, toTanks, isSpecial, forceName);
    }
    public int getSecundusHighThreshold() {
        return secundusHighThreshold;
    }

    public int getSecundusLowThreshold() {
        return secundusLowThreshold;
    }

    public int getSecundusOccupiedIncome() {
        return secundusOccupiedIncome;
    }
}
