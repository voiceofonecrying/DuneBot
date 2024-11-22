package model.factions;

import constants.Emojis;
import enums.GameOption;
import model.DuneChoice;
import model.Game;
import model.Territory;
import model.TleilaxuTanks;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FremenFaction extends Faction {
    List<String> wormRides;
    boolean wormRideActive;
    int wormsToPlace;

    public FremenFaction(String player, String userName) throws IOException {
        super("Fremen", player, userName);

        this.spice = 3;
        this.freeRevival = 3;
        this.emoji = Emojis.FREMEN;
        this.highThreshold = 3;
        this.lowThreshold = 2;
        this.occupiedIncome = 0;
        this.homeworld = "Southern Hemisphere";
        this.wormRides = null;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
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

    public boolean hasRidesRemaining() {
        return wormRides != null;
    }

    public void addWormRide(String rideFrom) {
        if (wormRides == null)
            wormRides = new ArrayList<>();
        wormRides.add(rideFrom);
    }

    public void presentNextWormRideChoices() {
        if (wormRides == null || wormRides.isEmpty())
            return;
        String ridingFrom = wormRides.removeFirst();
        presentWormRideChoices(ridingFrom);
        if (wormRides.isEmpty())
            wormRides = null;
    }

    public void presentWormRideChoices(String territoryName) {
        String wormName = territoryName.equals(homeworld) ? "Great Maker" : "Shai-Hulud";
        Territory territory = getGame().getTerritory(territoryName);
        String fremenForces = "";
        int strength = territory.getForceStrength("Fremen");
        if (strength > 0)
            fremenForces += strength + " " + Emojis.FREMEN_TROOP + " ";
        strength = territory.getForceStrength("Fremen*");
        if (strength > 0)
            fremenForces += strength + " " + Emojis.FREMEN_FEDAYKIN + " ";
        if (fremenForces.isEmpty()) {
            game.getTurnSummary().publish(Emojis.FREMEN_TROOP + " have no forces in " + territoryName + " to ride " + wormName + ".");
            return;
        }
        game.getTurnSummary().publish(fremenForces + "may ride " + wormName + " from " + territoryName + "!");
        getMovement().setMovingFrom(territoryName);
        String buttonSuffix = "-fremen-ride";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("stronghold" + buttonSuffix, "Stronghold"));
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("rock" + buttonSuffix, "Rock Territories"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap) choices.add(new DuneChoice("discovery-tokens" + buttonSuffix, "Discovery Tokens"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Somewhere else"));
        choices.add(new DuneChoice("danger", "pass-shipment" + buttonSuffix, "No ride"));
        chat.publish("Where would you like to ride to from " + territoryName + "? " + player, choices);
        wormRideActive = true;
    }

    public boolean isWormRideActive() {
        return wormRideActive;
    }

    public void setWormRideActive(boolean wormRideActive) {
        this.wormRideActive = wormRideActive;
    }

    public void presentWormPlacementChoices(String territoryName, String wormName) {
        boolean greatMaker = wormName.equals("Great Maker");
        getMovement().setMovingFrom(territoryName);
        String buttonSuffix = greatMaker ? "-place-great-maker" : "-place-shai-hulud";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Other Sand Territories"));
        choices.add(new DuneChoice("secondary", "pass-shipment" + buttonSuffix, "Keep it in " + territoryName));
        chat.publish("Where would you like to place " + wormName + "? " + player, choices);
    }

    public void addWormToPlace() {
        wormsToPlace++;
    }

    public void wormWasPlaced() {
        wormsToPlace--;
    }

    public int getWormsToPlace() {
        return wormsToPlace;
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

    @Override
    public String getNoRevivableForcesMessage() {
        boolean starsInTanks = game.getTleilaxuTanks().getForceStrength(name + "*") > 0;
        if (starsInTanks)
            return emoji + " has no revivable forces in the tanks";
        else
            return emoji + " has no forces in the tanks";
    }
}
