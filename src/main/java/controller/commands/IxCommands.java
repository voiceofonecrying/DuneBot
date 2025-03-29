package controller.commands;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.util.*;

import static controller.commands.CommandOptions.*;

public class IxCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("ix", "Commands related to the Ix Faction.").addSubcommands(
                new SubcommandData("put-card-back", "Send one treachery card to the top or bottom of the deck.").addOptions(CommandOptions.putBackCard, CommandOptions.topOrBottom),
                new SubcommandData("block-bidding-advantage", "Prevent Ix from seeing the cards up for bid."),
                new SubcommandData("technology", "Swap a card in hand for the next card up for bid.").addOptions(CommandOptions.ixCard),
                new SubcommandData("ally-card-swap", "Ix ally can swap card just won for top card from treachery deck."),
                new SubcommandData("place-hms", "Place or move the HMS into a territory.").addOptions(CommandOptions.hmsTerritory),
                new SubcommandData("reposition-hms", "Rotate presentation of HMS by 90 degrees").addOptions(CommandOptions.clockDirection)
        ));
        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "put-card-back" -> sendCardBackToDeck(discordGame, game);
            case "block-bidding-advantage" -> blockBiddingAdvantage(discordGame, game);
            case "technology" -> technology(discordGame, game);
            case "ally-card-swap" -> allyCardSwap(discordGame, game);
            case "place-hms" -> placeHMS(discordGame, game);
            case "reposition-hms" -> rotateHMSGraphic(discordGame, game);
        }
    }

    public static void sendCardBackToDeck(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(putBackCard).getAsString();
        String location = discordGame.required(topOrBottom).getAsString();
        game.getIxFaction().sendCardBack(cardName, location, false);
        discordGame.pushGame();
    }

    public static void blockBiddingAdvantage(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().blockIxBiddingAdvantage(game);
        discordGame.pushGame();
    }

    public static void technology(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(ixCard).getAsString();
        game.getBidding().ixTechnology(game, cardName);
        discordGame.pushGame();
    }

    public static void allyCardSwap(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().ixAllyCardSwap(game);
        discordGame.pushGame();
    }

    public static void placeHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String territoryName = discordGame.required(hmsTerritory).getAsString();
        game.getIxFaction().placeHMS(territoryName);
        discordGame.pushGame();
    }

    public static void rotateHMSGraphic(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (discordGame.optional(clockDirection) != null && discordGame.optional(clockDirection).getAsString().equals("CCW")) {
            game.rotateHMS90degrees();
            game.rotateHMS90degrees();
        }
        game.rotateHMS90degrees();
        discordGame.pushGame();
    }
}
