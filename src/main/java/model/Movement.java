package model;

import constants.Emojis;
import enums.MoveType;
import exceptions.InvalidGameStateException;
import helpers.Exclude;
import model.factions.EcazFaction;
import model.factions.Faction;
import model.factions.FremenFaction;

import java.util.ArrayList;
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
        else if (moveType == MoveType.GUILD_AMBASSADOR)
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

    public void pass() {
        Game game = faction.getGame();
        if (moveType == MoveType.FREMEN_RIDE) {
            faction.getChat().reply("You will not ride the worm.");
            game.getTurnSummary().publish(faction.getEmoji() + " does not ride the worm.");
            ((FremenFaction) faction).setWormRideActive(false);
        } else if (moveType == MoveType.FREMEN_AMBASSADOR) {
            faction.getChat().reply("You will not ride the worm with the Fremen Ambassador.");
            game.getTurnSummary().publish(faction.getEmoji() + " does not ride the worm with the Fremen Ambassador.");
        } else if (moveType == MoveType.GUILD_AMBASSADOR) {
            faction.getChat().reply("You will not ship with the Guild Ambassador.");
            game.getTurnSummary().publish(faction.getEmoji() + " does not ship with the Guild Ambassador.");
        }
        clear();
        moveType = MoveType.TBD;
    }

    public void startOver() {
        String saveMovingFrom = movingFrom;
        clear();
        if (moveType == MoveType.FREMEN_RIDE)
            ((FremenFaction) faction).presentWormRideChoices(saveMovingFrom);
        else if (moveType == MoveType.FREMEN_AMBASSADOR)
            ((EcazFaction) faction).presentFremenAmbassadorRideFromChoices();
        else if (moveType == MoveType.GUILD_AMBASSADOR)
            ((EcazFaction) faction).presentGuildAmbassadorDestinationChoices();
    }

    public void presentStrongholdChoices() {
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

    public void presentDiscoveryTokenChoices() {
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
        boolean wormRide = moveType == MoveType.FREMEN_RIDE || moveType == MoveType.FREMEN_AMBASSADOR;
        List<Territory> sectors = game.getTerritories().values().stream().filter(s -> s.getTerritoryName().startsWith(wholeTerritoryName)).toList();
        choice.setDisabled(sectors.stream().anyMatch(s -> s.factionMayNotEnter(game, faction, !wormRide, isInitialPlacement)));
        return choice;
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
                choices.add(new DuneChoice("primary", getChoicePrefix() + "add-force-" + (i + 1), "+" + (i + 1), Emojis.getForceEmoji(faction.getName()), false));
            if (i < buttonLimitSpecialForces)
                choices.add(new DuneChoice("primary", getChoicePrefix() + "add-special-force-" + (i + 1), "+" + (i + 1) + " *", Emojis.getForceEmoji(faction.getName() + "*"), false));
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
    }

    public void addSpecialForces(int numForces) {
        specialForce += numForces;
    }

    public void resetForces() {
        force = 0;
        specialForce = 0;
        secondForce = 0;
        secondSpecialForce = 0;
    }

    public void execute() throws InvalidGameStateException {
        if (moveType == MoveType.FREMEN_RIDE) {
            executeMovement();
            ((FremenFaction) faction).setWormRideActive(false);
            faction.getChat().reply("Worm ride complete.");
        } else if (moveType == MoveType.FREMEN_AMBASSADOR) {
            executeMovement();
            faction.getChat().reply("Ride with Fremen Ambassador complete.");
        } else if (moveType == MoveType.GUILD_AMBASSADOR)
            executeGuildAmbassador();
        clear();
        moveType = MoveType.TBD;
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
