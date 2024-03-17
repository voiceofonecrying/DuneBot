package controller.commands;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import exceptions.InvalidGameStateException;
import model.Game;
import model.factions.ChoamFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.choamInflationType;

public class ChoamCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("choam", "Commands related to CHOAM.").addSubcommands(
                        new SubcommandData("set-inflation", "Set CHOAM Inflation for next round.").addOptions(choamInflationType),
                        new SubcommandData("clear-inflation", "Clear CHOAM inflation to allow setting on a different turn.")
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "set-inflation" -> setInflation(discordGame, game);
            case "clear-inflation" -> clearInflation(discordGame, game);
            default -> throw new IllegalArgumentException("Invalid command name: " + name);
        }
    }

    public static void setInflation(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.hasFaction("CHOAM"))
            throw new InvalidGameStateException(Emojis.CHOAM + " is not in the game.");

        String inflationType = discordGame.required(choamInflationType).getAsString();
        ChoamInflationType choamInflationType = ChoamInflationType.valueOf(inflationType);

        ChoamFaction faction = (ChoamFaction) game.getFaction("CHOAM");

        faction.setFirstInflation(choamInflationType);

        discordGame.pushGame();
    }

    public static void clearInflation(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        if (!game.hasFaction("CHOAM"))
            throw new InvalidGameStateException(Emojis.CHOAM + " is not in the game.");

        ChoamFaction faction = (ChoamFaction) game.getFaction("CHOAM");

        faction.clearInflation();

        discordGame.pushGame();
        discordGame.getTurnSummary().queueMessage(Emojis.CHOAM + " inflation has been deactivated and can be set for a different turn.");
    }
}
