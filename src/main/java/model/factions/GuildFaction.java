package model.factions;

import constants.Emojis;
import model.Force;
import model.Game;
import model.Territory;

import java.io.IOException;

public class GuildFaction extends Faction {
    public GuildFaction(String player, String userName, Game game) throws IOException {
        super("Guild", player, userName, game);

        setSpice(5);
        this.freeRevival = 1;
        this.emoji = Emojis.GUILD;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
        this.homeworld = "Junction";
        game.getTerritories().put("Junction", new Territory("Junction", -1, false, false, false));
        game.getTerritory("Junction").addForce(new Force("Guild", 15));
        game.getTerritories().get("Tuek's Sietch").getForces().add(new Force("Guild", 5));
        game.getHomeworlds().put(getName(), homeworld);
    }
}
