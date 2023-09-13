package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Force;
import model.Game;
import model.TraitorCard;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BTFaction extends Faction {
    private Set<TraitorCard> revealedFaceDancers;
    private int revivalRatesSet;

    public BTFaction(String player, String userName, Game game) throws IOException {
        super("BT", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.reserves = new Force("BT", 20);
        this.emoji = Emojis.BT;
        this.revivalRatesSet = 0;
        this.highThreshold = 9;
        this.lowThreshold = 8;
        this.occupiedIncome = 2;
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

    public int getRevivalRatesSet() {
        return revivalRatesSet;
    }

    public void setRevivalRatesSet(int revivalRatesSet) {
        this.revivalRatesSet = revivalRatesSet;
    }
}
