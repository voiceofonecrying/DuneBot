package model.factions;

import constants.Emojis;
import enums.UpdateType;
import helpers.Exclude;
import model.Force;
import model.Game;
import model.Territory;
import model.TraitorCard;

import java.io.IOException;
import java.util.*;

public class BTFaction extends Faction {
    private Set<TraitorCard> revealedFaceDancers;

    @Exclude
    private int revivalRatesSet;
    private Set<String> factionRevivalRatesSet;

    public BTFaction(String player, String userName, Game game) throws IOException {
        super("BT", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.emoji = Emojis.BT;
        this.factionRevivalRatesSet = new HashSet<>();
        this.highThreshold = 9;
        this.lowThreshold = 8;
        this.occupiedIncome = 2;
        this.homeworld = "Tleilax";
        game.getTerritories().put("Tleilax", new Territory("Tleilax", -1, false, false, false));
        game.getTerritory("Tleilax").addForce(new Force("BT", 20));
        game.getHomeworlds().put(getName(), homeworld);
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
    public void addRevealedFaceDancer(TraitorCard revealedFaceDancer) {
        if (!getTraitorHand().remove(revealedFaceDancer)) {
            throw new IllegalArgumentException("Revealed face dancer must be in traitor hand");
        }

        if (revealedFaceDancers == null) {
            revealedFaceDancers = new HashSet<>();
        }
        revealedFaceDancers.add(revealedFaceDancer);
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    public void addRevivalRatesSet(String revivalRatesSet) {
        if (this.factionRevivalRatesSet == null) this.factionRevivalRatesSet = new HashSet<>();
        this.factionRevivalRatesSet.add(revivalRatesSet);
    }

    public boolean hasSetAllRevivalRates() {
        if (this.factionRevivalRatesSet == null) this.factionRevivalRatesSet = new HashSet<>();
        return this.factionRevivalRatesSet.size() == getGame().getFactions().size() - 1;
    }

    public void clearRevivalRatesSet() {
        if (this.factionRevivalRatesSet == null) this.factionRevivalRatesSet = new HashSet<>();
        this.factionRevivalRatesSet.clear();
    }
}
