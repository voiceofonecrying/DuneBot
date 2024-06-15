package model.factions;

import constants.Emojis;
import enums.GameOption;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.TleilaxuTanks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        Territory southernHemisphere = game.getTerritories().addHomeworld(game, homeworld, name);
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

    public int countFreeStarredRevival() {
        TleilaxuTanks tanks = game.getTleilaxuTanks();
        if (tanks.getForceStrength(name + "*") > 0) {
            if (game.hasGameOption(GameOption.HOMEWORLDS) && isHighThreshold()) {
                List<DuneChoice> choices = new ArrayList<>();
                for (Territory territory : game.getTerritories().values()) {
                    if (!territory.getActiveFactionNames().contains("Fremen")) continue;
                    choices.add(new DuneChoice("danger", "fremen-ht-" + territory.getTerritoryName(), territory.getTerritoryName()));
                }
                choices.add(new DuneChoice("fremen-cancel", "Don't use HT advantage"));
                chat.publish("You are at high threshold, where would you like to place your revived " + Emojis.FREMEN_FEDAYKIN + "?", choices);
            }
            return 1;
        }
        return 0;
    }
}
