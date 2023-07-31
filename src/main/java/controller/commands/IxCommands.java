package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static controller.commands.CommandOptions.*;

public class IxCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("ix", "Commands related to the Ix Faction.").addSubcommands(
                        new SubcommandData(
                                "put-card-back",
                                "Send one treachery card to the top or bottom of the deck."
                        ).addOptions(CommandOptions.putBackCard, CommandOptions.topOrBottom),
                        new SubcommandData(
                                "technology",
                                "Swap a card in hand for the next card up for bid."
                        ).addOptions(CommandOptions.ixCard),
                        new SubcommandData(
                                "ally-card-swap",
                                "Ix ally can swap card just won for top card from treachery deck."
                        )
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "put-card-back" -> sendCardBackToDeck(discordGame, game);
            case "technology" -> technology(discordGame, game);
            case "ally-card-swap" -> allyCardSwap(discordGame, game);
        }
    }

    public static void sendCardBackToDeck(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(putBackCard).getAsString();
        String location = discordGame.required(topOrBottom).getAsString();

        if (game.hasFaction("Ix")) {
            game.getBidding().putBackIxCard(game, cardName, location);
            discordGame.sendMessage("ix-chat", "You sent " + cardName + " to the " + location.toLowerCase() + " of the deck.");
            discordGame.sendMessage("turn-summary", Emojis.IX + " sent a " + Emojis.TREACHERY + " to the " + location.toLowerCase() + " of the deck.");
            discordGame.pushGame();
        }
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

    public static void sendIxBiddingMarket(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        StringBuilder message = new StringBuilder();
        if (game.hasFaction("Ix")) {
            message.append(
                    MessageFormat.format(
                            "Turn {0} - Select one of the following {1} cards to send back to the deck.",
                            game.getTurn(), Emojis.TREACHERY
                    )
            );
            for (TreacheryCard card : bidding.getMarket()) {
                message.append("\n\t**" + card.name() + "** _" + card.type() + "_");
            }
            discordGame.sendMessage("ix-chat", message.toString());
        }
    }
}
