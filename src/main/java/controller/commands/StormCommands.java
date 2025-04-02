package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class StormCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("storm", "Commands for moving and placing the storm.").addSubcommands(
                new SubcommandData("initial-dials", "Sets the storm to an initial sector. Assign Tech Tokens if applicable.").addOptions(dialOne, dialTwo),
                new SubcommandData("set-next-movement", "Override the storm movement, e.g. for Weather Control").addOptions(sectors),
                new SubcommandData("place-storm", "Place the storm in a sector without movement, so no force or spice losses").addOptions(sector)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
        switch (name) {
            case "initial-dials" -> setInitialStorm(discordGame, game);
            case "set-next-movement" -> setStormMovement(discordGame, game);
            case "place-storm" -> placeStorm(discordGame, game);
        }
    }

    public static void setInitialStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int stormDialOne = discordGame.required(dialOne).getAsInt();
        int stormDialTwo = discordGame.required(dialTwo).getAsInt();
        game.setInitialStorm(stormDialOne, stormDialTwo);
        discordGame.pushGame();
    }

    public static void setStormMovement(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int stormMovement = discordGame.required(sectors).getAsInt();
        game.setStormMovement(stormMovement);
        discordGame.pushGame();
    }

    public static void placeStorm(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int stormSector = discordGame.required(sector).getAsInt();
        game.placeStorm(stormSector);
        discordGame.pushGame();
    }
}
