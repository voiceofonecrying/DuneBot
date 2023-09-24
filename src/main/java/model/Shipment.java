package model;

import constants.Emojis;
import controller.channels.TurnSummary;
import controller.commands.CommandManager;
import controller.commands.RicheseCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import model.factions.Faction;

import java.io.IOException;

public class Shipment {
    private int force;
    private int specialForce;
    private String territoryName;
    private boolean hasShipped;
    private boolean isNoField;
    private boolean toReserves;
    private String crossShipFrom;
    public Shipment() {
    }

    public void execute(DiscordGame discordGame, Game game, Faction faction, boolean karama) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritory(territoryName);
        if (isNoField) {
            RicheseCommands.moveNoFieldFromBoardToFrontOfShield(game, discordGame);
            territory.setRicheseNoField(force);
            int spice = territory.isStronghold() ? 1 : 2;
            faction.subtractSpice(spice);
            discordGame.getTurnSummary().queueMessage(Emojis.RICHESE + " ship a no-field to " + territoryName);
        }
        else if (isToReserves()) {
            CommandManager.removeForces(territoryName, faction, force, specialForce, false, game, discordGame);
            int spice = Math.ceilDiv(force, 2);
            faction.subtractSpice(spice);
            discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " ship " + force + " " + Emojis.getForceEmoji("Guild") + " from " + territoryName + " to reserves. for " + spice + " " + Emojis.SPICE + " paid to the bank.");
        }
        else if (!crossShipFrom.isEmpty()) {
            CommandManager.removeForces(crossShipFrom, faction, force, 0, false, game, discordGame);
            CommandManager.placeForces(territory, faction, force, specialForce, true, discordGame, game, false);
            discordGame.getTurnSummary().queueMessage(Emojis.GUILD + " cross shipped from " + crossShipFrom + " to " + territoryName);
        }
        else CommandManager.placeForces(territory, faction, force, specialForce, true, discordGame, game, karama);
        ShowCommands.showBoard(discordGame, game);
        clear();
        discordGame.pushGame();
    }

    public void clear() {
        this.territoryName = "";
        this.force = 0;
        this.specialForce = 0;
        this.hasShipped = true;
        this.isNoField = false;
        this.toReserves = false;
        this.crossShipFrom = "";
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

    public boolean isToReserves() {
        return toReserves;
    }

    public void setToReserves(boolean toReserves) {
        this.toReserves = toReserves;
    }

    public String getCrossShipFrom() {
        if (crossShipFrom == null) crossShipFrom = "";
        return crossShipFrom;
    }

    public void setCrossShipFrom(String crossShipFrom) {
        this.crossShipFrom = crossShipFrom;
    }
}