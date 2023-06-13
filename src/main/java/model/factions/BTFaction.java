package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class BTFaction extends Faction {
    public BTFaction(String player, String userName, Game game) {
        super("BT", player, userName, game);

        this.spice = 5;
        this.freeRevival = 2;
        this.reserves = new Force("BT", 20);
        this.emoji = Emojis.BT;
    }
}
