package model;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Game extends GameFactionBase {
    private String gameRole;
    private String modRole;
    private Boolean mute;
    private int turn;
    private int phase;
    private int storm;

    private int stormMovement;

    private int marketSize;

    private final List<Faction> factions;

    private final Map<String, Territory> territories;
    private final LinkedList<TreacheryCard> treacheryDeck;
    private final LinkedList<TreacheryCard> treacheryDiscard;
    private final LinkedList<SpiceCard> spiceDeck;
    private final LinkedList<SpiceCard> spiceDiscardA;
    private final LinkedList<SpiceCard> spiceDiscardB;
    private final LinkedList<TraitorCard> traitorDeck;
    private final LinkedList<TreacheryCard> market;
    private final LinkedList<LeaderSkillCard> leaderSkillDeck;
    private final LinkedList<Force> tanks;
    private final LinkedList<Leader> leaderTanks;
    public Game() {
        super();

        factions = new LinkedList<>();
        territories = new HashMap<>();
        CSVParser csvParser = getCSVFile("Territories.csv");
        for (CSVRecord csvRecord : csvParser) {
            territories.put(csvRecord.get(0), new Territory(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Boolean.parseBoolean(csvRecord.get(2)), Boolean.parseBoolean(csvRecord.get(3))));
        }

        this.treacheryDeck = new LinkedList<>();
        this.spiceDeck = new LinkedList<>();
        this.traitorDeck = new LinkedList<>();
        this.leaderSkillDeck = new LinkedList<>();
        this.treacheryDiscard = new LinkedList<>();
        this.spiceDiscardA = new LinkedList<>();
        this.spiceDiscardB = new LinkedList<>();
        this.tanks = new LinkedList<>();
        this.leaderTanks = new LinkedList<>();
        this.market = new LinkedList<>();
        this.marketSize = 0;
        this.turn = 0;
        this.phase = 0;
        this.storm = 18;
        this.stormMovement = 0;

        csvParser = getCSVFile("TreacheryCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            treacheryDeck.add(new TreacheryCard(csvRecord.get(0), csvRecord.get(1)));
        }
        csvParser = getCSVFile("SpiceCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            spiceDeck.add(new SpiceCard(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Integer.parseInt(csvRecord.get(2))));
        }
        csvParser = getCSVFile("LeaderSkillCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            leaderSkillDeck.add(new LeaderSkillCard(csvRecord.get(0)));
        }
    }

    private CSVParser getCSVFile (String name) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass()
                    .getClassLoader().getResourceAsStream(name))));
            return CSVParser.parse(bufferedReader, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public Map<String, Territory> getTerritories() {
        return territories;
    }

    public String getGameRole() {
        return gameRole;
    }

    public void setGameRole(String gameRole) {
        this.gameRole = gameRole;
    }

    public String getModRole() {
        return modRole;
    }

    public void setModRole(String modRole) {
        this.modRole = modRole;
    }

    public Boolean getMute() {
        return mute;
    }

    public void setMute(Boolean mute) {
        this.mute = mute;
    }

    private Optional<Faction> findFaction(String name) {
        return factions.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }

    public Faction getFaction(String name) {
        return findFaction(name).get();
    }

    public Boolean hasFaction(String name) {
        return findFaction(name).isPresent();
    }

    public void addFaction(Faction faction) {
        factions.add(faction);
    }

    public LinkedList<TreacheryCard> getTreacheryDeck() {
        return treacheryDeck;
    }

    public LinkedList<SpiceCard> getSpiceDeck() {
        return spiceDeck;
    }
    public LinkedList<TraitorCard> getTraitorDeck() {
        return traitorDeck;
    }

    public LinkedList<LeaderSkillCard> getLeaderSkillDeck() {
        return leaderSkillDeck;
    }

    public LinkedList<TreacheryCard> getTreacheryDiscard() {
        return treacheryDiscard;
    }

    public LinkedList<SpiceCard> getSpiceDiscardA() {
        return spiceDiscardA;
    }

    public LinkedList<SpiceCard> getSpiceDiscardB() {
        return spiceDiscardB;
    }

    public LinkedList<TreacheryCard> getMarket() {
        return market;
    }

    public int getTurn() {
        return turn;
    }

    public int getPhase() {
        return phase;
    }

    public int getStorm() {
        return storm;
    }

    public LinkedList<Force> getTanks() {
        return tanks;
    }

    public Force getForceFromTanks(String forceName) {
        return this.tanks.stream().filter(force -> force.getName().equals(forceName)).findFirst().orElse(new Force(forceName, 0));
    }

    public LinkedList<Leader> getLeaderTanks() {
        return leaderTanks;
    }

    public Leader removeLeaderFromTanks(String name) {
        Leader remove = null;
        for (Leader leader : leaderTanks) {
            if (leader.name().equals(name)) {
                remove = leader;
            }
        }
        if (remove == null) throw new IllegalArgumentException("Leader not found.");
        leaderTanks.remove(remove);
        return remove;
    }

    public void advanceTurn() {
        this.turn += 1;
        this.phase = 1;
    }

    public void advancePhase() {
        this.phase += 1;
        if (phase == 10) {
            advanceTurn();
        }
    }

    public void setStorm(int storm) {
        this.storm = storm;
    }

    public void advanceStorm(int movement) {
        this.storm += movement;
    }

    public void breakShieldWall() {
        this.territories.get("Carthag").setRock(false);
        this.territories.get("Imperial Basin (Center Sector)").setRock(false);
        this.territories.get("Imperial Basin (East Sector)").setRock(false);
        this.territories.get("Imperial Basin (West Sector)").setRock(false);
        this.territories.get("Arrakeen").setRock(false);
    }

    public int getMarketSize() {
        return marketSize;
    }

    public void setMarketSize(int marketSize) {
        this.marketSize = marketSize;
    }

    public int getStormMovement() {
        return stormMovement;
    }

    public void setStormMovement(int stormMovement) {
        this.stormMovement = stormMovement;
    }
}
