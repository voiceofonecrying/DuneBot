package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import controller.commands.CommandManager;
import controller.commands.RunCommands;
import controller.commands.ShowCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidOptionException;
import model.factions.Faction;

import java.io.IOException;
import java.io.InputStream;

public class Movement {
    private String movingFrom;
    private String movingTo;
    private int force;
    private int specialForce;

    public Movement() {}

    public void execute(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException, InvalidOptionException, IOException {
        Territory from = game.getTerritory(movingFrom);
        Territory to = game.getTerritory(movingTo);
        CommandManager.moveForces(faction, from, to, force, specialForce, discordGame);
        ShowCommands.showBoard(discordGame, game);
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
}
