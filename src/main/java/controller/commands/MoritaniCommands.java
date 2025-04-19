package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.Game;
import model.Territory;
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
                new SubcommandData("place-terror-token", "Place a Moritani Terror Token on the map").addOptions(moritaniTerrorTokenInSupply, territory),
                new SubcommandData("move-terror-token", "Move a Moritani Terror Token to a different territory").addOptions(moritaniTerrorTokenOnMap, territory),
                new SubcommandData("remove-terror-token-from-map", "Remove a Moritani Terror Token from the map").addOptions(moritaniTerrorTokenOnMap, toPlayer),
                new SubcommandData("trigger-terror-token", "Trigger a Moritani Terror Token").addOptions(faction, moritaniTerrorTokenOnMap),
                new SubcommandData("robbery-draw", "Draw a card with Robbery Terror Token"),
                new SubcommandData("robbery-discard", "Discard after Robbery to get back to hand limit").addOptions(card)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "place-terror-token" -> placeTerrorToken(discordGame, game);
            case "move-terror-token" -> moveTerrorToken(discordGame, game);
            case "remove-terror-token-from-map" -> removeTerrorTokenFromMap(discordGame, game);
            case "trigger-terror-token" -> triggerTerrorToken(discordGame, game);
            case "robbery-draw" -> robberyDraw(discordGame, game);
            case "robbery-discard" -> robberyDiscard(discordGame, game);
        }
    }

    public static void placeTerrorToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String terrorTokenName = discordGame.required(moritaniTerrorTokenInSupply).getAsString();
        String territoryName = discordGame.required(territory).getAsString();
        game.getMoritaniFaction().placeTerrorToken(game.getTerritory(territoryName), terrorTokenName);
        discordGame.pushGame();
    }

    public static void moveTerrorToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String terrorTokenName = discordGame.required(moritaniTerrorTokenOnMap).getAsString();
        String toTerritoryName = discordGame.required(territory).getAsString();
        game.getMoritaniFaction().moveTerrorToken(game.getTerritory(toTerritoryName), terrorTokenName);
        discordGame.pushGame();
    }

    public static void removeTerrorTokenFromMap(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String terrorTokenName = discordGame.required(moritaniTerrorTokenOnMap).getAsString();
        boolean toHand = discordGame.required(CommandOptions.toPlayer).getAsBoolean();
        game.getTerritories().removeTerrorTokenFromMap(game, terrorTokenName, toHand);
        discordGame.pushGame();
    }

    public static void triggerTerrorToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String terrorTokenName = discordGame.required(moritaniTerrorTokenOnMap).getAsString();
        String triggeringFaction = discordGame.required(CommandOptions.faction).getAsString();
        Territory territory = game.getTerritories().getTerritoryWithTerrorToken(terrorTokenName);
        game.getMoritaniFaction().triggerTerrorToken(game.getFaction(triggeringFaction), territory, terrorTokenName);
        discordGame.pushGame();
    }

    public static void robberyDraw(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.getMoritaniFaction().robberyDraw();
        discordGame.pushGame();
    }

    public static void robberyDiscard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String cardToDiscard = discordGame.required(card).getAsString();
        game.getMoritaniFaction().robberyDiscard(cardToDiscard);
        discordGame.pushGame();
    }
}
