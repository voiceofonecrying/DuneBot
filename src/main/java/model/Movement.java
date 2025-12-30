package model;

import constants.Emojis;
import enums.GameOption;
import enums.MoveType;
import exceptions.InvalidGameStateException;
import helpers.Exclude;
import model.factions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
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
    private boolean movingNoField;
    @Exclude
    private Faction faction;

    public Movement(Faction faction) {
        this.moveType = MoveType.TBD;
        this.faction = faction;
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
        this.movingNoField = false;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public void setMoveType(MoveType moveType) {
        this.moveType = moveType;
    }

    public String getChoicePrefix() {
        if (moveType == MoveType.FREMEN_RIDE)
            return "fremen-ride-";
        else if (moveType == MoveType.SHAI_HULUD_PLACEMENT)
            return "fremen-place-shai-hulud-";
        else if (moveType == MoveType.GREAT_MAKER_PLACEMENT)
            return "fremen-place-great-maker-";
        else if (moveType == MoveType.GUILD_AMBASSADOR)
            return "ambassador-guild-";
        else if (moveType == MoveType.FREMEN_AMBASSADOR)
            return "ambassador-fremen-";
        else if (moveType == MoveType.BT_HT)
            return "bt-ht-";
        else if (moveType == MoveType.HMS_PLACEMENT)
            return "ix-hms-placement-";
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

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public void pass() throws InvalidGameStateException {
        Game game = faction.getGame();
        if (moveType == MoveType.FREMEN_RIDE) {
            faction.getChat().reply("You will not ride the worm.");
            game.getTurnSummary().publish(faction.getEmoji() + " does not ride the worm.");
            ((FremenFaction) faction).setWormRideActive(false);
        } else if (moveType == MoveType.SHAI_HULUD_PLACEMENT) {
            ((FremenFaction) faction).placeWorm(game.getTerritory(faction.getMovement().getMovingFrom()));
        } else if (moveType == MoveType.GREAT_MAKER_PLACEMENT) {
            ((FremenFaction) faction).placeWorm(game.getTerritory(faction.getMovement().getMovingFrom()));
        } else if (moveType == MoveType.FREMEN_AMBASSADOR) {
            faction.getChat().reply("You will not ride the worm with the Fremen Ambassador.");
            game.getTurnSummary().publish(faction.getEmoji() + " does not ride the worm with the Fremen Ambassador.");
        } else if (moveType == MoveType.GUILD_AMBASSADOR) {
            faction.getChat().reply("You will not ship with the Guild Ambassador.");
            game.getTurnSummary().publish(faction.getEmoji() + " does not ship with the Guild Ambassador.");
        } else if (moveType == MoveType.BT_HT) {
            faction.getChat().reply("You will leave your free revivals on Tleilax.");
            game.getTurnSummary().publish(Emojis.BT + " leaves their free revivals on Tleilax.");
            ((BTFaction) faction).setBtHTActive(false);
        } else if (moveType == MoveType.HMS_PLACEMENT) {
            throw new InvalidGameStateException("Ix must place the HMS.");
        }
        clear();
        moveType = MoveType.TBD;
    }

    public void startOver() {
        String saveMovingFrom = movingFrom;
        clear();
        if (moveType == MoveType.FREMEN_RIDE)
            ((FremenFaction) faction).presentWormRideChoices(saveMovingFrom);
        else if (moveType == MoveType.SHAI_HULUD_PLACEMENT)
            ((FremenFaction) faction).presentWormPlacementChoices(saveMovingFrom, "Shai-Hulud");
        else if (moveType == MoveType.GREAT_MAKER_PLACEMENT)
            ((FremenFaction) faction).presentWormPlacementChoices(saveMovingFrom, "Great Maker");
        else if (moveType == MoveType.FREMEN_AMBASSADOR)
            ((EcazFaction) faction).presentFremenAmbassadorRideFromChoices();
        else if (moveType == MoveType.GUILD_AMBASSADOR)
            ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
        else if (moveType == MoveType.BT_HT)
            ((BTFaction) faction).presentHTChoices();
        else if (moveType == MoveType.HMS_PLACEMENT)
            ((IxFaction) faction).presentHMSPlacementChoices();
    }

    public void presentTerritoryTypeChoices() {
        Game game = faction.getGame();
        boolean fremenAmbassador = moveType == MoveType.FREMEN_AMBASSADOR;
        boolean fremenRide = moveType == MoveType.FREMEN_RIDE;
        boolean wormRide = fremenRide || fremenAmbassador;
        String choicePrefix = getChoicePrefix();
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice(choicePrefix + "stronghold", "Stronghold"));
        choices.add(new DuneChoice(choicePrefix + "spice-blow", "Spice Blow Territories"));
        choices.add(new DuneChoice(choicePrefix + "rock", "Rock Territories"));
        if (game.hasGameOption(GameOption.HOMEWORLDS) && !wormRide)
            choices.add(new DuneChoice("homeworlds", "Homeworlds"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap)
            choices.add(new DuneChoice(choicePrefix + "discovery-tokens", "Discovery Tokens"));
        choices.add(new DuneChoice(choicePrefix + "other", "Somewhere else"));
        choices.add(new DuneChoice("danger", choicePrefix + "pass", wormRide ? "No ride" : "I don't want to ship."));
        if (wormRide)
            faction.getChat().reply("Where would you like to ride to from " + faction.getMovement().getMovingFrom() + "? " + faction.getPlayer(), choices);
        else
            faction.getChat().reply("Where would you like to ship to?", choices);
    }

    public void presentStrongholdChoices() throws InvalidGameStateException {
        if (moveType == MoveType.HMS_PLACEMENT)
            throw new InvalidGameStateException("The HMS cannot be placed in a Stronghold.");
        Game game = faction.getGame();
        boolean fremenAmbassador = moveType == MoveType.FREMEN_AMBASSADOR;
        boolean fremenRide = moveType == MoveType.FREMEN_RIDE;
        boolean btHTPlacement = moveType == MoveType.BT_HT;
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        List<DuneChoice> choices = game.getTerritories().values().stream()
                .filter(t -> t.isValidStrongholdForShipmentFremenRideAndBTHT(faction, fremenAmbassador || fremenRide || btHTPlacement))
                .map(t -> faction.getMovement().shipToTerritoryChoice(t.getTerritoryName(), startingForces)).sorted(Comparator.comparing(DuneChoice::getLabel)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply("Which Stronghold?", choices);
    }

    public void presentSpiceBlowChoices() {
        Game game = faction.getGame();
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        boolean hmsPlacement = moveType == MoveType.HMS_PLACEMENT;
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> spiceBlowTerritories = game.getTerritories().getSpiceBlowTerritoryNames();
        List<DuneChoice> choices = spiceBlowTerritories.stream().map(s -> faction.getMovement().shipToTerritoryChoice(s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply("Which Spice Blow Territory?", choices);
    }

    public void presentRockChoices() {
        Game game = faction.getGame();
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        boolean hmsPlacement = moveType == MoveType.HMS_PLACEMENT;
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> rockTerritories = game.getTerritories().getRockTerritoryNames();
        List<DuneChoice> choices = rockTerritories.stream().map(s -> faction.getMovement().shipToTerritoryChoice(s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply("Which Rock Territory?", choices);
    }

    public void presentDiscoveryTokenChoices() throws InvalidGameStateException {
        if (moveType == MoveType.HMS_PLACEMENT)
            throw new InvalidGameStateException("There are no Discovery Tokens on the board at game start.");
        Game game = faction.getGame();
        List<DuneChoice> choices = game.getTerritories().values().stream().filter(Territory::isDiscovered)
                .map(territory -> faction.getMovement().shipToTerritoryChoice(territory.getDiscoveryToken(), false)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply("Which Discovery Token?", choices);
    }

    public void presentNonSpiceNonRockChoices() {
        Game game = faction.getGame();
        boolean startingForces = moveType == MoveType.STARTING_FORCES;
        boolean hmsPlacement = moveType == MoveType.HMS_PLACEMENT;
        boolean initialPlacement = startingForces || hmsPlacement;
        List<String> nonSpiceNonRockTerritories = game.getTerritories().getNonSpiceNonRockTerritoryNames();
        List<DuneChoice> choices = nonSpiceNonRockTerritories.stream().map(s -> faction.getMovement().shipToTerritoryChoice(s, initialPlacement)).collect(Collectors.toList());
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply("Which Territory?", choices);
    }

    public DuneChoice shipToTerritoryChoice(String wholeTerritoryName, boolean isInitialPlacement) {
        Game game = faction.getGame();
        DuneChoice choice = new DuneChoice(getChoicePrefix() + "territory-" + wholeTerritoryName, wholeTerritoryName);
        boolean shipment = !List.of(MoveType.FREMEN_RIDE, MoveType.FREMEN_AMBASSADOR, MoveType.SHAI_HULUD_PLACEMENT, MoveType.GREAT_MAKER_PLACEMENT).contains(moveType);
        List<Territory> sectors = game.getTerritories().values().stream().filter(s -> s.getTerritoryName().startsWith(wholeTerritoryName)).toList();
        choice.setDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, shipment, isInitialPlacement)));
        return choice;
    }

    /**
     * If the territory has only one sector, process for that sector.
     * If it has more than one sector, present sector choices so player can pick a single sector to process.
     *
     * @param aggregateTerritoryName  The name of the entire territory.
     * @return true if game state has changed, false if only new choices were presented to player
     */
    public boolean processTerritory(String aggregateTerritoryName) {
        Game game = faction.getGame();
        List<Territory> territorySectors = game.getTerritories().getTerritorySectorsInStormOrder(aggregateTerritoryName);
        if (territorySectors.size() == 1) {
            processSector(territorySectors.getFirst().getTerritoryName());
            return true;
        } else {
            presentSectorChoices(aggregateTerritoryName, territorySectors);
            return false;
        }
    }

    public void presentSectorChoices(String aggregateTerritoryName, List<Territory> territorySectors) {
        List<DuneChoice> choices = new ArrayList<>();
        for (Territory sector : territorySectors) {
            int sectorNameStart = sector.getTerritoryName().indexOf("(");
            String sectorName = sector.getTerritoryName().substring(sectorNameStart + 1, sector.getTerritoryName().length() - 1);
            String spiceString = "";
            if (sector.getSpice() > 0)
                spiceString = " (" + sector.getSpice() + " spice)";
            choices.add(new DuneChoice(getChoicePrefix() + "sector-" + sector.getTerritoryName(), sector.getSector() + " - " + sectorName + spiceString));
        }
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply("Which sector of " + aggregateTerritoryName + "?", choices);
    }

    /**
     * Process the movement to the sector based on move type.
     *
     * @param territoryName  The name of the territory sector.
     */
    public void processSector(String territoryName) {
        Game game = faction.getGame();
        if (moveType == MoveType.SHAI_HULUD_PLACEMENT || moveType == MoveType.GREAT_MAKER_PLACEMENT) {
            Territory territory = game.getTerritory(territoryName);
            game.getFremenFaction().placeWorm(territory);
        } else if (moveType == MoveType.BT_HT) {
            setMovingTo(territoryName);
            game.getBTFaction().presentHTExecutionChoices();
        } else if (moveType == MoveType.HMS_PLACEMENT) {
            setMovingTo(territoryName);
            game.getIxFaction().presentHMSPlacementExecutionChoices();
        } else {
            setMovingTo(territoryName);
            presentForcesChoices();
        }
    }

    public void presentForcesChoices() {
        Game game = faction.getGame();
        boolean fremenAmbassador = moveType == MoveType.FREMEN_AMBASSADOR;
        boolean fremenRide = moveType == MoveType.FREMEN_RIDE;
        boolean wormRide = fremenRide || fremenAmbassador;
        boolean guildAmbassador = moveType == MoveType.GUILD_AMBASSADOR;
        List<DuneChoice> choices = new ArrayList<>();
        int buttonLimitForces = guildAmbassador ? faction.getReservesStrength() - force :
                game.getTerritory(movingFrom).getForceStrength(faction.getName()) - force;
        int buttonLimitSpecialForces = guildAmbassador ? faction.getSpecialReservesStrength() - specialForce :
                game.getTerritory(movingFrom).getForceStrength(faction.getName() + "*") - specialForce;
        if (guildAmbassador) {
            int forcesAllocated = force + specialForce;
            buttonLimitForces = Math.min(4 - forcesAllocated, buttonLimitForces);
            buttonLimitSpecialForces = Math.min(4 - forcesAllocated, buttonLimitSpecialForces);
        }

        for (int i = 0; i < Math.max(buttonLimitForces, buttonLimitSpecialForces); i++) {
            if (i < buttonLimitForces)
                choices.add(new DuneChoice("primary", getChoicePrefix() + "add-force-" + (i + 1), "+" + (i + 1), faction.getForceEmoji(), false));
            if (i < buttonLimitSpecialForces)
                choices.add(new DuneChoice("primary", getChoicePrefix() + "add-special-force-" + (i + 1), "+" + (i + 1) + " *", faction.getSpecialForceEmoji(), false));
        }

        String message = "Use buttons below to add forces to your ";
        if (wormRide)
            message += "ride. Currently moving:";
        else if (guildAmbassador)
            message += "shipment. Currently shipping:";
        else
            message += "movement. Currently moving:";
        message += "\n**" + faction.forcesStringWithZeroes(force, specialForce) + "** to " + movingTo;
        if (force != 0 || specialForce != 0) {
            String executeLabel = "Confirm Movement";
            if (guildAmbassador)
                executeLabel = "Confirm Shipment";
            choices.add(new DuneChoice("success", getChoicePrefix() + "execute", executeLabel));
            choices.add(new DuneChoice("secondary", getChoicePrefix() + "reset-forces", "Reset forces"));
        }
        choices.add(new DuneChoice("secondary", getChoicePrefix() + "start-over", "Start over"));
        faction.getChat().reply(message, choices);
    }

    public void addRegularForces(int numForces) {
        force += numForces;
        presentForcesChoices();
    }

    public void addSpecialForces(int numForces) {
        specialForce += numForces;
        presentForcesChoices();
    }

    public void resetForces() {
        force = 0;
        specialForce = 0;
        secondForce = 0;
        secondSpecialForce = 0;
        presentForcesChoices();
    }

    public boolean execute() throws InvalidGameStateException {
        boolean advanceGame = false;
        if (moveType == MoveType.FREMEN_RIDE) {
            executeMovement();
            ((FremenFaction) faction).setWormRideActive(false);
            faction.getChat().reply("Worm ride complete.");
        } else if (moveType == MoveType.FREMEN_AMBASSADOR) {
            executeMovement();
            faction.getChat().reply("Ride with Fremen Ambassador complete.");
        } else if (moveType == MoveType.GUILD_AMBASSADOR)
            executeGuildAmbassador();
        else if (moveType == MoveType.BT_HT)
            ((BTFaction) faction).executeHTPlacement();
        else if (moveType == MoveType.HMS_PLACEMENT) {
            ((IxFaction) faction).placeHMS();
            advanceGame = true;
        }
        clear();
        moveType = MoveType.TBD;
        return advanceGame;
    }

    public void executeGuildAmbassador() throws InvalidGameStateException {
        Territory territory = faction.getGame().getTerritory(movingTo);
        if (force > 0 || specialForce > 0)
            faction.placeForces(territory, force, specialForce, false, true, true, false, false);
        faction.getChat().reply("Shipment with Guild Ambassador complete.");
    }

    public void executeMovement() throws InvalidGameStateException {
        Game game = faction.getGame();
        if (movingNoField) {
            game.getRicheseFaction().moveNoField(movingTo, false);
            game.moveForces(faction, movingFrom, movingTo, secondMovingFrom, force, specialForce, secondForce, secondSpecialForce, true);
        } else {
            game.moveForces(faction, movingFrom, movingTo, secondMovingFrom, force, specialForce, secondForce, secondSpecialForce, false);
        }
    }
}
