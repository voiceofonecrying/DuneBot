package controller.buttons;

import constants.Emojis;
import exceptions.InvalidGameStateException;
import controller.commands.RicheseCommands;
import controller.commands.RunCommands;
import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import java.io.IOException;

public class RicheseButtons implements Pressable {

    public static void press(ButtonInteractionEvent event, Game game, DiscordGame discordGame) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        if (event.getComponentId().startsWith("richeserunblackmarket-")) runBlackMarket(event, discordGame, game);
        else if (event.getComponentId().startsWith("richeseblackmarketmethod-")) blackMarketMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richeseblackmarket-")) confirmBlackMarket(event, discordGame, game);
        else if (event.getComponentId().startsWith("richesecachetime-")) cacheCardTime(event, discordGame);
        else if (event.getComponentId().equals("richesecachelast-confirm")) confirmLast(discordGame, game);
        else if (event.getComponentId().startsWith("richesecachecard-")) cacheCard(event, discordGame);
        else if (event.getComponentId().startsWith("richesecachecardmethod-")) cacheCardMethod(event, discordGame, game);
        else if (event.getComponentId().startsWith("richesecachecardconfirm-")) confirmCacheCard(event, discordGame, game);
    }

    private static void runBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[1];
        if (cardName.equals("skip")) {
            discordGame.queueMessage("No black market this turn");
            RunCommands.advance(discordGame, game);
        } else {
            discordGame.queueMessage("You selected " + cardName.trim() + ".");
            RicheseCommands.blackMarketMethod(discordGame, cardName);
        }
        discordGame.queueDeleteMessage();
    }

    private static void blackMarketMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException{
        String cardName = event.getComponentId().split("-")[1];
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            RicheseCommands.askBlackMarket(discordGame, game);
        } else {
            String method = event.getComponentId().split("-")[2];
            discordGame.queueMessage("You selected " + method + " auction.");
            RicheseCommands.confirmBlackMarket(discordGame, cardName, method);
        }
        discordGame.queueDeleteMessage();
    }

    private static void confirmBlackMarket(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[1];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            RicheseCommands.askBlackMarket(discordGame, game);
        } else {
            String method = event.getComponentId().split("-")[2];
            discordGame.queueMessage("You are selling " + cardName.trim() + " by " + method + " auction.");
            RicheseCommands.blackMarketBid(discordGame, game, cardName, method);
            if (method.equals("Silent")) {
                discordGame.queueMessage("mod-info", "Players should use the bot to enter their bids for the silent auction.");
            }
        }
    }

    private static void cacheCardTime(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException{
        String time = event.getComponentId().split("-")[1];
        discordGame.queueDeleteMessage();
        if (time.equals("last")) {
            discordGame.queueMessage("You selected last.");
            RicheseCommands.confirmLast(discordGame);
        }
    }

    private static void confirmLast(DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        discordGame.queueDeleteMessage();
        discordGame.queueMessage("You will sell last.");
        discordGame.queueMessage("mod-info", Emojis.RICHESE + "will be given buttons when it is time for the last card.");
        RunCommands.bidding(discordGame, game);
    }

    private static void cacheCard(ButtonInteractionEvent event, DiscordGame discordGame) throws ChannelNotFoundException{
        discordGame.queueDeleteMessage();
        String cardName = event.getComponentId().split("-")[1];
        discordGame.queueMessage("You selected " + cardName.trim() + ".");
        RicheseCommands.cacheCardMethod(discordGame, cardName);
    }

    private static void cacheCardMethod(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[1];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            RicheseCommands.cacheCard(discordGame, game);
        } else {
            String method = event.getComponentId().split("-")[2];
            discordGame.queueMessage("You selected " + method);
            RicheseCommands.confirmCacheCard(discordGame, cardName, method);
        }
    }

    private static void confirmCacheCard(ButtonInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, InvalidGameStateException {
        String cardName = event.getComponentId().split("-")[1];
        discordGame.queueDeleteMessage();
        if (cardName.equals("reselect")) {
            discordGame.queueMessage("Starting over");
            RicheseCommands.cacheCard(discordGame, game);
        } else {
            String method = event.getComponentId().split("-")[2];
            discordGame.queueMessage("You are selling " + cardName.trim() + " by " + method + " auction.");
            RicheseCommands.cardBid(discordGame, game, cardName, method);
            if (method.equals("Silent")) {
                discordGame.queueMessage("mod-info", "Players should use the bot to enter their bids for the silent auction.");
            }
        }
    }
}
