package model.factions;

import constants.Emojis;
import controller.commands.SetupCommands;
import enums.GameOption;
import enums.MoveType;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.DuneChoice;
import model.Game;
import model.IdualiTerritory;
import model.Territories;
import model.Territory;
import model.TleilaxuTanks;
import model.TreacheryCard;
import model.factions.Faction;

import org.jetbrains.annotations.NotNull;

import static controller.commands.CommandOptions.territory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IdualiFaction extends Faction {
    private boolean cavesBlocked;
    private int startingForcesPlaced;
    private int territoriesPlaced;
    private boolean wormRideActive;
    private List<String> wormRides;
    private boolean placedShuloch;
    private boolean placedKharuz;
    private boolean placedJacarutu;
    private List<TerritoryLocation> territoryLocations;
    private List<TreacheryCard> treacheryCardCache;
    public static final List<String> FREMEN_MAY_SHIP = List.of("Great Flat", "The Greater Flat", "Funeral Plain");

    private static class TerritoryLocation {
        int sector;
        String name;
        String adjacentTerritoryName;

        public TerritoryLocation(String name, int sector, String adjacent) {
            this.sector = sector;
            this.name = name;
            this.adjacentTerritoryName = adjacent;
        }
    }

    public IdualiFaction(String player, String userName) throws IOException {
        super("Iduali", player, userName);

        this.spice = 10;
        this.freeRevival = 3;
        this.emoji = Emojis.getFactionEmoji(name);
        this.forceEmoji = Emojis.getForceEmoji(name);
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 1;
        this.homeworld = "Deep Desert";
        this.wormRides = null;
        this.startingForcesPlaced = 0;
        this.territoriesPlaced = 0;
        this.territoryLocations = new LinkedList<>();

        treacheryCardCache = new ArrayList<>();
        treacheryCardCache.add(new TreacheryCard("Hidden Blades"));
        treacheryCardCache.add(new TreacheryCard("Water of Life"));
        treacheryCardCache.add(new TreacheryCard("Body Double"));
        treacheryCardCache.add(new TreacheryCard("Iduali Thumpers"));
        treacheryCardCache.add(new TreacheryCard("Mining Laser"));
    }

    public boolean isCavesBlocked() {
        return cavesBlocked;
    }

    public void setCavesBlocked(boolean isBlocked) {
        cavesBlocked = isBlocked;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory deepDesert = game.getTerritories().addHomeworld(game, homeworld, name);
        deepDesert.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
        game.removeHieregToken("Jacurutu Sietch");
    }

    public int getStartingForcesPlaced() {
        return startingForcesPlaced;
    }

    public int getTerritoriesPlaced() {
        return territoriesPlaced;
    }

    public void resetPlacingTerritories() {
        placedShuloch = false;
        placedJacarutu = false;
        placedKharuz = false;
        territoryLocations = new LinkedList<>();
        territoriesPlaced = 0;
    }

    public int placeTerritory(String territoryName, int sector, String adjacentTerritoryName) {
        TerritoryLocation location = new TerritoryLocation(territoryName, sector, adjacentTerritoryName);
        territoryLocations.add(location);
        ++territoriesPlaced;
        switch(territoryName) {
            case "Sietch Jacurutu" -> placedJacarutu = true;
            case "Shuloch" -> placedShuloch = true;
            case "Kharuz" -> placedKharuz = true;
        }
        if (territoriesPlaced == 3)
            finalizeTerritories();
        return territoriesPlaced;
    }

    private void finalizeTerritories() {
        for (TerritoryLocation location : territoryLocations) {
            game.getTerritories().addIdualiTerritory(game, location.name, location.sector, location.adjacentTerritoryName);
        }
    }

    public void presentTerritoriesChoices() {
        String buttonPrefix = "iduali-place-territory";
        List<DuneChoice> choices = new LinkedList<>();
        if (!placedJacarutu) choices.add(new DuneChoice(buttonPrefix + "-Sietch Jacurutu", "Sietch Jacurutu"));
        if (!placedShuloch) choices.add(new DuneChoice(buttonPrefix + "-Shuloch", "Shuloch"));
        if (!placedKharuz) choices.add(new DuneChoice(buttonPrefix + "-Kharuz", "Kharuz"));
        choices.add(new DuneChoice("secondary", buttonPrefix + "-reset", "Reset"));
        chat.reply("You have " + (3 - territoriesPlaced) + " territories remaining to place.\nWhich would you like to place next? " + player, choices);
    }

    public List<String> getTerritoryNames(boolean forSelf) {
        List<String> territories = List.of(
            "Sietch Jacurutu", "Shuloch", "Kharuz"
        );
        if (forSelf) {
            return territories.stream().map(t -> {
                IdualiTerritory territory = (IdualiTerritory)game.getTerritory(t);
                return territory.getTerritoryName() + " (Sector " + territory.getSector() + ")";
            }).toList();
        }
        return territories.stream().map(t -> game.getTerritory(t).getPublicName()).toList();
    }

    @Override
    public void presentStartingForcesChoices() {
        shipment.clear();
        String buttonPrefix = "ship-starting-forces";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice(buttonPrefix + "-Sietch Jacurutu", "Sietch Jacurutu (Sector " + getTerritorySector("Sietch Jacurutu") + ")"));
        choices.add(new DuneChoice(buttonPrefix + "-Shuloch", "Shuloch (Sector " + getTerritorySector("Shuloch") + ")"));
        choices.add(new DuneChoice(buttonPrefix + "-Kharuz", "Kharuz (Sector " + getTerritorySector("Kharuz") + ")"));
        chat.reply("You have " + (10 - startingForcesPlaced) + " " + forceEmoji + " remaining to place.\nWhere would you like to place next? " + player, choices);
    }

    @Override
    public boolean placeChosenStartingForces() throws InvalidGameStateException {
        int amount = shipment.getForce();
        int totalPlaced = startingForcesPlaced + amount;
        if (amount <= 0 || totalPlaced > 10)
            throw new InvalidGameStateException("Iduali cannot place " + amount + " starting forces.");
        executeShipment(game, false, true);
        startingForcesPlaced = totalPlaced;
        if (startingForcesPlaced == 10) {
            chat.reply("Initial force placement complete.");
            return true;
        }
        presentStartingForcesChoices();
        return false;
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
        String wormName = "Shai-Hulud";
        Territory territory = getGame().getTerritory(territoryName);
        String idualiForces = "";
        int strength = territory.getForceStrength("Iduali");
        if (strength > 0)
            idualiForces += strength + " " + forceEmoji;
        if (idualiForces.isEmpty()) {
            game.getTurnSummary().publish(forceEmoji + " have no forces in " + territoryName + " to ride Shai-Hulud.");
            return;
        }
        game.getTurnSummary().publish(idualiForces + " may ride Shai-Hulud from " + territoryName + "!");
        movement.setMovingFrom(territoryName);
        movement.setMoveType(MoveType.WORM_RIDE);
        movement.presentTerritoryTypeChoices();
        wormRideActive = true;
    }

    public boolean isWormRideActive() {
        return wormRideActive;
    }

    public void setWormRideActive(boolean wormRideActive) {
        this.wormRideActive = wormRideActive;
    }

    public Set<String> getCaveMovableTerritories() {
        Territories territories = game.getTerritories();
        int fromSector = game.getTerritory(getMovement().getCaveFrom()).getSector();
        int plus5 = fromSector + 5;
        int minus5 = fromSector - 5;
        if (plus5 > 18) plus5 -= 18;
        if (minus5 < 1) minus5 += 18;

        Set<String> result = new HashSet<>();
        if (game.getStorm() != plus5)
            result.addAll(territories.getTerritoryNamesInSector(plus5));
        if (game.getStorm() != minus5)
            result.addAll(territories.getTerritoryNamesInSector(minus5));
        return result;
    }

    public void craft(String craftCardName, String firstCardName, String secondCardName, boolean forAlly) {
        TreacheryCard craftCard = getTreacheryCardFromCache(craftCardName);
        if (forAlly) {
            Faction ally = game.getFaction(getAlly());
            ally.getTreacheryHand().add(craftCard);
            ally.discard(firstCardName);
            ally.discard(secondCardName);
            game.getTurnSummary().publish(ally.getEmoji() + " crafts a card");
        }
        else {
            treacheryHand.add(craftCard);
            discard(firstCardName);
            discard(secondCardName);
            game.getTurnSummary().publish(emoji + " crafts a card");
        }
        removeTreacheryCardFromCache(craftCard);
    }

    private TreacheryCard getTreacheryCardFromCache(String name) {
        return treacheryCardCache.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found in cache"));
    }

    public TreacheryCard removeTreacheryCardFromCache(TreacheryCard card) {
        treacheryCardCache.remove(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        return card;
    }

    public List<TreacheryCard> getTreacheryCardCache() {
        return treacheryCardCache;
    }

    public List<Integer> getPlacedSectors() {
        List<Integer> result = new ArrayList<>();
        for (TerritoryLocation location : territoryLocations)
            result.add(location.sector);
        return result;
    }

    private int getTerritorySector(String territoryName) {
        for (TerritoryLocation location : territoryLocations) {
            if (location.name.equals(territoryName))
                return location.sector;
        }
        return 0;
    }
}
