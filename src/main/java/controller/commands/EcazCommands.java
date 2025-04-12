package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class EcazCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("ecaz", "Commands related to the Ecaz Faction.").addSubcommands(
                new SubcommandData("place-ambassador", "Place an Ecaz Ambassador token in a stronghold").addOptions(ecazAmbassadorsInSupply, strongholdWithoutAmbassador, ambassadorCost),
                new SubcommandData("remove-ambassador-from-map", "Remove an Ecaz Ambassador token from the map").addOptions(ecazAmbassadorsOnMap, toPlayer),
                new SubcommandData("assign-duke-vidal", "Assign Duke Vidal to a faction").addOptions(faction)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        game.modExecutedACommand(event.getUser().getAsMention());
        switch (name) {
            case "place-ambassador" -> placeAmbassador(discordGame, game);
            case "remove-ambassador-from-map" -> removeAmbassadorFromMap(discordGame, game);
            case "assign-duke-vidal" -> assignDukeVidal(discordGame, game);
        }
    }

    private static void placeAmbassador(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String ambassadorName = discordGame.required(ecazAmbassadorsInSupply).getAsString();
        String strongholdName = discordGame.required(strongholdWithoutAmbassador).getAsString();
        int cost = discordGame.required(ambassadorCost).getAsInt();
        game.getEcazFaction().placeAmbassador(strongholdName, ambassadorName, cost);
        discordGame.pushGame();
    }

    private static void removeAmbassadorFromMap(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String ambassadorName = discordGame.required(ecazAmbassadorsOnMap).getAsString();
        boolean toHand = discordGame.required(CommandOptions.toPlayer).getAsBoolean();
        game.getEcazFaction().removeAmbassadorFromMap(ambassadorName, toHand);
        discordGame.pushGame();
    }

    private static void assignDukeVidal(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String factionName = discordGame.required(faction).getAsString();
        game.assignDukeVidalToAFaction(factionName);
        discordGame.pushGame();
    }
}
