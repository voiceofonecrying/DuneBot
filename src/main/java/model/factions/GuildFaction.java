package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;

import java.io.IOException;

public class GuildFaction extends Faction {
    private boolean allySpiceForShipping;

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
        Territory junction = game.getTerritories().addHomeworld(game, homeworld, name);
        junction.addForces(name, 15);
        game.getHomeworlds().put(name, homeworld);
        allySpiceForShipping = false;
    }

    public boolean isAllySpiceForShipping() {
        return allySpiceForShipping;
    }

    public void setAllySpiceForShipping(boolean allySpiceForShipping) {
        this.allySpiceForShipping = allySpiceForShipping;
    }

    @Override
    public int getShippingSupport() {
        return allySpiceForShipping ? getSpiceForAlly() : 0;
    }
}
