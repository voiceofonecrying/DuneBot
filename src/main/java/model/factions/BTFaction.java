package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;
import model.TraitorCard;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BTFaction extends Faction {
    private Set<TraitorCard> revealedFaceDancers;

    public BTFaction(String player, String userName, Game game) {
        super("BT", player, userName, game);

        setSpice(5);
        this.freeRevival = 2;
        this.reserves = new Force("BT", 20);
        this.emoji = Emojis.BT;
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
    }
}
