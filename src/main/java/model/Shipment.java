package model;

import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import model.factions.Faction;

import java.io.IOException;

public class Shipment {
    private int force;
    private int specialForce;
    private String territoryName;
    public Shipment() {
    }

    public void execute(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritory(territoryName);
        CommandManager.placeForces(territory, faction, this.force, this.specialForce, true, discordGame, game);
        this.territoryName = "";
        this.force = 0;
        this.specialForce = 0;
        discordGame.pushGame(game);
        ShowCommands.showBoard(discordGame, game);
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getSpecialForce() {
        return specialForce;
    }

    public void setSpecialForce(int specialForce) {
        this.specialForce = specialForce;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(String territoryName) {
        this.territoryName = territoryName;
    }

}