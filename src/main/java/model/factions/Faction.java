package model.factions;

import helpers.Exclude;
import model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Faction {
    @Exclude
    private boolean backOfShieldModified;
    @Exclude
    private boolean frontOfShieldModified;
    private final String name;
    protected String emoji;
    private String player;
    private final String userName;
    protected int handLimit;
    protected int spice;
    private final List<TechToken> techTokens;
    protected Force reserves;
    protected Force specialReserves;
    private int frontOfShieldSpice;
    protected int freeRevival;
    protected boolean hasMiningEquipment;
    private String ally;
    private String bid;

    private int maxBid;
    private boolean useExact;
    private boolean autoBid;
    private boolean outbidAlly;
    private final List<TreacheryCard> treacheryHand;
    private final List<TraitorCard> traitorHand;
    private final List<LeaderSkillCard> leaderSkillsHand;
    private final List<StrongholdCard> strongholdCards;
    private final List<Leader> leaders;
    protected final List<Resource> resources;

    private Game game;

    public Faction(String name, String player, String userName, Game game) throws IOException {
        this.handLimit = 4;
        this.name = name;
        this.player = player;
        this.userName = userName;
        this.treacheryHand = new LinkedList<>();
        this.frontOfShieldSpice = 0;
        this.hasMiningEquipment = false;

        this.traitorHand = new LinkedList<>();
        this.leaders = new LinkedList<>();
        this.resources = new LinkedList<>();
        this.techTokens = new LinkedList<>();
        this.leaderSkillsHand = new LinkedList<>();
        this.strongholdCards = new LinkedList<>();
        this.spice = 0;
        this.bid = "0";
        this.autoBid = false;
        this.useExact = true;
        this.outbidAlly = false;
        this.specialReserves = new Force("", 0);

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

    public void setFrontOfShieldModified() {
        setFrontOfShieldModified(true);
    }

    public void setFrontOfShieldModified(boolean frontOfShieldModified) {
        this.frontOfShieldModified = frontOfShieldModified;
    }

    public boolean isFrontOfShieldModified() {
        return this.frontOfShieldModified;
    }

    public void setBackOfShieldModified() {
        setBackOfShieldModified(true);
    }

    public void setBackOfShieldModified(boolean backOfShieldModified) {
        this.backOfShieldModified = backOfShieldModified;
    }

    public boolean isBackOfShieldModified() {
        return this.backOfShieldModified;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public Resource getResource(String name) {
        return resources.stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
    }

    public List<Resource> getResources(String name) {
        return resources.stream()
                .filter(r -> r.getName().equals(name))
                .toList();
    }

    public boolean hasResource(String name) {
        return resources.stream()
                .anyMatch(r -> r.getName().equals(name));
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

    public String getAlly() {
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
        return handLimit;
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
        if (treacheryHand.size() >= handLimit) {
            throw new IllegalStateException("Hand limit reached");
        }

        treacheryHand.add(card);
        setBackOfShieldModified();
    }

    public TreacheryCard removeTreacheryCard(String name) {
        return removeTreacheryCard(getTreacheryCard(name));
    }

    public TreacheryCard removeTreacheryCard(TreacheryCard card) {
        treacheryHand.remove(card);
        setBackOfShieldModified();
        return card;
    }

    public void addTraitorCard(TraitorCard card) {
        traitorHand.add(card);
        setBackOfShieldModified();
    }

    public TraitorCard removeTraitorCard(TraitorCard card) {
        traitorHand.remove(card);
        setBackOfShieldModified();
        return card;
    }

    public List<TraitorCard> getTraitorHand() {
        return traitorHand;
    }

    public List<LeaderSkillCard> getLeaderSkillsHand() {
        return leaderSkillsHand;
    }

    public List<StrongholdCard> getStrongholdCards() {return strongholdCards;}

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
        setBackOfShieldModified();
    }

    public void addSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.spice += spice;
        setBackOfShieldModified();
    }

    public void subtractSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.spice -= spice;
        if (this.spice < 0) throw new IllegalStateException("Faction cannot spend more spice than they have.");
        setBackOfShieldModified();
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public void removeResource(String resourceName) {
        resources.removeAll(getResources(resourceName));
        setFrontOfShieldModified();
    }

    public Force getReserves() {
        return reserves;
    }
    public void addReserves(int amount) {
        getReserves().addStrength(amount);
        setBackOfShieldModified();
    }

    public void removeReserves(int amount) {
        getReserves().removeStrength(amount);
        setBackOfShieldModified();
    }

    public Force getSpecialReserves() {
        return specialReserves == null ? new Force("", 0): specialReserves;
    }

    public void addSpecialReserves(int amount) {
        getSpecialReserves().addStrength(amount);
        setBackOfShieldModified();
    }

    public void removeSpecialReserves(int amount) {
        getSpecialReserves().removeStrength(amount);
        setBackOfShieldModified();
    }

    public int getFrontOfShieldSpice() {
        return frontOfShieldSpice;
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
        setBackOfShieldModified();
        return remove;
    }

    public void removeLeader(Leader leader) {
        getLeaders().remove(leader);
        setBackOfShieldModified();
        if (leader.skillCard() != null) {
            setFrontOfShieldModified();
        }
    }

    public void addLeader(Leader leader) {
        getLeaders().add(leader);
        setBackOfShieldModified();
    }

    public void setFrontOfShieldSpice(int frontOfShieldSpice) {
        this.frontOfShieldSpice = frontOfShieldSpice;
        setFrontOfShieldModified();
    }

    public void addFrontOfShieldSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.frontOfShieldSpice += spice;
        setFrontOfShieldModified();
    }

    public void subtractFrontOfShieldSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.frontOfShieldSpice -= spice;
        if (this.frontOfShieldSpice < 0) throw new IllegalStateException("Faction cannot spend more spice than they have.");
        setFrontOfShieldModified();
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


    /**
     * Adds forces from a Territory to the reserves or tanks
     * @param territoryName The name of the Territory.
     * @param amount The amount of the force.
     * @param isSpecial Whether the force is special or not.
     * @param toTanks Whether the force is going to the tanks or not.
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
     * @param territoryName The name of the Territory.
     * @param forceName The name of the force.
     * @param amount The amount of the force.
     * @param toTanks Weather the force is going to the tanks or not.
     * @param isSpecial Whether the force is special or not.
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
                specialReserves.addStrength(amount);
            } else {
                reserves.addStrength(amount);
            }
            setBackOfShieldModified();
        }

    }

}