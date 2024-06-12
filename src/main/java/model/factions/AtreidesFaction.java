package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Game;
import model.Territory;

import java.io.IOException;

public class AtreidesFaction extends Faction {
    private int forcesLost;

    public AtreidesFaction(String player, String userName, Game game) throws IOException {
        super("Atreides", player, userName, game);

        setSpice(10);
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.emoji = Emojis.ATREIDES;
        this.highThreshold = 6;
        this.lowThreshold = 5;
        this.occupiedIncome = 2;
        this.homeworld = "Caladan";
        game.getTerritory("Arrakeen").addForces("Atreides", 10);
        Territory caladan = game.getTerritories().addHomeworld(game, homeworld, name);
        caladan.addForces(name, 10);
        game.getHomeworlds().put(name, homeworld);

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
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    /**
     * @param forceLost The number of forces lost in battle by the Atreides player
     */
    public void addForceLost(int forceLost) {
        setForcesLost(this.forcesLost + forceLost);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    /**
     * @return Whether the Atreides player has earned the Kwisatz Haderach
     */
    public boolean isHasKH() {
        return forcesLost >= 7;
    }
}
