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
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

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
        String message = "You sent " + cardName.trim() + " to the " + location.toLowerCase() + " of the deck.";
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
        discordGame.queueMessage("turn-summary", MessageFormat.format(
                "{0} used technology to swap a card from their hand for R{1}:C{2}.",
                Emojis.IX, game.getTurn(), game.getBidding().getBidCardNumber() + 1
        ));
        discordGame.pushGame();
    }

    public static void allyCardSwap(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().ixAllyCardSwap(game);
        discordGame.queueMessage("turn-summary", MessageFormat.format(
                "{0} ({1} ally) swapped the card just won for the {2} deck top card.",
                game.getFaction(game.getFaction("Ix").getAlly()).getEmoji(), Emojis.IX, Emojis.TREACHERY
        ));
        discordGame.pushGame();
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

    public static void initialCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = (IxFaction)game.getFaction("Ix");
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
        discordGame.queueMessage("ix-chat", message.toString());
        initialCardButtons(discordGame, game);
    }

    public static void initialCardButtons(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        IxFaction ixFaction = (IxFaction)game.getFaction("Ix");
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (TreacheryCard card : ixFaction.getTreacheryHand()) {
            i++;
            buttons.add(Button.primary("ix-starting-card-" + i + "-" + card.name(), card.name()));
        }
        if (buttons.size() > 5) {
            discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("")
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.subList(5, buttons.size())));
        }
        else {
            discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("")
                    .addActionRow(buttons));
        }
    }

    public static void cardToReject(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        StringBuilder message = new StringBuilder();
        IxFaction ixFaction = (IxFaction)game.getFaction("Ix");
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
        discordGame.queueMessage("ix-chat", message.toString());
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
        if (buttons.size() > 5) {
            discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("")
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.subList(5, buttons.size())));
        }
        else {
            discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("")
                    .addActionRow(buttons));
        }
    }

    public static void sendBackLocationButtons(DiscordGame discordGame, Game game, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("ix-reject-" + game.getTurn() + "-" + cardName + "-top", "Top"));
        buttons.add(Button.primary("ix-reject-" + game.getTurn() + "-" + cardName + "-bottom", "Bottom"));
        buttons.add(Button.secondary("ix-reset-card-selection", "Choose a different card"));
        discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("Where do you want to send the " + cardName + "?")
                .addActionRow(buttons));
    }

    public static void confirmCardToSendBack(DiscordGame discordGame, String cardName, String location) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("ix-confirm-reject" + "-" + cardName + "-" + location, "Confirm " + cardName + " to " + location));
        buttons.add(Button.secondary("ix-confirm-reject-reset", "Start over"));
        discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("Confirm your selection of " + cardName.trim() + " to " + location + ".")
                .addActionRow(buttons));
    }

    public static void confirmStartingCard(DiscordGame discordGame, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("ix-confirm-start-" + cardName, "Confirm " + cardName));
        buttons.add(Button.secondary("ix-confirm-start-reset", "Choose a different card"));
        discordGame.queueMessage("ix-chat", new MessageCreateBuilder().addContent("Confirm your selection of " + cardName.trim() + ".")
                .addActionRow(buttons));
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
