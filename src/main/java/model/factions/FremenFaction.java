package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

public class FremenFaction extends Faction {
    public FremenFaction(String player, String userName, Game game) {
        super("Fremen", player, userName, game);

        this.spice = 3;
        this.freeRevival = 3;
        this.reserves = new Force("Fremen", 17);
        this.specialReserves = new Force("Fremen*", 3);
        this.emoji = Emojis.FREMEN;
    }
}
