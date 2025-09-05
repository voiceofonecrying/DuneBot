package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.MoveType;
import exceptions.InvalidGameStateException;
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
    int startingForcesPlaced;
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

    public int getStartingForcesPlaced() {
        return startingForcesPlaced;
    }

    @Override
    public void presentStartingForcesChoices() {
        shipment.clear();
        String buttonSuffix = "-starting-forces";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("ship" + buttonSuffix + "-Sietch Tabr", "Sietch Tabr"));
        choices.add(new DuneChoice("ship" + buttonSuffix + "-False Wall South", "False Wall South"));
        choices.add(new DuneChoice("ship" + buttonSuffix + "-False Wall West", "False Wall West"));
        chat.reply("You have " + (10 - startingForcesPlaced) + " total " + Emojis.FREMEN_TROOP + " " + Emojis.FREMEN_FEDAYKIN + " remaining to place.\nWhere would you like to place next? " + player, choices);
    }

    @Override
    public boolean placeChosenStartingForces() throws InvalidGameStateException {
        startingForcesPlaced += shipment.getForce() + shipment.getSpecialForce();
        if (startingForcesPlaced > 10)
            throw new InvalidGameStateException("Fremen cannot place " + startingForcesPlaced + " starting forces.");
        executeShipment(game, false, true);
        if (startingForcesPlaced == 10) {
            chat.reply("Initial force placement complete.");
            return true;
        } else {
            presentStartingForcesChoices();
            return false;
        }
    }

    @Override
    public String forcesStringWithZeroes(int numForces, int numSpecialForces) {
        return numForces + " " + Emojis.getForceEmoji(name) + " " + numSpecialForces + " " + Emojis.getForceEmoji(name + "*");
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
        movement.setMovingFrom(territoryName);
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("fremen-ride-stronghold", "Stronghold"));
        choices.add(new DuneChoice("fremen-ride-spice-blow", "Spice Blow Territories"));
        choices.add(new DuneChoice("fremen-ride-rock", "Rock Territories"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap)
            choices.add(new DuneChoice("fremen-ride-discovery-tokens", "Discovery Tokens"));
        choices.add(new DuneChoice("fremen-ride-other", "Somewhere else"));
        choices.add(new DuneChoice("danger", "fremen-ride-pass", "No ride"));
        chat.reply("Where would you like to ride to from " + territoryName + "? " + player, choices);
        movement.setMoveType(MoveType.FREMEN_RIDE);
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
        movement.setMovingFrom(territoryName);
        String buttonSuffix = greatMaker ? "-place-great-maker" : "-place-shai-hulud";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Other Sand Territories"));
        choices.add(new DuneChoice("secondary", "pass-shipment" + buttonSuffix, "Keep it in " + territoryName));
        chat.publish("Where would you like to place " + wormName + "? " + player, choices);
        wormsToPlace++;
    }

    public int getWormsToPlace() {
        return wormsToPlace;
    }

    public void placeWorm(boolean greatMaker, Territory territory, boolean leftWhereItAppeared) {
        String wormName = greatMaker ? "Great Maker" : "Shai-Hulud";
        String action = leftWhereItAppeared ? "left " : "placed ";
        String territoryName = territory.getTerritoryName();
        game.placeShaiHulud(territoryName, wormName, false);
        wormsToPlace--;
        chat.reply("You " + action + wormName + " in " + territoryName + ".");
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

    public void placeFreeRevivalWithHighThreshold(String territoryName) throws InvalidGameStateException {
        Territory territory = game.getTerritory(territoryName);
        placeForces(territory, 0, 1, false, true, true, false, false);
        game.getTurnSummary().publish(Emojis.FREMEN + " place their revived " + Emojis.FREMEN_FEDAYKIN + " with their forces in " + territory.getTerritoryName() + ".");
        chat.reply("Your " + Emojis.FREMEN_FEDAYKIN + " has left for the northern hemisphere.");
    }
}
