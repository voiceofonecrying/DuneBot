package model;

import enums.GameOption;
import enums.SetupStep;
import model.factions.Faction;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static controller.Initializers.getCSVFile;

public class Game {
    private String gameRole;
    private String modRole;
    private Boolean mute;
    private int turn;
    private Set<GameOption> gameOptions;
    private List<SetupStep> setupSteps;
    private boolean setupStarted;
    private boolean setupFinished;

    private int phase;
    private int subPhase;
    private int storm;

    private int stormMovement;
    private int bidCardNumber;

    private List<String> bidOrder;
    private TreacheryCard bidCard;

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
    private boolean shieldWallDestroyed;
    private String currentBidder;
    private int currentBid;
    private String bidLeader;
    private boolean sandtroutInPlay;

    public Game() throws IOException {
        super();

        factions = new LinkedList<>();
        territories = new HashMap<>();
        CSVParser csvParser = getCSVFile("Territories.csv");
        for (CSVRecord csvRecord : csvParser) {
            territories.put(csvRecord.get(0), new Territory(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Boolean.parseBoolean(csvRecord.get(2)), Boolean.parseBoolean(csvRecord.get(3)), Boolean.parseBoolean(csvRecord.get(4))));
        }

        this.gameOptions = new HashSet<>();
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
        this.turn = 0;
        this.phase = 0;
        this.subPhase = 0;
        this.storm = 18;
        this.stormMovement = 0;
        this.shieldWallDestroyed = false;
        this.bidLeader = "";
        this.currentBidder = "";

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

        this.bidOrder = new ArrayList<>();
        this.bidCardNumber = 0;
        this.sandtroutInPlay = false;
    }

    public Set<GameOption> getGameOptions() {
        return gameOptions;
    }

    public boolean hasGameOption(GameOption gameOption) {
        return getGameOptions().contains(gameOption);
    }

    public void setGameOptions(Set<GameOption> gameOptions) {
        this.gameOptions = gameOptions;
    }

    public void addGameOption(GameOption gameOption) {
        if (this.gameOptions == null) {
            this.gameOptions = new HashSet<>();
        }
        this.gameOptions.add(gameOption);
    }

    public void removeGameOption(GameOption gameOption) {
        this.gameOptions.remove(gameOption);
    }

    public List<SetupStep> getSetupSteps() {
        return setupSteps;
    }

    public void setSetupSteps(List<SetupStep> setupSteps) {
        this.setupSteps = setupSteps;
    }

    public boolean isSetupStarted() {
        return setupStarted;
    }

    public void setSetupStarted(boolean setupStarted) {
        this.setupStarted = setupStarted;
    }

    public boolean isSetupFinished() {
        return setupFinished;
    }

    public void setSetupFinished(boolean setupFinished) {
        this.setupFinished = setupFinished;
        this.getFactions().forEach(Faction::setFrontOfShieldModified);
    }

    public TreacheryCard getBidCard() {
        return bidCard;
    }

    public void setBidCard(TreacheryCard bidCard) {
        this.bidCard = bidCard;
    }

    public int getBidCardNumber() {
        return bidCardNumber;
    }

    public void setBidCardNumber(int bidCardNumber) {
        this.bidCardNumber = bidCardNumber;
    }

    public void incrementBidCardNumber() {
        bidCardNumber++;
    }

    public List<String> getBidOrder() {
        return bidOrder;
    }

    public List<String> getEligibleBidOrder() {
        return bidOrder
                .stream()
                .filter(f -> getFaction(f).getHandLimit() > getFaction(f).getTreacheryHand().size())
                .collect(Collectors.toList());
    }

    public void setBidOrder(List<String> bidOrder) {
        this.bidOrder = bidOrder;
    }

