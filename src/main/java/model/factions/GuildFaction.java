package model.factions;

import constants.Emojis;
import enums.UpdateType;
import model.Game;
import model.Territory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GuildFaction extends Faction {
    private boolean allySpiceForShipping;

    public GuildFaction(String player, String userName) throws IOException {
        super("Guild", player, userName);

        setSpice(5);
        this.freeRevival = 1;
        this.emoji = Emojis.GUILD;
        this.highThreshold = 5;
        this.lowThreshold = 4;
        this.occupiedIncome = 2;
        this.homeworld = "Junction";
        allySpiceForShipping = false;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        game.getTerritories().get("Tuek's Sietch").addForces("Guild", 5);
        Territory junction = game.getTerritories().addHomeworld(game, homeworld, name);
        junction.addForces(name, 15);
        game.getHomeworlds().put(name, homeworld);
    }

    public boolean isAllySpiceForShipping() {
        return allySpiceForShipping;
    }

    public void setAllySpiceForShipping(boolean allySpiceForShipping) {
        this.allySpiceForShipping = allySpiceForShipping;
        setUpdated(UpdateType.MISC_BACK_OF_SHIELD);
    }

    @Override
    public int getShippingSupport() {
        return allySpiceForShipping ? getSpiceForAlly() : 0;
    }

    @Override
    public String getSpiceSupportPhasesString() {
        return allySpiceForShipping ? " for bidding and shipping!" : " for bidding only!";
    }
}
