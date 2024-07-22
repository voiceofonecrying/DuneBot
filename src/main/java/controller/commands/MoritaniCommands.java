package controller.commands;

import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import model.Game;
import model.Territory;
import model.factions.MoritaniFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

public class MoritaniCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(
                Commands.slash("moritani", "Commands related to the Moritani Faction.").addSubcommands(
                        new SubcommandData(
                                "remove-terror-token-from-map",
                                "Remove a Moritani Terror Token from the map"
                        ).addOptions(
                                CommandOptions.moritaniTerrorTokenOnMap,
                                CommandOptions.toHand
                        ),
                        new SubcommandData(
                                "trigger-terror-token",
                                "Trigger a Moritani Terror Token"
                        ).addOptions(
                                CommandOptions.faction,
                                CommandOptions.moritaniTerrorTokenOnMap
                        )
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (game.hasFaction("Moritani") && name.equals("remove-terror-token-from-map")) {
            removeTerrorTokenFromMap(discordGame, game);
        } else if (game.hasFaction("Moritani") && name.equals("trigger-terror-token")) {
            triggerTerrorToken(discordGame, game);
        } else {
            throw new IllegalArgumentException("Invalid command");
        }
    }

    public static void removeTerrorTokenFromMap(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String terrorTokenName = discordGame.required(CommandOptions.moritaniTerrorTokenOnMap).getAsString();
        boolean toHand = discordGame.optional(CommandOptions.toHand) != null
                && discordGame.required(CommandOptions.toHand).getAsBoolean();
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");

        Territory territory = game.getTerritories().values().stream()
                .filter(t -> t.hasTerrorToken(terrorTokenName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Terror Token not found on map"));

        territory.removeTerrorToken(terrorTokenName);
        game.setUpdated(UpdateType.MAP);

        if (toHand) moritaniFaction.addTerrorToken(terrorTokenName);

        discordGame.pushGame();
    }

    public static void triggerTerrorToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String terrorTokenName = discordGame.required(CommandOptions.moritaniTerrorTokenOnMap).getAsString();
        String triggeringFaction = discordGame.required(CommandOptions.faction).getAsString();
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");

        Territory territory = game.getTerritories().values().stream()
                .filter(t -> t.hasTerrorToken(terrorTokenName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Terror Token not found on map"));
        moritaniFaction.triggerTerrorToken(game.getFaction(triggeringFaction), territory, terrorTokenName);
        moritaniFaction.getChat().publish("You have triggered your " + terrorTokenName + " token in " + territory.getTerritoryName());
        discordGame.pushGame();
    }
}
