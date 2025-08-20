package model.factions;

import constants.Colors;
import constants.Emojis;
import enums.ChoamInflationType;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import helpers.Exclude;
import model.*;
import model.topics.DuneTopic;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class Faction {
    protected final String name;
    private final List<TechToken> techTokens;
    protected final List<TreacheryCard> treacheryHand;
    protected final List<TraitorCard> traitorHand;
    protected List<String> traitorsUsed;
    private final List<LeaderSkillCard> leaderSkillsHand;
    private final List<StrongholdCard> strongholdCards;
    private StrongholdCard hmsStrongholdProxy;
    protected final List<Leader> leaders;
    protected String emoji;
    protected int handLimit;
    protected int spice;
    protected int freeRevival;
    protected int maxRevival;
    protected boolean starRevived;
    protected String paidRevivalMessage;
    protected boolean paidRevivalTBD;
    protected boolean hasMiningEquipment;
    protected boolean decliningCharity;
    protected int highThreshold;
    protected int lowThreshold;
    protected int occupiedIncome;
    protected String homeworld;
    @Exclude
    private Set<UpdateType> updateTypes;
    protected String player;
    private String userName;
    private String infoChannelPrefix;
    private boolean graphicDisplay;
    private int frontOfShieldSpice;
    protected String ally;
    private String bid;
    private int maxBid;
    private boolean useExact;
    private boolean autoBid;
    private boolean autoBidTurn;
    private boolean outbidAlly;
    private boolean specialKaramaPowerUsed;
    private NexusCard nexusCard;
    protected Shipment shipment;
    private Movement movement;
    private int spiceForAlly;
    private boolean allySpiceFinishedForTurn;
    protected boolean isHighThreshold;
    private Boolean ornithoperToken;
    private Map<String, String> lastWhisper;
    private Map<String, Integer> whisperCount;
    private Map<String, Integer> whispersToTurnSummary;
    private Map<String, Integer> whisperCountPerPhase;
    private Map<String, Integer> whispersToTurnSummaryPerPhase;
    @Exclude
    protected Game game;
    @Exclude
    protected DuneTopic ledger;
    @Exclude
    protected DuneTopic chat;
    @Exclude
    protected DuneTopic allianceThread;
    public boolean refreshedForAllActionsUX;

    public Faction(String name, String player, String userName) throws IOException {
        this.handLimit = 4;
        this.name = name;
        this.infoChannelPrefix = name.toLowerCase();
        this.player = player;
        this.userName = userName;
        this.graphicDisplay = false;
        this.treacheryHand = new LinkedList<>();
        this.frontOfShieldSpice = 0;
        this.hasMiningEquipment = false;
        this.decliningCharity = false;
        this.traitorHand = new LinkedList<>();
        this.traitorsUsed = new ArrayList<>();
        this.leaders = new LinkedList<>();
        this.techTokens = new LinkedList<>();
        this.leaderSkillsHand = new LinkedList<>();
        this.strongholdCards = new LinkedList<>();
        this.spice = 0;
        this.bid = "";
        this.autoBid = false;
        this.autoBidTurn = false;
        this.useExact = true;
        this.outbidAlly = false;
        this.specialKaramaPowerUsed = false;
        this.shipment = new Shipment();
        this.movement = new Movement();
        this.spiceForAlly = 0;
        this.allySpiceFinishedForTurn = false;
        this.nexusCard = null;
        this.maxRevival = 3;
        this.starRevived = false;
        this.paidRevivalTBD = false;
        this.isHighThreshold = true;
        this.ornithoperToken = false;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));
        for (CSVRecord csvRecord : CSVParser.parse(bufferedReader, CSVFormat.EXCEL))
            if (csvRecord.get(0).equals(this.getName()))
                this.leaders.add(new Leader(csvRecord.get(1), Integer.parseInt(csvRecord.get(2)), csvRecord.get(0), null, false));
    }

    public void joinGame(@NotNull Game game) {
        this.game = game;
        leaders.forEach(l -> game.getTraitorDeck().add(new TraitorCard(l.getName(), l.getOriginalFactionName(), l.getValue())));
    }

    public void setUpdated(UpdateType updateType) {
        if (this.updateTypes == null) this.updateTypes = new HashSet<>();
        this.updateTypes.add(updateType);
    }

    public Set<UpdateType> getUpdateTypes() {
        if (this.updateTypes == null) this.updateTypes = new HashSet<>();
        return this.updateTypes;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
        getHomeworldTerritory().setGame(game);
    }

    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the prefix used for the information channel. If the prefix is not set,
     * it defaults to the lowercase version of the name.
     *
     * @return the information channel prefix, or the name in lowercase if the prefix is not set
     */
    public String getInfoChannelPrefix() {
        if (this.infoChannelPrefix == null) return this.getName().toLowerCase();
        return infoChannelPrefix;
    }

    public void setInfoChannelPrefix(String infoChannelPrefix) {
        this.infoChannelPrefix = infoChannelPrefix;
    }

    public String getEmoji() {
        return emoji;
    }
    public Color getColor() {
        return Colors.getFactionColor(getName());
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAlly() {
        if (ally == null) return "";
        return ally;
    }

    public void setAlly(String ally) {
        this.ally = ally;
        ledger.publish("You are now allies with " + Emojis.getFactionEmoji(ally) + "!");
        if (game.hasGameOption(GameOption.HOMEWORLDS) && game.hasCHOAMFaction()) {
            HomeworldTerritory tupile = (HomeworldTerritory) game.getTerritory("Tupile");
            if (tupile.getOccupyingFaction() == this)
                tupile.increaseHandLimitForTupile(game.getFaction(ally));
        }
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeAlliance() {
        ledger.publish("Your alliance with " + Emojis.getFactionEmoji(ally) + " has been dissolved!");
        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            if (game.hasCHOAMFaction()) {
                HomeworldTerritory tupile = (HomeworldTerritory) game.getTerritory("Tupile");
                if (tupile.getOccupyingFaction() == this)
                    tupile.reduceHandLimitForTupile(game.getFaction(ally));
            }
            if (game.hasEcazFaction()) {
                HomeworldTerritory ecazHomeworld = (HomeworldTerritory) game.getTerritory("Ecaz");
                if (ecazHomeworld.getOccupyingFaction() == this && game.getFaction(ally).getLeader("Duke Vidal").isPresent()) {
                    game.releaseDukeVidal(false);
                    game.assignDukeVidalToAFaction(name);
                }
            }
        }
        ally = null;
        spiceForAlly = 0;
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public boolean hasAlly() {
        return ally != null;
    }

    public int getHandLimit() {
        return handLimit;
    }

    public void setHandLimit(int handLimit) {
        this.handLimit = handLimit;
    }

    public List<TreacheryCard> getTreacheryHand() {
        return treacheryHand;
    }

    public TreacheryCard getTreacheryCard(String name) {
        return treacheryHand.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Treachery card not found"));
    }

    public void addTreacheryCard(TreacheryCard card) {
        if (treacheryHand.size() >= getHandLimit()) {
            throw new IllegalStateException("Hand limit reached");
        }

        treacheryHand.add(card);
        setUpdated(UpdateType.TREACHERY_CARDS);
    }

    /**
     * Remove card from faction, put it in discard, and publish to turn summary.
     * Allow simple call for discards that don't require a reason.
     *
     * @param cardName the name of the card to discard
     */
    public void discard(String cardName) {
        discard(cardName, "");
    }

    /**
     * Remove card from faction, put it in discard, and publish to turn summary.
     *
     * @param cardName the name of the card to discard
     * @param reason optional reason for the discard
     */
    public void discard(String cardName, String reason) {
        TreacheryCard treacheryCard = removeTreacheryCardWithoutDiscard(cardName);
        game.getTreacheryDiscard().add(treacheryCard);
        game.getTurnSummary().publish(emoji + " discards " + cardName + (reason.isEmpty() ? "" : " " + reason) + ".");
        ledger.publish(cardName + " discarded from hand.");

        if (game.hasGameOption(GameOption.HOMEWORLDS) && game.hasEcazFaction() && game.getEcazFaction().isHighThreshold() && (treacheryCard.type().contains("Weapon - Poison") || treacheryCard.name().equals("Poison Blade"))) {
            game.getEcazFaction().addSpice(3, "Poison weapon was discarded");
            game.getTurnSummary().publish(Emojis.ECAZ + " gain 3 " + Emojis.SPICE + " for the discarded poison weapon");
        }
    }

    /**
     * Remove card from faction. Do not put in discard, and do not publish to turn summary.
     * Whenever the card would go to the discard pile, a discard() method should be used.
     *
     * @param name the name of the card to discard
     *
     * @return The TreacheryCard that was removed
     */
    public TreacheryCard removeTreacheryCardWithoutDiscard(String name) {
        return removeTreacheryCardWithoutDiscard(getTreacheryCard(name));
    }

    /**
     * Remove card from faction. Do not put in discard, and do not publish to turn summary.
     * Whenever the card would go to the discard pile, a discard() method should be used.
     *
     * @param card the card to discard. If faction does not have this card, the treacheryHand is left unchanged.
     *
     * @return The TreacheryCard that was requested to be removed
     */
    public TreacheryCard removeTreacheryCardWithoutDiscard(TreacheryCard card) {
        treacheryHand.remove(card);
        setUpdated(UpdateType.TREACHERY_CARDS);
        return card;
    }

    public boolean hasTreacheryCard(String cardName) {
        return treacheryHand.stream().anyMatch(c -> c.name().equals(cardName));
    }

    /**
     * Gets the most recently added treachery card from the Faction's hand.
     * @return Treachery Card
     */
    public TreacheryCard getLastTreacheryCard() {
        return treacheryHand.getLast();
    }

    public void addTraitorCard(TraitorCard card) {
        traitorHand.add(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeTraitorCard(TraitorCard card) {
        traitorHand.remove(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public boolean hasTraitor(String leaderName) {
        return traitorHand.stream().anyMatch(t -> t.getName().equals(leaderName));
    }

    public void useTraitor(String leaderName) {
        // Can remove this list creation after games 77, 78, 82, 83, 85, 86, and 87 end
        if (traitorsUsed == null)
            traitorsUsed = new ArrayList<>();

        traitorsUsed.add(leaderName);
    }

    public List<TraitorCard> getTraitorHand() {
        return traitorHand;
    }

    public void presentTraitorSelection() {
        List<DuneChoice> traitors = traitorHand.stream().map(t -> new DuneChoice("primary", "traitor-selection-" + t.getName(), t.getNameAndStrengthString(), t.getFactionEmoji(), false)).toList();
        chat.publish("Please select your traitor " + player, traitors);
    }

    public List<LeaderSkillCard> getLeaderSkillsHand() {
        return leaderSkillsHand;
    }

    public List<StrongholdCard> getStrongholdCards() {
        return strongholdCards;
    }

    public boolean hasStrongholdCard(String strongholdName) {
        return strongholdCards.stream().anyMatch(c -> c.name().equals(strongholdName));
    }

    public void addStrongholdCard(StrongholdCard card) {
        strongholdCards.add(card);
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void removeAllStrongholdCards() {
        strongholdCards.clear();
        hmsStrongholdProxy = null;
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public boolean hasHmsStrongholdProxy(String strongholdName) {
        return hmsStrongholdProxy != null && hmsStrongholdProxy.name().equals(strongholdName);
    }

    public void setHmsStrongholdProxy(StrongholdCard hmsStrongholdProxy) throws InvalidGameStateException {
        if (!hasStrongholdCard("Hidden Mobile Stronghold"))
            throw new InvalidGameStateException(getEmoji() + " does not have HMS Stronghold card");
        else if (!hasStrongholdCard(hmsStrongholdProxy.name()))
            throw new InvalidGameStateException(getEmoji() + " does not have " + hmsStrongholdProxy.name() + " Stronghold card");

        this.hmsStrongholdProxy = hmsStrongholdProxy;
    }

    public int getSpice() {
        return spice;
    }

    public String forcesString(int numForces, int numSpecialForces) {
        String forcesString = "";
        if (numForces > 0)
            forcesString += numForces + " " + Emojis.getForceEmoji(name) + " ";
        if (numSpecialForces > 0)
            forcesString += numSpecialForces + " " + Emojis.getForceEmoji(name + "*");
        forcesString = forcesString.trim();
        if (forcesString.isEmpty())
            forcesString = "no " + emoji + " forces";
        return forcesString;
    }

    public String forcesStringWithZeroes(int numForces, int numSpecialForces) {
        return numForces + " " + Emojis.getForceEmoji(name) + " ";
    }

    public void placeForces(Territory targetTerritory, int amountValue, int starredAmountValue, boolean isShipment, boolean isIntrusion, boolean canTrigger, boolean karama, boolean crossShip) throws InvalidGameStateException {
        String forcesPlaced = placeAndReportWhatWasPlaced(targetTerritory, amountValue, starredAmountValue);
        String placedWhere = " placed on " + targetTerritory.getTerritoryName();
        String costString = "";
        if (isShipment) {
            getShipment().setShipped(true);
            int cost = game.shipmentCost(this, amountValue + starredAmountValue, targetTerritory, karama, crossShip);
            if (cost > 0)
                costString = payForShipment(cost, targetTerritory, karama, false);
        }

        BGFaction bg = game.getBGFactionOrNull();
        if (isIntrusion && bg != null) {
            if (targetTerritory.hasActiveFaction(bg) && !(this instanceof BGFaction))
                bg.presentFlipMessage(game, targetTerritory.getTerritoryName());
            if (isShipment && getShipment().getCrossShipFrom().isEmpty())
                bg.presentAdvisorChoices(game, this, targetTerritory);
        }

        game.getTurnSummary().publish(forcesPlaced + placedWhere + costString);

        if (isShipment && !(this instanceof GuildFaction)
                && !(this instanceof FremenFaction && !(targetTerritory instanceof HomeworldTerritory))
                && game.hasGameOption(GameOption.TECH_TOKENS))
            TechToken.addSpice(game, TechToken.HEIGHLINERS);

        if (canTrigger)
            game.checkForTriggers(targetTerritory, this, amountValue + starredAmountValue);
    }

    /**
     * Places forces from reserves into this territory.
     *
     * @param territory The territory to place the force in.
     * @param amount    The number of regular forces to place.
     * @param starredAmount   The number of starred forces to place.
     *
     * @return A string with faction emoji and the number of forces placed.
     */
    protected String placeAndReportWhatWasPlaced(Territory territory, int amount, int starredAmount) {
        if (amount > 0)
            placeForcesFromReserves(territory, amount, false);
        if (starredAmount > 0)
            placeForcesFromReserves(territory, starredAmount, true);
        return emoji + ": " + forcesString(amount, starredAmount);
    }

    /**
     * Places forces from reserves into this territory.
     * Reports removal from reserves to ledger.
     * Switches homeworld to low threshold if applicable.
     *
     * @param territory The territory to place the force in.
     * @param amount    The number of forces to place.
     * @param special   Whether the force is a special reserve.
     */
    public void placeForcesFromReserves(Territory territory, int amount, boolean special) {
        String forceName = name + (special ? "*" : "");
        if (special)
            removeSpecialReserves(amount);
        else
            removeReserves(amount);
        ledger.publish(MessageFormat.format("{0} {1} removed from reserves.", amount, Emojis.getForceEmoji(forceName)));
        territory.addForces(forceName, amount);
        game.setUpdated(UpdateType.MAP);
    }

    public void addReserves(int amount) {
        game.getTerritory(homeworld).addForces(name, amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeReserves(int amount) {
        game.getTerritory(homeworld).removeForces(game, name, amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void addSpecialReserves(int amount) {
        game.getTerritory(homeworld).addForces(name + "*", amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeSpecialReserves(int amount) {
        game.getTerritory(homeworld).removeForces(game, name + "*", amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public int getReservesStrength() {
        return game.getTerritory(homeworld).getForceStrength(name);
    }

    public int getSpecialReservesStrength() {
        return game.getTerritory(homeworld).getForceStrength(name + "*");
    }

    public int getTotalReservesStrength() {
        return getReservesStrength() + getSpecialReservesStrength();
    }

    public int getFrontOfShieldSpice() {
        return frontOfShieldSpice;
    }

    public void collectFrontOfShieldSpice() {
        if (frontOfShieldSpice > 0) {
            game.getTurnSummary().publish(emoji + " collects " + frontOfShieldSpice + " " + Emojis.SPICE + " from front of shield.");
            addSpice(frontOfShieldSpice, "front of shield");
            frontOfShieldSpice = 0;
            setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
        }
    }

    public int getFreeRevival() {
        int adjustedFreeRevival = freeRevival;
        if (ally != null && ally.equals("Fremen"))
            adjustedFreeRevival = 3;
        if (!isHighThreshold)
            adjustedFreeRevival++;
        if (game.isRecruitsInPlay())
            adjustedFreeRevival = Math.min(7, adjustedFreeRevival * 2);
        return adjustedFreeRevival;
    }

    public Optional<Leader> getLeader(String leaderName) {
        return getLeaders().stream()
                .filter(l -> l.getName().equalsIgnoreCase(leaderName))
                .findFirst();
    }

    public List<Leader> getLeaders() {
        return leaders;
    }

    public void assignSkillToLeader(String leaderName, String leaderSkillName) throws InvalidGameStateException {
        Leader leader = getLeader(leaderName).orElseThrow(() -> new IllegalArgumentException("Leader not found"));
        LeaderSkillCard leaderSkillCard = leaderSkillsHand.stream()
                .filter(l -> l.name().equalsIgnoreCase(leaderSkillName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Leader Skill not found"));
        leader.setSkillCard(leaderSkillCard);
        chat.publish("After years of training, " + leaderName + " has become a " + leaderSkillCard.name() + "!");

        LeaderSkillCard returnedLeaderSkillCard = getLeaderSkillsHand().stream()
                .filter(l -> !l.name().equalsIgnoreCase(leaderSkillName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Rejected leader Skill not found"));
        game.getLeaderSkillDeck().add(returnedLeaderSkillCard);
        Collections.shuffle(game.getLeaderSkillDeck());
        leaderSkillsHand.clear();
    }

    public List<Leader> getSkilledLeaders() {
        return getLeaders().stream().filter(l -> l.getSkillCard() != null).toList();
    }

    public boolean hasSkill(String skillName) {
        return getSkilledLeaders().stream().anyMatch(l -> l.getSkillCard().name().equals(skillName));
    }

    public Leader removeLeader(String name) {
        Leader leader = leaders.stream()
                .filter(l -> l.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Leader " + name + " not found."));
        removeLeader(leader);
        return leader;
    }

    public void removeLeader(Leader leader) {
        leaders.remove(leader);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        if (leader.getSkillCard() != null)
            setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void addLeader(Leader leader) {
        getLeaders().add(leader);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    /**
     * Just revive the leader. Calling function handles payment and messaging.
     *
     * @param leaderToRevive the name of the leader
     */
    protected void reviveLeader(String leaderToRevive) {
        Leader leader = game.removeLeaderFromTanks(leaderToRevive);
        addLeader(leader);
        leader.setBattleTerritoryName(null);
    }

    /**
     * Revive a leader from the tanks and make payment to BT if applicable or to the spice bank
     *
     * @param leaderToRevive the name of the leader
     * @param revivalCost    optional alternate cost charged by BT
     */
    public void reviveLeader(String leaderToRevive, Integer revivalCost) throws InvalidGameStateException {
        if (revivalCost != null && revivalCost != 0) {
            if (this instanceof BTFaction)
                throw new IllegalArgumentException("BT cannot set an alternative cost for themselves;");
            else if (!game.hasBTFaction())
                throw new IllegalArgumentException("Alternative cost cannot be set without BT in the game.");
        }
        Leader leader = null;
        if (revivalCost == null) {
            leader = game.findLeaderInTanks(leaderToRevive);
            revivalCost = leader.getRevivalCost(this);
        }
        if (spice < revivalCost)
            throw new InvalidGameStateException(name + " does not have enough spice to revive " + leaderToRevive);

        reviveLeader(leaderToRevive);
        subtractSpice(revivalCost, "revive " + leaderToRevive);
        String message = leaderToRevive + " was revived from the tanks for " + revivalCost + " " + Emojis.SPICE;
        if (this instanceof BTFaction bt && leader != null && !leader.getOriginalFactionName().equals("BT")) {
            message = "revived " + Emojis.getFactionEmoji(leader.getOriginalFactionName()) + " " + leaderToRevive + " as a Ghola for " + revivalCost + " " + Emojis.SPICE;
            if (leader.getName().equals("Duke Vidal"))
                bt.setDukeVidalGhola(true);
        }
        BTFaction bt = game.getBTFactionOrNull();
        if (bt != null && this != bt) {
            message += " paid to " + Emojis.BT;
            bt.addSpice(revivalCost, emoji + " revived " + leaderToRevive);
        }
        game.getTurnSummary().publish(emoji + " " + message);
        if (this instanceof EcazFaction && leaderToRevive.equals("Duke Vidal")) {
            game.releaseDukeVidal(true);
        }
        game.setUpdated(UpdateType.MAP);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void addFrontOfShieldSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        if (spice == 0) return;
        this.frontOfShieldSpice += spice;
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void subtractFrontOfShieldSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        if (spice == 0) return;
        this.frontOfShieldSpice -= spice;
        if (this.frontOfShieldSpice < 0)
            throw new IllegalStateException("Faction cannot spend more spice than they have.");
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public boolean hasMiningEquipment() {
        return hasMiningEquipment;
    }

    public void setHasMiningEquipment(boolean hasMiningEquipment) {
        this.hasMiningEquipment = hasMiningEquipment;
    }

    public boolean isDecliningCharity() {
        return decliningCharity;
    }

    public void setDecliningCharity(boolean decliningCharity) throws InvalidGameStateException {
        this.decliningCharity = decliningCharity;
    }

    public List<TechToken> getTechTokens() {
        return techTokens;
    }

    public boolean hasTechToken(String techToken) {
        return techTokens.stream().anyMatch(t -> t.getName().equals(techToken));
    }

    public void addTechToken(String techToken) throws InvalidGameStateException {
        if (hasTechToken(techToken))
            throw new InvalidGameStateException(name + " already has " + techToken);
        techTokens.add(new TechToken(techToken));
    }

    public void removeTechToken(String techToken) throws InvalidGameStateException {
        if (!hasTechToken(techToken))
            throw new InvalidGameStateException(name + " does not have " + techToken);
        techTokens.removeIf(t -> t.getName().equals(techToken));
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public int getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(int maxBid) {
        this.maxBid = maxBid;
    }

    public boolean isUseExactBid() {
        return useExact;
    }

    public boolean isAutoBid() {
        return autoBid;
    }

    public void setAutoBid(boolean autoBid) {
        this.autoBid = autoBid;
    }

    public boolean isAutoBidTurn() {
        return autoBidTurn;
    }

    public void setAutoBidTurn(boolean autoBidTurn) {
        this.autoBidTurn = autoBidTurn;
    }

    public boolean isOutbidAlly() {
        return outbidAlly;
    }

    public void setOutbidAlly(boolean outbidAlly) {
        this.outbidAlly = outbidAlly;
    }

    protected boolean doesNotHaveKarama() {
        if (this instanceof BGFaction && treacheryHand.stream().anyMatch(c -> c.type().equals("Worthless Card"))) {
            return false;
        }
        return treacheryHand.stream().noneMatch(c -> c.name().equals("Karama"));
    }

    public String bid(Game game, boolean useExact, int bidAmount, Boolean newOutbidAllySetting, Boolean enableAutoPass) throws InvalidGameStateException {
        int spiceFromAlly = hasAlly() ? game.getFaction(ally).getSpiceForAlly() : 0;
        if (bidAmount > spice + spiceFromAlly
                && doesNotHaveKarama())
            throw new InvalidGameStateException("You have insufficient " + Emojis.SPICE + " for this bid and no Karama to avoid paying.");

        this.useExact = useExact;
        maxBid = bidAmount;
        String modMessage = emoji + " set their bid to " + (useExact ? "exactly " : "increment up to ") + bidAmount + ".";
        String responseMessage = "You will bid ";
        boolean silentAuction = game.getBidding().isSilentAuction();
        if (silentAuction) {
            responseMessage += "exactly " + bidAmount + " in the silent auction.";
        } else if (useExact) {
            responseMessage += "exactly " + bidAmount + " if possible.";
        } else {
            responseMessage += "+1 up to " + bidAmount + ".";
        }
        int spiceAvaiable = spice + spiceFromAlly;
        if (bidAmount > spiceAvaiable)
            responseMessage += "\nIf you win for more than " + spiceAvaiable + ", you will have to use your Karama.";
        if (enableAutoPass != null) {
            autoBid = enableAutoPass;
            modMessage += enableAutoPass ? " Auto-pass enabled." : " No auto-pass.";
        }
        game.getModLedger().publish(modMessage);
        String responseMessage2 = "";
        if (!silentAuction) {
            if (autoBid) {
                responseMessage += "\nYou will then auto-pass.";
            } else {
                responseMessage += "\nYou will not auto-pass.\nA new bid or pass will be needed if you are outbid.";
            }
            boolean outbidAllyValue = outbidAlly;
            if (newOutbidAllySetting != null) {
                outbidAllyValue = newOutbidAllySetting;
                outbidAlly = outbidAllyValue;
                responseMessage2 = "\nYou set your outbid ally policy to " + outbidAllyValue;
                game.getModLedger().publish(responseMessage2);
                chat.publish(responseMessage2);
            }
            if (hasAlly()) {
                responseMessage2 = "\nYou will" + (outbidAllyValue ? "" : " not") + " outbid your ally";
            }
        }
        return responseMessage + responseMessage2;
    }

    public void payForCard(String currentCard, int spentValue) throws InvalidGameStateException {
        int spiceFromAlly = hasAlly() ? game.getFaction(ally).getSpiceForAlly() : 0;
        if (spice + spiceFromAlly < spentValue)
            throw new InvalidGameStateException(name + " does not have enough spice to buy the card.");
        else if (treacheryHand.size() >= handLimit)
            throw new InvalidGameStateException(emoji + " already has a full hand.");

        int allySupport = Math.min(spiceFromAlly, spentValue);
        String allyString = hasAlly() && spiceFromAlly > 0 ? " (" + allySupport + " from " + game.getFaction(ally).getEmoji() + ")" : "";
        subtractSpice(spentValue - allySupport, currentCard);
        if (allySupport > 0)
            game.getFaction(ally).subtractSpiceForAlly(allySupport, currentCard + " (ally support)");
        game.getTurnSummary().publish(emoji + " wins " + currentCard + " for " + spentValue + " " + Emojis.SPICE + allyString);
    }

    public String payForShipment(int spice, Territory territory, boolean karamaShipment, boolean noField) throws InvalidGameStateException {
        String paymentMessage = " for " + spice + " " + Emojis.SPICE;
        int spiceFromAlly = 0;
        if (hasAlly())
            spiceFromAlly = game.getFaction(ally).getShippingSupport();
        if (this.spice + spiceFromAlly < spice)
            throw new InvalidGameStateException(name + " does not have enough spice to make this shipment.");
        int support = 0;
        int guildSupport = 0;
        if (spiceFromAlly > 0) {
            support = Math.min(spiceFromAlly, spice);
            Faction allyFaction = game.getFaction(ally);
            if (allyFaction instanceof GuildFaction)
                guildSupport = support;
            allyFaction.subtractSpiceForAlly(support, emoji + " shipment support");
            paymentMessage += MessageFormat.format(" ({0} from {1})", support, game.getFaction(ally).getEmoji());
        }
        String noFieldString = noField ? Emojis.NO_FIELD + " " : "";
        subtractSpice(spice - support, noFieldString + "shipment to " + territory.getTerritoryName());
        Faction guild = game.getGuildFactionOrNull();
        if (!karamaShipment && !(this instanceof GuildFaction) && guild != null) {
            int spicePaidToGuild = spice - guildSupport;
            if (!guild.isHighThreshold())
                spicePaidToGuild = Math.ceilDiv(spicePaidToGuild, 2);
            if (spicePaidToGuild != spice)
                paymentMessage += ", " + spicePaidToGuild + " " + Emojis.SPICE;
            paymentMessage += " paid to " + Emojis.GUILD;
            guild.addSpice(spicePaidToGuild, emoji + " shipment");
            if (guild.isHomeworldOccupied()) {
                Faction occupier = guild.getOccupier();
                occupier.addSpice(spice - spicePaidToGuild, emoji + " shipment");
                paymentMessage += ", " + (spice - spicePaidToGuild) + " " + Emojis.SPICE + " paid to " + occupier.getEmoji();
            }
        }
        return paymentMessage;
    }

    public void bribe(Game game, Faction recipientFaction, int amountValue, String reasonString) throws InvalidGameStateException {
        ChoamFaction choam = game.getCHOAMFactionOrNull();
        if (choam != null) {
            boolean inflationDoubled = false;
            if (game.getPhase() == 10 && choam.getInflationType(game.getTurn() + 1) == ChoamInflationType.DOUBLE)
                inflationDoubled = true;
            else if (choam.getInflationType(game.getTurn()) == ChoamInflationType.DOUBLE)
                inflationDoubled = true;
            if (inflationDoubled) {
                if (amountValue > 0)
                    throw new InvalidGameStateException("Bribes cannot be made when the Inflation Token is double side up.");
                else
                    game.getModInfo().publish("0 " + Emojis.SPICE + " deal made with Inflation Token double side up. Cancel if not allowed by your game. " + game.getModOrRoleMention());
            }
        }
        String message = emoji + " bribes " + recipientFaction.getEmoji() + " " + amountValue + " " + Emojis.SPICE;
        if (!reasonString.isBlank())
            message += " " + reasonString;
        game.addNewBribe(message);
        if (amountValue != 0) {
            if (spice < amountValue)
                throw new InvalidGameStateException("Faction does not have enough spice to pay the bribe!");

            subtractSpice(amountValue, "bribe to " + recipientFaction.getEmoji());
            recipientFaction.addFrontOfShieldSpice(amountValue);
            message += "\n" + emoji + " places " + amountValue + " " + Emojis.SPICE + " in front of " + recipientFaction.getEmoji() + " shield.";
        } else {
            message += "\nBribe " + Emojis.SPICE + " NA or TBD.";
        }
        game.getTurnSummary().publish(message);
    }

    public boolean isSpecialKaramaPowerUsed() {
        return specialKaramaPowerUsed;
    }

    public void setSpecialKaramaPowerUsed(boolean specialKaramaPowerUsed) {
        this.specialKaramaPowerUsed = specialKaramaPowerUsed;
    }

    public boolean isNearShieldWall() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory.isNearShieldWall() && territory.getTotalForceCount(this) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds forces from a Territory to the reserves or tanks
     *
     * @param territoryName The name of the Territory.
     * @param amount        The amount of the force.
     * @param isSpecial     Whether the force is special or not.
     * @param toTanks       Whether the force is going to the tanks or not.
     */
    public void removeForces(String territoryName, int amount, boolean isSpecial, boolean toTanks) {
        if (isSpecial) {
            throw new IllegalArgumentException("Faction does not have special forces.");
        }

        String forceName = getName();
        removeForces(territoryName, forceName, amount, toTanks, false);
    }

    /**
     * Removes forces from a Territory and adds them to the reserves or tanks
     *
     * @param territoryName The name of the Territory.
     * @param forceName     The name of the force.
     * @param amount        The amount of the force.
     * @param toTanks       Weather the force is going to the tanks or not.
     * @param isSpecial     Whether the force is special or not.
     */
    public void removeForces(String territoryName, String forceName, int amount, boolean toTanks, boolean isSpecial) {
        game.getTerritory(territoryName).removeForces(game, forceName, amount);
        if (toTanks) {
            game.getTleilaxuTanks().addForces(forceName, amount);
        } else {
            if (isSpecial)
                addSpecialReserves(amount);
            else
                addReserves(amount);
            setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        }
    }

    /**
     * Get the total spice that would be collected from a territory.  This function does not actually add or subtract
     * spice.  It only calculates the total
     *
     * @param territory The territory to calculate the spice from
     * @return The total spice that would be collected from the territory
     */
    public int getSpiceCollectedFromTerritory(Territory territory) {
        int multiplier = hasMiningEquipment() ? 3 : 2;
        int totalForces = territory.getTotalForceCount(this);
        return Math.min(multiplier * totalForces, territory.getSpice());
    }

    public Shipment getShipment() {
        if (shipment == null) this.shipment = new Shipment();
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public Movement getMovement() {
        if (movement == null) this.movement = new Movement();
        return movement;
    }

    public void setMovement(Movement movement) {
        this.movement = movement;
    }

    public void noFieldMessage(int noField, String territoryName) {
        getLedger().publish(
                MessageFormat.format(
                        "{0} {1} placed on {2}",
                        noField,
                        Emojis.NO_FIELD,
                        territoryName
                )
        );
    }

    public int getSpiceForAlly() {
        return spiceForAlly;
    }

    public void setSpiceForAlly(int spiceForAlly) {
        this.spiceForAlly = spiceForAlly;
    }

    public String getSpiceSupportPhasesString() {
        return " for bidding and shipping!";
    }

    public boolean isAllySpiceFinishedForTurn() {
        return allySpiceFinishedForTurn;
    }

    public void setAllySpiceFinishedForTurn(boolean allySpiceFinishedForTurn) {
        this.allySpiceFinishedForTurn = allySpiceFinishedForTurn;
    }

    public int getShippingSupport() {
        return spiceForAlly;
    }

    public void resetAllySpiceSupportAfterShipping(Game game) {
        if (hasAlly()) {
            Faction allyFaction = game.getFaction(ally);
            if (!(allyFaction instanceof EmperorFaction) && !(allyFaction instanceof ChoamFaction)) {
                allyFaction.setAllySpiceFinishedForTurn(true);
                if (allyFaction.getSpiceForAlly() != 0)
                    allyFaction.getChat().publish(Emojis.SPICE + " support for ally reset to 0 after ally completed shipping.");
                allyFaction.setSpiceForAlly(0);
                allyFaction.setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
            }
        }
    }

    public void resetAllySpiceSupportInMentatPause() {
        setAllySpiceFinishedForTurn(false);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        if (spiceForAlly != 0) {
            chat.publish(Emojis.SPICE + " support for ally is reset to 0 in Mentat Pause.");
            spiceForAlly = 0;
        }
    }

    public int getBattleSupport() {
        return 0;
    }

    public NexusCard getNexusCard() {
        return nexusCard;
    }

    public void setNexusCard(NexusCard nexusCard) {
        this.nexusCard = nexusCard;
    }

    public boolean hasNexusCard(String nexusCardName) {
        return nexusCard != null && nexusCard.name().equals(nexusCardName);
    }

    public boolean isGraphicDisplay() {
        return graphicDisplay;
    }

    public void setGraphicDisplay(boolean graphicDisplay) {
        this.graphicDisplay = graphicDisplay;
    }

    public int getMaxRevival() {
        return game.isRecruitsInPlay() ? 7 : maxRevival;
    }

    public void setMaxRevival(int maxRevival) {
        this.maxRevival = maxRevival;
    }

    public boolean isStarNotRevived() {
        return !starRevived;
    }

    public void setStarRevived(boolean starRevived) {
        this.starRevived = starRevived;
    }

    public boolean isPaidRevivalTBD() {
        return paidRevivalTBD;
    }

    public int getOccupiedIncome() {
        return occupiedIncome;
    }

    public String getHomeworld() {
        return homeworld;
    }

    public void setHomeworld(String homeworld) {
        this.homeworld = homeworld;
    }

    public int homeworldDialAdvantage(Game game, Territory territory) {
        String territoryName = territory.getTerritoryName();
        return game.hasGameOption(GameOption.HOMEWORLDS) && homeworld.equals(territoryName) ? 2 : 0;
    }

    public boolean isHighThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return true;
        return isHighThreshold;
    }

    protected HomeworldTerritory getHomeworldTerritory() {
        return (HomeworldTerritory) game.getTerritory(homeworld);
    }

    public boolean isHomeworldOccupied() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS))
            return false;
        return getHomeworldTerritory().getOccupierName() != null;
    }

    public Faction getOccupier() {
        if (isHomeworldOccupied())
            return getHomeworldTerritory().getOccupyingFaction();
        return null;
    }

    public void checkForHighThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS))
            return;
        if (getHomeworldTerritory().getOccupyingFaction() != null)
            isHighThreshold = false;
        else if (!isHighThreshold && getReservesStrength() + getSpecialReservesStrength() > lowThreshold) {
            game.getTurnSummary().publish(homeworld + " has flipped to High Threshold");
            isHighThreshold = true;
        }
    }

    public void checkForLowThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS))
            return;
        if (isHighThreshold && getReservesStrength() + getSpecialReservesStrength() < highThreshold) {
            game.getTurnSummary().publish(homeworld + " has flipped to Low Threshold.");
            isHighThreshold = false;
        }
    }

    public boolean hasOrnithoperToken() {
        if (ornithoperToken == null) this.ornithoperToken = false;
        return ornithoperToken;
    }

    public void setOrnithoperToken(boolean ornithoperToken) {
        this.ornithoperToken = ornithoperToken;
    }

    public void clearWhisperCounts() {
        whisperCount = null;
        whispersToTurnSummary = null;
    }

    public void clearWhisperCountsPerPhase() {
        whisperCountPerPhase = null;
        whispersToTurnSummaryPerPhase = null;
    }

    public String getWhisperTime(String recipientName) {
        if (lastWhisper == null)
            lastWhisper = new HashMap<>();
        if (whisperCount == null)
            whisperCount = new HashMap<>();
        if (whispersToTurnSummary == null)
            whispersToTurnSummary = new HashMap<>();
        if (whisperCountPerPhase == null)
            whisperCountPerPhase = new HashMap<>();
        if (whispersToTurnSummaryPerPhase == null)
            whispersToTurnSummaryPerPhase = new HashMap<>();
        return lastWhisper.get(recipientName);
    }

    public void sendWhisper(Faction recipient, String whisperedMessage, DuneTopic senderWhispers, DuneTopic recipientWhispers) {
        String fullMessage = emoji + " :speaking_head: " + whisperedMessage;
        recipientWhispers.publish(fullMessage);
        senderWhispers.publish(fullMessage);

        String recipientName = recipient.getName();
        String lastWhisperString = getWhisperTime(recipientName);
        whisperCount.putIfAbsent(recipientName, 0);
        whispersToTurnSummary.putIfAbsent(recipientName, 0);
        whisperCountPerPhase.putIfAbsent(recipientName, 0);
        whispersToTurnSummaryPerPhase.putIfAbsent(recipientName, 0);
        boolean announceWhisperToTurnSummary = lastWhisperString == null;
        if (lastWhisperString != null) {
            Instant lastWhisperTime = Instant.parse(lastWhisperString);
            if (lastWhisperTime.plus(120, ChronoUnit.MINUTES).isBefore(Instant.now()))
                announceWhisperToTurnSummary = true;
        }
        if (announceWhisperToTurnSummary) {
//            game.getTurnSummary().publish(emoji + " is whispering to " + recipient.getEmoji());
            whispersToTurnSummary.put(recipientName, whispersToTurnSummary.get(recipientName) + 1);
            whispersToTurnSummaryPerPhase.put(recipientName, whispersToTurnSummaryPerPhase.get(recipientName) + 1);
        }
        game.publishWhisper(emoji + " is whispering to " + recipient.getEmoji());
        whisperCount.put(recipientName, whisperCount.get(recipientName) + 1);
        whisperCountPerPhase.put(recipientName, whisperCountPerPhase.get(recipientName) + 1);
        lastWhisper.put(recipientName, Instant.now().toString());
    }

    public DuneTopic getLedger() {
        return ledger;
    }

    public void setLedger(DuneTopic ledger) {
        this.ledger = ledger;
    }

    public DuneTopic getChat() {
        return chat;
    }

    public void setChat(DuneTopic chat) {
        this.chat = chat;
    }

    public DuneTopic getAllianceThread() {
        return allianceThread;
    }

    public void setAllianceThread(DuneTopic allianceThread) {
        this.allianceThread = allianceThread;
    }

    public void addSpice(int spice, String message) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        if (spice == 0) return;
        this.spice += spice;
        spiceMessage(spice, message, true);
        setUpdated(UpdateType.SPICE_BACK);
        if (game.hasGameOption(GameOption.SPICE_PUBLIC))
            setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void subtractSpice(int spice, String message) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        if (spice == 0) return;
        this.spice -= spice;
        if (this.spice < 0) throw new IllegalStateException("Faction cannot spend more spice than they have.");
        spiceForAlly = Math.min(spiceForAlly, this.spice);
        spiceMessage(spice, message, false);
        setUpdated(UpdateType.SPICE_BACK);
        if (game.hasGameOption(GameOption.SPICE_PUBLIC))
            setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    private void subtractSpiceForAlly(int spice, String message) {
        // This method should remain private. If that changes, it should throw an exception if spice > spiceForAlly.
        spiceForAlly -= spice;
        subtractSpice(spice, message);
    }

    private void spiceMessage(int amount, String message, boolean plus) {
        if (amount == 0) return;
        String plusSign = plus ? "+" : "-";
        ledger.publish(
                MessageFormat.format(
                        "{0}{1} {2} {3} = {4} {5}",
                        plusSign,
                        amount,
                        Emojis.SPICE,
                        message,
                        spice,
                        Emojis.SPICE
                ));
    }

    public void selectTraitor(String traitorName) {
        TraitorCard traitor = traitorHand.stream().filter(
                        traitorCard -> traitorCard.getName().toLowerCase()
                                .contains(traitorName.toLowerCase())
                ).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Traitor not found"));
        for (TraitorCard card : traitorHand) {
            if (card.equals(traitor)) continue;
            game.getTraitorDeck().add(card);
            ledger.publish(card.getEmojiNameAndStrengthString() + " was sent back to the Traitor Deck.");
        }
        traitorHand.clear();
        addTraitorCard(traitor);

        Collections.shuffle(game.getTraitorDeck());

        ledger.publish(
                MessageFormat.format(
                        "{0} is in debt to you.  I'm sure they'll find a way to pay you back...",
                        traitor.getEmojiNameAndStrengthString()
                ));
    }

    public void discardTraitor(String traitorName, boolean reveal) {
        TraitorCard traitor = traitorHand.stream().filter(
                        traitorCard -> traitorCard.getName().toLowerCase()
                                .contains(traitorName.toLowerCase())
                ).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Traitor not found"));
        traitorHand.remove(traitor);
        game.getTraitorDeck().add(traitor);
        game.shuffleTraitorDeck();
        ledger.publish(traitor.getEmojiNameAndStrengthString() + " was sent back to the Traitor Deck.");
        if (reveal)
            game.getTurnSummary().publish(emoji + " reveals and discards the " + traitor.getEmojiNameAndStrengthString() + " Traitor card.");
        else
            game.getTurnSummary().publish(emoji + " has discarded a Traitor card.");
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void drawTwoTraitorsWithRihaniDecipherer(String reason) {
        // Can remove this list creation after games 77, 78, 82, 83, 85, 86, and 87 end
        if (traitorsUsed == null)
            traitorsUsed = new ArrayList<>();

        LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();
        Collections.shuffle(traitorDeck);
        List<TraitorCard> drawnTraitors = new ArrayList<>();
        drawnTraitors.add(traitorDeck.pop());
        drawnTraitors.add(traitorDeck.pop());
        drawnTraitors.forEach(this::addTraitorCard);
        chat.publish("You must discard two Traitors. " + player);
        List<DuneChoice> choices = traitorHand.stream().map(t -> new DuneChoice("traitor-reveal-and-discard-" + t.getName(), t.getName(), traitorsUsed.stream().anyMatch(tu -> tu.equals(t.getName())))).toList();
        chat.publish("Reveal and discard an unused traitor:", choices);
        choices = drawnTraitors.stream().map(t -> new DuneChoice("traitor-discard-" + t.getName(), t.getName())).toList();
        chat.publish("Discard a traitor just drawn:", choices);
        game.getTurnSummary().publish(emoji + " has drawn 2 Traitor cards for " + reason + ".");
    }

    public void drawTwoTraitorsWithHarkonnenSecretAlly(String reason) {
        LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();
        Collections.shuffle(traitorDeck);
        List<TraitorCard> drawnTraitors = new ArrayList<>();
        drawnTraitors.add(traitorDeck.pop());
        drawnTraitors.add(traitorDeck.pop());
        drawnTraitors.forEach(this::addTraitorCard);
        chat.publish("You must discard two Traitors. " + player);
        List<DuneChoice> choices = traitorHand.stream().map(t -> new DuneChoice("traitor-discard-" + t.getName(), t.getName())).toList();
        chat.publish("First discard:", choices);
        chat.publish("Second discard:", choices);
        game.getTurnSummary().publish(emoji + " has drawn 2 Traitor cards for " + reason + ".");
    }

    public void presentStartingForcesChoices() throws InvalidGameStateException {
        throw new InvalidGameStateException(name + " does not need to place starting forces now.");
    }

    public void presentStartingForcesExecutionChoices() throws InvalidGameStateException {
        throw new InvalidGameStateException(name + " does not need to place starting forces now.");
    }

    /**
     * Places starting forces in the specified territory.
     *
     * @return true if placement is complete, false if more forces need to be placed.
     */
    public boolean placeChosenStartingForces() throws InvalidGameStateException {
        throw new InvalidGameStateException(name + " does not need to place starting forces now.");
    }

    public int countFreeStarredRevival() {
        return 0;
    }

    public int performFreeRevivals() {
        int numStarRevived = countFreeStarredRevival();
        starRevived = numStarRevived > 0;
        TleilaxuTanks tanks = game.getTleilaxuTanks();
        int numRegularRevived = Math.min(getFreeRevival() - numStarRevived, tanks.getForceStrength(name));
        if (numRegularRevived + numStarRevived > 0)
            game.reviveForces(this, false, numRegularRevived, numStarRevived);
        return numRegularRevived + numStarRevived;
    }

    public int baseRevivalCost(int regular, int starred) {
        return regular * 2 + starred * 2;
    }

    public int revivalCost(int regular, int starred) {
        int cost = baseRevivalCost(regular, starred);
        if (getAlly().equals("BT"))
            cost = Math.ceilDiv(cost, 2);
        return cost;
    }

    protected int getRevivableForces() {
        return game.getTleilaxuTanks().getForceStrength(name);
    }

    protected String paidRevivalMessage() {
        return "Would you like to purchase additional revivals? " + player;
    }

    /**
     * Give player choices for number of pad revivals
     *
     * @param numRevived  The number revived for free.
     * @return True if the faction can buy more revivals, false if not.
     */
    public boolean presentPaidRevivalChoices(int numRevived) throws InvalidGameStateException {
        paidRevivalMessage = null;
        paidRevivalTBD = false;
        if (getMaxRevival() > numRevived) {
            int revivableForces = getRevivableForces();
            if (revivableForces > 0) {
                paidRevivalTBD = true;
                List<DuneChoice> choices = new ArrayList<>();
                int maxButton = Math.min(revivableForces, getMaxRevival() - numRevived);
                for (int i = 0; i <= maxButton; i++) {
                    DuneChoice choice = new DuneChoice("revival-forces-" + i, Integer.toString(i));
                    choice.setDisabled(spice < revivalCost(i, 0));
                    choices.add(choice);
                }
                chat.publish(paidRevivalMessage(), choices);
            } else {
                paidRevivalMessage = getNoRevivableForcesMessage();
            }
        } else {
            paidRevivalMessage = getRevivedMaximumMessage();
        }
        return paidRevivalTBD;
    }

    public String getNoRevivableForcesMessage() {
        return emoji + " has no forces in the tanks";
    }

    public String getRevivedMaximumMessage() {
        return emoji + " has revived their maximum";
    }

    public String getPaidRevivalMessage() {
        return paidRevivalMessage;
    }

    /**
     * Just revive the leader. Calling function handles payment and messaging.
     *
     * @param isPaid indicates if faction must pay spice for the revival
     * @param numForces the number of forces to revive
     */
    public void reviveForces(boolean isPaid, int numForces) {
        paidRevivalTBD = false;
        if (numForces == 0)
            game.getTurnSummary().publish(emoji + " does not purchase additional revivals.");
        else
            game.reviveForces(this, isPaid, numForces, 0);
    }

    /**
     * Execute shipping as described in the faction's shipment object
     *
     * @param game    The game instance
     * @param karama  True if faction is playing karama for Guild shipping rate
     * @param free    True if unpaid due to Guild ambassador, BT High Treshold
     */
    public void executeShipment(Game game, boolean karama, boolean free) throws InvalidGameStateException {
        int totalForces = shipment.getForce() + shipment.getSpecialForce();
        String territoryName = shipment.getTerritoryName();
        Territory territory = game.getTerritory(territoryName);
        int spiceNeeded = free ? 0 : game.shipmentCost(this, totalForces, territory, karama, !shipment.getCrossShipFrom().isEmpty());
        int spiceFromAlly = hasAlly() ? game.getFaction(ally).getShippingSupport() : 0;
        if (spiceNeeded > spice + spiceFromAlly)
            throw new InvalidGameStateException("You cannot afford this shipment.");

        int noField = shipment.getNoField();
        int force = shipment.getForce();
        int specialForce = shipment.getSpecialForce();
        String crossShipFrom = shipment.getCrossShipFrom();
        if (shipment.isToReserves()) {
            game.removeForces(territoryName, this, force, specialForce, false);
            int spice = Math.ceilDiv(force, 2);
            subtractSpice(spice, "shipment from " + territoryName + " back to reserves");
            game.getTurnSummary().publish(Emojis.GUILD + " ship " + force + " " + Emojis.getForceEmoji("Guild") + " from " + territoryName + " to reserves for " + spice + " " + Emojis.SPICE + " paid to the bank.");
        } else {
            if (territory.factionMustMoveOut(game, this))
                movement.setMustMoveOutOf(territoryName);
            if (noField >= 0) {
                game.getRicheseFaction().shipNoField(this, territory, noField, karama, !crossShipFrom.isEmpty(), force);
            } else if (!crossShipFrom.isEmpty()) {
                game.removeForces(crossShipFrom, this, force, specialForce, false);
                placeForces(territory, force, specialForce, true, true, true, false, true);
                game.getTurnSummary().publish(emoji + " cross shipped from " + crossShipFrom + " to " + territoryName);
            } else if (force > 0 || specialForce > 0)
                placeForces(territory, force, specialForce, !free, true, true, karama, false);
        }
        game.setUpdated(UpdateType.MAP);
        shipment.clear();
        if (free)
            shipment.setShipped(false);
        else
            resetAllySpiceSupportAfterShipping(game);
    }

    public void loseDukeVidalToMoritani() {
        if (getLeader("Duke Vidal").isEmpty())
            return;
        removeLeader("Duke Vidal");
        chat.publish("Duke Vidal has left to fight for the " + Emojis.MORITANI + "!");
    }

    public void executeMovement(Game game) throws InvalidGameStateException {
        String movingFrom = movement.getMovingFrom();
        String movingTo = movement.getMovingTo();
        boolean movingNoField = movement.isMovingNoField();
        int force = movement.getForce();
        int specialForce = movement.getSpecialForce();
        int secondForce = movement.getSecondForce();
        int secondSpecialForce = movement.getSecondSpecialForce();
        String secondMovingFrom = movement.getSecondMovingFrom();
        Territory from = game.getTerritory(movingFrom);
        Territory to = game.getTerritory(movingTo);
        if (movingNoField) {
            game.getRicheseFaction().moveNoField(movingTo, false);
            game.moveForces(this, from, to, movingTo, secondMovingFrom, force, specialForce, secondForce, secondSpecialForce, true);
        } else {
            game.moveForces(this, from, to, movingTo, secondMovingFrom, force, specialForce, secondForce, secondSpecialForce, false);
        }
        movement.clear();
        setUpdated(UpdateType.MAP);
    }

    public void withdrawForces(int regularForces, int starredForces, List<Territory> sectorsToWithdrawFrom, String reason) {
        int regularLeftToWithdraw = regularForces;
        int starredLeftToWithdraw = starredForces;
        for (Territory t : sectorsToWithdrawFrom) {
            if (regularLeftToWithdraw == 0 && starredLeftToWithdraw == 0)
                break;
            int regularPresent = t.getForceStrength(name);
            int starredPresent = t.getForceStrength(name + "*");
            int regularToWithdrawNow = Math.min(regularLeftToWithdraw, regularPresent);
            int starredToWithdrawNow = Math.min(starredLeftToWithdraw, starredPresent);
            regularLeftToWithdraw -= regularToWithdrawNow;
            starredLeftToWithdraw -= starredToWithdrawNow;
            if (regularToWithdrawNow > 0 || starredToWithdrawNow > 0) {
                game.removeForces(t.getTerritoryName(), this, regularToWithdrawNow, starredToWithdrawNow, false);
                game.getTurnSummary().publish(forcesString(regularToWithdrawNow, starredToWithdrawNow) + " returned to reserves with " + reason + ".");
            }
        }
    }

    public void acceptTerrorAlliance(Faction moritani, String territoryName, String terror) throws InvalidGameStateException {
        chat.reply("You have sent the emissary away with news of their new alliance!");
        game.getTerritory(territoryName).removeTerrorToken(game, terror, true);
        game.createAlliance(moritani, this);
        // Alliance threads are created in controller.Alliance. When that moves to model, it can be done here and tested.
        // controller = Discord side, model = Dune logic side
    }

    public void denyTerrorAlliance(String territoryName, String terror) throws InvalidGameStateException {
        Territory territory = game.getTerritory(territoryName);
        chat.reply("You have sent the emissary away empty-handed. Time to prepare for the worst.");
        MoritaniFaction moritani = game.getMoritaniFaction();
        moritani.getChat().publish("Your ambassador has returned with news that no alliance will take place.");
        moritani.triggerTerrorToken(this, territory, terror);
    }

    protected void presentExtortionChoices() {
        if (spice >= 3) {
            List<DuneChoice> choices = new ArrayList<>();
            choices.add(new DuneChoice("faction-pay-extortion", "Yes"));
            choices.add(new DuneChoice("faction-decline-extortion", "No"));
            chat.publish("Will you pay " + Emojis.MORITANI + " 3 " + Emojis.SPICE + " to remove the Extortion token from the game? " + player, choices);
        } else {
            chat.publish("You do not have enough spice to pay Extortion.");
        }
    }

    public void performMentatPauseActions(boolean extortionTokenTriggered) {
        collectFrontOfShieldSpice();
        resetAllySpiceSupportInMentatPause();
        if (decliningCharity && spice < 2)
            chat.publish("You have only " + spice + " " + Emojis.SPICE + " but are declining CHOAM charity.\nYou must change this in your info channel if you want to receive charity. " + player);

        DuneTopic modInfo = game.getModInfo();
        for (TreacheryCard card : treacheryHand) {
            if (card.name().equalsIgnoreCase("Weather Control")) {
                modInfo.publish(emoji + " has Weather Control.");
            } else if (card.name().equalsIgnoreCase("Family Atomics")) {
                modInfo.publish(emoji + " has Family Atomics.");
            }
        }

        if (extortionTokenTriggered)
            presentExtortionChoices();
    }

    public void payExtortion() {
        MentatPause mentatPause = game.getMentatPause();
        if (mentatPause == null || mentatPause.isExtortionInactive())
            chat.reply("Extortion has already been resolved. You were willing to pay.");
        else if (spice >= 3) {
            game.getMentatPause().factionWouldPayExtortion(game, this);
            chat.reply("You are willing to pay Extortion.");
        } else {
            game.getMentatPause().factionDeclinesExtortion(game, this);
            chat.reply("You are willing to pay Extortion but do not have enough spice.");
        }
    }

    public void declineExtortion() {
        MentatPause mentatPause = game.getMentatPause();
        if (mentatPause == null || mentatPause.isExtortionInactive())
            chat.reply("Extortion has already been resolved. You were not willing to pay.");
        else {
            game.getMentatPause().factionDeclinesExtortion(game, this);
            chat.reply("You will not pay Extortion.");
        }
    }
}
