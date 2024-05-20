package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.Faction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.text.MessageFormat;
import java.util.*;
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
                                .addOptions(richeseCard),
                        new SubcommandData("karama-block-cache-card", "Prevent Richese from auctioning a card on the black market")
                                .addOptions(karamaFaction)
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
            case "karama-block-cache-card" -> karamaBlockCacheCard(discordGame, game);
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
                CommandManager.placeForceInTerritory(discordGame, game, territory, game.getFaction("Richese"), noField, false);
                discordGame.getTurnSummary().queueMessage("The no-field in " + territory.getTerritoryName() + " has opened with " + noField + " " + Emojis.getForceEmoji("Richese") + "!");
            }
            territory.setRicheseNoField(null);
        }
        if (noField != -1) {
            RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
            richese.setFrontOfShieldNoField(noField);
        }
    }

    public static void cardBid(DiscordGame discordGame, Game game, String cardName, String bidType) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (!bidding.isRicheseCacheCardOutstanding()) {
            if (bidding.getBidCardNumber() != 0 && bidding.getBidCardNumber() == bidding.getNumCardsForBid()) {
                throw new InvalidGameStateException("All cards for this round have already been bid on.");
            } else {
                throw new InvalidGameStateException(Emojis.RICHESE + " card is not eligible to be sold.");
            }
        }

        RicheseFaction faction = (RicheseFaction) game.getFaction("Richese");
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

    public static void cardBid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(richeseCard).getAsString();
        String bidType = discordGame.required(richeseBidType).getAsString();
        cardBid(discordGame, game, cardName, bidType);
    }

    public static void blackMarketBid(DiscordGame discordGame, Game game, String cardName, String bidType) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() != null) {
            throw new InvalidGameStateException("There is already a card up for bid.");
        } else if (bidding.getBidCardNumber() != 0) {
            throw new InvalidGameStateException("Black market card must be first in the bidding round.");
        }

        Faction faction = game.getFaction("Richese");
        List<TreacheryCard> cards = faction.getTreacheryHand();

        TreacheryCard card = cards.stream()
                .filter(c -> c.name().equalsIgnoreCase(cardName))
                .findFirst()
                .orElseThrow();

        cards.remove(card);
        bidding.setBlackMarketCard(true);
        bidding.setBidCard(card);
        bidding.incrementBidCardNumber();

        if (bidType.equalsIgnoreCase("Normal")) {
            bidding.updateBidOrder(game);
            List<String> bidOrder = bidding.getEligibleBidOrder(game);
            for (Faction f : game.getFactions()) {
                f.setMaxBid(0);
                f.setAutoBid(false);
                f.setBid("");
            }
            Faction factionBeforeFirstToBid = game.getFaction(bidOrder.getLast());
            bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
            RunCommands.createBidMessage(discordGame, game);
            bidding.advanceBidder(game);
        } else {
            runRicheseBid(discordGame, game, bidType, true);
        }

        AtreidesCommands.sendAtreidesCardPrescience(discordGame, game, card);

        discordGame.pushGame();
    }

    public static void blackMarketBid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(richeseBlackMarketCard).getAsString();
        String bidType = discordGame.required(richeseBlackMarketBidType).getAsString();
        blackMarketBid(discordGame, game, cardName, bidType);
    }

    public static void removeRicheseCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        if (bidding.getBidCard() == null) {
            throw new InvalidGameStateException("There is no card up for bid.");
        } else if (!bidding.isRicheseCacheCard()) {
            throw new InvalidGameStateException("The card up for bid did not come from the Richese cache.");
        }
        discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                "{0} {1} has been removed from the game.",
                Emojis.RICHESE, bidding.getBidCard().name()));
        bidding.clearBidCardInfo(null);
        discordGame.pushGame();
    }

    public static void karamaBuy(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(richeseCard).getAsString();
        RicheseFaction faction = (RicheseFaction) game.getFaction("Richese");
        TreacheryCard karama;
        try {
            karama = faction.removeTreacheryCard("Karama ");
            game.getTreacheryDiscard().add(karama);
        } catch (Exception e) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have a Karama.");
        }
        if (faction.isSpecialKaramaPowerUsed()) {
            throw new InvalidGameStateException(Emojis.RICHESE + " has already used their special Karama power.");
        } else if (faction.getSpice() < 3) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have 3 spice for this action.");
        }
        TreacheryCard cacheCard = faction.removeTreacheryCardFromCache(faction.getTreacheryCardFromCache(cardName));
        faction.addTreacheryCard(cacheCard);
        faction.subtractSpice(3);
        discordGame.getTurnSummary().queueMessage(faction.getEmoji() + " played Karama and paid 3 spice to take a " + faction.getEmoji() + " cache card.");
        discordGame.pushGame();
    }

    public static void karamaBlockCacheCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String karamaFactionName = discordGame.required(karamaFaction).getAsString();
        Faction karamaFaction = game.getFaction(karamaFactionName);
        Bidding bidding = game.getBidding();
        int bidCardNumber = bidding.getBidCardNumber();
        if (bidCardNumber > 1 || bidCardNumber == 1 && !bidding.isBlackMarketCard()) {
            throw new InvalidGameStateException("It is too late to Karama the " + Emojis.RICHESE + " card.");
        }
        TreacheryCard karama;
        try {
            karama = karamaFaction.removeTreacheryCard("Karama ");
        } catch (Exception e) {
            throw new InvalidGameStateException(karamaFaction.getEmoji() + " does not have a Karama.");
        }
        game.getTreacheryDiscard().add(karama);
        discordGame.getModInfo().queueMessage(MessageFormat.format(
                "The Karama has been discarded from {0} hand.",
                karamaFaction.getEmoji()
        ));
        discordGame.getTurnSummary().queueMessage(MessageFormat.format(
                "{0} played Karama to block the {1} from selling their cache card.",
                karamaFaction.getEmoji(), Emojis.RICHESE
        ));
        bidding.setRicheseCacheCardOutstanding(false);
        discordGame.pushGame();
    }

    public static void placeNoFieldToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Integer noField = discordGame.required(richeseNoFields).getAsInt();
        String territoryName = discordGame.required(territory).getAsString();

        Territory territory = game.getTerritories().get(territoryName);
        territory.setRicheseNoField(noField);
        game.setUpdated(UpdateType.MAP);

        discordGame.pushGame();
    }

    public static void removeNoFieldToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        Optional<Territory> territory = game.getTerritories().values().stream()
                .filter(Territory::hasRicheseNoField)
                .findFirst();

        territory.ifPresent(value -> value.setRicheseNoField(null));
        game.setUpdated(UpdateType.MAP);

        discordGame.pushGame();
    }

    public static void runRicheseBid(DiscordGame discordGame, Game game, String bidType, boolean blackMarket) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        for (Faction faction : game.getFactions()) {
            faction.setMaxBid(0);
            faction.setAutoBid(false);
            faction.setBid("");
        }
        if (bidType.equalsIgnoreCase("Silent")) {
            bidding.setSilentAuction(true);
            if (blackMarket) {
                discordGame.queueMessage("bidding-phase",
                        MessageFormat.format(
                                "{0} We will now silently auction a card from {1} hand on the black market! Please use the bot to place your bid.",
                                game.getGameRoleMention(), Emojis.RICHESE
                        )
                );
            } else {
                discordGame.queueMessage("bidding-phase",
                        MessageFormat.format(
                                "{0} We will now silently auction a brand new Richese {1} {2} {1}!  Please use the bot to place your bid.",
                                game.getGameRoleMention(), Emojis.TREACHERY, bidding.getBidCard().name()
                        )
                );
            }
            List<Faction> factions = game.getFactions();
            for (Faction faction : factions) {
                if (faction.getHandLimit() > faction.getTreacheryHand().size()) {
                    discordGame.getFactionChat(faction.getName()).queueMessage(
                            MessageFormat.format(
                                    "{0} Use the bot to place your bid for the silent auction. Your bid will be the exact amount you set.",
                                    faction.getPlayer()
                            )
                    );
                }
            }
            int firstBid = Math.ceilDiv(game.getStorm(), 3) % factions.size();
            List<Faction> bidOrderFactions = new ArrayList<>();
            bidOrderFactions.addAll(factions.subList(firstBid, factions.size()));
            bidOrderFactions.addAll(factions.subList(0, firstBid));
            List<String> bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
            bidding.setRicheseBidOrder(game, bidOrder);
            List<String> filteredBidOrder = bidding.getEligibleBidOrder(game);
            Faction factionBeforeFirstToBid = game.getFaction(filteredBidOrder.getLast());
            bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
        } else {
            StringBuilder message = new StringBuilder();
            if (blackMarket) {
                message.append(
                        MessageFormat.format("{0} You may now place your bids for a black market card from {1} hand!\n",
                                game.getGameRoleMention(), Emojis.RICHESE
                        )
                );
            } else {
                message.append(
                        MessageFormat.format("{0} You may now place your bids for a shiny, brand new {1} {2}!\n",
                                game.getGameRoleMention(), Emojis.RICHESE, bidding.getBidCard().name()
                        )
                );
            }

            List<Faction> factions = game.getFactions();
            List<Faction> bidOrderFactions = new ArrayList<>();
            List<Faction> factionsInBidDirection;
            if (bidType.equalsIgnoreCase("OnceAroundCW")) {
                factionsInBidDirection = new ArrayList<>(factions);
                Collections.reverse(factionsInBidDirection);
            } else {
                factionsInBidDirection = factions;
            }

            int richeseIndex = factionsInBidDirection.indexOf(game.getFaction("Richese"));
            bidOrderFactions.addAll(factionsInBidDirection.subList(richeseIndex + 1, factions.size()));
            bidOrderFactions.addAll(factionsInBidDirection.subList(0, richeseIndex + 1));
            List<String> bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
            bidding.setRicheseBidOrder(game, bidOrder);
            List<String> filteredBidOrder = bidding.getEligibleBidOrder(game);
            Faction factionBeforeFirstToBid = game.getFaction(filteredBidOrder.getLast());
            bidding.setCurrentBidder(factionBeforeFirstToBid.getName());
            discordGame.queueMessage("bidding-phase", message.toString());
            RunCommands.createBidMessage(discordGame, game);
            bidding.advanceBidder(game);
        }
    }

    public static void askBlackMarket(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();
        RicheseFaction richeseFaction = (RicheseFaction) game.getFaction("Richese");
        message.append(
                MessageFormat.format(
                        "Turn {0} - Select a {1} card to sell on the black market. {2}",
                        game.getTurn(), Emojis.TREACHERY, richeseFaction.getPlayer()
                )
        );
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for (TreacheryCard card : richeseFaction.getTreacheryHand()) {
            i++;
            buttons.add(Button.primary("richeserunblackmarket-" + card.name() + "-" + i, card.name()));
        }
        buttons.add(Button.danger("richeserunblackmarket-skip", "No black market"));
        discordGame.getRicheseChat().queueMessage(message.toString(), buttons);
    }

    public static void blackMarketMethod(DiscordGame discordGame, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("richeseblackmarketmethod-" + cardName + "-" + "Normal", "Normal"));
        buttons.add(Button.primary("richeseblackmarketmethod-" + cardName + "-" + "OnceAroundCCW", "OnceAroundCCW"));
        buttons.add(Button.primary("richeseblackmarketmethod-" + cardName + "-" + "OnceAroundCW", "OnceAroundCW"));
        buttons.add(Button.primary("richeseblackmarketmethod-" + cardName + "-" + "Silent", "Silent"));
        buttons.add(Button.secondary("richeseblackmarketmethod-reselect", "Start over"));
        discordGame.getRicheseChat().queueMessage("How would you like to sell " + cardName.trim() + "?", buttons);
    }

    public static void confirmBlackMarket(DiscordGame discordGame, String cardName, String method) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("richeseblackmarket-" + cardName + "-" + method, "Confirm " + cardName + " by " + method + " auction."));
        buttons.add(Button.secondary("richeseblackmarket-reselect", "Start over"));
        discordGame.getRicheseChat().queueMessage("", buttons);
    }

    public static void cacheCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        Bidding bidding = game.getBidding();
        RicheseFaction richeseFaction = (RicheseFaction) game.getFaction("Richese");
        List<Button> buttons = new LinkedList<>();
        String message;
        for (TreacheryCard card : richeseFaction.getTreacheryCardCache()) {
            buttons.add(Button.primary("richesecachecard-" + card.name(), card.name()));
        }
        if (bidding.getBidCardNumber() < bidding.getNumCardsForBid() - 1) {
            message = "Please select your cache card to sell or choose to sell last. " + richeseFaction.getPlayer();
            buttons.add(Button.danger("richesecachetime-last", "Sell cache card last"));
        } else {
            message = "Please select your cache card to sell. You must sell now. " + richeseFaction.getPlayer();
        }
        if (!richeseFaction.isHomeworldOccupied()) discordGame.getRicheseChat().queueMessage(message, buttons);
        else {
            discordGame.getFactionChat(richeseFaction.getOccupier().getName()).queueMessage(message, buttons);
            discordGame.getFactionChat(richeseFaction.getOccupier().getName()).queueMessage("(You are getting these buttons because you occupy " + Emojis.RICHESE + " homeworld)");
        }
    }

    public static void confirmLast(DiscordGame discordGame) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("richesecachelast-confirm", "Confirm you wish to sell your card last."));
        buttons.add(Button.secondary("richesecachecardmethod-reselect", "Start over"));
        discordGame.getRicheseChat().queueMessage("", buttons);
    }

    public static void cacheCardMethod(DiscordGame discordGame, String cardName) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.primary("richesecachecardmethod-" + cardName + "-" + "OnceAroundCCW", "OnceAroundCCW"));
        buttons.add(Button.primary("richesecachecardmethod-" + cardName + "-" + "OnceAroundCW", "OnceAroundCW"));
        buttons.add(Button.primary("richesecachecardmethod-" + cardName + "-" + "Silent", "Silent"));
        buttons.add(Button.secondary("richesecachecardmethod-reselect", "Start over"));
        discordGame.getRicheseChat().queueMessage("How would you like to sell " + cardName.trim() + "?", buttons);
    }

    public static void confirmCacheCard(DiscordGame discordGame, String cardName, String method) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        buttons.add(Button.success("richesecachecardconfirm-" + cardName + "-" + method, "Confirm " + cardName + " by " + method + " auction."));
        buttons.add(Button.secondary("richesecachecardconfirm-reselect", "Start over"));
        discordGame.getRicheseChat().queueMessage("", buttons);
    }
}
