package model;

import caches.LeaderSkillCardsCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import constants.Emojis;
import enums.GameOption;
import enums.SetupStep;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import helpers.Exclude;
import model.factions.*;
import model.topics.DuneTopic;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static model.Initializers.getCSVFile;
import static model.Initializers.getJSONString;

public class Game {
    private String gameRole;
    private int turn;
    private int phase;
    private int subPhase;
    private int phaseForTracker;
    private Bidding bidding;
    private Battles battles;
    private MentatPause mentatPause;
    private final Deque<String> turnOrder;
    private final List<Faction> factions;
    private final Territories territories;
    private final LinkedList<SpiceCard> spiceDeck;
    private final LinkedList<SpiceCard> spiceDiscardA;
    private final LinkedList<SpiceCard> spiceDiscardB;
    private final LinkedList<TraitorCard> traitorDeck;
    private final LinkedList<LeaderSkillCard> leaderSkillDeck;
    private final LinkedList<NexusCard> nexusDeck;
    private final LinkedList<NexusCard> nexusDiscard;
    private final LinkedList<Force> tanks;
    private final LinkedList<Leader> leaderTanks;
    private transient final HashMap<String, List<String>> adjacencyList;
    private final HashMap<String, String> homeworlds;
    private final List<String> hieregTokens;
    private final List<String> smugglerTokens;
    private final LinkedList<TreacheryCard> treacheryDeck;
    private final LinkedList<TreacheryCard> treacheryDiscard;
    private boolean extortionTokenRevealed;
    private int hmsRotation = 0;
    private boolean ixHMSActionRequired;
    private HashMap<Integer, List<String>> quotes;
    private String modRole;
    private Boolean mute;
    private String phaseForWhispers;
    private Set<GameOption> gameOptions;
    private String mod;
    private String gameRoleMention;
    private boolean shieldWallDestroyed;
    private boolean sandtroutInPlay;
    private List<SetupStep> setupSteps;
    private boolean setupStarted;
    private boolean setupFinished;
    private int storm;
    private int stormMovement;
    private final ArrayList<Integer> stormDeck;
    private boolean onHold;

    @Exclude
    private Set<UpdateType> updateTypes;
    @Exclude
    private DuneTopic turnSummary;
    @Exclude
    private DuneTopic whispers;
    @Exclude
    private DuneTopic gameActions;
    @Exclude
    private DuneTopic modInfo;

    public Game() throws IOException {
        super();

        this.turn = 0;
        this.phase = 0;
        this.subPhase = 0;
        this.phaseForTracker = 0;
        this.bidding = null;
        turnOrder = new LinkedList<>();
        factions = new LinkedList<>();
        territories = new Territories();
        CSVParser csvParser = getCSVFile("Territories.csv");
        for (CSVRecord csvRecord : csvParser) {
            territories.put(csvRecord.get(0), new Territory(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Boolean.parseBoolean(csvRecord.get(2)), Boolean.parseBoolean(csvRecord.get(3)), Boolean.parseBoolean(csvRecord.get(4))));
        }

        this.gameOptions = new HashSet<>();
        this.spiceDeck = new LinkedList<>();
        this.traitorDeck = new LinkedList<>();
        this.leaderSkillDeck = new LinkedList<>();
        this.spiceDiscardA = new LinkedList<>();
        this.spiceDiscardB = new LinkedList<>();
        this.tanks = new LinkedList<>();
        this.leaderTanks = new LinkedList<>();
        this.nexusDeck = new LinkedList<>();
        this.nexusDiscard = new LinkedList<>();
        this.homeworlds = new HashMap<>();
        this.hieregTokens = new LinkedList<>();
        this.smugglerTokens = new LinkedList<>();
        this.treacheryDeck = new LinkedList<>();
        this.treacheryDiscard = new LinkedList<>();
        this.shieldWallDestroyed = false;
        this.phaseForWhispers = "";
        this.mod = "";
        this.gameRoleMention = "";
        this.storm = 18;
        this.stormMovement = 0;
        this.stormDeck = null;
        this.onHold = false;
        this.quotes = new HashMap<>();

        csvParser = getCSVFile("TreacheryCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            treacheryDeck.add(new TreacheryCard(csvRecord.get(0)));
        }

        csvParser = getCSVFile("SpiceCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            spiceDeck.add(new SpiceCard(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Integer.parseInt(csvRecord.get(2)), null, null));
        }

        LeaderSkillCardsCache.getNames().forEach(name -> leaderSkillDeck.add(new LeaderSkillCard(LeaderSkillCardsCache.getCardInfo(name).get("Name"))));

        csvParser = getCSVFile("NexusCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            nexusDeck.add(new NexusCard(csvRecord.get(0)));
        }

        csvParser = getCSVFile("quotes.csv");
        for (CSVRecord csvRecord : csvParser) {
            quotes.computeIfAbsent(Integer.valueOf(csvRecord.get(0)), k -> new LinkedList<>());
            quotes.get(Integer.valueOf(csvRecord.get(0))).add(csvRecord.get(1));
        }

        smugglerTokens.add("Orgiz Processing Station");
        smugglerTokens.add("Treachery Card Stash");
        smugglerTokens.add("Spice Stash");
        smugglerTokens.add("Ornithopter");

        hieregTokens.add("Jacurutu Sietch");
        hieregTokens.add("Cistern");
        hieregTokens.add("Ecological Testing Station");
        hieregTokens.add("Shrine");

        Collections.shuffle(nexusDeck);
        Collections.shuffle(smugglerTokens);
        Collections.shuffle(hieregTokens);

        String json = getJSONString("AdjacencyList.json");
        this.adjacencyList = new HashMap<>();
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        for (Map.Entry<String, JsonElement> territory : jsonObject.entrySet()) {
            List<String> adjacent = new LinkedList<>();
            for (JsonElement adj : territory.getValue().getAsJsonArray().asList()) {
                adjacent.add(adj.getAsString());
            }
            adjacencyList.put(territory.getKey(), adjacent);
        }

        this.sandtroutInPlay = false;
    }

