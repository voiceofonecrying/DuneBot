package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import model.factions.EcazFaction;
import model.factions.Faction;
import model.factions.MoritaniFaction;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.text.MessageFormat;
import java.util.*;

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

    public static void cardBid(DiscordGame discordGame, Game game, String cardName, String bidType) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().richeseCardAuction(game, cardName, bidType);
        discordGame.pushGame();
    }

    public static void cardBid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(richeseCard).getAsString();
        String bidType = discordGame.required(richeseBidType).getAsString();
        cardBid(discordGame, game, cardName, bidType);
    }

    public static void blackMarketBid(DiscordGame discordGame, Game game, String cardName, String bidType) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().blackMarketAuction(game, cardName, bidType);
        discordGame.pushGame();
    }

    public static void blackMarketBid(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = discordGame.required(richeseBlackMarketCard).getAsString();
        String bidType = discordGame.required(richeseBlackMarketBidType).getAsString();
        blackMarketBid(discordGame, game, cardName, bidType);
    }

    public static void removeRicheseCard(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        game.getBidding().removeRicheseCacheCardFromGame(game);
        discordGame.pushGame();
    }

    public static void karamaBuy(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        RicheseFaction faction = (RicheseFaction) game.getFaction("Richese");
        if (faction.isSpecialKaramaPowerUsed()) {
            throw new InvalidGameStateException(Emojis.RICHESE + " has already used their special Karama power.");
        } else if (faction.getSpice() < 3) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have 3 spice for this action.");
        }
        String cardName = discordGame.required(richeseCard).getAsString();
        TreacheryCard karama;
        try {
            karama = faction.removeTreacheryCard("Karama");
            game.getTreacheryDiscard().add(karama);
        } catch (Exception e) {
            throw new InvalidGameStateException(faction.getEmoji() + " does not have a Karama.");
        }
        TreacheryCard cacheCard = faction.removeTreacheryCardFromCache(faction.getTreacheryCardFromCache(cardName));
        faction.addTreacheryCard(cacheCard);
        faction.subtractSpice(3, "one time Karama ability to purchase a " + faction.getEmoji() + " cache card.");
        faction.setSpecialKaramaPowerUsed(true);
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
            karama = karamaFaction.removeTreacheryCard("Karama");
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

    public static void placeNoFieldToken(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        int noField = discordGame.required(richeseNoFields).getAsInt();
        String territoryName = discordGame.required(territory).getAsString();

        Territory territory = game.getTerritories().get(territoryName);
        RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
        richese.shipNoField(richese, territory, noField, false, false, 0);
        if (game.hasFaction("Ecaz"))
            ((EcazFaction) game.getFaction("Ecaz")).checkForAmbassadorTrigger(territory, richese);
        if (game.hasFaction("Moritani"))
            ((MoritaniFaction)game.getFaction("Moritani")).checkForTerrorTrigger(territory, richese, 1);

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
