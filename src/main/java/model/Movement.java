package model;

import constants.Emojis;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.factions.Faction;

import java.io.IOException;

public class Movement {
    private String movingFrom;
    private String movingTo;
    private int force;
    private int specialForce;
    private boolean hasMoved;
    private boolean movingNoField;

    public Movement() {}

    public void execute(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Territory from = game.getTerritory(movingFrom);
        Territory to = game.getTerritory(movingTo);
        if (movingNoField) {
            to.setRicheseNoField(from.getRicheseNoField());
            from.setRicheseNoField(null);
            discordGame.sendMessage("turn-summary", Emojis.RICHESE + " move their No-Field token to " + to.getTerritoryName());
        }
        if (force != 0 || specialForce != 0) CommandManager.moveForces(faction, from, to, force, specialForce, discordGame, game);
        clear();
        ShowCommands.showBoard(discordGame, game);
    }

    public void clear() {
        this.movingFrom = "";
        this.movingTo = "";
        this.force = 0;
        this.specialForce = 0;
        this.hasMoved = true;
        this.movingNoField = false;
    }

    public String getMovingFrom() {
        return movingFrom;
    }

    public void setMovingFrom(String movingFrom) {
        this.movingFrom = movingFrom;
    }

    public String getMovingTo() {
        return movingTo;
    }

    public void setMovingTo(String movingTo) {
        this.movingTo = movingTo;
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

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public boolean isMovingNoField() {
        return movingNoField;
    }

    public void setMovingNoField(boolean movingNoField) {
        this.movingNoField = movingNoField;
    }
}
