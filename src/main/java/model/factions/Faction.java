package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import helpers.Exclude;
import model.*;
import model.topics.DuneTopic;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;

public class Faction {
    private final String name;
    private final List<TechToken> techTokens;
    private final List<TreacheryCard> treacheryHand;
    private final List<TraitorCard> traitorHand;
    private final List<LeaderSkillCard> leaderSkillsHand;
    private final List<StrongholdCard> strongholdCards;
    private final List<Long> buttonMessages;
    private final List<Leader> leaders;
    @Exclude
    public Force reserves;
    protected String emoji;
    protected int handLimit;
    protected int spice;
    protected int freeRevival;
    protected boolean hasMiningEquipment;
    protected int highThreshold;
    protected int lowThreshold;
    protected int occupiedIncome;
    protected String homeworld;
    @Exclude
    protected Force specialReserves;
    @Exclude
    private Set<UpdateType> updateTypes;
    private String player;
    private String userName;
    private boolean graphicDisplay;
    private int frontOfShieldSpice;
    private String ally;
    private String bid;
    private int maxBid;
    private boolean useExact;
    private boolean autoBid;
    private boolean outbidAlly;
    private boolean specialKaramaPowerUsed;
    private NexusCard nexusCard;
    private Shipment shipment;
    private Movement movement;
    private int allySpiceShipment;
    private int allySpiceBidding;
    private int maxRevival;
    private boolean isHighThreshold;
    private Boolean ornithoperToken;
    @Exclude
    private Game game;
    @Exclude
    private DuneTopic ledger;

