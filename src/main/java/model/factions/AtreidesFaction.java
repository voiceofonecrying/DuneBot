package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class AtreidesFaction extends Faction {
    private int forcesLost;

    public AtreidesFaction(String player, String userName, Game game) {
        super("Atreides", player, userName, game);

        setSpice(10);
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.reserves = new Force("Atreides", 10);
        this.emoji = Emojis.ATREIDES;
        game.getTerritories().get("Arrakeen").getForces().add(new Force("Atreides", 10));

        this.forcesLost = 0;
    }

    /**
     * @return The number of forces lost in battle by the Atreides player
     */
    public int getForcesLost() {
        return forcesLost;
    }

    /**
     * @param forcesLost The number of forces lost in battle by the Atreides player
     */
    public void setForcesLost(int forcesLost) {
        this.forcesLost = forcesLost;
    }

    /**
     * @param forceLost The number of forces lost in battle by the Atreides player
     */
    public void addForceLost(int forceLost) {
        setForcesLost(this.forcesLost + forceLost);
    }

    /**
     * @return Whether the Atreides player has earned the Kwisatz Haderach
     */
    public boolean isHasKH() {
        return forcesLost >= 7;
    }
}
