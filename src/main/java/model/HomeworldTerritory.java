package model;

import constants.Emojis;
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

    public void increaseHandLimitForTupile(Faction faction) {
        faction.setHandLimit(faction.getHandLimit() + 1);
        game.getTurnSummary().publish(faction.getEmoji() + " " + Emojis.TREACHERY + " limit has been increased to " + faction.getHandLimit() + ".");
    }

    public void reduceHandLimitForTupile(Faction faction) {
        faction.setHandLimit(faction.getHandLimit() - 1);
        game.getTurnSummary().publish(faction.getEmoji() + " " + Emojis.TREACHERY + " limit has been reduced to " + faction.getHandLimit() + ".");
        if (faction.getTreacheryHand().size() > faction.getHandLimit())
            game.getTurnSummary().publish(faction.getEmoji() + " must discard a " + Emojis.TREACHERY + " card.");
    }

    public void establishOccupier(String occupierName) {
        this.occupierName = occupierName;
        Faction occupier = getOccupyingFaction();
        getNativeFaction().checkForLowThreshold();
        game.getTurnSummary().publish(territoryName + " is now occupied by " + occupier.getEmoji());

        if (territoryName.equals("Tupile")) {
            increaseHandLimitForTupile(occupier);
            if (occupier.hasAlly())
                increaseHandLimitForTupile(game.getFaction(occupier.getAlly()));
        }

        checkForOccupierTakingDukeVidal();
    }

    public void clearOccupier() {
        Faction occupier = getOccupyingFaction();
        this.occupierName = null;
        game.getTurnSummary().publish(territoryName + " is no longer occupied.");

        if (occupier != null && territoryName.equals("Tupile")) {
            reduceHandLimitForTupile(occupier);
            if (occupier.hasAlly())
                reduceHandLimitForTupile(game.getFaction(occupier.getAlly()));
        }

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
        else if (nativeForces && countFactions() == 1 && wasOccupied)
            clearOccupier();
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
            boolean isOccupied = !name.equals(nativeName);
            if (isOccupied && (!wasOccupied || !name.equals(formerOccupier)))
                establishOccupier(name);
            else if (!isOccupied && wasOccupied)
                clearOccupier();
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
