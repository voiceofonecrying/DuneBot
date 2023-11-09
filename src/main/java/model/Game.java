package model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import constants.Emojis;
import enums.GameOption;
import enums.SetupStep;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import helpers.Exclude;
import model.factions.EmperorFaction;
import model.factions.Faction;
import model.factions.RicheseFaction;
import model.topics.DuneTopic;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.faction;
import static model.Initializers.getCSVFile;
import static model.Initializers.getJSONString;

public class Game {
    private String gameRole;
    private int turn;
    private int phase;
    private int subPhase;
    private Bidding bidding;
    private final Deque<String> turnOrder;
    private final List<Faction> factions;
    private final Map<String, Territory> territories;
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
    private HashMap<Integer, List<String>> quotes;
    private String modRole;
    private Boolean mute;
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
    private boolean onHold;

    @Exclude
    private Set<UpdateType> updateTypes;
    @Exclude
    private DuneTopic turnSummary;
    @Exclude
    private DuneTopic modInfo;

    public Game() throws IOException {
        super();

        this.turn = 0;
        this.phase = 0;
        this.subPhase = 0;
        this.bidding = null;
        turnOrder = new LinkedList<>();
        factions = new LinkedList<>();
        territories = new HashMap<>();
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
        this.mod = "";
        this.gameRoleMention = "";
        this.storm = 18;
        this.stormMovement = 0;
        this.onHold = false;
        this.quotes = new HashMap<>();

        csvParser = getCSVFile("TreacheryCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            treacheryDeck.add(new TreacheryCard(csvRecord.get(0), csvRecord.get(1)));
        }
        csvParser = getCSVFile("SpiceCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            spiceDeck.add(new SpiceCard(csvRecord.get(0), Integer.parseInt(csvRecord.get(1)), Integer.parseInt(csvRecord.get(2)), null, null));
        }
        csvParser = getCSVFile("LeaderSkillCards.csv");
        for (CSVRecord csvRecord : csvParser) {
            leaderSkillDeck.add(new LeaderSkillCard(csvRecord.get(0)));
        }

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
        if (gameOption == GameOption.SUMMARY_THREAD_PER_TURN) return;
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

    public List<Faction> getFactions() {
        return factions;
    }

    public Map<String, Territory> getTerritories() {
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

    public int getPhase() {
        return phase;
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

        if (turn >= 1 && phase > 10) {
            advanceTurn();
        }
    }

    public void advanceSubPhase() {
        subPhase++;
    }

    public void advanceStorm(int movement) {
        setStorm(getStorm() + movement);
    }

    public Faction getFactionWithAtomics() {
        for (Faction faction : getFactions()) {
            try {
                faction.getTreacheryCard("Family Atomics ");
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

    public boolean hasLeaderSkills() {
        return hasGameOption(GameOption.LEADER_SKILLS);
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
        if (hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
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

        Faction factionWithAtomics = null;
        try {
            factionWithAtomics = getFactionsWithTreacheryCard("Family Atomics ").get(0);
        } catch (IndexOutOfBoundsException e) {
            // No faction has Family Atomics
        }
        if (factionWithAtomics != null && factionWithAtomics.isNearShieldWall()) {
            factionWithAtomics.getChat().publish(factionWithAtomics.getPlayer() + " will you play Family Atomics?");
        }

        Faction factionWithWeatherControl = null;
        try {
            factionWithWeatherControl = getFactionsWithTreacheryCard("Weather Control ").get(0);
        } catch (IndexOutOfBoundsException e) {
            // No faction has Weather Control
        }
        if (factionWithWeatherControl != null && turn != 1) {
            factionWithWeatherControl.getChat().publish(factionWithWeatherControl.getPlayer() + " will you play Weather Control?");
        }

        boolean atomicsEligible = factions.stream().anyMatch(Faction::isNearShieldWall);
        if (atomicsEligible && factionWithAtomics == null) {
            boolean atomicsInDeck = treacheryDeck.stream().anyMatch(c -> c.name().equals("Family Atomics "));
            boolean atomicsInDiscard = treacheryDiscard.stream().anyMatch(c -> c.name().equals("Family Atomics "));
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

    public void drawSpiceBlow(String spiceBlowDeckName) throws ChannelNotFoundException {
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
                                Faction fremen = getFaction("Fremen");
                                if (fremen.hasAlly() && force.getName().contains(fremen.getAlly())) continue;
                                message.append(MessageFormat.format("{0} {1} devoured by {2}\n",
                                        force.getStrength(), Emojis.getForceEmoji(force.getName()), drawn.name()
                                ));
                                forcesToRemove.add(force);
                            }
                            for (Force force : forcesToRemove) {
                                if (force.getName().contains("*")) {
                                    removeForces(lastCard.name(), getFaction(force.getFactionName()), 0, force.getStrength(),  true);
                                } else {
                                    removeForces(lastCard.name(), getFaction(force.getFactionName()), force.getStrength(), 0, true);
                                }
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
                for (Force force : getTerritory(drawn.name()).getForces()) {
                    if (force.getName().contains("*")) removeForces(drawn.name(), getFaction(force.getFactionName()), 0, force.getStrength(),  true);
                    else removeForces(drawn.name(), getFaction(force.getFactionName()), force.getStrength(), 0, true);
                }
            }
            message.append(drawn.discoveryToken()).append(" has been placed in ").append(drawn.tokenLocation()).append("\n");
            if (drawn.discoveryToken().equals("Hiereg")) getTerritory(drawn.tokenLocation()).setDiscoveryToken(hieregTokens.remove(0));
            else getTerritory(drawn.tokenLocation()).setDiscoveryToken(smugglerTokens.remove(0));
            getTerritory(drawn.tokenLocation()).setDiscovered(false);
            if (hasFaction("Guild") && drawn.discoveryToken().equals("Smuggler")) getFaction("Guild").getChat()
                    .publish("The discovery token at " + drawn.tokenLocation() + " is a(n) " + getTerritory(drawn.tokenLocation()).getDiscoveryToken());
            if (hasFaction("Fremen") && drawn.discoveryToken().equals("Hiereg")) getFaction("Fremen").getChat()
                    .publish("The discovery token at " + drawn.tokenLocation() + " is a(n) " + getTerritory(drawn.tokenLocation()).getDiscoveryToken());
        }
        if (storm == drawn.sector()) getTerritory(drawn.name()).setSpice(0);

        turnSummary.publish(message.toString());
        if (hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            setUpdated(UpdateType.MAP);
    }

    public void removeForces(String territoryName, Faction targetFaction, int amountValue, int specialAmount, boolean isToTanks) throws ChannelNotFoundException {
        targetFaction.removeForces(territoryName, amountValue, false, isToTanks);
        if (specialAmount > 0) targetFaction.removeForces(territoryName, specialAmount, true, isToTanks);
        if (hasGameOption(GameOption.HOMEWORLDS) && homeworlds.containsValue(territoryName)) {
            Faction homeworldFaction = factions.stream().filter(f -> f.getHomeworld().equals(territoryName) || (f.getName().equals("Emperor") && territoryName.equals("Salusa Secundus"))).findFirst().get();
            if (territoryName.equals("Salusa Secundus") && ((EmperorFaction) homeworldFaction).getSecundusHighThreshold() > getTerritory("Salusa Secundus").getForce("Emperor*").getStrength() && ((EmperorFaction) homeworldFaction).isSecundusHighThreshold()) {
                ((EmperorFaction) homeworldFaction).setSecundusHighThreshold(false);
                turnSummary.publish("Salusa Secundus has flipped to low threshold.");

            } else if (homeworldFaction.isHighThreshold() && homeworldFaction.getHighThreshold() > getTerritory(territoryName).getForce(faction.getName()).getStrength() + getTerritory(territoryName).getForce(faction.getName() + "*").getStrength()) {
                System.out.println(
                        MessageFormat.format(
                                "isHigh = {0}\ngetHigh = {1}\ngetForce = {2}\ngetSpecial = {3}",
                                homeworldFaction.isHighThreshold(),homeworldFaction.getHighThreshold(),
                                getTerritory(territoryName).getForce(faction.getName()).getStrength(), getTerritory(territoryName).getForce(faction.getName() + "*").getStrength()
                        )
                );
                homeworldFaction.setHighThreshold(false);
                turnSummary.publish(homeworldFaction.getHomeworld() + " has flipped to low threshold.");
            }

            if (territoryName.equals("Ecaz") && getFaction("Ecaz").isHomeworldOccupied()) {
                for (Faction faction1 : factions) {
                    faction1.getLeaders().removeIf(leader1 -> leader1.name().equals("Duke Vidal"));
                }
                getFaction("Ecaz").getOccupier().getLeaders().add(new Leader("Duke Vidal", 6, null, false));
                turnSummary.publish("Duke Vidal has left to work for " + getFaction("Ecaz").getOccupier().getEmoji() + " (planet Ecaz occupied)");
            }
        }
        if (hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            setUpdated(UpdateType.MAP);
        }
    }
}
