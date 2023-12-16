package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import controller.channels.TurnSummary;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.Faction;
import model.factions.IxFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

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
                                "block-bidding-advantage",
                                "Prevent Ix from seeing the cards up for bid."
                        ),
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
                        ).addOptions(CommandOptions.hmsTerritory),
                        new SubcommandData(
                                "move-hms",
                                "Move Hidden Mobile Stronghold to another territory"
                        ).addOptions(CommandOptions.hmsTerritory),
                        new SubcommandData(
                                "reposition-hms",
                                "Rotate presentation of HMS by 90 degrees"
                        ).addOptions(CommandOptions.clockDirection)
                )
        );

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
            case "move-hms" -> moveHMS(discordGame, game);
            case "reposition-hms" -> rotateHMSGraphic(discordGame, game);
        }
    }

    public static void sendCardBackToDeck(ButtonInteractionEvent event, DiscordGame discordGame, Game game, boolean fromButton, String cardName, String location) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().putBackIxCard(game, cardName, location);
        String message = "You sent " + cardName.trim() + " to the " + location.toLowerCase() + " of the deck.";
        if (fromButton)
            event.getHook().sendMessage(message).queue();
        else {
            discordGame.getIxChat().queueMessage(message);
        }
        discordGame.getTurnSummary().queueMessage(Emojis.IX + " sent a " + Emojis.TREACHERY + " to the " + location.toLowerCase() + " of the deck.");
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

    public static void blockBiddingAdvantage(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        bidding.blockIxBiddingAdvantage(game);
        discordGame.getTurnSummary().queueMessage(
                MessageFormat.format(
                        "{0} have been blocked from using their bidding advantage.\n{1} cards will be pulled from the {2} deck for bidding.",
                        Emojis.IX, bidding.getMarket().size(), Emojis.TREACHERY
                )
        );
        discordGame.pushGame();
    }

    public static void technology(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(ixCard).getAsString();
        TreacheryCard newCard = game.getBidding().ixTechnology(game, cardName);
        discordGame.getIxLedger().queueMessage(
                MessageFormat.format("Received {0} and put {1} back as the next card for bid.",
                        newCard.name(), cardName)
        );
        discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                "{0} used technology to swap a card from their hand for R{1}:C{2}.",
                Emojis.IX, game.getTurn(), game.getBidding().getBidCardNumber() + 1
        ));
        discordGame.getIxChat().queueMessage("You took " + newCard.name() + " instead of " + cardName);
        discordGame.pushGame();
    }

    public static void allyCardSwap(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        TreacheryCard cardToDiscard = bidding.getPreviousCard();
        TreacheryCard newCard = game.getBidding().ixAllyCardSwap(game);
        Faction ixAlly = game.getFaction(game.getFaction("Ix").getAlly());
        discordGame.getFactionLedger(ixAlly).queueMessage(
                MessageFormat.format("Received {0} from the {1} deck and discarded {2} with {3} ally power.",
                        newCard.name(), Emojis.TREACHERY, cardToDiscard.name(), Emojis.IX)
        );
        discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                "{0} as {1} ally discarded {2}and took the {3} deck top card.",
                ixAlly.getEmoji(), Emojis.IX, cardToDiscard.name(), Emojis.TREACHERY
        ));
        TurnSummary turnSummary = discordGame.getTurnSummary();
        if (bidding.isTreacheryDeckReshuffled()) {
            turnSummary.queueMessage(MessageFormat.format(
                    "There were no cards left in the {0} deck. The {0} deck has been replenished from the discard pile.",
                    Emojis.TREACHERY
            ));
        }
        discordGame.pushGame();
    }

    public static Territory placeHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory targetTerritory = game.getTerritories().get(discordGame.required(hmsTerritory).getAsString());
        targetTerritory.getForces().add(new Force("Hidden Mobile Stronghold", 1));
        game.putTerritoryInAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), targetTerritory);
        game.setIxHMSActionRequired(false);
        discordGame.pushGame();
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
        return targetTerritory;
    }

    public static void moveHMS(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territoryWithHMS = game.getTerritories().values().stream().filter(territory -> territory.getForces().stream().anyMatch(force -> force.getName().equals("Hidden Mobile Stronghold"))).findFirst().orElse(null);
        for (Territory territory : game.getTerritories().values()) {
            territory.getForces().removeIf(force -> force.getName().equals("Hidden Mobile Stronghold"));
            game.removeTerritoryFromAnotherTerritory(game.getTerritory("Hidden Mobile Stronghold"), territory);
        }
        Territory targetTerritory = placeHMS(discordGame, game);
        if (territoryWithHMS == targetTerritory)
            discordGame.getTurnSummary().queueMessage(Emojis.IX + " does not move the HMS.");
        else
            discordGame.getTurnSummary().queueMessage(Emojis.IX + " moved the HMS to " + targetTerritory.getTerritoryName() + ".");
    }

    public static void rotateHMSGraphic(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        if (discordGame.optional(clockDirection) != null && discordGame.optional(clockDirection).getAsString().equals("CCW")) {
            game.rotateHMS90degrees();
            game.rotateHMS90degrees();
        }
        game.rotateHMS90degrees();
        if (game.hasGameOption(GameOption.MAP_IN_FRONT_OF_SHIELD))
            game.setUpdated(UpdateType.MAP);
        else
            ShowCommands.showBoard(discordGame, game);
        discordGame.pushGame();
    }

    public static void initialCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        message.append(
                MessageFormat.format(
                        "Select one of the following {0} cards as your starting card. {1}",
                        Emojis.TREACHERY, ixFaction.getPlayer()
                )
        );
        for (TreacheryCard card : ixFaction.getTreacheryHand()) {
            message.append(
                    MessageFormat.format(
                            "\n\t**{0}** _{1}_",
                            card.name(), card.type()
                    ));
        }
        discordGame.getIxChat().queueMessage(message.toString());
        initialCardButtons(discordGame, game);
    }

    public static void initialCardButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (TreacheryCard card : ixFaction.getTreacheryHand()) {
            i++;
            buttons.add(Button.primary("ix-starting-card-" + i + "-" + card.name(), card.name()));
        }
        discordGame.getIxChat().queueMessage("", buttons);
    }

    public static void cardToReject(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        message.append(
                MessageFormat.format(
                        "Turn {0} - Select one of the following {1} cards to send back to the deck. {2}",
                        game.getTurn(), Emojis.TREACHERY, ixFaction.getPlayer()
                )
        );
        for (TreacheryCard card : bidding.getMarket()) {
            message.append(
                    MessageFormat.format("\n\t**{0}** _{1}_",
                            card.name(), card.type()
                    ));
        }
        discordGame.getIxChat().queueMessage(message.toString());
        cardToRejectButtons(discordGame, game);
    }

    public static void cardToRejectButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (TreacheryCard card : bidding.getMarket()) {
            i++;
            buttons.add(Button.primary("ix-card-to-reject-" + game.getTurn() + "-" + i + "-" + card.name(), card.name()));
        }
        discordGame.getIxChat().queueMessage("", buttons);
    }

    public static void sendBackLocationButtons(DiscordGame discordGame, Game game, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("ix-reject-" + game.getTurn() + "-" + cardName + "-top", "Top"));
        buttons.add(Button.primary("ix-reject-" + game.getTurn() + "-" + cardName + "-bottom", "Bottom"));
        buttons.add(Button.secondary("ix-reset-card-selection", "Choose a different card"));
        discordGame.getIxChat().queueMessage("Where do you want to send the " + cardName + "?", buttons);
    }

    public static void confirmCardToSendBack(DiscordGame discordGame, String cardName, String location) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("ix-confirm-reject" + "-" + cardName + "-" + location, "Confirm " + cardName + " to " + location));
        buttons.add(Button.secondary("ix-confirm-reject-reset", "Start over"));
        discordGame.getIxChat().queueMessage("Confirm your selection of " + cardName.trim() + " to " + location + ".", buttons);
    }

    public static void confirmStartingCard(DiscordGame discordGame, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("ix-confirm-start-" + cardName, "Confirm " + cardName));
        buttons.add(Button.secondary("ix-confirm-start-reset", "Choose a different card"));
        discordGame.getIxChat().queueMessage("Confirm your selection of " + cardName.trim() + ".", buttons);
    }

    public static void ixHandSelection(DiscordGame discordGame, Game game, String ixCardName) throws ChannelNotFoundException {
        IxFaction faction = (IxFaction) game.getFaction("Ix");
        List<TreacheryCard> hand = game.getFaction("Ix").getTreacheryHand();
        Collections.shuffle(hand);
        TreacheryCard card = hand.stream().filter(treacheryCard -> treacheryCard.name().equals(ixCardName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        LinkedList<TreacheryCard> ixRejects = new LinkedList<>();
        boolean cardToKeepFound = false;
        for (TreacheryCard treacheryCard : hand) {
            if (!cardToKeepFound && treacheryCard.equals(card)) {
                cardToKeepFound = true;
                continue;
            }
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
