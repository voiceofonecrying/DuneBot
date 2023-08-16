package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
                        ),
                        new SubcommandData(
                                "place-hms",
                                "Place the HMS in a territory."
                        ).addOptions(CommandOptions.territory),
                        new SubcommandData(
                                "move-hms",
                                "Move Hidden Mobile Stronghold to another territory"
                        ).addOptions(CommandOptions.territory)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "put-card-back" -> sendCardBackToDeck(discordGame, game);
            case "technology" -> technology(discordGame, game);
            case "ally-card-swap" -> allyCardSwap(discordGame, game);
            case "place-hms" -> placeHMS(discordGame, game);
            case "move-hms" -> moveHMS(discordGame, game);
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
            IxFaction ixFaction = (IxFaction)game.getFaction("Ix");
            message.append(
                    MessageFormat.format(
                            "{0}\nTurn {1} - Select one of the following {2} cards to send back to the deck.",
                            ixFaction.getPlayer(), game.getTurn(), Emojis.TREACHERY
                    )
            );
            for (TreacheryCard card : bidding.getMarket()) {
                message.append("\n\t**" + card.name() + "** _" + card.type() + "_");
            }
            discordGame.sendMessage("ix-chat", message.toString());
        }
    }
    public static void placeHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory targetTerritory = game.getTerritories().get(discordGame.required(territory).getAsString());
        targetTerritory.getForces().add(new Force("Hidden Mobile Stronghold", 1));
        discordGame.pushGame();
        ShowCommands.showBoard(discordGame, game);
    }

    public static void moveHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        for (Territory territory : game.getTerritories().values()) {
            territory.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
        }
        placeHMS(discordGame, game);
    }
}
