package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import enums.UpdateType;
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
                                "Place or move the HMS into a territory."
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
            case "reposition-hms" -> rotateHMSGraphic(discordGame, game);
        }
    }

    public static void sendCardBackToDeck(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(putBackCard).getAsString();
        String location = discordGame.required(topOrBottom).getAsString();
        sendCardBackToDeck(discordGame, game, cardName, location, false);
    }

    public static void sendCardBackToDeck(DiscordGame discordGame, Game game, String cardName, String location, boolean requestTechnology) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().putBackIxCard(game, cardName, location, requestTechnology);
        String message = "You sent " + cardName.trim() + " to the " + location.toLowerCase() + " of the deck.";
        game.getFaction("Ix").getChat().reply(message);
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
        ((IxFaction) game.getFaction("Ix")).placeHMS(territoryName);
        discordGame.pushGame();
    }

    public static void rotateHMSGraphic(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        if (discordGame.optional(clockDirection) != null && discordGame.optional(clockDirection).getAsString().equals("CCW")) {
            game.rotateHMS90degrees();
            game.rotateHMS90degrees();
        }
        game.rotateHMS90degrees();
        game.setUpdated(UpdateType.MAP);
        discordGame.pushGame();
    }

    public static void initialCard(Game game) {
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
        ixFaction.getChat().publish(message.toString());
        initialCardButtons(game);
    }

    public static void initialCardButtons(Game game) {
        IxFaction ixFaction = (IxFaction) game.getFaction("Ix");
        List<DuneChoice> choices = new ArrayList<>();
        int i = 0;
        for (TreacheryCard card : ixFaction.getTreacheryHand()) {
            i++;
            choices.add(new DuneChoice("ix-starting-card-" + i + "-" + card.name(), card.name()));
        }
        ixFaction.getChat().reply("", choices);
    }

    public static void sendBackLocationButtons(Game game, String cardName) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("ix-reject-" + game.getTurn() + "-" + cardName + "-top", "Top"));
        choices.add(new DuneChoice("ix-reject-" + game.getTurn() + "-" + cardName + "-bottom", "Bottom"));
        choices.add(new DuneChoice("secondary", "ix-reset-card-selection", "Choose a different card"));
        game.getFaction("Ix").getChat().reply("Where do you want to send the " + cardName + "?", choices);
    }

    public static void confirmCardToSendBack(Game game, String cardName, String location) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("success", "ix-confirm-reject-" + cardName + "-" + location, "Confirm " + cardName + " to " + location));
        choices.add(new DuneChoice("ix-confirm-reject-technology-" + cardName + "-" + location, "Confirm and use Technology on first card"));
        choices.add(new DuneChoice("secondary", "ix-confirm-reject-reset", "Start over"));
        game.getFaction("Ix").getChat().reply("Confirm your selection of " + cardName.trim() + " to " + location + ".", choices);
    }

    public static void confirmStartingCard(Game game, String cardName) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("ix-confirm-start-" + cardName, "Confirm " + cardName));
        choices.add(new DuneChoice("secondary", "ix-confirm-start-reset", "Choose a different card"));
        game.getFaction("Ix").getChat().reply("Confirm your selection of " + cardName.trim() + ".", choices);
    }

    public static void ixHandSelection(Game game, String ixCardName) {
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
    }
}
