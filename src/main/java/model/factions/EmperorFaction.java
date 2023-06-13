package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class EmperorFaction extends Faction {
    public EmperorFaction(String player, String userName, Game game) {
        super("Emperor", player, userName, game);

        this.spice = 10;
        this.freeRevival = 1;
        this.reserves = new Force("Emperor", 15);
        this.specialReserves = new Force("Emperor*", 5);
        this.emoji = Emojis.EMPEROR;
    }
}
