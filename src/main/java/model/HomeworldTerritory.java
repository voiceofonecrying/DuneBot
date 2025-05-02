package model;

import helpers.Exclude;
import model.factions.Faction;

import java.util.HashSet;
import java.util.Set;

public class HomeworldTerritory extends Territory {
    // make nativeName final after all games have started with HomeworldTerritory class
    private final String nativeName;
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

    public String getOccupierName() {
        return occupierName;
    }

    public void establishOccupier(String occupierName) {
        this.occupierName = occupierName;
        getNativeFaction().checkForLowThreshold();
        game.getTurnSummary().publish(territoryName + " is now occupied by " + getOccupyingFaction().getEmoji());
        checkForOccupierTakingDukeVidal();
    }

    public void clearOccupier() {
        this.occupierName = null;
        game.getTurnSummary().publish(territoryName + " is no longer occupied.");
        game.getFaction(nativeName).checkForHighThreshold();
    }

    public void resetOccupation() {
        if (countFactions() == 0 && occupierName != null)
            clearOccupier();
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
        String formerOccupier = occupierName;
        super.addForces(forceName, amount);
        Faction nativeFaction = getNativeFaction();
        // nativeFaction will be null when initial forces are placed before Faction is added to game
        if (nativeFaction == null)
            return;
        String factionName = Force.getFactionNameFromForceName(forceName);
        boolean nativeForces = factionName.equals(nativeName);
        if (!nativeForces && countFactions() == 1 && (!wasOccupied || !factionName.equals(formerOccupier)))
            establishOccupier(factionName);
        else if (nativeForces)
            nativeFaction.checkForHighThreshold();
    }

    @Override
    public void removeForces(Game game, String forceName, int amount) {
        boolean wasOccupied = occupierName != null;
        String formerOccupier = occupierName;
        super.removeForces(game, forceName, amount);
        Set<String> factionNames = new HashSet<>();
        forces.forEach(force -> factionNames.add(force.getFactionName()));
        if (hasRicheseNoField())
            factionNames.add("Richese");
        if (factionNames.size() == 1) {
            String name = factionNames.stream().findFirst().orElseThrow();
            occupierName = name.equals(nativeName) ? null : name;
            if (wasOccupied && name.equals(nativeName)) {
                clearOccupier();
            } else if (!wasOccupied && occupierName != null || wasOccupied && !occupierName.equals(formerOccupier))
                establishOccupier(name);
        }
        game.getFaction(nativeName).checkForLowThreshold();
    }

    protected void checkForOccupierTakingDukeVidal() {
        if (territoryName.equals("Ecaz") && game.getEcazFaction().isHomeworldOccupied()) {
            if (game.getLeaderTanks().contains(game.getDukeVidal())) {
                game.getTurnSummary().publish(game.getEcazFaction().getOccupier().getEmoji() + " may revive Duke Vidal from the tanks.");
                game.getEcazFaction().getOccupier().getChat().publish("Would you like to revive Duke Vidal from the tanks as Ecaz occupier? ");
            } else {
                for (Faction faction1 : game.getFactions()) {
                    faction1.getLeaders().removeIf(leader1 -> leader1.getName().equals("Duke Vidal"));
                }
                game.getEcazFaction().getOccupier().getLeaders().add(game.getDukeVidal());
                game.getTurnSummary().publish("Duke Vidal has left to work for " + game.getEcazFaction().getOccupier().getEmoji() + " (Ecaz homeworld occupied)");
            }
        }
    }
}
