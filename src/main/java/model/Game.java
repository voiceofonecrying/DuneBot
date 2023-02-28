package model;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

import static controller.Initializers.getCSVFile;

public class Game extends GameFactionBase {
    private String gameRole;
    private String modRole;
    private Boolean mute;
    private boolean techTokens;
    private boolean leaderSkills;
    private boolean strongholdSkills;
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
        this.techTokens = false;
        this.leaderSkills = false;
        this.strongholdSkills = false;

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

    /**
     * Returns the faction's turn index (0-5), accounting for the storm.
     * @param name The faction's name.
     * @return The faction's turn index.
     */
    public int getFactionTurnIndex(String name) {
        int rawTurnIndex = factions.indexOf(findFaction(name).get());
        int stormSection = Math.ceilDiv(getStorm(), 3);
        return (rawTurnIndex - stormSection) % factions.size();
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

    public void removeZeroStrengthTanks() {
        this.tanks.removeIf(f-> f.getStrength() == 0);
    }

    public Force getForceFromTanks(String forceName) {
        // This is a temporary fix for duplicates in the tanks list.
        removeZeroStrengthTanks();

        List<Force> forces = this.tanks.stream().filter(f-> f.getName().equalsIgnoreCase(forceName)).toList();

        Force force;
        if (forces.size() > 1) {
            throw new IllegalArgumentException("Duplicate forces found in tanks list.");
        } else if (forces.size() == 1) {
            return forces.get(0);
        } else {
            force = new Force(forceName, 0);
            this.tanks.add(force);
            return force;
        }
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

    public boolean hasTechTokens() {
        return techTokens;
    }

    public void setTechTokens(boolean techTokens) {
        this.techTokens = techTokens;
    }

    public boolean hasLeaderSkills() {
        return leaderSkills;
    }

    public void setLeaderSkills(boolean leaderSkills) {
        this.leaderSkills = leaderSkills;
    }

    public boolean hasStrongholdSkills() {
        return strongholdSkills;
    }

    public void setStrongholdSkills(boolean strongholdSkills) {
        this.strongholdSkills = strongholdSkills;
    }

    public void drawCard(String deckName, String faction) {
        switch (deckName) {
            case "traitor deck" -> getFaction(faction).getTraitorHand().add(getTraitorDeck().pollLast());
            case "treachery deck" -> getFaction(faction).getTreacheryHand().add(getTreacheryDeck().pollLast());
            case "leader skills deck" -> getFaction(faction).getLeaderSkillsHand().add(getLeaderSkillDeck().pollLast());
        }
    }
}
