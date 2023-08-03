package controller.commands;

import constants.Emojis;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.Faction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static controller.commands.CommandOptions.*;

public class RicheseCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("richese", "Commands related to the Richese Faction.").addSubcommands(
                        new SubcommandData(
                                "no-fields-to-front-of-shield",
                                "Move the Richese No-Fields token to the Front of Shield."
                        ).addOptions(richeseNoFields),
                        new SubcommandData(
                                "place-no-fields-token",
                                "Place a No-Fields token on the map."
                        ).addOptions(richeseNoFields, CommandOptions.territory),
                        new SubcommandData(
                                "remove-no-field",
                                "Remove the No-Field token from the board"
                        ),
                        new SubcommandData("card-bid", "Start bidding on a Richese card")
                                .addOptions(richeseCard, richeseBidType),
                        new SubcommandData("black-market-bid", "Start bidding on a black market card")
                                .addOptions(CommandOptions.richeseBlackMarketCard, CommandOptions.richeseBlackMarketBidType),
                        new SubcommandData("remove-card", "Remove the Richese card from the game"),
                        new SubcommandData("karama-buy", "Richese can take any card from cache for 3 spice.")
                                .addOptions(richeseCard)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "no-fields-to-front-of-shield" -> moveNoFieldsToFrontOfShield(discordGame, game);
            case "card-bid" -> cardBid(discordGame, game);
            case "black-market-bid" -> blackMarketBid(discordGame, game);
            case "remove-card" -> removeRicheseCard(discordGame, game);
            case "karama-buy" -> karamaBuy(discordGame, game);
            case "place-no-fields-token" -> placeNoFieldToken(discordGame, game);
            case "remove-no-field" -> removeNoFieldToken(discordGame, game);
        }
    }

    public static void moveNoFieldsToFrontOfShield(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int noFieldValue = discordGame.required(richeseNoFields).getAsInt();

        if (game.hasFaction("Richese")) {
            RicheseFaction faction = (RicheseFaction) game.getFaction("Richese");

            faction.setFrontOfShieldNoField(noFieldValue);

            discordGame.pushGame();
        }
    }

    public static void moveNoFieldFromBoardToFrontOfShield(Game game, DiscordGame discordGame) throws ChannelNotFoundException {
        int noField = -1;
        for (Territory territory : game.getTerritories().values()) {
            noField = territory.getRicheseNoField() == null ? noField : territory.getRicheseNoField();
            if (territory.getRicheseNoField() != null) {
                CommandManager.placeForceInTerritory(territory, game.getFaction("Richese"), noField, false);
                discordGame.sendMessage("turn-summary", "The no-field in " + territory.getTerritoryName() + " has opened with " + noField + " " + Emojis.getForceEmoji("Richese") + "!");
            }
            territory.setRicheseNoField(null);
        }
        if (noField != -1) {
            RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
            richese.setFrontOfShieldNoField(noField);
        }
    }

    public static void cardBid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (bidding.getBidCardNumber() != 0 && bidding.getBidCardNumber() == bidding.getNumCardsForBid()) {
            throw new InvalidGameStateException("All cards for this round have already been bid on.");
        } else if (!bidding.isRicheseCacheCardOutstanding()) {
            throw new InvalidGameStateException(Emojis.RICHESE + " card is not eligible to be sold.");
        }
        String cardName = discordGame.required(richeseCard).getAsString();
        String bidType = discordGame.required(richeseBidType).getAsString();

        RicheseFaction faction = (RicheseFaction)game.getFaction("Richese");

        bidding.setRicheseCacheCard(true);
        bidding.setBidCard(
                faction.removeTreacheryCardFromCache(
                        faction.getTreacheryCardFromCache(cardName)
                )
        );

        bidding.incrementBidCardNumber();

        runRicheseBid(discordGame, game, bidType, false);

        discordGame.pushGame();
    }

    public static void blackMarketBid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (bidding.getBidCardNumber() != 0) {
            throw new InvalidGameStateException("Black market card must be first in the bidding round.");
        }
        String cardName = discordGame.required(richeseBlackMarketCard).getAsString();
        String bidType = discordGame.required(richeseBlackMarketBidType).getAsString();

        Faction faction = game.getFaction("Richese");
        List<TreacheryCard> cards = faction.getTreacheryHand();

        TreacheryCard card = cards.stream()
                .filter(c -> c.name().equalsIgnoreCase(cardName))
                .findFirst()
                .orElseThrow();

        cards.remove(card);
        bidding.setBidCard(card);
        bidding.incrementBidCardNumber();

        if (bidType.equalsIgnoreCase("Normal")) {
            RunCommands.updateBidOrder(game);
            List<String> bidOrder = game.getEligibleBidOrder();
            Faction factionBeforeFirstToBid = game.getFaction(bidOrder.get(bidOrder.size() - 1 ));
            bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
            RunCommands.createBidMessage(discordGame, game, bidOrder, factionBeforeFirstToBid);
        } else {
            runRicheseBid(discordGame, game, bidType, true);
        }

        AtreidesCommands.sendAtreidesCardPrescience(discordGame, game, card);

        discordGame.pushGame();
    }

    public static void removeRicheseCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() == null) {
            throw new InvalidGameStateException("There is no card up for bid.");
        } else if (!bidding.isRicheseCacheCard()) {
            throw new InvalidGameStateException("The card up for bid did not come from the Richese cache.");
        }
        discordGame.sendMessage("turn-summary", MessageFormat.format(
                    "{0} {1} has been removed from the game.",
                    Emojis.RICHESE, bidding.getBidCard().name()));
        bidding.clearBidCardInfo(null);
        discordGame.pushGame();
    }

    public static void karamaBuy(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(richeseCard).getAsString();
        RicheseFaction faction = (RicheseFaction)game.getFaction("Richese");
        TreacheryCard karama;
        try {
            karama = faction.removeTreacheryCard("Karama ");
        } catch (Exception e) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have a Karama.");
        }
        if (faction.isSpecialKaramaPowerUsed()) {
            throw new InvalidGameStateException(Emojis.RICHESE + " has already used their special Karama power.");
        } else if (faction.getSpice() < 3) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have 3 spice for this action.");
        }
        game.getTreacheryDiscard().add(karama);
        TreacheryCard cacheCard = faction.removeTreacheryCardFromCache(faction.getTreacheryCardFromCache(cardName));
        faction.addTreacheryCard(cacheCard);
        faction.subtractSpice(3);
        discordGame.sendMessage("turn-summary", faction.getEmoji() + " played Karama and paid 3 spice to take a " + faction.getEmoji() + " cache card.");
        discordGame.pushGame();
    }

    public static void placeNoFieldToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Integer noField = discordGame.required(richeseNoFields).getAsInt();
        String territoryName = discordGame.required(territory).getAsString();

        Territory territory = game.getTerritories().get(territoryName);
        territory.setRicheseNoField(noField);

        discordGame.pushGame();
    }

    public static void removeNoFieldToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Optional<Territory> territory = game.getTerritories().values().stream()
                .filter(Territory::hasRicheseNoField)
                .findFirst();

        territory.ifPresent(value -> value.setRicheseNoField(null));

        discordGame.pushGame();
    }

    public static void runRicheseBid(DiscordGame discordGame, Game game, String bidType, boolean blackMarket) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidType.equalsIgnoreCase("Silent")) {
            if (blackMarket) {
                discordGame.sendMessage("bidding-phase", "We will now silently auction a card from Richese's " +
                        "hand on the black market! Please place your bid in your private channels.");
            } else {
                discordGame.sendMessage("bidding-phase",
                        MessageFormat.format(
                                "We will now silently auction a brand new Richese {0}!  Please place your bid in your private channels.",
                                bidding.getBidCard().name()
                        )
                );
            }
        } else {
            StringBuilder message = new StringBuilder();
            if (blackMarket) {
                message.append("We are now bidding on a card from Richese's hand on the black market!\n");
            } else {
                message.append(
                        MessageFormat.format("We are now bidding on a shiny, brand new Richese {0}!\n",
                                bidding.getBidCard().name()
                        )
                );
            }

            List<Faction> factions = game.getFactions();

            List<Faction> bidOrder = new ArrayList<>();

            List<Faction> factionsInBidDirection;

            if (bidType.equalsIgnoreCase("OnceAroundCW")) {
                factionsInBidDirection = new ArrayList<>(factions);
                Collections.reverse(factionsInBidDirection);
            } else {
                factionsInBidDirection = factions;
            }

            int richeseIndex = factionsInBidDirection.indexOf(game.getFaction("Richese"));
            bidOrder.addAll(factionsInBidDirection.subList(richeseIndex + 1, factions.size()));
            bidOrder.addAll(factionsInBidDirection.subList(0, richeseIndex + 1));

            List<Faction> filteredBidOrder = bidOrder.stream()
                    .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                    .toList();

            message.append(
                    MessageFormat.format(
                            "R{0}:C{1} (Once Around)\n{2} - {3}\n",
                            game.getTurn(), bidding.getBidCardNumber(),
                            filteredBidOrder.get(0).getEmoji(), filteredBidOrder.get(0).getPlayer()
                    )
            );

            message.append(
                    filteredBidOrder.subList(1, filteredBidOrder.size()).stream()
                            .map(f -> f.getEmoji() + " - \n")
                            .collect(Collectors.joining())
            );

            discordGame.sendMessage("bidding-phase", message.toString());
        }
    }
}
