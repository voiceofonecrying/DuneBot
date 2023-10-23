package model;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.CommandManager;
import controller.commands.ShowCommands;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.factions.Faction;

import java.io.IOException;

public class Movement {
    private String movingFrom;
    private String secondMovingFrom;
    private String movingTo;
    private int force;
    private int secondForce;
    private int specialForce;

    private int secondSpecialForce;
    private boolean hasMoved;
    private boolean movingNoField;

    public Movement() {
    }

    public void execute(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Territory from = game.getTerritory(movingFrom);
        Territory to = game.getTerritory(movingTo);
        if (movingNoField) {
            to.setRicheseNoField(from.getRicheseNoField());
            from.setRicheseNoField(null);
            discordGame.getTurnSummary().queueMessage(Emojis.RICHESE + " move their No-Field token to " + to.getTerritoryName());
        }
        if (force != 0 || specialForce != 0)
            CommandManager.moveForces(faction, from, to, force, specialForce, discordGame, game);
        if (secondForce != 0 || secondSpecialForce != 0) {
            discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " use Planetologist to move another force to " + movingTo);
            CommandManager.moveForces(faction, game.getTerritory(secondMovingFrom), to, secondForce, secondSpecialForce, discordGame, game);
        }
        clear();
        if (!game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD)) {
            ShowCommands.showBoard(discordGame, game);
        }
    }

    public void clear() {
        this.movingFrom = "";
        this.movingTo = "";
        this.force = 0;
        this.specialForce = 0;
        this.secondForce = 0;
        this.secondSpecialForce = 0;
        this.secondMovingFrom = "";
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

    public String getSecondMovingFrom() {
        return secondMovingFrom;
    }

    public void setSecondMovingFrom(String secondMovingFrom) {
        this.secondMovingFrom = secondMovingFrom;
    }

    public int getSecondForce() {
        return secondForce;
    }

    public void setSecondForce(int secondForce) {
        this.secondForce = secondForce;
    }

    public int getSecondSpecialForce() {
        return secondSpecialForce;
    }

    public void setSecondSpecialForce(int secondSpecialForce) {
        this.secondSpecialForce = secondSpecialForce;
    }
}
