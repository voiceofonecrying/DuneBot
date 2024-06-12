package model;

import helpers.Exclude;
import model.factions.Faction;

import java.util.HashSet;
import java.util.Set;

public class HomeworldTerritory extends Territory {
    // make nativeName final after all games have started with HomeworldTerritory class
    private String nativeName;
    private String occupierName;
    @Exclude
    private Game game;

    public HomeworldTerritory(Game game, String homeworldName, String nativeName) {
        super(homeworldName, -1, false, false, false, false);
        this.game = game;
        this.nativeName = nativeName;
        this.occupierName = null;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getOccupierName() {
        return occupierName;
    }

    public void setOccupierName(String occupierName) {
        this.occupierName = occupierName;
        game.getTurnSummary().publish(getTerritoryName() + " is now occupied by " + getOccupyingFaction().getEmoji());
    }

    public void clearOccupier() {
        this.occupierName = null;
        game.getTurnSummary().publish(getTerritoryName() + " is no longer occupied.");
    }

    public Faction getNativeFaction() {
        try {
            return game.getFaction(nativeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Faction getOccupyingFaction() {
        try {
            return game.getFaction(occupierName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public int costToShipInto() {
        return 1;
    }

    @Override
    public void addForces(String forceName, int amount) {
        boolean wasOccupied = occupierName != null;
        super.addForces(forceName, amount);
        Faction nativeFaction = getNativeFaction();
        // nativeFaction will be null when initial forces are placed before Faction is added to game
        if (nativeFaction == null)
            return;
        String factionName = Force.getFactionNameFromForceName(forceName);
        boolean nativeForces = factionName.equals(nativeName);
        if (!wasOccupied && countFactions() == 1 && !nativeForces) {
            occupierName = factionName;
            game.getTurnSummary().publish(getTerritoryName() + " is now occupied by " + getOccupyingFaction().getEmoji());
            nativeFaction.checkForLowThreshold();
        } else if (nativeForces)
            nativeFaction.checkForHighThreshold();
    }

    @Override
    public void removeForces(String forceName, int amount) {
        boolean wasOccupied = occupierName != null;
        super.removeForces(forceName, amount);
        Set<String> factionNames = new HashSet<>();
        forces.forEach(force -> factionNames.add(force.getFactionName()));
        if (hasRicheseNoField()) factionNames.add("Richese");
        if (factionNames.size() == 1) {
            String name = factionNames.stream().findFirst().orElseThrow();
            occupierName = name.equals(nativeName) ? null : name;
            if (wasOccupied && occupierName == null) {
                game.getTurnSummary().publish(getTerritoryName() + " is no longer occupied.");
                game.getFaction(nativeName).checkForHighThreshold();
            } else if (!wasOccupied && occupierName != null) {
                game.getTurnSummary().publish(getTerritoryName() + " is now occupied by " + getOccupyingFaction().getEmoji());
            }
        }
        game.getFaction(nativeName).checkForLowThreshold();
    }

    public void resetOccupation() {
        boolean wasOccupied = occupierName != null;
        if (countFactions() == 0) {
            occupierName = null;
            if (wasOccupied)
                game.getTurnSummary().publish(getTerritoryName() + " is no longer occupied.");
        }
    }
}