    public Faction(String name, String player, String userName, Game game) throws IOException {
        this.handLimit = 4;
        this.name = name;
        this.player = player;
        this.userName = userName;
        this.graphicDisplay = false;
        this.treacheryHand = new LinkedList<>();
        this.frontOfShieldSpice = 0;
        this.hasMiningEquipment = false;
        this.buttonMessages = new LinkedList<>();

        this.traitorHand = new LinkedList<>();
        this.leaders = new LinkedList<>();
        this.techTokens = new LinkedList<>();
        this.leaderSkillsHand = new LinkedList<>();
        this.strongholdCards = new LinkedList<>();
        this.spice = 0;
        this.bid = "";
        this.autoBid = false;
        this.useExact = true;
        this.outbidAlly = false;
        this.specialKaramaPowerUsed = false;
        this.shipment = new Shipment();
        this.movement = new Movement();
        this.allySpiceShipment = 0;
        this.allySpiceBidding = 0;
        this.nexusCard = null;
        this.maxRevival = 3;
        this.isHighThreshold = true;
        this.ornithoperToken = false;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));
        CSVParser csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);

        LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();

        for (CSVRecord csvRecord : csvParser) {
            if (csvRecord.get(0).equals(this.getName())) {
                TraitorCard traitorCard = new TraitorCard(
                        csvRecord.get(1),
                        csvRecord.get(0),
                        Integer.parseInt(csvRecord.get(2))
                );
                this.leaders.add(new Leader(csvRecord.get(1), Integer.parseInt(csvRecord.get(2)), null, false));
                traitorDeck.add(traitorCard);
            }
        }

        this.game = game;
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
    }

    public String getName() {
        return this.name;
    }

    public String getEmoji() {
        return emoji;
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
    }

    public void removeAlly() {
        ally = null;
    }

    public boolean hasAlly() {
        return ally != null;
    }

    public int getHandLimit() {
        if (game.hasGameOption(GameOption.HOMEWORLDS) && game.hasFaction("CHOAM")
        && game.getFaction("CHOAM").isHomeworldOccupied() && !name.equals("CHOAM")
        && (game.getTerritory("Tupile").getActiveFactionNames().contains(name)
        || game.getTerritory("Tupile").getActiveFactionNames().contains(ally))) return handLimit + 1;
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
     * Gets the most recently added treachery card from the Faction's hand.
     * @return Treachery Card
     */
    public TreacheryCard getLastTreacheryCard() {
        return treacheryHand.get(treacheryHand.size() - 1);
    }

    public TreacheryCard removeTreacheryCard(String name) {
        return removeTreacheryCard(getTreacheryCard(name));
    }

    public TreacheryCard removeTreacheryCard(TreacheryCard card) {
        treacheryHand.remove(card);
        setUpdated(UpdateType.TREACHERY_CARDS);
        return card;
    }

    public void addTraitorCard(TraitorCard card) {
        traitorHand.add(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeTraitorCard(TraitorCard card) {
        traitorHand.remove(card);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public List<TraitorCard> getTraitorHand() {
        return traitorHand;
    }

    public List<LeaderSkillCard> getLeaderSkillsHand() {
        return leaderSkillsHand;
    }

    public List<StrongholdCard> getStrongholdCards() {
        return strongholdCards;
    }

    public void addStrongholdCard(StrongholdCard card) {
        strongholdCards.add(card);
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void removeAllStrongholdCards() {
        strongholdCards.clear();
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
        setUpdated(UpdateType.SPICE_BACK);
    }

    public void addSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.spice += spice;
        setUpdated(UpdateType.SPICE_BACK);
    }

    public void subtractSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.spice -= spice;
        if (this.spice < 0) throw new IllegalStateException("Faction cannot spend more spice than they have.");
        setUpdated(UpdateType.SPICE_BACK);
    }

    public Force getReserves() {
        if (reserves != null) return reserves;
        return game.getTerritory(homeworld).getForce(name);
    }

    public void addReserves(int amount) {
        getReserves().addStrength(amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeReserves(int amount) {
        getReserves().removeStrength(amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public Force getSpecialReserves() {
        if (specialReserves != null) return specialReserves;
        return game.getTerritory(homeworld).getForce(name + "*");
    }

    public void addSpecialReserves(int amount) {
        getSpecialReserves().addStrength(amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void removeSpecialReserves(int amount) {
        getSpecialReserves().removeStrength(amount);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public int getFrontOfShieldSpice() {
        return frontOfShieldSpice;
    }

    public void setFrontOfShieldSpice(int frontOfShieldSpice) {
        this.frontOfShieldSpice = frontOfShieldSpice;
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public int getFreeRevival() {
        return freeRevival;
    }

    public Optional<Leader> getLeader(String leaderName) {
        return getLeaders().stream()
                .filter(l -> l.name().equalsIgnoreCase(leaderName))
                .findFirst();
    }

    public List<Leader> getLeaders() {
        return leaders;
    }

    public List<Leader> getSkilledLeaders() {
        return getLeaders().stream().filter(l -> l.skillCard() != null).toList();
    }

    public Leader removeLeader(String name) {
        Leader remove = leaders.stream()
                .filter(l -> l.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Leader not found."));

        leaders.remove(remove);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);

        return remove;
    }

    public void removeLeader(Leader leader) {
        getLeaders().remove(leader);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        if (leader.skillCard() != null) {
            setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
        }
    }

    public void addLeader(Leader leader) {
        getLeaders().add(leader);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void addFrontOfShieldSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.frontOfShieldSpice += spice;
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public void subtractFrontOfShieldSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
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

    public List<TechToken> getTechTokens() {
        return techTokens;
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

    public void setUseExact(boolean useExact) {
        this.useExact = useExact;
    }

    public boolean isAutoBid() {
        return autoBid;
    }

    public void setAutoBid(boolean autoBid) {
        this.autoBid = autoBid;
    }

    public boolean isOutbidAlly() {
        return outbidAlly;
    }

    public void setOutbidAlly(boolean outbidAlly) {
        this.outbidAlly = outbidAlly;
    }

    public boolean isSpecialKaramaPowerUsed() {
        return specialKaramaPowerUsed;
    }

    public void setSpecialKaramaPowerUsed(boolean specialKaramaPowerUsed) {
        this.specialKaramaPowerUsed = specialKaramaPowerUsed;
    }

    public boolean isNearShieldWall() {
        for (Territory territory : game.getTerritories().values()) {
            if (territory.isNearShieldWall() &&
                    (territory.getForce(name).getStrength() > 0 || territory.getForce(name + "*").getStrength() > 0)) {
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
        removeForces(territoryName, forceName, amount, toTanks, false, forceName);
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
    public void removeForces(String territoryName, String forceName, int amount, boolean toTanks, boolean isSpecial,
                             String targetForceName) {
        Territory territory = game.getTerritory(territoryName);

        Force force = territory.getForce(forceName);
        int forceStrength = force.getStrength();

        if (forceStrength < amount) throw new IllegalArgumentException("Not enough forces in territory.");

        if (forceStrength == amount) {
            territory.removeForce(forceName);
        } else {
            force.setStrength(forceStrength - amount);
        }

        if (toTanks) {
            game.getForceFromTanks(targetForceName).addStrength(amount);
        } else {
            if (isSpecial) {
                if (game.getTerritory(homeworld).hasForce(name + "*"))
                    game.getTerritory(homeworld).getForce(name + "*").addStrength(amount);
                else game.getTerritory(homeworld).addForce(new Force(name + "*", amount));
            } else {
                if (game.getTerritory(homeworld).hasForce(name))
                    game.getTerritory(homeworld).getForce(name).addStrength(amount);
                else game.getTerritory(homeworld).addForce(new Force(name, amount));
            }
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
        int totalForces = territory.getForces(this).stream().map(Force::getStrength).reduce(0, Integer::sum);
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

    public int getAllySpiceShipment() {
        return allySpiceShipment;
    }

    public void setAllySpiceShipment(int allySpiceShipment) {
        this.allySpiceShipment = allySpiceShipment;
    }

    public int getAllySpiceBidding() {
        return allySpiceBidding;
    }

    public void setAllySpiceBidding(int allySpiceBidding) {
        this.allySpiceBidding = allySpiceBidding;
    }

    public NexusCard getNexusCard() {
        return nexusCard;
    }

    public void setNexusCard(NexusCard nexusCard) {
        this.nexusCard = nexusCard;
    }

    public boolean isGraphicDisplay() {
        return graphicDisplay;
    }

    public void setGraphicDisplay(boolean graphicDisplay) {
        this.graphicDisplay = graphicDisplay;
    }

    public List<Long> getButtonMessages() {
        return buttonMessages;
    }

    public int getMaxRevival() {
        return maxRevival;
    }

    public void setMaxRevival(int maxRevival) {
        this.maxRevival = maxRevival;
    }

    public int getHighThreshold() {
        return highThreshold;
    }

    public int getLowThreshold() {
        return lowThreshold;
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

    public boolean isHighThreshold() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return true;
        return isHighThreshold;
    }

    public void setHighThreshold(boolean highThreshold) {
        isHighThreshold = highThreshold;
    }

    public boolean hasOrnithoperToken() {
        if (ornithoperToken == null) this.ornithoperToken = false;
        return ornithoperToken;
    }

    public void setOrnithoperToken(boolean ornithoperToken) {
        this.ornithoperToken = ornithoperToken;
    }

    public boolean isHomeworldOccupied() {
        if (!game.hasGameOption(GameOption.HOMEWORLDS)) return false;
        return game.getTerritory(homeworld).countActiveFactions() == 1 && !game.getTerritory(homeworld).getActiveFactions(game).get(0).getName().equals(this.name);
    }

    public Faction getOccupier() {
        if (isHomeworldOccupied()) return game.getTerritory(homeworld).getActiveFactions(game).get(0);
        return null;
    }

    public void setLedger(DuneTopic ledger) {
        this.ledger = ledger;
    }

    public DuneTopic getLedger() {
        return ledger;
    }

    public void spiceMessage(int amount, int newTotal, String message, boolean plus) {
        String plusSign = plus ? "+" : "-";
        ledger.publish(
                MessageFormat.format(
                        "{0}{1}{2} {3} = {4}{5}",
                        plusSign,
                        amount,
                        Emojis.SPICE,
                        message,
                        newTotal,
                        Emojis.SPICE
                ));
    }

    public void selectTraitor(String traitorName) {
        TraitorCard traitor = traitorHand.stream().filter(
                        traitorCard -> traitorCard.name().toLowerCase()
                                .contains(traitorName.toLowerCase())
                ).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Traitor not found"));
        for (TraitorCard card : traitorHand) {
            if (card.equals(traitor)) continue;
            game.getTraitorDeck().add(card);
            ledger.publish(card.name() + " was sent back to the Traitor Deck.");
        }
        traitorHand.clear();
        addTraitorCard(traitor);

        Collections.shuffle(game.getTraitorDeck());

        ledger.publish(
                MessageFormat.format(
                        "{0} is in debt to you.  I'm sure they'll find a way to pay you back...",
                        traitor.name()
                ));
    }
}
