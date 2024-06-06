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
        game.getTerritories().get("Tuek's Sietch").addForces("Guild", 5);
        Territory junction = game.getTerritories().addHomeworld(homeworld);
        junction.addForce(new Force(name, 15));
        game.getHomeworlds().put(name, homeworld);
    }
}
