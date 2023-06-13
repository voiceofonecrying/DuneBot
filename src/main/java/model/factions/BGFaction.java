package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class BGFaction extends Faction {
    public BGFaction(String player, String userName, Game game) {
        super("BG", player, userName, game);

        this.spice = 5;
        this.freeRevival = 1;
        this.reserves = new Force("BG", 20);
        this.emoji = Emojis.BG;
    }
}
