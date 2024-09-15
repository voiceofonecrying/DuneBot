package model.factions;

import constants.Emojis;
import model.Game;
import model.Territory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HarkonnenFaction extends Faction {
    private Boolean hasTriggeredHT;
    public HarkonnenFaction(String player, String userName) throws IOException {
        super("Harkonnen", player, userName);

        setSpice(10);
        this.freeRevival = 2;
        this.hasMiningEquipment = true;
        this.emoji = Emojis.HARKONNEN;
        this.highThreshold = 7;
        this.lowThreshold = 6;
        this.occupiedIncome = 2;
        this.homeworld = "Giedi Prime";
        this.hasTriggeredHT = false;
        this.handLimit = 8;
    }

    @Override
    public void joinGame(@NotNull Game game) {
        super.joinGame(game);
        game.getTerritories().get("Carthag").addForces("Harkonnen", 10);
        Territory giediPrime = game.getTerritories().addHomeworld(game, homeworld, name);
        giediPrime.addForces(name, 10);
        game.getHomeworlds().put(name, homeworld);
    }

    public boolean hasTriggeredHT() {
        if (hasTriggeredHT == null) hasTriggeredHT = false;
        return hasTriggeredHT;
    }

    public void setTriggeredHT(boolean hasTriggeredHT) {
        this.hasTriggeredHT = hasTriggeredHT;
    }
}