    public void startBidding() {
        bidding = new Bidding();
        setUpdated(UpdateType.MAP);
        try {
            RicheseFaction faction = (RicheseFaction) getFaction("Richese");
            if (!faction.getTreacheryCardCache().isEmpty()) bidding.setRicheseCacheCardOutstanding(true);
        } catch (IllegalArgumentException e) {
            // Richese not in the game
        }
    }

    public void endBidding() {
        bidding = null;
    }

    public Set<GameOption> getGameOptions() {
        return gameOptions;
    }

    public boolean hasGameOption(GameOption gameOption) {
        return getGameOptions().contains(gameOption);
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
        this.getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_FRONT_OF_SHIELD));
    }

    public void setUpdated(UpdateType updateType) {
        if (this.updateTypes == null) this.updateTypes = new HashSet<>();
        this.updateTypes.add(updateType);
    }

    public Set<UpdateType> getUpdateTypes() {
        if (this.updateTypes == null) this.updateTypes = new HashSet<>();
        return this.updateTypes;
    }

    public boolean isExtortionTokenRevealed() {
        return extortionTokenRevealed;
    }

    public void setExtortionTokenRevealed(boolean extortionTokenRevealed) {
        this.extortionTokenRevealed = extortionTokenRevealed;
    }

    public void rotateHMS90degrees() {
        hmsRotation = (hmsRotation + 90) % 360;
    }

    public int getHmsRotation() {
        return hmsRotation;
    }

    public boolean isIxHMSActionRequired() {
        return ixHMSActionRequired;
    }

    public void setIxHMSActionRequired(boolean ixHMSActionRequired) {
        this.ixHMSActionRequired = ixHMSActionRequired;
    }

    public boolean isOnHold() {
        return onHold;
    }

    public void setOnHold(boolean onHold) {
        this.onHold = onHold;
    }

    public Bidding getBidding() throws InvalidGameStateException {
        if (bidding == null) throw new InvalidGameStateException("Game is not in bidding phase.");
        return bidding;
    }

    public Battles getBattles() throws InvalidGameStateException {
        if (battles == null) throw new InvalidGameStateException("Game is not in battle phase.");
        return battles;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public Territories getTerritories() {
        return territories;
    }

    /**
     * Returns the territory with the given name.
     *
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

    public String getMod() {
        return mod;
    }

    public void setMod(String mod) {
        this.mod = mod;
    }

    public String getGameRoleMention() {
        return gameRoleMention;
    }

    public void setGameRoleMention(String gameRoleMention) {
        this.gameRoleMention = gameRoleMention;
    }

    public void setPhaseForWhispers(String phaseForWhispers) {
        this.phaseForWhispers = phaseForWhispers;
    }

    public void publishWhisper(String whisper) {
        whispers.publish(phaseForWhispers + whisper);
        phaseForWhispers = "";
    }

    public Boolean getMute() {
        return mute;
    }

    public void setMute(Boolean mute) {
        this.mute = mute;
    }

    private Optional<Faction> findFaction(String name) {
        return factions.stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Returns the faction's turn index (0-5), accounting for the storm.
     *
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

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public int getPhase() {
        return phase;
    }

    public int getPhaseForTracker() {
        if (phaseForTracker == 0) return phase;
        return phaseForTracker;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public int getSubPhase() {
        return subPhase;
    }

    public int getStorm() {
        return storm;
    }

    public ArrayList<Integer> getStormDeck() {
        return stormDeck;
    }

    public void setStorm(int storm) {
        this.storm = ((storm - 1) % 18) + 1;
    }

    public LinkedList<Force> getTanks() {
        return tanks;
    }

    public void removeZeroStrengthTanks() {
        this.tanks.removeIf(f -> f.getStrength() == 0);
    }

    public Force getForceFromTanks(String forceName) {
        // This is a temporary fix for duplicates in the tanks list.
        removeZeroStrengthTanks();

        List<Force> forces = this.tanks.stream().filter(f -> f.getName().equalsIgnoreCase(forceName)).toList();

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
                .filter(l -> l.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Leader not found."));

        leaderTanks.remove(remove);
        return remove;
    }

    public void advanceTurn() {
        turn++;
        phase = 1;
        phaseForTracker = 1;
        subPhase = 1;
        factions.forEach(Faction::clearWhisperCounts);
        setUpdated(UpdateType.MAP);
    }

    public void advancePhase() {
        phaseForTracker = phase;
        phase++;
        subPhase = 1;
        factions.forEach(Faction::clearWhisperCountsPerPhase);

        if (turn >= 1 && phase > 10) {
            advanceTurn();
        }
    }

    public void advanceSubPhase() {
        phaseForTracker = phase;
        subPhase++;
    }

    public void advanceStorm(int movement) {
        setStorm(getStorm() + movement);
    }

    public Faction getFactionWithAtomics() {
        for (Faction faction : getFactions()) {
            try {
                faction.getTreacheryCard("Family Atomics");
                return faction;
            } catch (IllegalArgumentException e) {
                // faction does not hold Family Atomics
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
        TreacheryCard familyAtomics = factionWithAtomics.removeTreacheryCard("Family Atomics");
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

    public boolean hasStrongholdSkills() {
        return hasGameOption(GameOption.STRONGHOLD_SKILLS);
    }

    public boolean drawTreacheryCard(String faction) {
        boolean deckReplenished = false;
        if (treacheryDeck.isEmpty()) {
            deckReplenished = true;
            treacheryDeck.addAll(treacheryDiscard);
            treacheryDiscard.clear();
        }
        getFaction(faction).addTreacheryCard(getTreacheryDeck().pollLast());
        return deckReplenished;
    }

    public void drawCard(String deckName, String faction) {
        switch (deckName) {
            case "traitor deck" -> getFaction(faction).addTraitorCard(getTraitorDeck().pollLast());
            case "treachery deck" -> drawTreacheryCard(faction);
            case "leader skills deck" -> getFaction(faction).getLeaderSkillsHand().add(getLeaderSkillDeck().pollLast());
        }
    }

    public void shuffleTreacheryDeck() {
        Collections.shuffle(getTreacheryDeck());
    }

    public HashMap<String, List<String>> getAdjacencyList() {
        return adjacencyList;
    }

    public Deque<String> getTurnOrder() {
        return turnOrder;
    }

    public LinkedList<NexusCard> getNexusDeck() {
        return nexusDeck;
    }

    public LinkedList<NexusCard> getNexusDiscard() {
        return nexusDiscard;
    }

    public void discardNexusCard(Faction faction) {
        nexusDiscard.add(faction.getNexusCard());
        faction.setNexusCard(null);
        turnSummary.publish(faction.getEmoji() + " has discarded a Nexus Card.");
        faction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public HashMap<String, String> getHomeworlds() {
        return homeworlds;
    }

    public HashMap<Integer, List<String>> getQuotes() throws IOException {
        if (quotes == null) {
            this.quotes = new HashMap<>();
            CSVParser csvParser = getCSVFile("quotes.csv");
            for (CSVRecord csvRecord : csvParser) {
                quotes.computeIfAbsent(Integer.valueOf(csvRecord.get(0)), k -> new LinkedList<>());
                quotes.get(Integer.valueOf(csvRecord.get(0))).add(csvRecord.get(1));
            }
        }
        return quotes;
    }

    public void transferCard(String giverName, String receiverName, String cardName) {
        Faction giver = getFaction(giverName);
        Faction receiver = getFaction(receiverName);
        receiver.addTreacheryCard(
                giver.removeTreacheryCard(cardName)
        );
        receiver.getLedger().publish("Received " + cardName + " from " + giver.getEmoji());
        giver.getLedger().publish("Sent " + cardName + " to " + receiver.getEmoji());
    }

    public DuneTopic getTurnSummary() {
        return turnSummary;
    }

    public void setTurnSummary(DuneTopic turnSummary) {
        this.turnSummary = turnSummary;
    }

    public DuneTopic getWhispers() {
        return whispers;
    }

    public void setWhispers(DuneTopic whispers) {
        this.whispers = whispers;
    }

    public DuneTopic getGameActions() {
        return gameActions;
    }

    public void setGameActions(DuneTopic gameActions) {
        this.gameActions = gameActions;
    }

    public DuneTopic getModInfo() {
        return modInfo;
    }

    public void setModInfo(DuneTopic modInfo) {
        this.modInfo = modInfo;
    }

    public List<Faction> getFactionsWithTreacheryCard(String cardName) {
        return factions.stream().filter(f -> f.hasTreacheryCard(cardName))
                .collect(Collectors.toList());
    }

    public List<Faction> getFactionsInStormOrder() {
        int firstFactionIndex = Math.ceilDiv(storm, 3) % factions.size();
        List<Faction> stormOrderFactions = new ArrayList<>();
        stormOrderFactions.addAll(factions.subList(firstFactionIndex, factions.size()));
        stormOrderFactions.addAll(factions.subList(0, firstFactionIndex));
        return stormOrderFactions;
    }

    public void setInitialStorm(int stormDialOne, int stormDialTwo) {
        advanceStorm(stormDialOne + stormDialTwo);
        turnSummary.publish("The storm has been initialized to sector " + storm + " (" + stormDialOne + " + " + stormDialTwo + ")");
        if (hasGameOption(GameOption.TECH_TOKENS)) {
            List<TechToken> techTokens = new LinkedList<>();
            Map<String, TechToken> defaultAssignments = Map.of(
                    "BT", new TechToken(TechToken.AXLOTL_TANKS),
                    "Ix", new TechToken(TechToken.HEIGHLINERS),
                    "Fremen", new TechToken(TechToken.SPICE_PRODUCTION)
            );
            defaultAssignments.forEach((k, v) -> {
                try {
                    getFaction(k).getTechTokens().add(v);
                } catch (IllegalArgumentException e) {
                    techTokens.add(v);
                }
            });
            Collections.shuffle(techTokens);
            for (TechToken techToken : techTokens) {
                getFactionsInStormOrder().stream()
                        .filter(f -> f.getTechTokens().isEmpty()).findFirst()
                        .ifPresent(faction -> faction.getTechTokens().add(techToken));
            }
        }
        setUpdated(UpdateType.MAP);
    }

    public boolean ixCanMoveHMS() {
        if (hasFaction("Ix")) {
            Territory hms = getTerritory("Hidden Mobile Stronghold");
            int cyborgsInHMS = hms.getForce("Ix*").getStrength();
            int suboidsInHMS = hms.getForce("Ix").getStrength();
            return (cyborgsInHMS + suboidsInHMS > 0);
        }
        return false;
    }

    public void startStormPhase() {
        turnSummary.publish("Turn " + turn + " Storm Phase:");
        phaseForWhispers = "Turn " + turn + " Storm Phase\n";

        Faction factionWithAtomics = null;
        try {
            factionWithAtomics = getFactionsWithTreacheryCard("Family Atomics").get(0);
        } catch (IndexOutOfBoundsException e) {
            // No faction has Family Atomics
        }
        if (factionWithAtomics != null && factionWithAtomics.isNearShieldWall()) {
            factionWithAtomics.getChat().publish(factionWithAtomics.getPlayer() + " will you play Family Atomics?");
        }

        Faction factionWithWeatherControl = null;
        try {
            factionWithWeatherControl = getFactionsWithTreacheryCard("Weather Control").get(0);
        } catch (IndexOutOfBoundsException e) {
            // No faction has Weather Control
        }
        if (factionWithWeatherControl != null && turn != 1) {
            factionWithWeatherControl.getChat().publish(factionWithWeatherControl.getPlayer() + " will you play Weather Control?");
        }

        boolean atomicsEligible = factions.stream().anyMatch(Faction::isNearShieldWall);
        if (atomicsEligible && factionWithAtomics == null) {
            boolean atomicsInDeck = treacheryDeck.stream().anyMatch(c -> c.name().equals("Family Atomics"));
            boolean atomicsInDiscard = treacheryDiscard.stream().anyMatch(c -> c.name().equals("Family Atomics"));
            if (!atomicsInDeck && !atomicsInDiscard) atomicsEligible = false;
        }
        if (turn != 1) {
            turnSummary.publish(
                    MessageFormat.format(
                            "The storm would move {0} sectors this turn. Weather Control {1}may be played at this time.",
                            stormMovement, (atomicsEligible ? "and Family Atomics " : ""))
            );
            if (atomicsEligible && storm >= 5 && storm <= 9) {
                turnSummary.publish("(Check if storm position prevents use of Family Atomics.)");
            }
        }
    }

    public void drawSpiceBlow(String spiceBlowDeckName) {
        LinkedList<SpiceCard> discard = spiceBlowDeckName.equalsIgnoreCase("A") ?
                spiceDiscardA : spiceDiscardB;
        LinkedList<SpiceCard> wormsToReshuffle = new LinkedList<>();

        StringBuilder message = new StringBuilder();

        SpiceCard drawn;

        message.append("**Spice Deck ").append(spiceBlowDeckName).append("**\n");

        boolean shaiHuludSpotted = false;
        int spiceMultiplier = 1;

        do {
            if (spiceDeck.isEmpty()) {
                spiceDeck.addAll(spiceDiscardA);
                spiceDeck.addAll(spiceDiscardB);
                Collections.shuffle(spiceDeck);
                spiceDiscardA.clear();
                spiceDiscardB.clear();
                message.append("The Spice Deck is empty, and will be recreated from the Discard piles.\n");
            }

            drawn = spiceDeck.pop();
            boolean saveWormForReshuffle = false;
            if (drawn.name().equalsIgnoreCase("Shai-Hulud") || drawn.name().equalsIgnoreCase("Great Maker")) {
                if (turn <= 1) {
                    saveWormForReshuffle = true;
                    message.append(drawn.name())
                            .append(" will be reshuffled back into deck.\n");
                } else if (!discard.isEmpty() && !shaiHuludSpotted) {
                    shaiHuludSpotted = true;

                    if (sandtroutInPlay) {
                        spiceMultiplier = 2;
                        sandtroutInPlay = false;
                        message.append(drawn.name())
                                .append(" has been spotted! The next Shai-Hulud will cause a Nexus!\n");
                    } else {
                        SpiceCard lastCard = discard.getLast();
                        message.append(drawn.name())
                                .append(" has been spotted in ").append(lastCard.name()).append("!\n");
                        int spice = territories.get(lastCard.name()).getSpice();
                        if (spice > 0) {
                            message.append(spice);
                            message.append(Emojis.SPICE);
                            message.append(" is eaten by the worm!\n");
                            territories.get(lastCard.name()).setSpice(0);
                        }
                        if (!getTerritory(lastCard.name()).getForces().isEmpty()) {
                            List<Force> forcesToRemove = new ArrayList<>();
                            for (Force force : getTerritory(lastCard.name()).getForces()) {
                                if (force.getName().contains("Fremen")) continue;
                                if (hasFaction("Fremen")) {
                                    Faction fremen = getFaction("Fremen");
                                    if (fremen.hasAlly() && force.getName().contains(fremen.getAlly())) continue;
                                }
                                message.append(MessageFormat.format("{0} {1} devoured by {2}\n",
                                        force.getStrength(), Emojis.getForceEmoji(force.getName()), drawn.name()
                                ));
                                forcesToRemove.add(force);
                            }
                            for (Force force : forcesToRemove) {
                                removeForces(lastCard.name(), getFaction(force.getFactionName()), force.getStrength(), force.getName().contains("*"), true);
                            }
                        }
                    }
                } else {
                    shaiHuludSpotted = true;
                    spiceMultiplier = 1;
                    message.append(drawn.name())
                            .append(" has been spotted!\n");
                }
            } else if (drawn.name().equalsIgnoreCase("Sandtrout")) {
                shaiHuludSpotted = true;
                message.append("Sandtrout has been spotted, and all alliances have ended!\n");
                factions.forEach(Faction::removeAlly);
                sandtroutInPlay = true;
            } else {
                message.append("Spice has been spotted in ");
                message.append(drawn.name());
                message.append("!\n");
            }
            if (saveWormForReshuffle) {
                wormsToReshuffle.add(drawn);
            } else if (!drawn.name().equalsIgnoreCase("Sandtrout")) {
                discard.add(drawn);
            }
        } while (drawn.name().equalsIgnoreCase("Shai-Hulud") ||
                drawn.name().equalsIgnoreCase("Great Maker") ||
                drawn.name().equalsIgnoreCase("Sandtrout"));

        while (!wormsToReshuffle.isEmpty()) {
            spiceDeck.add(wormsToReshuffle.pop());
            if (wormsToReshuffle.isEmpty()) {
                Collections.shuffle(spiceDeck);
            }
        }

        if (storm == drawn.sector()) message.append(" (blown away by the storm!)\n");
        if (drawn.discoveryToken() == null) territories.get(drawn.name()).addSpice(drawn.spice() * spiceMultiplier);
        else {
            getTerritory(drawn.name()).setSpice(6 * spiceMultiplier);
            if (!getTerritory(drawn.name()).getForces().isEmpty()) {
                message.append("all forces in the territory were killed in the spice blow!\n");
                List<Force> forcesToRemove = new ArrayList<>(getTerritory(drawn.name()).getForces());
                for (Force force : forcesToRemove) {
                    removeForces(drawn.name(), getFaction(force.getFactionName()), force.getStrength(), force.getName().contains("*"), true);
                }
            }
            message.append(drawn.discoveryToken()).append(" has been placed in ").append(drawn.tokenLocation()).append("\n");
            if (drawn.discoveryToken().equals("Hiereg"))
                getTerritory(drawn.tokenLocation()).setDiscoveryToken(hieregTokens.remove(0));
            else getTerritory(drawn.tokenLocation()).setDiscoveryToken(smugglerTokens.remove(0));
            getTerritory(drawn.tokenLocation()).setDiscovered(false);
            if (hasFaction("Guild") && drawn.discoveryToken().equals("Smuggler")) getFaction("Guild").getChat()
                    .publish("The discovery token at " + drawn.tokenLocation() + " is a(n) " + getTerritory(drawn.tokenLocation()).getDiscoveryToken());
            if (hasFaction("Fremen") && drawn.discoveryToken().equals("Hiereg")) getFaction("Fremen").getChat()
                    .publish("The discovery token at " + drawn.tokenLocation() + " is a(n) " + getTerritory(drawn.tokenLocation()).getDiscoveryToken());
        }
        if (storm == drawn.sector()) getTerritory(drawn.name()).setSpice(0);

        turnSummary.publish(message.toString());
        setUpdated(UpdateType.MAP);
    }

    public void removeForces(String territoryName, Faction targetFaction, int amountValue, boolean special, boolean isToTanks) {
        removeForces(territoryName, targetFaction, (special ? 0 : amountValue), (special ? amountValue : 0), isToTanks);
    }

    public void removeForces(String territoryName, Faction targetFaction, int amountValue, int specialAmount, boolean isToTanks) {
        targetFaction.removeForces(territoryName, amountValue, false, isToTanks);
        if (specialAmount > 0) targetFaction.removeForces(territoryName, specialAmount, true, isToTanks);
        targetFaction.checkForHighThreshold();
        targetFaction.checkForLowThreshold();
        if (hasGameOption(GameOption.HOMEWORLDS) && homeworlds.containsValue(territoryName)) {
            if (territoryName.equals("Ecaz") && getFaction("Ecaz").isHomeworldOccupied()) {
                for (Faction faction1 : factions) {
                    faction1.getLeaders().removeIf(leader1 -> leader1.getName().equals("Duke Vidal"));
                }
                getFaction("Ecaz").getOccupier().getLeaders().add(new Leader("Duke Vidal", 6, null, false));
                turnSummary.publish("Duke Vidal has left to work for " + getFaction("Ecaz").getOccupier().getEmoji() + " (planet Ecaz occupied)");
            }
        }
        setUpdated(UpdateType.MAP);
    }

    public void reviveForces(Faction faction, boolean isPaid, int regularAmount, int starredAmount) {
        int revivalCost;
        if (faction instanceof ChoamFaction) revivalCost = regularAmount + starredAmount;
        else if (faction instanceof BTFaction) revivalCost = regularAmount + starredAmount;
        else if (faction instanceof IxFaction) revivalCost = regularAmount * 2 + starredAmount * 3;
        else revivalCost = (regularAmount + starredAmount) * 2;
        if (faction.getAlly().equals("BT")) revivalCost = Math.ceilDiv(revivalCost, 2);

        Force force = getForceFromTanks(faction.getName());
        int strength = force.getStrength();
        if (strength < regularAmount)
            throw new IllegalArgumentException("There are only " + strength + " " + force.getName() + " forces in the tanks.");
        force.setStrength(strength - regularAmount);
        faction.addReserves(regularAmount);

        force = getForceFromTanks(faction.getName() + "*");
        strength = force.getStrength();
        if (strength < starredAmount)
            throw new IllegalArgumentException("There are only " + strength + " " + force.getName() + " forces in the tanks.");
        force.setStrength(strength - starredAmount);
        faction.addSpecialReserves(starredAmount);

        faction.checkForHighThreshold();

        String costString = " for free";
        if (isPaid) {
            faction.subtractSpice(revivalCost);
            faction.spiceMessage(revivalCost, "Revivals", false);
            costString = "for " + revivalCost + " " + Emojis.SPICE;
            if (hasFaction("BT") && !(faction instanceof BTFaction)) {
                Faction btFaction = getFaction("BT");
                btFaction.addSpice(revivalCost);
                costString += " paid to " + Emojis.BT;
                btFaction.spiceMessage(revivalCost, faction.getEmoji() + " revivals", true);
            }
        }

        String forcesString = "";
        if (regularAmount > 0) forcesString += MessageFormat.format("{0} {1} ", regularAmount, Emojis.getForceEmoji(faction.getName()));
        if (starredAmount > 0) forcesString += MessageFormat.format("{0} {1} ", starredAmount, Emojis.getForceEmoji(faction.getName() + "*"));
        faction.getLedger().publish(forcesString + "returned to reserves.");
        turnSummary.publish(faction.getEmoji() + " revives " + forcesString + costString);
        faction.setUpdated(UpdateType.MAP);
    }

    public void putTerritoryInAnotherTerritory(Territory insertedTerritory, Territory containingTerritory) {
        String insertedName = insertedTerritory.getTerritoryName();
        String containingName = containingTerritory.getTerritoryName().replaceAll("\\(.*\\)", "").strip();
        territories.putIfAbsent(insertedName, insertedTerritory);
        adjacencyList.putIfAbsent(insertedName, new ArrayList<>());
        adjacencyList.get(insertedName).add(containingName);
        adjacencyList.get(containingName).add(insertedName);
    }

    public void removeTerritoryFromAnotherTerritory(Territory insertedTerritory, Territory containingTerritory) {
        String insertedName = insertedTerritory.getTerritoryName();
        String containingName = containingTerritory.getTerritoryName().replaceAll("\\(.*\\)", "").strip();
        try {
            adjacencyList.get(insertedName).remove(containingName);
        } catch (NullPointerException e) {
            // containing was not adjacent to inserted
        }
        try {
            adjacencyList.get(containingName).remove(insertedName);
        } catch (NullPointerException e) {
            // inserted was not adjacent to containing
        }
    }

    public void startBattlePhase() {
        if (hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(this, TechToken.HEIGHLINERS);
        turnSummary.publish("Turn " + turn + " Battle Phase:");
        phaseForWhispers = "Turn " + turn + " Battle Phase\n";

        getFactions().forEach(f -> f.getLeaders().forEach(l -> { l.setBattleTerritoryName(null); l.setPulledBehindShield(false); } ));
        // Get list of aggregate territory names with multiple factions
        battles = new Battles();
        battles.startBattlePhase(this);
        if (battles.isMoritaniCanTakeVidal() && leaderTanks.stream().noneMatch(leader -> leader.getName().equals("Duke Vidal")) && !(hasFaction("Ecaz") && getFaction("Ecaz").isHomeworldOccupied())) {
            for (Faction faction : factions) {
                if (faction.getLeader("Duke Vidal").isEmpty()) continue;
                faction.removeLeader("Duke Vidal");
                if (faction instanceof EcazFaction) {
                    faction.getChat().publish("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!");
                }
                if (faction instanceof HarkonnenFaction) {
                    faction.getChat().publish("Duke Vidal has escaped to fight for the " + Emojis.MORITANI + "!");
                }
            }
            ((MoritaniFaction) getFaction("Moritani")).getDukeVidal();
            getFaction("Moritani").getChat().publish("Duke Vidal has come to fight for you!");
        }

        List<Battle> battleTerritories = battles.getBattles(this);
        if (!battleTerritories.isEmpty()) {
            String battleMessages = battleTerritories.stream()
                    .map((battle) ->
                            MessageFormat.format("{0} in {1}",
                                    battle.getFactionsMessage(this),
                                    battle.getWholeTerritoryName()
                            )
                    ).collect(Collectors.joining("\n"));
            turnSummary.publish("The following battles will take place this turn:\n" + battleMessages);
        } else {
            turnSummary.publish("There are no battles this turn.");
        }
        setUpdated(UpdateType.MAP);
    }

    public void endBattlePhase() throws InvalidGameStateException {
        if (!battles.noBattlesRemaining(this))
            throw new InvalidGameStateException("There are battles remaining to be resolved.");
        getFactions().forEach(f -> f.getLeaders().forEach(l -> { l.setBattleTerritoryName(null); l.setPulledBehindShield(false); } ));
        getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_BACK_OF_SHIELD));
        getLeaderTanks().forEach(l -> l.setBattleTerritoryName(null));
        battles = null;
    }

    public MentatPause getMentatPause() {
        return mentatPause;
    }

    public void startMentatPause() {
        mentatPause = new MentatPause(this);
    }

    public void endMentatPause() throws InvalidGameStateException {
        mentatPause.endPhase();
        mentatPause = null;
        extortionTokenRevealed = false;
    }

    public void updateStrongholdSkills() {
        if (hasStrongholdSkills()) {
            getFactions().forEach(Faction::removeAllStrongholdCards);

            List<Pair<String, Faction>> controllingFactions = new ArrayList<>();
            List<Territory> strongholds = getTerritories().values().stream()
                    .filter(Territory::isStronghold)
                    .toList();

            strongholds.stream().filter(t -> t.countActiveFactions() == 1)
                    .forEach(t -> controllingFactions.add(new ImmutablePair<>(t.getTerritoryName(), t.getActiveFactions(this).get(0))));
            if (hasFaction("Ecaz"))
                strongholds.stream().filter(t -> t.countActiveFactions() == 2)
                        .filter(t -> t.getActiveFactions(this).get(0).getName().equals("Ecaz") && t.getActiveFactions(this).get(1).getAlly().equals("Ecaz")
                                || t.getActiveFactions(this).get(1).getName().equals("Ecaz") && t.getActiveFactions(this).get(0).getAlly().equals("Ecaz"))
                        .forEach(t -> controllingFactions.add(new ImmutablePair<>(t.getTerritoryName(), getFaction("Ecaz"))));

            for (Pair<String, Faction> controllingFaction : controllingFactions) {
                String strongholdName = controllingFaction.getLeft();
                Faction faction = controllingFaction.getRight();
                faction.addStrongholdCard(new StrongholdCard(strongholdName));

                turnSummary.publish(MessageFormat.format("{0} controls {1}{2}{1}",
                        faction.getEmoji(), Emojis.WORM, strongholdName));
            }
        }
    }

    /**
     * Create an alliance between two factions.
     * @param faction1 The first faction.
     * @param faction2 The second faction.
     */
    public void createAlliance(Faction faction1, Faction faction2) {
        if (faction1.getNexusCard() != null)
            discardNexusCard(faction1);
        if (faction2.getNexusCard() != null)
            discardNexusCard(faction2);

        removeAlliance(faction1);
        removeAlliance(faction2);

        faction1.setAlly(faction2.getName());
        faction2.setAlly(faction1.getName());

        turnSummary.publish(faction1.getEmoji() + " and " + faction2.getEmoji() + " have formed an alliance.");
        faction1.getLedger().publish("You are now allies with " + faction2.getEmoji() + "!");
        faction2.getLedger().publish("You are now allies with " + faction1.getEmoji() + "!");

        faction1.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        faction2.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        setUpdated(UpdateType.MAP);
    }

    /**
     * Remove an alliance between two factions.
     * @param faction The faction to remove the alliance from.
     */
    public void removeAlliance(@NotNull Faction faction) {
        if (faction.hasAlly()) {
            Faction allyFaction = getFaction(faction.getAlly());
            faction.removeAlly();
            allyFaction.removeAlly();
            turnSummary.publish(faction.getEmoji() + " and " + allyFaction.getEmoji() + " are no longer allies.");
            faction.getLedger().publish("Your alliance with " + allyFaction.getEmoji() + " has been dissolved!");
            allyFaction.getLedger().publish("Your alliance with " + faction.getEmoji() + " has been dissolved!");

            faction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            allyFaction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            setUpdated(UpdateType.MAP);
        }
    }


}
