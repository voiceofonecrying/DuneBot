package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class ChoamFaction extends Faction {
    public ChoamFaction(String player, String userName, Game game) {
        super("CHOAM", player, userName, game);

        setSpice(2);
        this.freeRevival = 0;
        this.reserves = new Force("CHOAM", 20);
        this.emoji = Emojis.CHOAM;
        this.handLimit = 5;
    }
}
