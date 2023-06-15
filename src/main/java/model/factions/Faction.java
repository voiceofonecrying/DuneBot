package model.factions;

import model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Faction {
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
    private final List<Leader> leaders;
    protected final List<Resource> resources;

    public Faction(String name, String player, String userName, Game game) {
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
        this.spice = 0;
        this.bid = "0";
        this.autoBid = false;
        this.useExact = true;
        this.outbidAlly = false;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));
        CSVParser csvParser = null;
        try {
            csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
    }

    public Resource getResource(String name) {
        return resources.stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .get();
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

    public void setEmoji(String emoji) {
        this.emoji = emoji;
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

    public void addTreacheryCard(TreacheryCard card) {
        treacheryHand.add(card);
    }

    public void removeTreacheryCard(String cardName) {
        TreacheryCard remove = null;
        for (TreacheryCard card : treacheryHand) {
            if (card.name().equals(cardName)) {
               remove = card;
            }
        }
        if (remove != null) treacheryHand.remove(remove);
        else throw new IllegalArgumentException("Card not found");
    }

    public List<TraitorCard> getTraitorHand() {
        return traitorHand;
    }

    public List<LeaderSkillCard> getLeaderSkillsHand() {
        return leaderSkillsHand;
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
    }

    public void addSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.spice += spice;
    }

    public void subtractSpice(int spice) {
        this.spice -= Math.abs(spice);
        if (this.spice < 0) throw new IllegalStateException("Faction cannot spend more spice than they have.");
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public void removeResource(String resourceName) {
        resources.removeAll(getResources(resourceName));
    }

    public Force getReserves() {
        return reserves;
    }

    public Force getSpecialReserves() {
        return specialReserves;
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
        Leader remove = null;
        for (Leader leader : leaders) {
            if (leader.name().equals(name)) {
                remove = leader;
            }
        }
        if (remove == null) throw new IllegalArgumentException("Leader not found.");
        leaders.remove(remove);
        return remove;
    }

    public Leader removeLeader(Leader leader) {
        getLeaders().remove(leader);

        return leader;
    }

    public void addLeader(Leader leader) {
        getLeaders().add(leader);
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setFrontOfShieldSpice(int frontOfShieldSpice) {
        this.frontOfShieldSpice = frontOfShieldSpice;
    }

    public void addFrontOfShieldSpice(int amount) {
        this.frontOfShieldSpice += amount;
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
}