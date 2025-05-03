package model.factions;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class BTFaction extends Faction {
    private Set<TraitorCard> revealedFaceDancers;
    private List<String> factionsNeedingRevivalLimit;
    private boolean btHTActive;
    private int numFreeRevivals;

    public BTFaction(String player, String userName) throws IOException {
        super("BT", player, userName);

        this.spice = 5;
        this.freeRevival = 2;
        this.maxRevival = 20;
        this.emoji = Emojis.BT;
        this.factionsNeedingRevivalLimit = new ArrayList<>();
        this.highThreshold = 9;
        this.lowThreshold = 8;
        this.occupiedIncome = 2;
        this.homeworld = "Tleilax";
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        Territory tleilax = game.getTerritories().addHomeworld(game, homeworld, name);
        tleilax.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    @Override
    public int getMaxRevival() {
        return maxRevival;
    }

    @Override
    public void setMaxRevival(int maxRevival) {
        // BT revival limit always remains 20
        this.maxRevival = 20;
    }

    public void drawFaceDancer() {
        game.drawCard("traitor deck", "BT");
        ledger.publish(traitorHand.getLast().getEmojiNameAndStrengthString() + " is your new Face Dancer!");
    }

    public void drawFaceDancers() throws InvalidGameStateException {
        if (!getTraitorHand().isEmpty())
            throw new InvalidGameStateException("New Face Dancers cannot be drawn until all have been revealed.");

        if (revealedFaceDancers != null) {
            revealedFaceDancers.forEach(fd -> game.getTraitorDeck().add(fd));
            game.getTurnSummary().publish(Emojis.BT + " revealed all of their Face Dancers and drew a new set of 3.");
        } else
            game.getTurnSummary().publish(Emojis.BT + " have drawn their Face Dancers.");
        revealedFaceDancers = null;
        Collections.shuffle(game.getTraitorDeck());
        drawFaceDancer();
        drawFaceDancer();
        drawFaceDancer();
    }

    public void nexusCardCunning() {
        if (revealedFaceDancers != null) {
            Collections.shuffle(game.getTraitorDeck());
            for (int i = 0; i < revealedFaceDancers.size(); i++)
                drawFaceDancer();
            revealedFaceDancers.forEach(fd -> game.getTraitorDeck().add(fd));
            revealedFaceDancers = null;
        }
        game.discardNexusCard(this);
        game.getTurnSummary().publish(Emojis.BT + " replaced their revealed Face Dancers using their Nexus card Cunning effect.");
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    /**
     * @return the revealed Face Dancers
     */
    public Set<TraitorCard> getRevealedFaceDancers() {
        return Optional.ofNullable(revealedFaceDancers).orElse(new HashSet<>());
    }

    /**
     * @param faceDancer name of the revealed Face Dancer
     */
    public void revealFaceDancer(String faceDancer, Game game) throws InvalidGameStateException {
        TraitorCard revealedFaceDancer = traitorHand.stream()
                .filter(t -> t.getName().equalsIgnoreCase(faceDancer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Face Dancer: " + faceDancer));
        if (!getTraitorHand().contains(revealedFaceDancer))
            throw new IllegalArgumentException("Revealed face dancer must be in traitor hand");
        if (revealedFaceDancers == null)
            revealedFaceDancers = new HashSet<>();

        getTraitorHand().remove(revealedFaceDancer);
        revealedFaceDancers.add(revealedFaceDancer);
        game.getTurnSummary().publish(emoji + " revealed " + revealedFaceDancer.getEmojiAndNameString() + " as a Face Dancer!");
        if (getTraitorHand().isEmpty())
            drawFaceDancers();
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
        setUpdated(UpdateType.MISC_FRONT_OF_SHIELD);
    }

    public List<String> getFactionsNeedingRevivalLimit() {
        if (factionsNeedingRevivalLimit == null) factionsNeedingRevivalLimit = new ArrayList<>();
        return factionsNeedingRevivalLimit;
    }

    public void addFactionNeedingRevivalLimit(String factionName) {
        if (factionsNeedingRevivalLimit == null) factionsNeedingRevivalLimit = new ArrayList<>();
        factionsNeedingRevivalLimit.add(factionName);
    }

    public boolean hasSetAllRevivalLimits() {
        if (factionsNeedingRevivalLimit == null) factionsNeedingRevivalLimit = new ArrayList<>();
        return factionsNeedingRevivalLimit.isEmpty();
    }

    public void leaveRevivalLimitsUnchanged() {
        if (factionsNeedingRevivalLimit == null) factionsNeedingRevivalLimit = new ArrayList<>();
        for (String name : factionsNeedingRevivalLimit) {
            Faction faction = game.getFaction(name);
            game.getTurnSummary().publish(faction.getEmoji() + " revival limit was left at " + faction.getMaxRevival());
        }
        factionsNeedingRevivalLimit.clear();
        chat.reply("Remaining revival limits left unchanged.");
    }

    public void setRevivalLimit(String factionName, int revivalLimit) {
        Faction faction = game.getFaction(factionName);
        faction.setMaxRevival(revivalLimit);
        if (factionsNeedingRevivalLimit == null) factionsNeedingRevivalLimit = new ArrayList<>();
        factionsNeedingRevivalLimit.removeIf(name -> name.equals(factionName));
        game.getTurnSummary().publish(faction.getEmoji() + " revival limit has been set to " + faction.getMaxRevival() + ".");
    }

    @Override
    public int performFreeRevivals() {
        int numStarRevived = countFreeStarredRevival();
        starRevived = numStarRevived > 0;
        TleilaxuTanks tanks = game.getTleilaxuTanks();
        boolean wasHighThreshold = game.hasGameOption(GameOption.HOMEWORLDS) && isHighThreshold();
        numFreeRevivals = Math.min(getFreeRevival(), tanks.getForceStrength(name));
        if (numFreeRevivals + numStarRevived > 0) {
            if (wasHighThreshold)
                presentHTChoices();
            game.reviveForces(this, false, numFreeRevivals, numStarRevived);
        }
        return numFreeRevivals;
    }

    public int getNumFreeRevivals() {
        return numFreeRevivals;
    }

    @Override
    public int baseRevivalCost(int regular, int starred) {
        return regular + starred;
    }

    @Override
    public void performMentatPauseActions(boolean extortionTokenTriggered) {
        super.performMentatPauseActions(extortionTokenTriggered);
        chat.publish("Would you like to swap a Face Dancer? " + player);
    }

    public void presentHTChoices() {
        game.getTurnSummary().publish(emoji + " may place " + numFreeRevivals + " free revived " + Emojis.getForceEmoji(name) + " in any territory or homeworld.");
        String buttonSuffix = "-bt-ht";
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("stronghold" + buttonSuffix, "Stronghold"));
        choices.add(new DuneChoice("spice-blow" + buttonSuffix, "Spice Blow Territories"));
        choices.add(new DuneChoice("rock" + buttonSuffix, "Rock Territories"));
        choices.add(new DuneChoice("homeworlds" + buttonSuffix, "Homeworlds"));
        boolean revealedDiscoveryTokenOnMap = game.getTerritories().values().stream().anyMatch(Territory::isDiscovered);
        if (game.hasGameOption(GameOption.DISCOVERY_TOKENS) && revealedDiscoveryTokenOnMap)
            choices.add(new DuneChoice("discovery-tokens" + buttonSuffix, "Discovery Tokens"));
        choices.add(new DuneChoice("other" + buttonSuffix, "Somewhere else"));
        choices.add(new DuneChoice("danger", "pass-shipment" + buttonSuffix, "Leave them on Tleilaxu"));
        chat.publish("Where would you like to place your " + numFreeRevivals + " " + Emojis.getForceEmoji(name) + " free revivals? " + player, choices);
        btHTActive = true;
    }

    public void presentHTExecutionChoices() {
        shipment.setForce(numFreeRevivals);
        List<DuneChoice> choices = new LinkedList<>();
        choices.add(new DuneChoice("execute-shipment-bt-ht", "Confirm placement"));
        choices.add(new DuneChoice("secondary", "reset-shipment-bt-ht", "Start over"));
        choices.add(new DuneChoice("danger", "pass-shipment-bt-ht", "Leave them on Tleilaxu"));
        chat.reply("Sending **" + forcesString(numFreeRevivals, 0) + "** free revivals to " + shipment.getTerritoryName(), choices);
    }

    public boolean isBtHTActive() {
        return btHTActive;
    }

    public void setBtHTActive(boolean btHTActive) {
        this.btHTActive = btHTActive;
    }

    public void swapFaceDancer(String faceDancer) {
        LinkedList<TraitorCard> traitorDeck = game.getTraitorDeck();

        TraitorCard traitorCard = traitorHand.stream()
                .filter(t -> t.getName().equalsIgnoreCase(faceDancer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Face Dancer: " + faceDancer));

        removeTraitorCard(traitorCard);
        traitorDeck.add(traitorCard);
        Collections.shuffle(traitorDeck);

        TraitorCard newFD = traitorDeck.pop();
        addTraitorCard(newFD);
        chat.publish(newFD.getEmojiNameAndStrengthString() + " is your new Face Dancer. You have swapped out " + traitorCard.getEmojiNameAndStrengthString() + ".");
        game.getTurnSummary().publish(emoji + " swapped a Face Dancer");
    }
}
