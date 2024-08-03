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

import static controller.commands.CommandOptions.*;

public class MoritaniCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("moritani", "Commands related to the Moritani Faction.").addSubcommands(
                new SubcommandData("remove-terror-token-from-map", "Remove a Moritani Terror Token from the map").addOptions(moritaniTerrorTokenOnMap, toHand),
                new SubcommandData("trigger-terror-token", "Trigger a Moritani Terror Token").addOptions(faction, moritaniTerrorTokenOnMap),
                new SubcommandData("robbery-draw", "Draw a card with Robbery Terror Token"),
                new SubcommandData("robbery-discard", "Discard after Robbery to get back to hand limit").addOptions(card)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "remove-terror-token-from-map" -> removeTerrorTokenFromMap(discordGame, game);
            case "trigger-terror-token" -> triggerTerrorToken(discordGame, game);
            case "robbery-draw" -> robberyDraw(discordGame, game);
            case "robbery-discard" -> robberyDiscard(discordGame, game);
        }
    }

    public static void removeTerrorTokenFromMap(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String terrorTokenName = discordGame.required(moritaniTerrorTokenOnMap).getAsString();
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
        String terrorTokenName = discordGame.required(moritaniTerrorTokenOnMap).getAsString();
        String triggeringFaction = discordGame.required(CommandOptions.faction).getAsString();
        MoritaniFaction moritaniFaction = (MoritaniFaction) game.getFaction("Moritani");

        Territory territory = game.getTerritories().values().stream()
                .filter(t -> t.hasTerrorToken(terrorTokenName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Terror Token not found on map"));
        moritaniFaction.triggerTerrorToken(game.getFaction(triggeringFaction), territory, terrorTokenName);
        moritaniFaction.getChat().publish("You have triggered your " + terrorTokenName + " token in " + territory.getTerritoryName());
        discordGame.pushGame();
    }

    public static void robberyDraw(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        ((MoritaniFaction) game.getFaction("Moritani")).robberyDraw();
        discordGame.pushGame();
    }

    public static void robberyDiscard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String cardToDiscard = discordGame.required(card).getAsString();

        ((MoritaniFaction) game.getFaction("Moritani")).robberyDiscard(cardToDiscard);
        discordGame.pushGame();
    }
}
