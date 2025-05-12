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
    private String modRole;
    private SpiceBlowAndNexus spiceBlowAndNexus;
    private Bidding bidding;
    private Revival revival;
    private Battles battles;
    private MentatPause mentatPause;
    private List<String> bribesThisTurn;
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
    private final TleilaxuTanks tleilaxuTanks;
    private final LinkedList<Leader> leaderTanks;
    private Leader dukeVidal;
    private transient final HashMap<String, List<String>> adjacencyList;
    private final HashMap<String, String> homeworlds;
    private final List<String> hieregTokens;
    private final List<String> smugglerTokens;
    private final LinkedList<TreacheryCard> treacheryDeck;
    private final LinkedList<TreacheryCard> treacheryDiscard;
    private boolean robberyDiscardOutstanding;
    private int hmsRotation = 0;
    private boolean ixHMSActionRequired;
    private HashMap<Integer, List<String>> quotes;
    private Boolean mute;
    private String phaseForWhispers;
    private Set<GameOption> gameOptions;
    private String mod;
    private final HashMap<String, Integer> modCommandExecutions;
    private boolean teamMod = false;
    private String modRoleMention;
    private String gameRoleMention;
    private boolean shieldWallDestroyed;
    private boolean sandtroutInPlay;
    private List<SetupStep> setupSteps;
    private boolean setupStarted;
    private boolean setupFinished;
    private int storm;
    private Integer dial1;
    private Integer dial2;
    private int stormMovement;
    private final ArrayList<Integer> stormDeck;
    private boolean onHold;

    @Exclude
    private Set<UpdateType> updateTypes;
    @Exclude
    private DuneTopic turnSummary;
    @Exclude
    private DuneTopic whispers;
    private boolean whispersTagged = false;
    @Exclude
    private DuneTopic gameActions;
    @Exclude
    private DuneTopic modInfo;
    @Exclude
    private DuneTopic modLedger;
    @Exclude
    private DuneTopic biddingPhase;
    @Exclude
    private DuneTopic bribes;

    public Game() throws IOException {
        super();

        this.turn = 0;
        this.phase = 0;
        this.subPhase = 0;
        this.phaseForTracker = 0;
        this.bidding = null;
        revival = null;
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
        this.tleilaxuTanks = new TleilaxuTanks();
        this.leaderTanks = new LinkedList<>();
        this.dukeVidal = null;
        this.nexusDeck = new LinkedList<>();
        this.nexusDiscard = new LinkedList<>();
        this.homeworlds = new HashMap<>();
        this.hieregTokens = new LinkedList<>();
        this.smugglerTokens = new LinkedList<>();
        this.treacheryDeck = new LinkedList<>();
        this.treacheryDiscard = new LinkedList<>();
        this.robberyDiscardOutstanding = false;
        this.ixHMSActionRequired = false;
        this.shieldWallDestroyed = false;
        this.phaseForWhispers = "";
        this.mod = "";
        this.modCommandExecutions = new HashMap<>();
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

    public Bidding startBidding() {
        bidding = new Bidding(this);
        setUpdated(UpdateType.MAP);
        return bidding;
    }

    public void endBidding() {
        bidding = null;
    }

    public void startRevival() throws InvalidGameStateException {
        revival = new Revival(this);
    }

    public boolean isRecruitsInPlay() {
        if (revival == null)
            return false;
        else
            return revival.isRecruitsInPlay();
    }

    public boolean endRevival(Game game) throws InvalidGameStateException {
        String factionsStillToRevive = String.join(", ", factions.stream().filter(Faction::isPaidRevivalTBD).map(Faction::getName).toList());
        if (!factionsStillToRevive.isEmpty())
            throw new InvalidGameStateException(factionsStillToRevive + " must decide on paid revivals before the game can advance.");
        if (revival.isEcazAmbassadorsToBePlaced())
            throw new InvalidGameStateException("Ecaz must finish placing ambassadors before the game can advance.");
        BTFaction bt = game.getBTFactionOrNull();
        if (bt != null && bt.isBtHTActive())
            throw new InvalidGameStateException("BT must decide on High Threshold free revival placement before the game can advance.");
        if (!revival.isEcazAskedAboutAmbassadors() && revival.ecazAmbassadorPlacement(game))
            return false;
        revival = null;
        return true;
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

        if (gameOption == GameOption.DISCOVERY_CARDS_IN_TOP_HALF)
            this.gameOptions.add(GameOption.DISCOVERY_TOKENS);

        if (gameOption == GameOption.DISCOVERY_TOKENS && hasGameOption(GameOption.REPLACE_SHAI_HULUD_WITH_MAKER))
            modInfo.publish("The game already has REPLACE_SHAI_HULUD_WITH_MAKER. The " + Emojis.SPICE + " deck will have 5 Shai-Huluds and 2 Great Makers.");
        else if (gameOption == GameOption.REPLACE_SHAI_HULUD_WITH_MAKER && hasGameOption(GameOption.DISCOVERY_TOKENS))
            modInfo.publish("The game already has DISCOVERY_TOKENS. The " + Emojis.SPICE + " deck will have 5 Shai-Huluds and 2 Great Makers.");
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

    public void setDotPosition(String factionName, int dotPosition) throws InvalidGameStateException {
        if (setupFinished && mentatPause == null)
            throw new InvalidGameStateException("Faction positions can only be changed during Setup or in Mentat Pause.");
        Faction faction = getFaction(factionName);
        int newPosition = dotPosition - 1;
        String orderBefore = String.join(" ", factions.stream().map(Faction::getEmoji).toList());
        int currentPosition = factions.indexOf(faction);
        factions.set(currentPosition, factions.get(newPosition));
        factions.set(newPosition, faction);
        String orderAfter = String.join(" ", factions.stream().map(Faction::getEmoji).toList());
        modInfo.publish("Moving " + Emojis.getFactionEmoji(factionName) + " to position " + dotPosition + ".\n"
                + "Order before: " + orderBefore + "\n" + "Order after: " + orderAfter);
        setUpdated(UpdateType.MAP);
    }

    public void setUpdated(UpdateType updateType) {
        if (this.updateTypes == null) this.updateTypes = new HashSet<>();
        this.updateTypes.add(updateType);
    }

    public Set<UpdateType> getUpdateTypes() {
        if (this.updateTypes == null) this.updateTypes = new HashSet<>();
        return this.updateTypes;
    }

    public void triggerExtortionToken() {
        if (mentatPause == null)
            mentatPause = new MentatPause();
        mentatPause.triggerExtortionToken();
    }

    public boolean isRobberyDiscardOutstanding() {
        return robberyDiscardOutstanding;
    }

    public void setRobberyDiscardOutstanding(boolean robberyDiscardOutstanding) {
        this.robberyDiscardOutstanding = robberyDiscardOutstanding;
    }

    public void rotateHMS90degrees() {
        hmsRotation = (hmsRotation + 90) % 360;
        setUpdated(UpdateType.MAP);
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

    public void choamCharity() {
        turnSummary.publish("**Turn " + turn + " CHOAM Charity Phase**");
        setPhaseForWhispers("Turn " + turn + " CHOAM Charity Phase\n");
        int multiplier = 1;

        int choamGiven = 0;
        ChoamFaction choam = getCHOAMFactionOrNull();
        if (choam != null) {
            multiplier = choam.getChoamMultiplier(turn);
            if (multiplier == 0) {
                turnSummary.publish("CHOAM Charity is cancelled!");
                return;
            } else if (multiplier == 2) {
                turnSummary.publish("CHOAM charity is doubled! No bribes may be made while the Inflation token is Double side up.");
            }
            int choamSpiceReceived = 2 * factions.size() * multiplier;
            choam.addSpice(choamSpiceReceived, "CHOAM Charity");
            turnSummary.publish(Emojis.CHOAM + " receives " + choamSpiceReceived + " " + Emojis.SPICE + " in dividends from their many investments.");
        }
        for (Faction faction : factions) {
            if (faction instanceof ChoamFaction)
                continue;
            if (faction.isDecliningCharity()) {
                faction.getLedger().publish("Charity was declined.");
                continue;
            }
            int spice = faction instanceof BGFaction ? 0 : faction.getSpice();
            if (spice < 2) {
                int charity = multiplier * (2 - spice);
                choamGiven += charity;
                if (hasGameOption(GameOption.HOMEWORLDS) && !faction.isHighThreshold())
                    charity++;
                turnSummary.publish(faction.getEmoji() + " have received " + charity + " " + Emojis.SPICE + " in CHOAM Charity.");
                if (hasGameOption(GameOption.TECH_TOKENS) && !hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION) && !(faction instanceof BGFaction))
                    TechToken.addSpice(this, TechToken.SPICE_PRODUCTION);
                faction.addSpice(charity, "CHOAM Charity");
            }
        }
        if (choam != null) {
            turnSummary.publish(Emojis.CHOAM + " has paid " + choamGiven + " " + Emojis.SPICE + " to factions in need.");
            choam.subtractSpice(choamGiven, "CHOAM Charity given");
        }
        if (hasGameOption(GameOption.TECH_TOKENS) && !hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION))
            TechToken.collectSpice(this, TechToken.SPICE_PRODUCTION);
    }

    public boolean isInBiddingPhase() {
        return bidding != null;
    }

    public Bidding getBidding() throws InvalidGameStateException {
        if (bidding == null) throw new InvalidGameStateException("Game is not in bidding phase.");
        return bidding;
    }

    public Revival getRevival() throws InvalidGameStateException {
        if (revival == null) throw new InvalidGameStateException("Game is not in revival phase.");
        return revival;
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

    public boolean isTeamMod() {
        return teamMod;
    }

    public void setTeamMod(boolean teamMod) {
        this.teamMod = teamMod;
    }

    public void modExecutedACommand(String modAsMention) {
        Integer numCalls = modCommandExecutions.get(modAsMention);
        if (numCalls == null)
            numCalls = 0;
        modCommandExecutions.put(modAsMention, ++numCalls);
    }

    public HashMap<String, Integer> getModCommandExecutions() {
        return modCommandExecutions;
    }

    public String getMod() {
        return mod;
    }

    public void setMod(String mod) {
        this.mod = mod;
    }

    public String getModRoleMention() {
        return modRoleMention;
    }

    public void setModRoleMention(String modRoleMention) {
        this.modRoleMention = modRoleMention;
    }

    public String getModOrRoleMention() {
        return teamMod ? modRoleMention : mod;
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
     * Get the named faction object
     *
     * @return the Faction object if the faction is in the game
     * @throws IllegalArgumentException if the faction is not in the game
     */
    public Faction getFaction(String name) {
        return findFaction(name).orElseThrow(() -> new IllegalArgumentException("No faction with name " + name));
    }

    /**
     * Get the named faction object
     *
     * @return the Faction object if the faction is in the game, null if not in the game
     */
    public Faction getFactionOrNull(String name) {
        return factions.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * Get the BT faction object
     *
     * @return the BTFaction object if BT is in the game
     * @throws IllegalArgumentException if BT is not in the game
     */
    public BTFaction getBTFaction() {
        return (BTFaction) getFaction("BT");
    }

    /**
     * Get the Harkonnen faction object
     *
     * @return the HarkonnenFaction object if Harkonnen is in the game
     * @throws IllegalArgumentException if Harkonnen is not in the game
     */
    public HarkonnenFaction getHarkonnenFaction() {
        return (HarkonnenFaction) getFaction("Harkonnen");
    }

    /**
     * Get the BT faction object
     *
     * @return the BTFaction object if BT is in the game or null if BT is not in the game
     */
    public BTFaction getBTFactionOrNull() {
        return (BTFaction) getFactionOrNull("BT");
    }

    /**
     * Get the Ix faction object
     *
     * @return the IxFaction object if Ix is in the game
     * @throws IllegalArgumentException if Ix is not in the game
     */
    public IxFaction getIxFaction() {
        return (IxFaction) getFaction("Ix");
    }

    /**
     * Get the CHOAM faction object
     *
     * @return the ChoamFaction object if CHOAM is in the game
     * @throws IllegalArgumentException if CHOAM is not in the game
     */
    public ChoamFaction getCHOAMFaction() {
        return (ChoamFaction) getFaction("CHOAM");
    }

    /**
     * Get the CHOAM faction object
     *
     * @return the ChoamFaction object if CHOAM is in the game or null if CHOAM is not in the game
     */
    public ChoamFaction getCHOAMFactionOrNull() {
        return (ChoamFaction) getFactionOrNull("CHOAM");
    }

    /**
     * Get the Richese faction object
     *
     * @return the RicheseFaction object if Richese is in the game
     * @throws IllegalArgumentException if Richese is not in the game
     */
    public RicheseFaction getRicheseFaction() {
        return (RicheseFaction) getFaction("Richese");
    }

    /**
     * Get the Richese faction object
     *
     * @return the RicheseFaction object if Richese is in the game or null if Richese is not in the game
     */
    public RicheseFaction getRicheseFactionOrNull() {
        return (RicheseFaction) getFactionOrNull("Richese");
    }

    /**
     * Get the Ecaz faction object
     *
     * @return the EcazFaction object if Ecaz is in the game
     * @throws IllegalArgumentException if Ecaz is not in the game
     */
    public EcazFaction getEcazFaction() {
        return (EcazFaction) getFaction("Ecaz");
    }

    /**
     * Get the Ecaz faction object
     *
     * @return the EcazFaction object if Ecaz is in the game or null if Ecaz is not in the game
     */
    public EcazFaction getEcazFactionOrNull() {
        return (EcazFaction) getFactionOrNull("Ecaz");
    }

    /**
     * Get the Moritani faction object
     *
     * @return the MoritaniFaction object if Moritani is in the game
     * @throws IllegalArgumentException if Moritani is not in the game
     */
    public MoritaniFaction getMoritaniFaction() {
        return (MoritaniFaction) getFaction("Moritani");
    }

    /**
     * Get the Moritani faction object
     *
     * @return the MoritaniFaction object if Moritani is in the game or null if Moritani is not in the game
     */
    public MoritaniFaction getMoritaniFactionOrNull() {
        return (MoritaniFaction) getFactionOrNull("Moritani");
    }

    public boolean hasFaction(String name) {
        return findFaction(name).isPresent();
    }

    public boolean hasBTFaction() {
        return hasFaction("BT");
    }

    public boolean hasIxFaction() {
        return hasFaction("Ix");
    }

    public boolean hasCHOAMFaction() {
        return hasFaction("CHOAM");
    }

    public boolean hasRicheseFaction() {
        return hasFaction("Richese");
    }

    public boolean hasEcazFaction() {
        return hasFaction("Ecaz");
    }

    public boolean hasMoritaniFaction() {
        return hasFaction("Moritani");
    }

    public void addFaction(Faction faction) {
        if (faction == null)
            throw new IllegalArgumentException("Cannot add a null faction");
        faction.joinGame(this);
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

    public void setStorm(int storm) {
        this.storm = ((storm - 1) % 18) + 1;
    }

    public boolean setStormDial(Faction faction, int dial) {
        Faction beforeFaction = factions.getLast();
        Faction afterFaction = factions.getFirst();
        if (beforeFaction.getName().equals(faction.getName()))
            dial1 = dial;
        else if (afterFaction.getName().equals(faction.getName()))
            dial2 = dial;
        if (dial1 != null && dial2 != null) {
            turnSummary.publish(beforeFaction.getEmoji() + " dials " + dial1 + ", " + afterFaction.getEmoji() + " dials " + dial2);
            setInitialStorm(dial1, dial2);
            return true;
        }
        return false;
    }

    public TleilaxuTanks getTleilaxuTanks() {
        return tleilaxuTanks;
    }

    public LinkedList<Leader> getLeaderTanks() {
        return leaderTanks;
    }

    public Leader findLeaderInTanks(String name) {
        return leaderTanks.stream()
                .filter(l -> l.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(name + " is not in the tanks."));
    }

    public Leader removeLeaderFromTanks(String name) {
        Leader leader = findLeaderInTanks(name);
        leaderTanks.remove(leader);
        return leader;
    }

    public void createDukeVidal() {
        if (dukeVidal == null)
            dukeVidal = new Leader("Duke Vidal", 6, "Ecaz", null, false);
    }

    public Leader getDukeVidal() {
        // Conditional creation can be removed after games 83, 86, and 88 have created Duke Vidal
        // Really should be created only when Ecaz or Moritani are added to the game
        if (dukeVidal == null)
            createDukeVidal();
        return dukeVidal;
    }

    public void assignDukeVidalToAFaction(String factionName) {
        releaseDukeVidal(false);
        Faction faction = getFaction(factionName);
        faction.addLeader(dukeVidal);
        faction.getChat().publish("Duke Vidal has come to fight for you!");
        turnSummary.publish("Duke Vidal now works for " + faction.getEmoji());
    }

    public void releaseDukeVidal(boolean justRevivedByEcaz) {
        Faction faction = factions.stream().filter(f -> f.getLeader("Duke Vidal").isPresent()).findFirst().orElse(null);
        if (faction != null) {
            faction.removeLeader("Duke Vidal");
            turnSummary.publish("Duke Vidal is no longer in service to " + faction.getEmoji() + (justRevivedByEcaz ? " - what a rotten scoundrel!" : ""));
        }
    }

    public void advanceTurn() {
        turn++;
        phase = 1;
        phaseForTracker = 1;
        subPhase = 1;
        factions.forEach(Faction::clearWhisperCounts);
        factions.forEach(Faction::resetOccupation);
        bribesThisTurn = null;
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

    public void destroyShieldWall() throws InvalidGameStateException {
        Faction factionWithAtomics = factions.stream().filter(f -> f.hasTreacheryCard("Family Atomics")).findFirst().orElse(null);
        if (factionWithAtomics == null)
            throw new InvalidGameStateException("No faction holds Family Atomics.");
        if (!factionWithAtomics.isNearShieldWall())
            throw new InvalidGameStateException(factionWithAtomics.getEmoji() + " is not in position to use Family Atomics.");

        shieldWallDestroyed = true;
        territories.get("Carthag").setRock(false);
        territories.get("Imperial Basin (Center Sector)").setRock(false);
        territories.get("Imperial Basin (East Sector)").setRock(false);
        territories.get("Imperial Basin (West Sector)").setRock(false);
        territories.get("Arrakeen").setRock(false);

        if (hasGameOption(GameOption.FAMILY_ATOMICS_TO_DISCARD)) {
            factionWithAtomics.discard("Family Atomics");
            turnSummary.publish("The Shield Wall has been destroyed!\nFamily Atomics is in the discard pile.");
        } else {
            factionWithAtomics.removeTreacheryCardWithoutDiscard("Family Atomics");
            turnSummary.publish(factionWithAtomics.getEmoji() + " plays Family Atomics.");
            turnSummary.publish("The Shield Wall has been destroyed!\nFamily Atomics has been removed from the game.");
        }

        String message = getTerritory("Shield Wall (North Sector)").shieldWallRemoveTroops(this) +
                getTerritory("Shield Wall (South Sector)").shieldWallRemoveTroops(this);
        turnSummary.publish(message);
        setUpdated(UpdateType.MAP);
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

    public void placeStorm(int sector) {
        storm = sector;
        turnSummary.publish("The storm has been placed over sector " + storm);
        setUpdated(UpdateType.MAP);
        setUpdated(UpdateType.MAP_ALSO_IN_TURN_SUMMARY);
    }

    public boolean hasStrongholdSkills() {
        return hasGameOption(GameOption.STRONGHOLD_SKILLS);
    }

    public boolean drawTreacheryCard(String factionName, boolean publishToLedger, boolean publishToTurnSummary) {
        boolean deckReplenished = false;
        if (treacheryDeck.isEmpty()) {
            deckReplenished = true;
            treacheryDeck.addAll(treacheryDiscard);
            treacheryDiscard.clear();
        }
        Faction faction = getFaction(factionName);
        faction.addTreacheryCard(treacheryDeck.pollLast());
        if (publishToLedger)
            faction.getLedger().publish(faction.getTreacheryHand().getLast().name() + " drawn from deck.");
        if (publishToTurnSummary)
            turnSummary.publish(faction.getEmoji() + " draws a card from the " + Emojis.TREACHERY + " deck.");
        return deckReplenished;
    }

    public void drawTraitorCard(String factionName) {
        Faction faction = getFaction(factionName);
        TraitorCard traitor = getTraitorDeck().pollLast();
        assert traitor != null;

        faction.addTraitorCard(traitor);
        faction.getLedger().publish(traitor.getEmojiNameAndStrengthString() + " drawn from traitor deck.");
        turnSummary.publish(faction.getEmoji() + " draws a card from the traitor deck." );
    }

    public void drawCard(String deckName, String faction) {
        switch (deckName) {
            case "traitor deck" -> getFaction(faction).addTraitorCard(getTraitorDeck().pollLast());
            case "leader skills deck" -> getFaction(faction).getLeaderSkillsHand().add(getLeaderSkillDeck().pollLast());
        }
    }

    public void removeTreacheryCard(String cardName) {
        TreacheryCard cardToRemove = treacheryDeck.stream().filter(c -> c.name().equals(cardName)).findFirst().orElseThrow();
        treacheryDeck.remove(cardToRemove);
        turnSummary.publish(cardName + " was permanently removed from the " + Emojis.TREACHERY + " deck.");
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void transferTreacheryCardFromDiscard(Faction receiver, String cardName) throws InvalidGameStateException {
        TreacheryCard card = treacheryDiscard.stream()
                .filter(c -> c.name().equalsIgnoreCase(cardName))
                .findFirst()
                .orElseThrow(() -> new InvalidGameStateException("Card not found in discard pile."));
        receiver.addTreacheryCard(card);
        treacheryDiscard.remove(card);
        receiver.getLedger().publish("Received " + cardName + " from discard.");
    }

    public void moritaniAllyRetainDiscard(Faction ally, String cardName) throws InvalidGameStateException {
        if (cardName.equals("None")) {
            ally.getChat().reply("You will not retain any discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally.");
            turnSummary.publish(ally.getEmoji() + " does not retain any discarded " + Emojis.TREACHERY + " as " + Emojis.MORITANI + " ally.");
        } else {
            ally.getChat().reply("You retained " + cardName + " as " + Emojis.MORITANI + " ally.");
            transferTreacheryCardFromDiscard(ally, cardName);
            turnSummary.publish(ally.getEmoji() + " retains " + cardName + " as " + Emojis.MORITANI + " ally.");
        }
    }

    public void shuffleTreacheryDeck() {
        Collections.shuffle(getTreacheryDeck());
    }

    public void shuffleTraitorDeck() {
        Collections.shuffle(getTraitorDeck());
    }

    public HashMap<String, List<String>> getAdjacencyList() {
        return adjacencyList;
    }

    protected Deque<String> getTurnOrder() {
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

    public void transferCard(Faction giver, Faction receiver, TreacheryCard card) {
        receiver.addTreacheryCard(giver.removeTreacheryCardWithoutDiscard(card));
        receiver.getLedger().publish("Received " + card.name() + " from " + giver.getEmoji());
        giver.getLedger().publish("Sent " + card.name() + " to " + receiver.getEmoji());
    }

    public void transferCard(String giverName, String receiverName, String cardName) {
        Faction giver = getFaction(giverName);
        Faction receiver = getFaction(receiverName);
        transferCard(giver, receiver, giver.getTreacheryCard(cardName));
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

    public boolean isWhispersTagged() {
        return whispersTagged;
    }

    // This can be eliminated after games 42, 63, 65, 66, and 67 have finished
    public void setWhispersTagged(boolean whispersTagged) {
        this.whispersTagged = whispersTagged;
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

    public DuneTopic getModLedger() {
        return modLedger;
    }

    public void setModLedger(DuneTopic modLedger) {
        this.modLedger = modLedger;
    }

    public DuneTopic getBiddingPhase() {
        return biddingPhase;
    }

    public void setBiddingPhase(DuneTopic biddingPhase) {
        this.biddingPhase = biddingPhase;
    }

    public DuneTopic getBribes() {
        return bribes;
    }

    public void setBribes(DuneTopic bribes) {
        this.bribes = bribes;
    }

    public List<Faction> getFactionsWithTreacheryCard(String cardName) {
        return factions.stream().filter(f -> f.hasTreacheryCard(cardName))
                .collect(Collectors.toList());
    }

    public Faction getFirstFactionWithTreacheryCard(String cardName) {
        Faction faction = null;
        List<Faction> factions = getFactionsWithTreacheryCard(cardName);
        if (!factions.isEmpty())
            faction = factions.getFirst();
        return faction;
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
        dial1 = stormDialOne;
        dial2 = stormDialTwo;
        publishInitialStormMessage();
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
        setUpdated(UpdateType.MAP_ALSO_IN_TURN_SUMMARY);
    }

    public void publishInitialStormMessage() {
        String message = "The storm has been initialized to sector " + storm;
        if (dial1 != null && dial2 != null)
            message += ": " + factions.getLast().getEmoji() + " " + dial1 + " + " + factions.getFirst().getEmoji() + " " + dial2 + ".";
        turnSummary.publish(message);
    }

    public boolean ixCanMoveHMS() {
        Territory hms = territories.get("Hidden Mobile Stronghold");
        if (hms != null) {
            int cyborgsInHMS = hms.getForceStrength("Ix*");
            int suboidsInHMS = hms.getForceStrength("Ix");
            return (cyborgsInHMS + suboidsInHMS > 0);
        }
        return false;
    }

    public void startStormPhase() {
        turnSummary.publish("**Turn " + turn + " Storm Phase**");
        phaseForWhispers = "Turn " + turn + " Storm Phase\n";
        if (hasGameOption(GameOption.BG_COEXIST_WITH_ALLY))
            territories.flipAdvisorsIfAlone(this);

        for (Territory newDiscovery : territories.values().stream().filter(Territory::isJustDiscovered).toList()) {
            for (String aggregateTerritoryName : territories.getDistinctAggregateTerritoryNames()) {
                List<List<Territory>> aggregateTerritoryList = territories.getAggregateTerritoryList(aggregateTerritoryName, storm, false);
                List<Territory> aggTerritoryWithDiscovery = null;
                for (List<Territory> aggTerritory : aggregateTerritoryList) {
                    for (Territory t : aggTerritory) {
                        String d = t.getDiscoveryToken();
                        if (d != null && d.equals(newDiscovery.getTerritoryName())) {
                            aggTerritoryWithDiscovery = aggTerritory;
                            break;
                        }
                    }
                }
                if (aggTerritoryWithDiscovery != null)
                    for (Territory t : aggTerritoryWithDiscovery)
                        for (Faction f : t.getActiveFactions(this)) {
                            turnSummary.publish(f.getEmoji() + " may move into " + newDiscovery.getTerritoryName() + " from " + t.getTerritoryName() + ".");
                            f.getMovement().setMovingFrom(t.getTerritoryName());
                            List<DuneChoice> choices = new ArrayList<>();
                            choices.add(new DuneChoice("move-sector-enter-discovery-token-" + newDiscovery.getTerritoryName(), "Yes"));
                            choices.add(new DuneChoice("danger", "pass-movement-enter-discovery-token", "No"));
                            f.getChat().publish("Would you like to move into " + newDiscovery.getTerritoryName() + " from " + t.getTerritoryName() + "? " + f.getPlayer(), choices);
                        }
            }
            newDiscovery.setJustDiscovered(false);
        }

        Faction factionWithAtomics = getFirstFactionWithTreacheryCard("Family Atomics");
        if (factionWithAtomics != null && factionWithAtomics.isNearShieldWall())
            factionWithAtomics.getChat().publish(factionWithAtomics.getPlayer() + " will you play Family Atomics?");

        Faction factionWithWeatherControl = getFirstFactionWithTreacheryCard("Weather Control");
        if (factionWithWeatherControl != null && turn != 1)
            factionWithWeatherControl.getChat().publish(factionWithWeatherControl.getPlayer() + " will you play Weather Control?");

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

        if (territories.get("Ecological Testing Station") != null && getTerritory("Ecological Testing Station").countActiveFactions() == 1) {
            Faction faction = getTerritory("Ecological Testing Station").getActiveFactions(this).getFirst();
            faction.getChat().publish("What have the ecologists at the testing station discovered about the storm movement? " + faction.getPlayer(),
                    List.of(new DuneChoice("storm-1", "-1"), new DuneChoice("secondary", "storm0", "0"), new DuneChoice("storm1", "+1")));
        }
    }

    public void endStormPhase() throws IOException {
        if (turn == 1) {
            publishInitialStormMessage();
            dial1 = null;
            dial2 = null;
        } else {
            turnSummary.publish("The storm moves " + stormMovement + " sectors this turn.");
            territories.moveStorm(this);
        }
        turnSummary.showMap(this);
        stormMovement = stormDeck == null ? new Random().nextInt(6) + 1 : stormDeck.get(new Random().nextInt(stormDeck.size()));
        if (hasFaction("Fremen"))
            getFaction("Fremen").getChat().publish("The storm will move " + stormMovement + " sectors next turn.");
    }

    public SpiceBlowAndNexus startSpiceBlowPhase() throws InvalidGameStateException, IOException {
        if (spiceBlowAndNexus != null)
            throw new InvalidGameStateException("Spice Blow and Nexus Phase is already in progress.");
        spiceBlowAndNexus = new SpiceBlowAndNexus(this);
        return spiceBlowAndNexus;
    }

    public SpiceBlowAndNexus getSpiceBlowAndNexus() {
        return spiceBlowAndNexus;
    }

    /**
     * Draws spice blows, announces Nexus, gives Fremen buttons for riding worms
     *
     * @return true if the Spice Blow and NexusPhase has ended
     */
    public boolean spiceBlowPhaseNextStep() throws InvalidGameStateException, IOException {
        if (spiceBlowAndNexus == null)
            throw new InvalidGameStateException("Spice Blow and Nexus Phase has not started.");
        int wormsToPlace = 0;
        boolean wormToRide = false;
        if (hasFaction("Fremen")) {
            FremenFaction fremen = (FremenFaction) getFaction("Fremen");
            wormsToPlace = fremen.getWormsToPlace();
            wormToRide = fremen.isWormRideActive();
        }
        if (wormsToPlace > 0)
            throw new InvalidGameStateException("Fremen must place " + wormsToPlace + " worms before the game can advance.");
        if (wormToRide)
            throw new InvalidGameStateException("Fremen must decide whether to ride the worm before the game can advance.");
        if (spiceBlowAndNexus.isHarvesterActive())
            throw new InvalidGameStateException(getFirstFactionWithTreacheryCard("Harvester").getName() + " must decide if they will play Harvester before the game can advance.");
        if (spiceBlowAndNexus.isThumperActive())
            throw new InvalidGameStateException(getFirstFactionWithTreacheryCard("Thumper").getName() + " must decide if they will play Thumper before the game can advance.");

        if (spiceBlowAndNexus.nextStep(this))
            spiceBlowAndNexus = null;
        return spiceBlowAndNexus == null;
    }

    public Pair<SpiceCard, Integer> drawSpiceBlow(String spiceBlowDeckName) {
        return drawSpiceBlow(spiceBlowDeckName, false);
    }

    public Pair<SpiceCard, Integer> drawSpiceBlow(String spiceBlowDeckName, boolean afterThumper) {
        LinkedList<SpiceCard> discard = spiceBlowDeckName.equalsIgnoreCase("A") ?
                spiceDiscardA : spiceDiscardB;
        SpiceCard lastCard = null;
        if (!discard.isEmpty())
            lastCard = discard.getLast();
        LinkedList<SpiceCard> wormsToReshuffle = new LinkedList<>();

        StringBuilder message = new StringBuilder();

        SpiceCard drawn;

        message.append("**Spice Deck ").append(spiceBlowDeckName).append("**\n");

        boolean shaiHuludSpotted = false;
        boolean nexus = false;
        boolean greatMaker = false;
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

            String spotted = "spotted";
            if (afterThumper) {
                drawn = new SpiceCard("Shai-Hulud", 0, 0, null, null);
                spotted = "summoned";
            } else
                drawn = spiceDeck.pop();
            boolean saveWormForReshuffle = false;
            boolean cardIsGreatMaker = drawn.name().equalsIgnoreCase("Great Maker");
            if (drawn.name().equalsIgnoreCase("Shai-Hulud") || cardIsGreatMaker) {
                if (turn <= 1) {
                    saveWormForReshuffle = true;
                    message.append(drawn.name())
                            .append(" will be reshuffled back into deck.\n");
                } else if (!shaiHuludSpotted) {
                    shaiHuludSpotted = true;

                    if (sandtroutInPlay) {
                        spiceMultiplier = 2;
                        sandtroutInPlay = false;
                        message.append(Emojis.WORM).append(" ").append(drawn.name()).append(" has been ").append(spotted).append("! The next Shai-Hulud will cause a Nexus!\n");
                    } else {
                        message.append(getTerritory(Objects.requireNonNull(lastCard).name()).shaiHuludAppears(this, drawn.name(), true));
                        nexus = true;
                        if (cardIsGreatMaker)
                            greatMaker = true;
                    }
                } else {
                    spiceMultiplier = 1;
                    FremenFaction fremen = null;
                    if (hasFaction("Fremen"))
                        fremen = (FremenFaction) getFaction("Fremen");
                    message.append(Emojis.WORM).append(" ").append(drawn.name()).append(" has been ").append(spotted).append("!");
                    nexus = true;
                    if (cardIsGreatMaker)
                        greatMaker = true;
                    if (fremen != null) {
                        message.append(" " + Emojis.FREMEN + " may place it in any sand territory.");
                        fremen.presentWormPlacementChoices(Objects.requireNonNull(lastCard).name(), drawn.name());
                        fremen.addWormToPlace();
                    }
                    message.append("\n");
                }
            } else if (drawn.name().equalsIgnoreCase("Sandtrout")) {
                shaiHuludSpotted = true;
                message.append("Sandtrout has been spotted, and all alliances have ended!\n");
                factions.forEach(this::removeAlliance);
                sandtroutInPlay = true;
            } else {
                String spiceMessage = drawn.spice() * spiceMultiplier + " " + Emojis.SPICE + " has been spotted in " + drawn.name();
                message.append(spiceMessage);
                message.append(drawn.sector() == storm ? " - blown away by the storm" : "").append("!\n");
                if (greatMaker)
                    gameActions.publish(gameRoleMention + " Please vote whether you want a Nexus. A majority is required to have one.");
                else if (nexus)
                    gameActions.publish(gameRoleMention + " We have a Nexus! Create your alliances, reaffirm, backstab, or go solo here.");
            }
            if (saveWormForReshuffle) {
                wormsToReshuffle.add(drawn);
            } else if (!afterThumper && !drawn.name().equalsIgnoreCase("Sandtrout")) {
                discard.add(drawn);
            }
            afterThumper = false;
        } while (drawn.name().equalsIgnoreCase("Shai-Hulud") ||
                drawn.name().equalsIgnoreCase("Great Maker") ||
                drawn.name().equalsIgnoreCase("Sandtrout"));

        while (!wormsToReshuffle.isEmpty()) {
            spiceDeck.add(wormsToReshuffle.pop());
            if (wormsToReshuffle.isEmpty()) {
                Collections.shuffle(spiceDeck);
            }
        }

        if (drawn.discoveryToken() == null) territories.get(drawn.name()).addSpice(this, drawn.spice() * spiceMultiplier);
        else {
            getTerritory(drawn.name()).setSpice(6 * spiceMultiplier);
            if (getTerritory(drawn.name()).countFactions() > 0) {
                message.append("all forces in the territory were killed in the spice blow!\n");
                List<Force> forcesToRemove = new ArrayList<>(getTerritory(drawn.name()).getForces());
                for (Force force : forcesToRemove) {
                    // Is this removing Advisors and No-Fields?
                    // Test for those and that KH counter does not increase
                    removeForces(drawn.name(), getFaction(force.getFactionName()), force.getStrength(), force.getName().contains("*"), true);
                }
            }
            message.append(drawn.discoveryToken()).append(" has been placed in ").append(drawn.tokenLocation()).append("\n");
            if (drawn.discoveryToken().equals("Hiereg"))
                getTerritory(drawn.tokenLocation()).setDiscoveryToken(hieregTokens.removeFirst());
            else getTerritory(drawn.tokenLocation()).setDiscoveryToken(smugglerTokens.removeFirst());
            getTerritory(drawn.tokenLocation()).setDiscovered(false);
            if (hasFaction("Guild") && drawn.discoveryToken().equals("Smuggler")) getFaction("Guild").getChat()
                    .publish("The discovery token at " + drawn.tokenLocation() + " is **" + getTerritory(drawn.tokenLocation()).getDiscoveryToken() + "**");
            if (hasFaction("Fremen") && drawn.discoveryToken().equals("Hiereg")) getFaction("Fremen").getChat()
                    .publish("The discovery token at " + drawn.tokenLocation() + " is **" + getTerritory(drawn.tokenLocation()).getDiscoveryToken() + "**");
        }
        if (storm == drawn.sector()) getTerritory(drawn.name()).setSpice(0);

        turnSummary.publish(message.toString());
        setUpdated(UpdateType.MAP);
        return ImmutablePair.of(drawn, spiceMultiplier);
    }

    public void placeShaiHulud(String territoryName, String wormName, boolean firstWorm) {
        String message = wormName + " has been placed in " + territoryName + "\n";
        message += getTerritory(territoryName).shaiHuludAppears(this, wormName, firstWorm);
        turnSummary.publish(message);
    }

    public void startShipmentPhase() {
        if (hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(this, TechToken.AXLOTL_TANKS);

        turnSummary.publish("**Turn " + turn + " Shipment and Movement Phase**");
        setPhaseForWhispers("Turn " + turn + " Shipment and Movement Phase\n");
        for (Faction faction : factions) {
            faction.getShipment().clear();
            faction.getMovement().clear();
            faction.getShipment().setShipped(false);
            faction.getMovement().setMoved(false);
        }
        turnOrder.clear();
        turnOrder.addAll(getFactionsInStormOrder().stream().map(Faction::getName).toList());
        boolean hasGuild = hasFaction("Guild");
        if (hasGuild) {
            turnOrder.remove("Guild");
            turnOrder.addFirst("AskGuild");
        }
        List<Faction> factions = getFactionsWithTreacheryCard("Juice of Sapho");
        Faction saphoFaction = null;
        if (!factions.isEmpty()) {
            saphoFaction = factions.getFirst();
            saphoFaction.getShipment().setMayPlaySapho(true);
        }
        if (saphoFaction != null &&
                (!turnOrder.getFirst().equals(factions.getFirst().getName()) || hasGuild)) {
            String message = "Will you play Juice of Sapho to ship and move first? " + saphoFaction.getPlayer();
            if (!turnOrder.getLast().equals(saphoFaction.getName()))
                message += "\nIf not, you will have the option to play it to go last on your turn.";
            else if (hasGuild)
                message += "\nIf not, you will have the option to play it to go last if " + Emojis.GUILD + " defers to you.";
            List<DuneChoice> choices = List.of(
                    new DuneChoice("juice-of-sapho-first", "Yes, go first"),
                    new DuneChoice("secondary", "juice-of-sapho-don't-play", "No"));
            saphoFaction.getChat().publish(message, choices);
        } else if (hasGuild) {
            promptGuildShippingDecision();
        } else
            promptNextFactionToShip();

        if (hasFaction("Atreides"))
            ((AtreidesFaction) getFaction("Atreides")).giveSpiceDeckPrescience();
        if (hasFaction("BG")) {
            Faction bgFaction = getFaction("BG");
            String bgPlayer = bgFaction.getPlayer();
            territories.flipAdvisorsIfAlone(this);
            for (Territory territory : territories.values()) {
                if (territory.getTerritoryName().equals("Polar Sink")) continue;
                if (territory.getForceStrength("Advisor") > 0) {
                    String bgAllyName = bgFaction.getAlly();
                    if (!bgAllyName.isEmpty()) {
                        Faction bgAlly = getFaction(bgAllyName);
                        if (territory.getTotalForceCount(bgAlly) > 0 && !bgAllyName.equals("Ecaz"))
                            continue;
                    }
                    if (territory.getSector() == storm) {
                        bgFaction.getChat().publish(territory.getTerritoryName() + " is under the storm. Ask the mod to flip for you if the game allows it. " + bgPlayer);
                        continue;
                    }
                    turnSummary.publish(bgFaction.getEmoji() + " to decide whether to flip their advisors in " + territory.getTerritoryName());
                    List<DuneChoice> choices = List.of(
                            new DuneChoice("bg-flip-" + territory.getTerritoryName(), "Flip"),
                            new DuneChoice("secondary", "bg-dont-flip-" + territory.getTerritoryName(), "Don't flip")
                    );
                    bgFaction.getChat().publish("Will you flip to fighters in " + territory.getTerritoryName() + "? " + bgPlayer, choices);
                }
            }
        }
        setUpdated(UpdateType.MAP_ALSO_IN_TURN_SUMMARY);
    }

    public int numFactionsLeftToMove() {
        int factionsLeftToGo = turnOrder.size();
        if (hasFaction("Guild") && !getFaction("Guild").getShipment().hasShipped() && !turnOrder.contains("Guild"))
            factionsLeftToGo++;
        return factionsLeftToGo;
    }

    public void promptGuildShippingDecision() {
        if (turnOrder.size() == 1) {
            guildTakeTurn();
            return;
        }

        turnOrder.pollFirst();
        String nextToShip = turnOrder.peekFirst();
        turnOrder.addFirst("AskGuild");
        DuneChoice takeTurn = new DuneChoice("guild-take-turn", "Take turn next.");
        DuneChoice defer = new DuneChoice("guild-defer", "Defer to " + nextToShip + ".");
        DuneChoice select = new DuneChoice("guild-select", "Defer to another faction.");
        DuneChoice last = new DuneChoice("guild-wait-last", "Take turn last.");
        if (turnOrder.getLast().equals("juice-of-sapho-last")) {
            last.setDisabled(true);
            defer.setDisabled(turnOrder.size() == 3);
            select.setDisabled(turnOrder.size() == 3);
        } else
            select.setDisabled(turnOrder.size() == 2);
        Faction guild = getFaction("Guild");
        guild.getChat().publish("Use buttons to take your turn out of order. " + guild.getPlayer(), List.of(takeTurn, defer, select, last));
    }

    public void promptGuildToSelectFactionToDeferTo() {
        List<String> factionsToShip = new ArrayList<>(turnOrder);
        factionsToShip.removeFirst();
        boolean juiceOfSaphoLast = false;
        if (factionsToShip.getLast().equals("juice-of-sapho-last")) {
            factionsToShip.removeLast();
            juiceOfSaphoLast = true;
        }
        List<DuneChoice> choices = new ArrayList<>(factionsToShip.stream().map(f -> new DuneChoice("primary", "guild-defer-to-" + f, null, Emojis.getFactionEmoji(f), false)).toList());
        if (juiceOfSaphoLast)
            choices.getLast().setDisabled(true);
        choices.add(new DuneChoice("secondary", "guild-take-turn", "Take turn next."));
        Faction guild = getFaction("Guild");
        guild.getChat().reply("Which faction would you like to defer to? " + guild.getPlayer(), choices);
    }

    public void guildDefer() throws InvalidGameStateException {
        turnOrder.pollFirst();
        String factionToDeferTo = turnOrder.pollFirst();
        if (factionToDeferTo == null)
            throw new InvalidGameStateException("There is no faction to defer to.");
        turnOrder.addFirst("AskGuild");
        turnOrder.addFirst(factionToDeferTo);
        turnSummary.publish(Emojis.GUILD + " does not ship at this time.");
        promptNextFactionToShip();
        Faction guild = getFaction("Guild");
        guild.getChat().reply("You will defer to " + Emojis.getFactionEmoji(factionToDeferTo));
    }

    public void guildDeferUntilAfter(String factionToDeferTo) throws InvalidGameStateException {
        turnOrder.pollFirst();
        Deque<String> earlyFactions = new LinkedList<>();
        boolean notFound = true;
        while (notFound && !turnOrder.isEmpty()) {
            String nextFaction = turnOrder.pollFirst();
            earlyFactions.addLast(nextFaction);
            notFound = !nextFaction.equals(factionToDeferTo);
        }
        if (notFound)
            throw new InvalidGameStateException("Faction to defer to not found.");
        turnOrder.addFirst("AskGuild");
        while (!earlyFactions.isEmpty())
            turnOrder.addFirst(earlyFactions.pollLast());
        turnSummary.publish(Emojis.GUILD + " does not ship at this time.");
        promptNextFactionToShip();
        Faction guild = getFaction("Guild");
        guild.getChat().reply("You will defer to " + Emojis.getFactionEmoji(factionToDeferTo));
    }

    public void guildWaitLast() {
        turnOrder.addLast("Guild");
        turnSummary.publish(Emojis.GUILD + " does not ship at this time.");
        turnOrder.pollFirst();
        promptNextFactionToShip();
        Faction guild = getFaction("Guild");
        guild.getChat().reply("You will take your turn last.");
    }

    public void guildTakeTurn() {
        turnOrder.pollFirst();
        turnOrder.addFirst("Guild");
        promptNextFactionToShip();
        getTurnSummary().publish(Emojis.GUILD + " will take their turn next.");
        Faction guild = getFaction("Guild");
        guild.getChat().reply("You will take your turn now.");
    }

    public void juiceOfSaphoDontPlay(Faction faction) throws InvalidGameStateException {
        if (faction.getTreacheryHand().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho")))
            throw new InvalidGameStateException("You do not have Juice of Sapho.");
        faction.getShipment().setMayPlaySapho(true);
        if (!turnOrder.isEmpty() && turnOrder.peekFirst().equals("AskGuild"))
            promptGuildShippingDecision();
        else
            promptNextFactionToShip();
    }
    
    public void playJuiceOfSapho(Faction faction, boolean last) throws InvalidGameStateException {
        if (faction.getTreacheryHand().stream().noneMatch(treacheryCard -> treacheryCard.name().equals("Juice of Sapho")))
            throw new InvalidGameStateException("You do not have Juice of Sapho.");
        faction.getShipment().setMayPlaySapho(false);
        turnOrder.remove(faction.getName());
        if (last) {
            turnOrder.addLast(faction.getName());
            turnOrder.addLast("juice-of-sapho-last");
        } else {
            turnOrder.addFirst(faction.getName());
        }
        faction.discard("Juice of Sapho", "to ship and move " + (last ? "last" : "first") + " this turn");
        if (last && !turnOrder.isEmpty() && turnOrder.peekFirst().equals("AskGuild"))
            promptGuildShippingDecision();
        else
            promptNextFactionToShip();
    }

    public boolean isGuildNeedsToShip() {
        return hasFaction("Guild") && !getFaction("Guild").getShipment().hasShipped() && !turnOrder.contains("Guild");
    }

    public void promptNextFactionToShip() {
        String factionName = turnOrder.peekFirst();
        Faction faction = getFaction(factionName);
        if (faction.getReservesStrength() == 0 && faction.getSpecialReservesStrength() == 0 && !(faction instanceof RicheseFaction) && !faction.getAlly().equals("Richese") && !(faction instanceof GuildFaction) && !faction.getAlly().equals("Guild")) {
            faction.getChat().publish("You have no troops in reserves to ship.", List.of(new DuneChoice("danger", "pass-shipment", "Pass shipment")));
            return;
        }
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("shipment", "Begin a ship action"));
        choices.add(new DuneChoice("danger", "pass-shipment", "Pass shipment"));
        boolean lastFaction = turnOrder.size() == 1 && !isGuildNeedsToShip();
        if (faction.getShipment().isMayPlaySapho()) {
            DuneChoice choice = new DuneChoice("secondary", "juice-of-sapho-last", "Play Juice of Sapho to go last");
            choice.setDisabled(lastFaction);
            choices.add(choice);
        }
        faction.getChat().publish("Use buttons to perform Shipment and Movement actions on your turn." + " " + faction.getPlayer(), choices);
    }

    public void completeCurrentFactionMovement() {
        turnOrder.pollFirst();
        if (turnOrder.size() == 1 && turnOrder.getFirst().equals("juice-of-sapho-last"))
            turnOrder.removeFirst();

        if (!turnOrder.isEmpty() && turnOrder.peekFirst().equals("AskGuild"))
            promptGuildShippingDecision();
        else if (!turnOrder.isEmpty())
            promptNextFactionToShip();

        if (!turnOrder.isEmpty() && Objects.requireNonNull(turnOrder.peekLast()).equals("Guild") && turnOrder.size() > 1)
            turnSummary.publish(Emojis.GUILD + " does not ship at this time");
    }

    public boolean allFactionsHaveMoved() {
        if (hasFaction("BG") && ((BGFaction) getFaction("BG")).hasIntrudedTerritoriesDecisions())
            return false;
        return turnOrder.isEmpty();
    }

    public void moveForces(Faction faction, Territory from, Territory to, String movingTo, String secondMovingFrom, int force, int specialForce, int secondForce, int secondSpecialForce, boolean noFieldWasMoved) {
        if (force != 0 || specialForce != 0)
            moveForces(faction, from, to, force, specialForce, false);
        if (secondForce != 0 || secondSpecialForce != 0) {
            turnSummary.publish(faction.getEmoji() + " use Planetologist to move another group to " + movingTo);
            moveForces(faction, getTerritory(secondMovingFrom), to, secondForce, secondSpecialForce, false);
        }
        checkForTriggers(to, faction, force + specialForce + secondForce + secondSpecialForce + (noFieldWasMoved ? 1 : 0));
    }

    public void moveForces(Faction targetFaction, Territory from, Territory to, int amountValue, int starredAmountValue, boolean canTrigger) {
        if (to.factionMayNotEnter(this, targetFaction, false, false))
            throw new IllegalArgumentException("You cannot move into this territory.");

        turnSummary.publish(targetFaction.getEmoji() + ": " + targetFaction.forcesString(amountValue, starredAmountValue) + " moved from " + from.getTerritoryName() + " to " + to.getTerritoryName() + ".");

        if (amountValue > 0) {
            String forceName = targetFaction.getName();
            String targetForceName = targetFaction.getName();
            if (targetFaction instanceof BGFaction && from.hasForce("Advisor")) {
                forceName = "Advisor";
                if (to.hasForce("Advisor")) targetForceName = "Advisor";
            }
            from.removeForces(this, forceName, amountValue);
            to.addForces(targetForceName, amountValue);
        }
        if (starredAmountValue > 0) {
            from.removeForces(this, targetFaction.getName() + "*", starredAmountValue);
            to.addForces(targetFaction.getName() + "*", starredAmountValue);
        }
        targetFaction.checkForHighThreshold();
        targetFaction.checkForLowThreshold();

        if (to.hasActiveFaction("BG") && !(targetFaction instanceof BGFaction)) {
            ((BGFaction) getFaction("BG")).bgFlipMessageAndButtons(this, to.getTerritoryName());
        }
        if (canTrigger)
            checkForTriggers(to, targetFaction, amountValue + starredAmountValue);
        setUpdated(UpdateType.MAP);
    }

    public void checkForTriggers(Territory territory, Faction faction, int numForces) {
        checkForAmbassadorTrigger(territory, faction);
        checkForTerrorTrigger(territory, faction, numForces);
    }

    public void checkForAmbassadorTrigger(Territory territory, Faction faction) {
        if (hasEcazFaction())
            getEcazFaction().checkForAmbassadorTrigger(territory, faction);
    }

    public void checkForTerrorTrigger(Territory territory, Faction faction, int numForces) {
        if (hasMoritaniFaction())
            getMoritaniFaction().checkForTerrorTrigger(territory, faction, numForces);
    }

    public void endShipmentMovement() {
        List<String> messages = new ArrayList<>();
        messages.add("Shipment and Movement has been forced to end. " + getModOrRoleMention());
        if (!turnOrder.isEmpty()) {
            messages.add(Emojis.getFactionEmoji(turnOrder.peekFirst()) + " was queued to ship/move.");
            turnOrder.clear();
        }
        if (hasFaction("BG")) {
            BGFaction bg = (BGFaction) getFaction("BG");
            if (bg.hasIntrudedTerritoriesDecisions()) {
                messages.add(Emojis.BG + " had flip decisions in " + bg.getIntrudedTerritoriesString());
                bg.clearIntrudedTerritories();
            }
        }
        if (messages.size() == 1)
            messages.add("There were no Shipment and Movement actions remaining.");
        modInfo.publish(String.join("\n", messages));
    }

    public void removeForces(String territoryName, Faction targetFaction, int amountValue, boolean special, boolean isToTanks) {
        removeForces(territoryName, targetFaction, (special ? 0 : amountValue), (special ? amountValue : 0), isToTanks);
    }

    public void removeForces(String territoryName, Faction targetFaction, int amountValue, int specialAmount, boolean isToTanks) {
        targetFaction.removeForces(territoryName, amountValue, false, isToTanks);
        if (specialAmount > 0)
            targetFaction.removeForces(territoryName, specialAmount, true, isToTanks);
        targetFaction.checkForHighThreshold();
        targetFaction.checkForLowThreshold();
        setUpdated(UpdateType.MAP);
    }

    public void removeForcesAndReportToTurnSummary(String territoryName, Faction targetFaction, int amountValue, int specialAmount, boolean isToTanks, boolean killedInBattle) {
        removeForces(territoryName, targetFaction, amountValue, specialAmount, isToTanks);
        turnSummary.publish(targetFaction.forcesString(amountValue, specialAmount) + " in " + territoryName + " were sent to " + (isToTanks ? "the tanks." : "reserves."));
        if (killedInBattle && targetFaction instanceof AtreidesFaction atreides)
            atreides.addForceLost(amountValue);
        targetFaction.checkForHighThreshold();
    }

    public void removeAdvisorsAndReportToTurnSummary(String territoryName, Faction targetFaction, int amountValue, boolean isToTanks) {
        removeForces(territoryName, targetFaction, amountValue, 0, isToTanks);
        turnSummary.publish(amountValue + " " + Emojis.BG_ADVISOR + " in " + territoryName + " were sent to " + (isToTanks ? "the tanks." : "reserves."));
        targetFaction.checkForHighThreshold();
    }

    public void killLeader(Faction targetFaction, String leaderName, boolean faceDown) {
        Leader leader = leaderTanks.stream().filter(l -> l.getName().equals(leaderName)).findFirst().orElse(null);
        if (leader != null) {
            leader.setFaceDown(faceDown);
            turnSummary. publish(leaderName + " has been placed face " + (faceDown ? "down" : "up") + " in the tanks.");
        } else {
            leader = targetFaction.removeLeader(leaderName);
            if (leader.getSkillCard() != null) {
                leaderSkillDeck.add(leader.getSkillCard());
                leader.removeSkillCard();
            }
            leader.setFaceDown(faceDown);
            leaderTanks.add(leader);
            BTFaction bt = getBTFactionOrNull();
            if (bt != null && faceDown) {
                bt.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
                bt.getChat().publish(leader.getEmoiNameAndValueString() + " is face down in the tanks.");
            }
            String message = leaderName + " was sent to the tanks" + (faceDown ? " face down." : ".");
            targetFaction.getLedger().publish(message);
            turnSummary.publish(targetFaction.getEmoji() + " " + message);
        }
        setUpdated(UpdateType.MAP);
    }

    public void killLeader(Faction targetFaction, String leaderName) {
        killLeader(targetFaction, leaderName, false);
    }

    public void harkonnenKeepLeader(String factionName, String leaderName) {
        Faction faction = getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();

        Faction harkonnen = getHarkonnenFaction();
        harkonnen.addLeader(leader);
        faction.removeLeader(leader);

        if (leader.getSkillCard() != null)
            turnSummary.publish(harkonnen.getEmoji() + " has captured the " + faction.getEmoji() + " skilled leader, " + leaderName + " the " + leader.getSkillCard().name()+ ".");
        else
            turnSummary.publish("asdf");

        faction.getChat().publish(leader.getName() + " has been captured by the treacherous " + Emojis.HARKONNEN + "!");
        faction.getLedger().publish(leader.getName() + " has been captured by the treacherous " + Emojis.HARKONNEN + "!");
        harkonnen.getLedger().publish("You have captured " + leader.getName() + ".");
    }

    public void harkonnenKillLeader(String factionName, String leaderName) {
        Faction faction = getFaction(factionName);
        Leader leader = faction.getLeader(leaderName).orElseThrow();
        faction.removeLeader(leader);

        Faction harkonnen = getHarkonnenFaction();
        harkonnen.addSpice(2, "killing " + leader.getName());

        if (leader.getSkillCard() != null) {
            leaderSkillDeck.add(leader.getSkillCard());
            turnSummary.publish(harkonnen.getEmoji() + " has killed the " + faction.getEmoji() + " skilled leader, " + leaderName + ", for 2 " + Emojis.SPICE);
        } else
            turnSummary.publish(harkonnen.getEmoji() + " has killed the " + faction.getEmoji() + " leader for 2 " + Emojis.SPICE);

        Leader killedLeader = new Leader(leader.getName(), leader.getValue(), leader.getOriginalFactionName(), null, true);
        leaderTanks.add(killedLeader);
        BTFaction bt = getBTFactionOrNull();
        if (bt != null) {
            bt.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            bt.getChat().publish(leader.getEmoiNameAndValueString() + " is face down in the tanks.");
        }

        faction.getChat().publish(killedLeader.getName() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        faction.getLedger().publish(killedLeader.getName() + " has been killed by the treacherous " + Emojis.HARKONNEN + "!");
        setUpdated(UpdateType.MAP);
    }

    public void assignTechToken(String tt, Faction recipient) throws InvalidGameStateException {
        Faction possessor = factions.stream().filter(f -> f.hasTechToken(tt)).findAny().orElseThrow();
        possessor.removeTechToken(tt);
        possessor.getLedger().publish(tt + " was sent to " + recipient.getEmoji());
        recipient.addTechToken(tt);
        recipient.getLedger().publish(tt + " transferred to you.");
        turnSummary.publish(Emojis.getTechTokenEmoji(tt) + " has been transferred to " + recipient.getEmoji());
        setUpdated(UpdateType.MAP);
    }

    public void reviveForces(Faction faction, boolean isPaid, int regularAmount, int starredAmount) {
        reviveForces(faction, isPaid, regularAmount, starredAmount, false);
    }

    public void reviveForces(Faction faction, boolean isPaid, int regularAmount, int starredAmount, boolean isEmperorAllyPower) {
        tleilaxuTanks.removeForces(faction.getName(), regularAmount);
        faction.addReserves(regularAmount);
        tleilaxuTanks.removeForces(faction.getName() + "*", starredAmount);
        faction.addSpecialReserves(starredAmount);
        faction.checkForHighThreshold();

        String costString = " for free.";
        if (isEmperorAllyPower)
            costString = " as " + Emojis.EMPEROR + " ally.";
        if (isPaid) {
            int revivalCost = faction.revivalCost(regularAmount, starredAmount);
            faction.subtractSpice(revivalCost, "revivals");
            costString = " for " + revivalCost + " " + Emojis.SPICE;
            BTFaction bt = getBTFactionOrNull();
            if (bt != null && !(faction instanceof BTFaction)) {
                costString += " paid to " + Emojis.BT;
                bt.addSpice(revivalCost, faction.getEmoji() + " revivals");
            }
        }

        String forcesString = faction.forcesString(regularAmount, starredAmount);
        faction.getLedger().publish(forcesString + "returned to reserves.");
        turnSummary.publish(faction.getEmoji() + " revives " + forcesString + costString);
        faction.setUpdated(UpdateType.MAP);

        if (faction instanceof IxFaction ix && hasGameOption(GameOption.HOMEWORLDS) && ix.isHighThreshold() && isPaid && !isEmperorAllyPower) {
            int suboidsInTanks = tleilaxuTanks.getForceStrength("Ix");
            int bonusSuboidRevivals = Math.min(suboidsInTanks, 2 * starredAmount);
            if (bonusSuboidRevivals > 0)
                reviveForces(faction, false, bonusSuboidRevivals, 0, false);
        }
    }

    public int shipmentCost(Faction targetFaction, int amountToShip, Territory targetTerritory, boolean karama, boolean crossShip) {
        int baseCost = amountToShip * targetTerritory.costToShipInto();
        if (targetFaction instanceof FremenFaction && !crossShip && !(targetTerritory instanceof HomeworldTerritory))
            return 0;
        else if (targetFaction instanceof GuildFaction || (targetFaction.hasAlly() && targetFaction.getAlly().equals("Guild")) || karama)
            return Math.ceilDiv(baseCost, 2);
        else
            return baseCost;
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

    public void addNewBribe(String bribeFactionsAndReason) {
        if (bribesThisTurn == null)
            bribesThisTurn = new ArrayList<>();
        bribesThisTurn.add(bribeFactionsAndReason);
        bribes.publish(bribeFactionsAndReason);
    }

    public Battles startBattlePhase() {
        getFactions().forEach(f -> f.resetAllySpiceSupportAfterShipping(this));
        if (hasGameOption(GameOption.TECH_TOKENS)) TechToken.collectSpice(this, TechToken.HEIGHLINERS);

        getFactions().forEach(f -> f.getLeaders().forEach(l -> { l.setBattleTerritoryName(null); l.setPulledBehindShield(false); } ));
        if (dukeVidal != null)
            dukeVidal.setBattleTerritoryName(null);
        // Get list of aggregate territory names with multiple factions
        battles = new Battles();
        battles.startBattlePhase(this);
        if (battles.isMoritaniCanTakeVidal() && leaderTanks.stream().noneMatch(leader -> leader.getName().equals("Duke Vidal")) && !(hasEcazFaction() && getEcazFaction().isHomeworldOccupied())) {
            factions.forEach(Faction::loseDukeVidalToMoritani);
            getMoritaniFaction().getDukeVidal();
            getMoritaniFaction().getChat().publish("Duke Vidal has come to fight for you!");
            turnSummary.publish("Duke Vidal now works for " + Emojis.MORITANI);
        }

        turnSummary.publish("**Turn " + turn + " Battle Phase**");
        phaseForWhispers = "Turn " + turn + " Battle Phase\n";
        battles.publishListOfBattles(this);
        setUpdated(UpdateType.MAP);
        setUpdated(UpdateType.MAP_ALSO_IN_TURN_SUMMARY);
        return battles;
    }

    public void endBattlePhase() throws InvalidGameStateException {
        Battle currentBattle = battles.getCurrentBattle();
        if (currentBattle != null) {
            if (currentBattle.isRihaniDeciphererMustBeResolved(this))
                throw new InvalidGameStateException("Rihani Decipherer must be resolved.");
            else if (currentBattle.isTechTokenMustBeResolved(this))
                throw new InvalidGameStateException("Tech Token must be selected before advancing.");
            else if (currentBattle.isHarkonnenCaptureMustBeResolved(this))
                throw new InvalidGameStateException("Harkonnen must decide to keep or kill " + currentBattle.getHarkonnenCapturedLeader() + ".");
            else if (currentBattle.isAuditorMustBeResolved())
                throw new InvalidGameStateException("Auditor must be resolved.");
            else if (currentBattle.isAssassinationMustBeResolved())
                throw new InvalidGameStateException("Moritani must decide on assassination.");
        }
        if (!battles.noBattlesRemaining(this))
            throw new InvalidGameStateException("There are battles remaining to be resolved.");
        getFactions().forEach(f -> f.getLeaders().forEach(l -> { l.setBattleTerritoryName(null); l.setPulledBehindShield(false); } ));
        if (dukeVidal != null)
            dukeVidal.setBattleTerritoryName(null);
        getFactions().forEach(f -> f.setUpdated(UpdateType.MISC_BACK_OF_SHIELD));
        getLeaderTanks().forEach(l -> l.setBattleTerritoryName(null));
        battles = null;
    }

    public void startSpiceHarvest() throws InvalidGameStateException {
        endBattlePhase();
        MoritaniFaction moritani = getMoritaniFactionOrNull();
        if (moritani != null) {
            if (moritani.getLeaders().removeIf(leader -> leader.getName().equals("Duke Vidal")))
                turnSummary.publish("Duke Vidal has left the " + Emojis.MORITANI + " services... for now.");
            moritani.startSpiceCollectionPhase();
        }

        turnSummary.publish("**Turn " + turn + " Spice Harvest Phase**");
        setPhaseForWhispers("Turn " + turn + " Spice Harvest Phase\n");
        if (!hasGameOption(GameOption.BG_COEXIST_WITH_ALLY))
            territories.flipAdvisorsIfAlone(this);

        for (Faction faction : factions) {
            faction.setHasMiningEquipment(false);
            if (territories.get("Arrakeen").hasActiveFaction(faction)) {
                faction.addSpice(2, "for Arrakeen");
                turnSummary.publish(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Arrakeen");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Carthag").hasActiveFaction(faction)) {
                faction.addSpice(2, "for Carthag");
                turnSummary.publish(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Carthag");
                faction.setHasMiningEquipment(true);
            }
            if (territories.get("Tuek's Sietch").hasActiveFaction(faction)) {
                turnSummary.publish(faction.getEmoji() + " collects 1 " + Emojis.SPICE + " from Tuek's Sietch");
                faction.addSpice(1, "for Tuek's Sietch");
            }
            if (territories.get("Cistern") != null && territories.get("Cistern").hasActiveFaction(faction)) {
                faction.addSpice(2, "for Cistern");
                turnSummary.publish(faction.getEmoji() + " collects 2 " + Emojis.SPICE + " from Cistern");
            }
        }

        for (Faction faction : factions) {
            Territory homeworld = getTerritory(faction.getHomeworld());
            if (homeworld.getForces().stream().anyMatch(force -> !force.getFactionName().equals(faction.getName()))) {
                Faction occupyingFaction = homeworld.getActiveFactions(this).getFirst();
                if (hasGameOption(GameOption.HOMEWORLDS) && occupyingFaction instanceof HarkonnenFaction harkonnenFaction && occupyingFaction.isHighThreshold() && !harkonnenFaction.hasTriggeredHT()) {
                    faction.addSpice(2, "for High Threshold advantage");
                    harkonnenFaction.setTriggeredHT(true);
                }
                turnSummary.publish(occupyingFaction.getEmoji() + " collects " + faction.getOccupiedIncome() + " " + Emojis.SPICE + " for occupying " + faction.getHomeworld());
                occupyingFaction.addSpice(faction.getOccupiedIncome(), "for occupying " + faction.getHomeworld());
            }
        }

        boolean altSpiceProductionTriggered = false;
        Territory orgiz = territories.get("Orgiz Processing Station");
        boolean orgizActive = orgiz != null && orgiz.getActiveFactions(this).size() == 1;
        for (Territory territory : territories.values()) {
            if (territory.getSpice() == 0 || territory.countActiveFactions() == 0) continue;
            if (orgizActive) {
                Faction orgizFaction = orgiz.getActiveFactions(this).getFirst();
                orgizFaction.addSpice(1, "for Orgiz Processing Station");
                territory.setSpice(territory.getSpice() - 1);
                turnSummary.publish(orgizFaction.getEmoji() + " collects 1 " + Emojis.SPICE + " from " + territory.getTerritoryName() + " with Orgiz Processing Station");
            }

            Faction faction = territory.getActiveFactions(this).getFirst();
            int spice = faction.getSpiceCollectedFromTerritory(territory);
            if (faction instanceof FremenFaction && faction.isHomeworldOccupied()) {
                faction.getOccupier().addSpice(Math.floorDiv(spice, 2),
                        "From " + Emojis.FREMEN + " " + Emojis.SPICE + " collection (occupied advantage).");
                turnSummary.publish(faction.getEmoji() +
                        " collects " + Math.floorDiv(spice, 2) + " " + Emojis.SPICE + " from " + Emojis.FREMEN + " collection at " + territory.getTerritoryName());
                spice = Math.ceilDiv(spice, 2);
            }
            faction.addSpice(spice, "for Spice Blow");
            territory.setSpice(territory.getSpice() - spice);

            if (hasGameOption(GameOption.TECH_TOKENS) && hasGameOption(GameOption.ALTERNATE_SPICE_PRODUCTION)
                    && (!(faction instanceof FremenFaction) || hasGameOption(GameOption.FREMEN_TRIGGER_ALTERNATE_SPICE_PRODUCTION)))
                altSpiceProductionTriggered = true;
            turnSummary.publish(faction.getEmoji() +
                    " collects " + spice + " " + Emojis.SPICE + " from " + territory.getTerritoryName());
            if (hasGameOption(GameOption.HOMEWORLDS) && faction instanceof HarkonnenFaction harkonnenFaction && faction.isHighThreshold() && !harkonnenFaction.hasTriggeredHT()) {
                faction.addSpice(2, "for High Threshold advantage");
                harkonnenFaction.setTriggeredHT(true);
                turnSummary.publish(Emojis.HARKONNEN + "gains 2 " + Emojis.SPICE + " for Giedi Prime High Threshold advantage");
            }
        }
        if (hasFaction("Harkonnen")) ((HarkonnenFaction) getFaction("Harkonnen")).setTriggeredHT(false);

        for (String aggregateTerritoryName : territories.getDistinctAggregateTerritoryNames()) {
            List<List<Territory>> aggregateTerritoryList = territories.getAggregateTerritoryList(aggregateTerritoryName, storm, false);
            for (List<Territory> territorySectors : aggregateTerritoryList) {
                String discoveryTerritoryName = "";
                String discoveryTokenName = "";
                for (Territory territory : territorySectors) {
                    if (territory.getDiscoveryToken() == null || territory.isDiscovered()) continue;
                    discoveryTerritoryName = territory.getTerritoryName();
                    discoveryTokenName = territory.getDiscoveryToken();
                }
                if (!discoveryTerritoryName.isEmpty()) {
                    for (Territory territory: territorySectors) {
                        if (territory.countActiveFactions() == 0) continue;
                        Faction faction = territory.getActiveFactions(this).getFirst();
                        List<DuneChoice> choices = new ArrayList<>();
                        choices.add(new DuneChoice("spicecollection-reveal-discovery-token-" + discoveryTerritoryName, "Yes"));
                        choices.add(new DuneChoice("danger", "spicecollection-don't-reveal-discovery-token", "No"));
                        faction.getChat().publish(faction.getPlayer() + "Would you like to reveal the discovery token at " + discoveryTerritoryName + "? (" + discoveryTokenName + ")", choices);
                    }
                }
            }
        }

        if (altSpiceProductionTriggered) {
            TechToken.addSpice(this, TechToken.SPICE_PRODUCTION);
            TechToken.collectSpice(this, TechToken.SPICE_PRODUCTION);
        }

        if (bribesThisTurn != null) {
            modInfo.publish("Check if any bribes need to be paid before Mentat Pause. " + getModOrRoleMention());
            modInfo.publish("The following bribes were made this turn:\n" + String.join("\n", bribesThisTurn));
        }

        setUpdated(UpdateType.MAP);
    }

    public MentatPause getMentatPause() {
        return mentatPause;
    }

    public void startMentatPause() throws InvalidGameStateException {
        MoritaniFaction moritani = getMoritaniFactionOrNull();
        if (moritani != null && moritani.isHtRemovalTBD())
            throw new InvalidGameStateException("Moritani must decide if they will remove a Terror Token before the game can advance.");
        if (mentatPause == null)
            mentatPause = new MentatPause();
        mentatPause.startPhase(this);
    }

    public void harkonnenSecretAlly() throws InvalidGameStateException {
        Faction faction = factions.stream().filter(f -> f.hasNexusCard("Harkonnen")).findFirst().orElseThrow(() -> new InvalidGameStateException("No faction has Harkonnen Nexus Card."));
        if (hasFaction("Harkonnen"))
            throw new InvalidGameStateException("Harkonnen is in the game.");
        faction.drawTwoTraitorsWithHarkonnenSecretAlly(Emojis.HARKONNEN + " Secret Ally");
    }

    public void endMentatPause() throws InvalidGameStateException {
        mentatPause.endPhase();
        mentatPause = null;
    }

    public void updateStrongholdSkills() {
        if (hasStrongholdSkills()) {
            getFactions().forEach(Faction::removeAllStrongholdCards);

            List<Pair<String, Faction>> controllingFactions = new ArrayList<>();
            List<Territory> strongholds = getTerritories().values().stream()
                    .filter(Territory::isStronghold)
                    .toList();

            strongholds.stream().filter(t -> t.countActiveFactions() == 1)
                    .forEach(t -> controllingFactions.add(new ImmutablePair<>(t.getTerritoryName(), t.getActiveFactions(this).getFirst())));
            if (hasEcazFaction())
                strongholds.stream().filter(t -> t.countActiveFactions() == 2)
                        .filter(t -> t.getActiveFactions(this).getFirst() instanceof EcazFaction && t.getActiveFactions(this).get(1).getAlly().equals("Ecaz")
                                || t.getActiveFactions(this).get(1) instanceof EcazFaction && t.getActiveFactions(this).getFirst().getAlly().equals("Ecaz"))
                        .forEach(t -> controllingFactions.add(new ImmutablePair<>(t.getTerritoryName(), getEcazFaction())));

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
        turnSummary.publish(faction1.getEmoji() + " and " + faction2.getEmoji() + " have formed an alliance.");
        faction1.setAlly(faction2.getName());
        faction2.setAlly(faction1.getName());
        setUpdated(UpdateType.MAP);
    }

    /**
     * Remove an alliance between two factions.
     * @param faction The first faction to remove the alliance from.
     */
    public void removeAlliance(@NotNull Faction faction) {
        if (faction.hasAlly()) {
            Faction allyFaction = getFaction(faction.getAlly());
            turnSummary.publish(faction.getEmoji() + " and " + allyFaction.getEmoji() + " are no longer allies.");
            faction.removeAlliance();
            allyFaction.removeAlliance();
            setUpdated(UpdateType.MAP);
        }
    }
}
