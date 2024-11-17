package controller.commands;

import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import model.Game;
import model.Territory;
import model.factions.EcazFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

public class EcazCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(
                Commands.slash("ecaz", "Commands related to the Ecaz Faction.").addSubcommands(
                        new SubcommandData(
                                "remove-ambassador-from-map",
                                "Remove an Ecaz Ambassador token from the map"
                        ).addOptions(
                                CommandOptions.ecazAmbassadorsOnMap,
                                CommandOptions.toPlayer
                        )
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        if (game.hasFaction("Ecaz") && name.equals("remove-ambassador-from-map")) {
            removeAmbassadorFromMap(discordGame, game);
        } else {
            throw new IllegalArgumentException("Invalid command");
        }
    }

    private static void removeAmbassadorFromMap(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String ambassadorName = discordGame.required(CommandOptions.ecazAmbassadorsOnMap).getAsString();
        boolean toHand = discordGame.required(CommandOptions.toPlayer).getAsBoolean();
        EcazFaction ecazFaction = (EcazFaction) game.getFaction("Ecaz");

        Territory territory = game.getTerritories().values().stream()
                .filter(t -> t.getEcazAmbassador() != null && t.getEcazAmbassador().equals(ambassadorName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Ambassador not found on map"));

        territory.removeEcazAmbassador();
        game.setUpdated(UpdateType.MAP);

        if (toHand) ecazFaction.addAmbassadorToSupply(ambassadorName);
        else ecazFaction.addToAmbassadorPool(ambassadorName);

        discordGame.pushGame();
    }
}
