package model;

import enums.MoveType;
import exceptions.InvalidGameStateException;
import model.factions.Faction;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Movement {
    private MoveType moveType;
    private String movingFrom;
    private String secondMovingFrom;
    private String movingTo;
    private String mustMoveOutOf;
    private int force;
    private int secondForce;
    private int specialForce;
    private int secondSpecialForce;
    private boolean hasMoved;
    private boolean movingNoField;

    public Movement() {
    }

    public void clear() {
        this.movingFrom = "";
        this.movingTo = "";
        this.mustMoveOutOf = null;
        this.force = 0;
        this.specialForce = 0;
        this.secondForce = 0;
        this.secondSpecialForce = 0;
        this.secondMovingFrom = "";
        this.hasMoved = true;
        this.movingNoField = false;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public void setMoveType(MoveType moveType) {
        this.moveType = moveType;
    }

    public String getChoicePrefix() {
        if (moveType == MoveType.GUILD_AMBASSADOR)
            return "ambassador-guild-";
        else if (moveType == MoveType.FREMEN_AMBASSADOR)
            return "ambassador-fremen-";
        return "";
    }

    public String getMovingFrom() {
        return movingFrom;
    }

    public void setMovingFrom(String movingFrom) {
        this.movingFrom = movingFrom;
    }

    public String getMovingTo() {
        return movingTo;
    }

    public void setMovingTo(String movingTo) {
        this.movingTo = movingTo;
    }

    public String getMustMoveOutOf() {
        return mustMoveOutOf;
    }

    public void setMustMoveOutOf(String mustMoveOutOf) {
        this.mustMoveOutOf = mustMoveOutOf;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getSpecialForce() {
        return specialForce;
    }

    public void setSpecialForce(int specialForce) {
        this.specialForce = specialForce;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public boolean isMovingNoField() {
        return movingNoField;
    }

    public void setMovingNoField(boolean movingNoField) {
        this.movingNoField = movingNoField;
    }

    public String getSecondMovingFrom() {
        return secondMovingFrom;
    }

    public void setSecondMovingFrom(String secondMovingFrom) {
        this.secondMovingFrom = secondMovingFrom;
    }

    public int getSecondForce() {
        return secondForce;
    }

    public void setSecondForce(int secondForce) {
        this.secondForce = secondForce;
    }

    public int getSecondSpecialForce() {
        return secondSpecialForce;
    }

    public void setSecondSpecialForce(int secondSpecialForce) {
        this.secondSpecialForce = secondSpecialForce;
    }

    public void presentStrongholdChoices(Game game, Faction faction) {
        boolean fremenAmbassador = moveType == MoveType.FREMEN_AMBASSADOR;
        boolean fremenRide = moveType == MoveType.FREMEN_RIDE;
        boolean btHTPlacement = moveType == MoveType.BT_HT;
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        List<DuneChoice> choices = game.getTerritories().values().stream()
                .filter(t -> t.isValidStrongholdForShipmentFremenRideAndBTHT(faction, fremenAmbassador || fremenRide || btHTPlacement))
                .map(t -> faction.getMovement().shipToTerritoryChoiceWithPrefix(game, faction, t.getTerritoryName(), startingForces)).sorted(Comparator.comparing(DuneChoice::getLabel)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "reset-shipment", "Start over"));
        faction.getChat().reply("Which Stronghold?", choices);
    }

    public void presentSpiceBlowChoices(Game game, Faction faction) {
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        boolean hmsPlacement = moveType == MoveType.HMS_PLACEMENT;
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> spiceBlowTerritories = game.getTerritories().getSpiceBlowTerritoryNames();
        List<DuneChoice> choices = spiceBlowTerritories.stream().map(s -> faction.getMovement().shipToTerritoryChoiceWithPrefix(game, faction, s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "reset-shipment", "Start over"));
        faction.getChat().reply("Which Spice Blow Territory?", choices);
    }

    public void presentRockChoices(Game game, Faction faction) {
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        boolean hmsPlacement = moveType == MoveType.HMS_PLACEMENT;
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> rockTerritories = game.getTerritories().getRockTerritoryNames();
        List<DuneChoice> choices = rockTerritories.stream().map(s -> faction.getMovement().shipToTerritoryChoiceWithPrefix(game, faction, s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "reset-shipment", "Start over"));
        faction.getChat().reply("Which Rock Territory?", choices);
    }

    public void presentDiscoveryTokenChoices(Game game, Faction faction) {
        List<DuneChoice> choices = game.getTerritories().values().stream().filter(Territory::isDiscovered)
                .map(territory -> faction.getMovement().shipToTerritoryChoiceWithPrefix(game, faction, territory.getDiscoveryToken(), false)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "reset-shipment", "Start over"));
        faction.getChat().reply("Which Discovery Token?", choices);
    }

    public void presentNonSpiceNonRockChoices(Game game, Faction faction) {
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        boolean hmsPlacement = moveType == MoveType.HMS_PLACEMENT;
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> nonSpiceNonRockTerritories = game.getTerritories().getNonSpiceNonRockTerritoryNames();
        List<DuneChoice> choices = nonSpiceNonRockTerritories.stream().map(s -> faction.getMovement().shipToTerritoryChoiceWithPrefix(game, faction, s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "reset-shipment", "Start over"));
        faction.getChat().reply("Which Territory?", choices);
    }

    public DuneChoice shipToTerritoryChoiceWithPrefix(Game game, Faction faction, String wholeTerritoryName, boolean isInitialPlacement) {
        DuneChoice choice = new DuneChoice(getChoicePrefix() + "territory-" + wholeTerritoryName, wholeTerritoryName);
        boolean wormRide = moveType == MoveType.FREMEN_RIDE || moveType == MoveType.FREMEN_AMBASSADOR;
        List<Territory> sectors = game.getTerritories().values().stream().filter(s -> s.getTerritoryName().startsWith(wholeTerritoryName)).toList();
        choice.setDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, !wormRide, isInitialPlacement)));
        return choice;
    }

    public void execute(Game game, Faction faction) throws InvalidGameStateException {
        if (moveType == MoveType.FREMEN_AMBASSADOR) {
            executeMovement(game, faction);
            faction.getChat().publish("Movement with Fremen ambasssador complete.");
        } else if (moveType == MoveType.GUILD_AMBASSADOR)
            executeGuildAmbassador(game, faction);
    }

    public void executeGuildAmbassador(Game game, Faction faction) throws InvalidGameStateException {
        Territory territory = game.getTerritory(movingTo);
        if (force > 0 || specialForce > 0)
            faction.placeForces(territory, force, specialForce, false, true, true, false, false);
        clear();
        faction.getChat().reply("Shipment with Guild Ambassador complete.");
    }

    public void executeMovement(Game game, Faction faction) throws InvalidGameStateException {
        if (movingNoField) {
            game.getRicheseFaction().moveNoField(movingTo, false);
            game.moveForces(faction, movingFrom, movingTo, secondMovingFrom, force, specialForce, secondForce, secondSpecialForce, true);
        } else {
            game.moveForces(faction, movingFrom, movingTo, secondMovingFrom, force, specialForce, secondForce, secondSpecialForce, false);
        }
        clear();
    }
}
