package model.factions;

import constants.Emojis;
import enums.UpdateType;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
import model.TraitorCard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class BTFaction extends Faction {
    private Set<TraitorCard> revealedFaceDancers;
    private List<String> factionsNeedingRevivalLimit;

    public BTFaction(String player, String userName) throws IOException {
        super("BT", player, userName);

        setSpice(5);
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
        game.drawCard("traitor deck", "BT");
        game.drawCard("traitor deck", "BT");
        game.drawCard("traitor deck", "BT");
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
                .filter(t -> t.name().equalsIgnoreCase(faceDancer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Face Dancer: " + faceDancer));
        if (!getTraitorHand().contains(revealedFaceDancer))
            throw new IllegalArgumentException("Revealed face dancer must be in traitor hand");
        if (revealedFaceDancers == null)
            revealedFaceDancers = new HashSet<>();

        getTraitorHand().remove(revealedFaceDancer);
        revealedFaceDancers.add(revealedFaceDancer);
        game.getTurnSummary().publish(emoji + " revealed " + revealedFaceDancer.name() + " as a Face Dancer!");
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
    }

    public void setRevivalLimit(String factionName, int revivalLimit) {
        Faction faction = game.getFaction(factionName);
        faction.setMaxRevival(revivalLimit);
        if (factionsNeedingRevivalLimit == null) factionsNeedingRevivalLimit = new ArrayList<>();
        factionsNeedingRevivalLimit.removeIf(name -> name.equals(factionName));
        game.getTurnSummary().publish(faction.getEmoji() + " revival limit has been set to " + faction.getMaxRevival() + ".");
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
}
