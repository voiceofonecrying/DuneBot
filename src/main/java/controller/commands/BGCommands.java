package controller.commands;

import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
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

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "flip" -> flip(discordGame, game);
            case "advise" -> adviseEventHandler(discordGame, game);
        }
    }

    private static void adviseEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        Territory territory = game.getTerritory(discordGame.required(CommandOptions.territory).getAsString());
        advise(discordGame, game, territory, 1);
    }

    public static void advise(DiscordGame discordGame, Game game, Territory territory, int amount) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        game.getBGFaction().advise(game, territory, amount);
        discordGame.pushGame();
    }

    public static void flip(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritories().get(discordGame.required(bgTerritories).getAsString());
        game.getBGFaction().flipForces(territory);
        discordGame.pushGame();
    }
}
