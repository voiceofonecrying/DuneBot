package controller.commands;

import constants.Emojis;
import controller.channels.TurnSummary;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.DiscordGame;
import model.Force;
import model.Game;
import model.Territory;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.bgTerritories;
import static controller.commands.CommandOptions.territory;

public class BGCommands {
    public static List<CommandData> getCommands() {

        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("bg", "Commands related to the BG Faction.").addSubcommands(
                        new SubcommandData(
                                "flip",
                                "Flip to advisor or fighter in a territory."
                        ).addOptions(bgTerritories),
                        new SubcommandData(
                                "advise",
                                "Advise to a territory or to the polar sink."
                        ).addOptions(territory)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "flip" -> flip(discordGame, game);
            case "advise" -> adviseEventHandler(discordGame, game);
        }
    }

    private static void adviseEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritory(discordGame.required(CommandOptions.territory).getAsString());
        advise(discordGame, game, territory);
    }

    public static void advise(DiscordGame discordGame, Game game, Territory territory) throws ChannelNotFoundException, IOException {

        CommandManager.placeForceInTerritory(territory, game.getFaction("BG"), 1, false);
        int fighters = territory.getForce("BG").getStrength();
        territory.getForces().removeIf(force -> force.getName().equals("BG"));
        territory.getForces().add(new Force("Advisor", fighters));
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " advise to " + territory.getTerritoryName());
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }

    public static void flip(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritories().get(discordGame.required(bgTerritories).getAsString());
        flip(discordGame, game, territory);
    }

    public static void flip(DiscordGame discordGame, Game game, Territory territory) throws ChannelNotFoundException, IOException {
        int strength = 0;
        String found = "";
        for (Force force : territory.getForces()) {
            if (force.getName().equals("BG") || force.getName().equals("Advisor")) {
                strength += force.getStrength();
                found = force.getName();
            }
        }
        territory.getForces().removeIf(force -> force.getName().equals("BG") || force.getName().equals("Advisor"));
        if (found.equals("Advisor")) territory.getForces().add(new Force("BG", strength));
        else if (found.equals("BG")) territory.getForces().add(new Force("Advisor", strength));
        else {
            discordGame.queueMessage("mod-info","No Bene Gesserit were found in that territory.");
            return;
        }
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }
}
