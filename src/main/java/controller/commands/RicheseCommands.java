package controller.commands;

import constants.Emojis;
import controller.DiscordGame;
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

import java.text.MessageFormat;
import java.util.*;

import static controller.commands.CommandOptions.*;

public class RicheseCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("richese", "Commands related to the Richese Faction.").addSubcommands(
                        new SubcommandData(
                                "place-no-fields-token",
                                "Place a No-Fields token on the map."
                        ).addOptions(richeseNoFields, CommandOptions.territory),
                        new SubcommandData("reveal-no-field", "Replace the No Field with Richese forces and move it to front of shield").addOptions(ecazAllyNoField),
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
            case "card-bid" -> cardBid(discordGame, game);
            case "black-market-bid" -> blackMarketBid(discordGame, game);
            case "remove-card" -> removeRicheseCard(discordGame, game);
            case "karama-buy" -> karamaBuy(discordGame, game);
            case "karama-block-cache-card" -> karamaBlockCacheCard(discordGame, game);
            case "place-no-fields-token" -> placeNoFieldToken(discordGame, game);
            case "reveal-no-field" -> revealNoField(discordGame, game);
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
        faction.discard("Karama", "and paid 3 spice to take a " + Emojis.RICHESE + " cache card.");
        TreacheryCard cacheCard = faction.removeTreacheryCardFromCache(faction.getTreacheryCardFromCache(cardName));
        faction.addTreacheryCard(cacheCard);
        faction.subtractSpice(3, "one time Karama ability to purchase a " + faction.getEmoji() + " cache card.");
        faction.setSpecialKaramaPowerUsed(true);
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
        karamaFaction.discard("Karama", "to block " + Emojis.RICHESE + " from selling their cache card");
        discordGame.getModInfo().queueMessage(MessageFormat.format(
                "The Karama has been discarded from {0} hand.",
                karamaFaction.getEmoji()
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

    public static void revealNoField(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        RicheseFaction richese = (RicheseFaction) game.getFaction("Richese");
        Faction factionToReveal = richese;
        if (discordGame.optional(ecazAllyNoField) != null && discordGame.required(ecazAllyNoField).getAsBoolean())
            factionToReveal = game.getFaction(richese.getAlly());
        richese.revealNoField(game, factionToReveal);
        discordGame.pushGame();
    }
}
