package controller.buttons;

import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import controller.DiscordGame;
import model.Game;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.IOException;

public class RicheseButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        // Buttons handled by this class must begin with "richese"
        // And any button that begins with "richese" must be handled by this class
        if (event.getComponentId().equals("richese-reveal-no-field")) revealNoField(discordGame, game);
        else if (event.getComponentId().startsWith("richese-run-black-market-")) runBlackMarket(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-black-market-method-")) blackMarketMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-black-market-")) confirmBlackMarket(event, discordGame, game);
        else if (event.getComponentId().equals("richese-cache-sell-first-occupier")) cacheCardFirstByOccupier(discordGame, game);
        else if (event.getComponentId().equals("richese-cache-sell-last")) cacheCardLast(discordGame, game);
        else if (event.getComponentId().startsWith("richese-cache-card-method-")) cacheCardMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-cache-card-confirm-")) confirmCacheCard(event, discordGame, game);
        else if (event.getComponentId().startsWith("richese-cache-card-")) cacheCard(event, discordGame, game);
    }

    private static void revealNoField(DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        game.getRicheseFaction().revealNoField(game);
        discordGame.queueMessage("You have revealed your No-Field.");
        discordGame.queueDeleteMessage();
        discordGame.pushGame();
    }

    private static void runBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        if (cardName.equals("skip")) {
            discordGame.queueMessage("No black market this turn");
            game.getBidding().setBlackMarketDecisionInProgress(false);
            RunCommands.advance(discordGame, game);
        } else
            game.getRicheseFaction().presentBlackMarketMethodChoices(cardName);
        discordGame.queueDeleteMessage();
    }

    private static void blackMarketMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            game.getBidding().askBlackMarket(game);
        } else {
            String method = event.getComponentId().split("-")[5];
            game.getRicheseFaction().presentConfirmBlackMarketChoices(cardName, method);
        }
        discordGame.queueDeleteMessage();
    }

    private static void confirmBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[3];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            game.getBidding().askBlackMarket(game);
        } else {
            String method = event.getComponentId().split("-")[4];
            game.getBidding().blackMarketAuction(game, cardName, method);
            discordGame.pushGame();
        }
    }

    private static void cacheCardFirstByOccupier(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        game.getBidding().richeseCardFirstByOccupier(game);
        discordGame.pushGame();
    }

    private static void cacheCardLast(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        game.getBidding().richeseCardLast(game);
        RunCommands.bidding(discordGame, game);
    }

    private static void cacheCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        discordGame.queueDeleteMessage();
        String cardName = event.getComponentId().split("-")[3];
        game.getBidding().presentCacheCardMethodChoices(game, cardName);
    }

    private static void cacheCardMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            game.getBidding().presentCacheCardChoices(game);
        } else {
            String method = event.getComponentId().split("-")[5];
            game.getBidding().presentCacheCardConfirmChoices(game, cardName, method);
        }
    }

    private static void confirmCacheCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[4];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            game.getBidding().presentCacheCardChoices(game);
        } else {
            String method = event.getComponentId().split("-")[5];
            game.getBidding().richeseCardAuction(game, cardName, method);
            discordGame.pushGame();
        }
    }
}
