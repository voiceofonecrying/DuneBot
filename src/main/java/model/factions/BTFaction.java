package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Game;
import model.Territory;
import model.TraitorCard;

import java.io.IOException;
import java.util.*;

public class BTFaction extends Faction {
    private Set<TraitorCard> revealedFaceDancers;
    private List<String> factionsNeedingRevivalLimit;

    public BTFaction(String player, String userName, Game game) throws IOException {
        super("BT", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.maxRevival = 20;
        this.emoji = Emojis.BT;
        this.factionsNeedingRevivalLimit = new ArrayList<>();
        this.highThreshold = 9;
        this.lowThreshold = 8;
        this.occupiedIncome = 2;
        this.homeworld = "Tleilax";
        Territory tleilax = game.getTerritories().addHomeworld(homeworld);
        tleilax.addForces(name, 20);
        game.getHomeworlds().put(name, homeworld);
    }

    /**
     * @return the revealed Face Dancers
     */
    public Set<TraitorCard> getRevealedFaceDancers() {
        return Optional.ofNullable(revealedFaceDancers).orElse(new HashSet<>());
    }

    /**
     * @param revealedFaceDancer the revealed Face Dancer to add
     */
    public void revealFaceDancer(TraitorCard revealedFaceDancer, Game game) {
        if (!getTraitorHand().remove(revealedFaceDancer)) {
            throw new IllegalArgumentException("Revealed face dancer must be in traitor hand");
        }

        if (revealedFaceDancers == null) {
            revealedFaceDancers = new HashSet<>();
        }
        revealedFaceDancers.add(revealedFaceDancer);
        if (getTraitorHand().isEmpty()) {
            revealedFaceDancers.forEach(fd -> game.getTraitorDeck().add(fd));
            revealedFaceDancers = null;
            Collections.shuffle(game.getTraitorDeck());
            game.drawCard("traitor deck", "BT");
            game.drawCard("traitor deck", "BT");
            game.drawCard("traitor deck", "BT");
        }
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
        game.getTurnSummary().publish(faction.getEmoji() + " revival limit has been set to " + faction.getMaxRevival());
    }
}