    public void clearBidCardInfo(){
        setBidCard(null);
        setBidLeader("");
        setCurrentBid(0);
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public Map<String, Territory> getTerritories() {
        return territories;
    }

    /**
     * Returns the territory with the given name.
     * @param name The territory's name.
     * @return The territory with the given name.
     */
    public Territory getTerritory(String name) {
        Territory territory = territories.get(name);
        if (territory == null) {
            throw new IllegalArgumentException("No territory with name " + name);
        }
        return territory;
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
        int rawTurnIndex = factions.indexOf(getFaction(name));
        int stormSection = Math.ceilDiv(getStorm(), 3);

        return Math.floorMod(rawTurnIndex - stormSection, factions.size());
    }

    public Faction getFaction(String name) {
        return findFaction(name).orElseThrow(() -> new IllegalArgumentException("No faction with name " + name));
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

    public int getSubPhase() {
        return subPhase;
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

    public void addToTanks(String forceName, int amount) {
        Force force = getForceFromTanks(forceName);
        force.addStrength(amount);
    }

    public LinkedList<Leader> getLeaderTanks() {
        return leaderTanks;
    }

    public Leader removeLeaderFromTanks(String name) {
        Leader remove = leaderTanks.stream()
                .filter(l -> l.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Leader not found."));

        leaderTanks.remove(remove);
        return remove;
    }

    public void advanceTurn() {
        turn++;
        phase = 1;
        subPhase = 1;
    }

    public void advancePhase() {
        phase++;
        subPhase = 1;

        if (turn >= 1 && phase >= 10) {
            advanceTurn();
        }
    }

    public void advanceSubPhase() {
        subPhase++;
    }

    public void setStorm(int storm) {
        this.storm = ((storm - 1) % 18) + 1;
    }

    public void advanceStorm(int movement) {
        setStorm(getStorm() + movement);
    }

    public Faction getFactionWithAtomics() {
        for (Faction faction : getFactions()) {
            try {
                faction.getTreacheryCard("Family Atomics ");
                return faction;
            }
            catch (IllegalArgumentException e) {
                continue;
            }
        }

        throw new NoSuchElementException("No faction holds Atomics");
    }

    public String breakShieldWall(Faction factionWithAtomics) {
        shieldWallDestroyed = true;
        territories.get("Carthag").setRock(false);
        territories.get("Imperial Basin (Center Sector)").setRock(false);
        territories.get("Imperial Basin (East Sector)").setRock(false);
        territories.get("Imperial Basin (West Sector)").setRock(false);
        territories.get("Arrakeen").setRock(false);

        String message = "The Shield Wall has been destroyed. ";
        TreacheryCard familyAtomics = factionWithAtomics.removeTreacheryCard("Family Atomics ");
        if (hasGameOption(GameOption.FAMILY_ATOMICS_TO_DISCARD)) {
            getTreacheryDiscard().add(familyAtomics);
            message += "Family Atomics has been moved to the discard pile.";
        } else {
            message += "Family Atomics has been removed from the game.";
        }
        return message;
    }

    public boolean isShieldWallDestroyed() {
        return shieldWallDestroyed;
    }

    public int getStormMovement() {
        return stormMovement;
    }

    public void setStormMovement(int stormMovement) {
        this.stormMovement = stormMovement;
    }

    public boolean hasTechTokens() {
        return hasGameOption(GameOption.TECH_TOKENS);
    }

    public boolean hasLeaderSkills() {
        return hasGameOption(GameOption.LEADER_SKILLS);
    }

    public boolean hasStrongholdSkills() {
        return hasGameOption(GameOption.STRONGHOLD_SKILLS);
    }

    public void drawCard(String deckName, String faction) {
        switch (deckName) {
            case "traitor deck" -> getFaction(faction).addTraitorCard(getTraitorDeck().pollLast());
            case "treachery deck" -> getFaction(faction).addTreacheryCard(getTreacheryDeck().pollLast());
            case "leader skills deck" -> getFaction(faction).getLeaderSkillsHand().add(getLeaderSkillDeck().pollLast());
        }
    }
    public String getCurrentBidder() {
        return currentBidder;
    }

    public void setCurrentBidder(String currentBidder) {
        this.currentBidder = currentBidder;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(int currentBid) {
        this.currentBid = currentBid;
    }
    public String getBidLeader() {
        return bidLeader;
    }

    public void setBidLeader(String bidLeader) {
        this.bidLeader = bidLeader;
    }

    /**
     * @return true if a Sandtrout was drawn and not canceled by a worm
     */
    public boolean isSandtroutInPlay() {
        return sandtroutInPlay;
    }

    /**
     * @param sandtroutInPlay true if a Sandtrout was drawn and not canceled by a worm
     */
    public void setSandtroutInPlay(boolean sandtroutInPlay) {
        this.sandtroutInPlay = sandtroutInPlay;
    }
}
