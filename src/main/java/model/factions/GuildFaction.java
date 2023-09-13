package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;

import java.io.IOException;

public class GuildFaction extends Faction {
    public GuildFaction(String player, String userName, Game game) throws IOException {
        super("Guild", player, userName, game);

        setSpice(5);
        this.freeRevival = 1;
        this.reserves = new Force("Guild", 15);
        this.emoji = Emojis.GUILD;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
        game.getTerritories().get("Tuek's Sietch").getForces().add(new Force("Guild", 5));
    }
}
