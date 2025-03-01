package controller.buttons;

import constants.Emojis;
import controller.commands.RicheseCommands;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.DuneChoice;
import model.Game;
import model.factions.RicheseFaction;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RicheseButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "richese"
        // And any button that begins with "richese" must be handled by this class
        if (event.getComponentId().equals("richese-reveal-no-field")) revealNoField(discordGame, game);
        else if (event.getComponentId().startsWith("richese-run-black-market-")) runBlackMarket(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-black-market-method-")) blackMarketMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-black-market-")) confirmBlackMarket(event, discordGame, game);
        else if (event.getComponentId().equals("richese-cache-sell-last")) cacheCardLast(discordGame, game);
        else if (event.getComponentId().startsWith("richese-cache-card-method-")) cacheCardMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-cache-card-confirm-")) confirmCacheCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-cache-card-")) cacheCard(event, discordGame, game);
    }

    private static void revealNoField(DiscordGame discordGame, Game game) {
        ((RicheseFaction) game.getFaction("Richese")).revealNoField(game);
        discordGame.queueMessage("You have revealed your No-Field.");
        discordGame.queueDeleteMessage();
    }

    private static void runBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        if (cardName.equals("skip")) {
            discordGame.queueMessage("No black market this turn");
            game.getBidding().setBlackMarketDecisionInProgress(false);
            RunCommands.advance(discordGame, game);
        } else
            blackMarketMethod(game, cardName);
        discordGame.queueDeleteMessage();
    }

    public static void blackMarketMethod(Game game, String cardName) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "Normal", "Normal"));
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "OnceAroundCCW", "OnceAroundCCW"));
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "OnceAroundCW", "OnceAroundCW"));
        choices.add(new DuneChoice("richese-black-market-method-" + cardName + "-" + "Silent", "Silent"));
        choices.add(new DuneChoice("secondary", "richese-black-market-method-reselect", "Start over"));
        game.getFaction("Richese").getChat().reply("How would you like to sell " + cardName + "?", choices);
    }

    private static void blackMarketMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            game.getBidding().askBlackMarket(game);
        } else {
            String method = event.getComponentId().split("-")[5];
            confirmBlackMarket(game, cardName, method);
        }
        discordGame.queueDeleteMessage();
    }

    public static void confirmBlackMarket(Game game, String cardName, String method) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("success", "richese-black-market-" + cardName + "-" + method, "Confirm " + cardName + " by " + method + " auction."));
        choices.add(new DuneChoice("secondary", "richese-black-market-reselect", "Start over"));
        game.getFaction("Richese").getChat().reply("", choices);
    }

    private static void confirmBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            game.getBidding().askBlackMarket(game);
        } else {
            String method = event.getComponentId().split("-")[4];
            discordGame.queueMessage("Selling " + cardName + " by " + method + " auction.");
            RicheseCommands.blackMarketBid(discordGame, game, cardName, method);
            if (method.equals("Silent")) {
                discordGame.getModInfo().queueMessage("Players should use the bot to enter their bids for the silent auction.");
            }
        }
    }

    private static void cacheCardLast(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You will sell last.");
        discordGame.getModInfo().queueMessage(Emojis.RICHESE + " will be given buttons when it is time for the last card.");
        game.getBidding().setCacheCardDecisionInProgress(false);
        RunCommands.bidding(discordGame, game);
    }

    private static void cacheCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) {
        discordGame.queueDeleteMessage();
        String cardName = event.getComponentId().split("-")[3];
        cacheCardMethod(game, cardName);
    }

    public static void cacheCardMethod(Game game, String cardName) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("richese-cache-card-method-" + cardName + "-" + "OnceAroundCCW", "OnceAroundCCW"));
        choices.add(new DuneChoice("richese-cache-card-method-" + cardName + "-" + "OnceAroundCW", "OnceAroundCW"));
        choices.add(new DuneChoice("richese-cache-card-method-" + cardName + "-" + "Silent", "Silent"));
        choices.add(new DuneChoice("secondary", "richese-cache-card-method-reselect", "Start over"));
        game.getFaction("Richese").getChat().reply("How would you like to sell " + cardName + "?", choices);
    }

    private static void cacheCardMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            game.getBidding().presentCacheCardChoices(game);
        } else {
            String method = event.getComponentId().split("-")[5];
            confirmCacheCard(game, cardName, method);
        }
    }

    public static void confirmCacheCard(Game game, String cardName, String method) {
        List<DuneChoice> choices = new ArrayList<>();
        choices.add(new DuneChoice("success", "richese-cache-card-confirm-" + cardName + "-" + method, "Confirm " + cardName + " by " + method + " auction."));
        choices.add(new DuneChoice("secondary", "richese-cache-card-confirm-reselect", "Start over"));
        game.getFaction("Richese").getChat().reply("", choices);
    }

    private static void confirmCacheCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            game.getBidding().presentCacheCardChoices(game);
        } else {
            String method = event.getComponentId().split("-")[5];
            discordGame.queueMessage("Selling " + cardName + " by " + method + " auction.");
            RicheseCommands.cardBid(discordGame, game, cardName, method);
            if (method.equals("Silent")) {
                discordGame.getModInfo().queueMessage("Players should use the bot to enter their bids for the silent auction.");
            }
        }
    }
}
