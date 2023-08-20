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
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.Collections;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
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

    public static void sendCardBackToDeck(ButtonInteractionEvent event, DiscordGame discordGame, Game game, boolean fromButton, String cardName, String location) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().putBackIxCard(game, cardName, location);
        String message = "You sent " + cardName + " to the " + location.toLowerCase() + " of the deck.";
        if (fromButton)
            event.getHook().sendMessage(message).queue();
        else
            discordGame.queueMessage("ix-chat", message);
        discordGame.queueMessage("turn-summary", Emojis.IX + " sent a " + Emojis.TREACHERY + " to the " + location.toLowerCase() + " of the deck.");
        discordGame.pushGame();
    }

    public static void sendCardBackToDeck(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(putBackCard).getAsString();
        String location = discordGame.required(topOrBottom).getAsString();
        sendCardBackToDeck(null, discordGame, game, false, cardName, location);
    }

    public static void sendCardBackToDeck(ButtonInteractionEvent event, DiscordGame discordGame, Game game, String cardName, String location) throws ChannelNotFoundException, InvalidGameStateException {
        sendCardBackToDeck(event, discordGame, game, true, cardName, location);
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
            discordGame.queueMessage("ix-chat", message.toString());
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

    public static void initialCardButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = (IxFaction)game.getFaction("Ix");
        message.append(
                MessageFormat.format(
                        "Select one of the following {0} cards as your starting card. {1}",
                        Emojis.TREACHERY, ixFaction.getPlayer()
                )
        );
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (TreacheryCard card : ixFaction.getTreacheryHand()) {
            i++;
            buttons.add(Button.primary("ix-starting-card-" + i + "-" + card.name(), card.name()));
        }
        if (buttons.size() > 5) {
            discordGame.prepareMessage("ix-chat", message.toString())
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.get(5)).queue();
        }
        else {
            discordGame.prepareMessage("ix-chat", message.toString())
                    .addActionRow(buttons).queue();
        }
    }

    public static void cardToRejectButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = (IxFaction)game.getFaction("Ix");
        message.append(
                MessageFormat.format(
                        "Turn {0} - Select one of the following {1} cards to send back to the deck. {2}",
                        game.getTurn(), Emojis.TREACHERY, ixFaction.getPlayer()
                )
        );
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (TreacheryCard card : bidding.getMarket()) {
            i++;
            buttons.add(Button.primary("ix-card-to-reject-" + game.getTurn() + "-" + i + "-" + card.name(), card.name()));
        }
        if (buttons.size() > 5) {
            discordGame.prepareMessage("ix-chat", message.toString())
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.get(5)).queue();
        }
        else {
            discordGame.prepareMessage("ix-chat", message.toString())
                    .addActionRow(buttons).queue();
        }
    }

    public static void sendBackLocationButtons(DiscordGame discordGame, Game game, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("ix-reject-" + game.getTurn() + "-" + cardName + "-top", "Top"));
        buttons.add(Button.primary("ix-reject-" + game.getTurn() + "-" + cardName + "-bottom", "Bottom"));
        buttons.add(Button.secondary("ix-reset-card-selection", "Choose a different card"));
        discordGame.prepareMessage("ix-chat", "Where do you want to send the " + cardName + "?")
                .addActionRow(buttons).queue();
    }

    public static void confirmStartingCard(DiscordGame discordGame, Game game, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("ix-confirm-" + cardName, "Confirm " + cardName));
        buttons.add(Button.secondary("ix-confirm-reset", "Choose a different card"));
        discordGame.prepareMessage("ix-chat", "Confirm your selection of " + cardName + ".")
                .addActionRow(buttons).queue();
    }

    public static void ixHandSelection(DiscordGame discordGame, Game game, String ixCardName) throws ChannelNotFoundException {
        IxFaction faction = (IxFaction)game.getFaction("Ix");
        List<TreacheryCard> hand = game.getFaction("Ix").getTreacheryHand();
        Collections.shuffle(hand);
        TreacheryCard card = hand.stream().filter(treacheryCard -> treacheryCard.name().equals(ixCardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        LinkedList<TreacheryCard> ixRejects = new LinkedList<>();
        for (TreacheryCard treacheryCard : hand) {
            if (treacheryCard.equals(card)) continue;
            ixRejects.add(treacheryCard);
        }
        Collections.shuffle(ixRejects);
        for (TreacheryCard treacheryCard : ixRejects) {
            game.getTreacheryDeck().add(treacheryCard);
        }
        faction.getTreacheryHand().clear();
        faction.setHandLimit(4);
        faction.addTreacheryCard(card);

        discordGame.pushGame();
    }
}
