package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;
import model.Territory;

import java.io.IOException;

public class EmperorFaction extends Faction {
    private final int secundusHighThreshold;
    private final int secundusLowThreshold;
    private final int secundusOccupiedIncome;
    private String secondHomeworld;
    private boolean isSecundusHighThreshold;

    public EmperorFaction(String player, String userName, Game game) throws IOException {
        super("Emperor", player, userName, game);

        setSpice(10);
        this.freeRevival = 1;
        this.emoji = Emojis.EMPEROR;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.homeworld = "Kaitain";
        this.secondHomeworld = "Salusa Secundus";
        game.getTerritories().put("Kaitain", new Territory("Kaitain", -1, false, false, false));
        game.getTerritory("Kaitain").addForce(new Force("Emperor", 15));
        game.getTerritories().put("Salusa Secundus", new Territory("Salusa Secundus", -1, false, false, false));
        game.getTerritory("Salusa Secundus").addForce(new Force("Emperor*", 5));
        game.getHomeworlds().put(getName(), homeworld);
        game.getHomeworlds().put(getName() + "*", secondHomeworld);
        this.occupiedIncome = 2;
        this.secundusHighThreshold = 2;
        this.secundusLowThreshold = 2;
        this.secundusOccupiedIncome = 0;
        this.isSecundusHighThreshold = true;
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

    public String getSecondHomeworld() {
        return secondHomeworld;
    }

    public boolean isSecundusHighThreshold() {
        return isSecundusHighThreshold;
    }

    public void setSecundusHighThreshold(boolean secundusHighThreshold) {
        isSecundusHighThreshold = secundusHighThreshold;
    }

    public void setSecondHomeworld(String secondHomeworld) {
        this.secondHomeworld = secondHomeworld;
    }

    @Override
    public Force getSpecialReserves() {
        if (this.specialReserves != null) return this.specialReserves;
        return getGame().getTerritory(getSecondHomeworld()).getForce("Emperor*");
    }
}
