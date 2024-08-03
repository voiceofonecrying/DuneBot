package model.factions;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.TleilaxuTanks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public int countFreeStarredRevival() {
        return Math.min(getFreeRevival(), game.getTleilaxuTanks().getForceStrength(name + "*"));
    }

    @Override
    public int baseRevivalCost(int regular, int starred) {
        return regular * 2 + starred * 3;
    }

    @Override
    public void presentPaidRevivalChoices(int numRevived) throws InvalidGameStateException {
        TleilaxuTanks tanks = game.getTleilaxuTanks();
        if (tanks.getForceStrength(name + "*") == 0 || spice < revivalCost(0, 1))
            game.getRevival().setCyborgRevivalComplete(true);
        boolean cyborgRevivalComplete = game.getRevival().isCyborgRevivalComplete();
        String idPrefix;
        String idSuffix;
        String labelSuffix;
        String chatMessage;
        if (cyborgRevivalComplete) {
            idPrefix = "revive-";
            idSuffix = "";
            labelSuffix = " Suboid";
            chatMessage = "Would you like to purchase additional " + Emojis.IX_SUBOID + " revivals? " + player;
        } else {
            idPrefix = "revive*-";
            idSuffix = "-" + numRevived;
            labelSuffix = " Cyborg";
            chatMessage = "Would you like to purchase additional " + Emojis.IX_CYBORG + " revivals? " + player;
            if (tanks.getForceStrength(name) > 0)
                chatMessage += "\n" + Emojis.IX_SUBOID + " revivals if possible would be the next step.";
            else
                chatMessage += "\nThere are no " + Emojis.IX_SUBOID + " in the tanks.";
        }
        if (getMaxRevival() > numRevived) {
            int revivableForces = cyborgRevivalComplete ? tanks.getForceStrength(name) : tanks.getForceStrength(name + "*");
            if (revivableForces > 0) {
                List<DuneChoice> choices = new ArrayList<>();
                int maxButton = Math.min(revivableForces, getMaxRevival() - numRevived);
                for (int i = 0; i <= maxButton; i++) {
                    DuneChoice choice = new DuneChoice(idPrefix + i + idSuffix, i + labelSuffix);
                    choice.setDisabled(cyborgRevivalComplete && spice < revivalCost(i, 0) || !cyborgRevivalComplete && spice < revivalCost(0, 1));
                    choices.add(choice);
                }
                chat.publish(chatMessage, choices);
            } else {
                game.getTurnSummary().publish(emoji + " has no forces in the tanks");
            }
        } else {
            game.getTurnSummary().publish(emoji + " has revived their maximum");
        }
    }
}
