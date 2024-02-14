package controller.commands;

import constants.Emojis;
import enums.ChoamInflationType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
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
                        new SubcommandData(
                                "set-inflation",
                                "Set CHOAM Inflation for next round."
                        ).addOptions(choamInflationType)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (name.equals("set-inflation")) {
            setInflation(discordGame, game);
        } else {
            throw new IllegalArgumentException("Invalid command name: " + name);
        }
    }

    public static void setInflation(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (game.hasFaction("CHOAM")) {
            String inflationType = discordGame.required(choamInflationType).getAsString();
            ChoamInflationType choamInflationType = ChoamInflationType.valueOf(inflationType);

            ChoamFaction faction = (ChoamFaction) game.getFaction("CHOAM");

            faction.setFirstInflation(game.getTurn(), choamInflationType);

            discordGame.pushGame();
            discordGame.getTurnSummary().queueMessage(Emojis.CHOAM + " set inflation to " + choamInflationType + " for next round!");
        }
    }
}
