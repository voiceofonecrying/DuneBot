package model;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.RicheseCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import model.factions.Faction;
import model.factions.RicheseFaction;

import java.io.IOException;

public class Shipment {
    private int force;
    private int specialForce;
    private String territoryName;
    private boolean hasShipped;
    private boolean isNoField;
    public Shipment() {
    }

    public void execute(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritory(territoryName);
        if (isNoField) {
            RicheseCommands.moveNoFieldFromBoardToFrontOfShield(game, discordGame);
            territory.setRicheseNoField(force);
            int spice = territory.isStronghold() ? 1 : 2;
            faction.subtractSpice(spice);
            discordGame.sendMessage("turn-summary", Emojis.RICHESE + " ship a no-field to " + territoryName);
        }
        else CommandManager.placeForces(territory, faction, this.force, this.specialForce, true, discordGame, game);
        this.territoryName = "";
        this.force = 0;
        this.specialForce = 0;
        this.hasShipped = true;
        this.isNoField = false;
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

    public boolean hasShipped() {
        return hasShipped;
    }

    public void setShipped(boolean hasShipped) {
        this.hasShipped = hasShipped;
    }

    public boolean isNoField() {
        return isNoField;
    }

    public void setNoField(boolean noField) {
        isNoField = noField;
    }
}